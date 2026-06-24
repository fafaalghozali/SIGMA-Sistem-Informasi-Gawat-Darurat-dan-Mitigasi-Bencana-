package com.mahasiswa.sigma.data.auth

import android.graphics.Bitmap
import com.mahasiswa.sigma.data.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.mindrot.jbcrypt.BCrypt
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthManager @Inject constructor(
    private val supabase: SupabaseClient
) {

    suspend fun registerUser(email: String, pass: String, role: UserRole, name: String): Result<Unit> {
        return try {

            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
                this.data = buildJsonObject {
                    put("full_name", name)

                    put("role", role.displayName)
                }
            }

            val userId = supabase.auth.currentSessionOrNull()?.user?.id
            if (userId != null) {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val now = LocalDateTime.now().format(formatter)
                val rememberToken = (1..40)
                    .map { "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".random() }
                    .joinToString("")
                val hashedPassword = withContext(Dispatchers.Default) {
                    BCrypt.hashpw(pass, BCrypt.gensalt(12))
                }

                runCatching {
                    supabase.from("profiles").upsert(
                        buildJsonObject {
                            put("id", userId)
                            put("full_name", name)
                            put("role", role.displayName)
                            put("email", email)
                            put("password", hashedPassword)
                            put("remember_token", rememberToken)
                            put("created_at", now)
                            put("updated_at", now)
                        }
                    )
                }
            }

            Result.success(Unit)
        } catch (e: AuthRestException) {
            val message = when {
                e.message?.contains("already registered", ignoreCase = true) == true ||
                e.message?.contains("already been registered", ignoreCase = true) == true ||
                e.message?.contains("User already registered", ignoreCase = true) == true ->
                    "Email sudah terdaftar. Gunakan email lain atau login dengan email tersebut."
                e.message?.contains("Password should be", ignoreCase = true) == true ->
                    "Password terlalu lemah. Gunakan minimal 6 karakter."
                e.message?.contains("Unable to validate email", ignoreCase = true) == true ->
                    "Format email tidak valid. Periksa kembali alamat email Anda."
                e.message?.contains("signup_disabled", ignoreCase = true) == true ->
                    "Pendaftaran akun sementara dinonaktifkan. Hubungi administrator."
                else -> "Registrasi gagal: ${e.message ?: "Terjadi kesalahan pada server autentikasi."}"
            }
            Result.failure(Exception(message))
        } catch (e: RestException) {
            val message = when {
                e.message?.contains("already registered", ignoreCase = true) == true ||
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true ->
                    "Email sudah terdaftar. Gunakan email lain atau login dengan email tersebut."
                e.message?.contains("violates", ignoreCase = true) == true ||
                e.message?.contains("constraint", ignoreCase = true) == true ->
                    "Gagal menyimpan data profil. Pastikan semua data yang diisi valid."
                else -> "Registrasi gagal: ${e.message ?: "Terjadi kesalahan pada server."}"
            }
            Result.failure(Exception(message))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Koneksi timeout. Periksa jaringan internet Anda dan coba lagi."))
        } catch (e: Exception) {
            val message = when {
                e.message?.contains("Unable to resolve host", ignoreCase = true) == true ->
                    "Tidak dapat terhubung ke server. Periksa koneksi internet Anda."
                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Koneksi timeout. Coba lagi beberapa saat."
                e.message.isNullOrBlank() || e.message == "Unknown Error" ->
                    "Terjadi kesalahan tidak dikenal. Pastikan koneksi internet aktif dan coba lagi."
                else -> "Registrasi gagal: ${e.message}"
            }
            Result.failure(Exception(message))
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<UserRole> {
        return try {
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }

            val session = supabase.auth.currentSessionOrNull()
            val userId = session?.user?.id
                ?: return Result.failure(Exception("Login berhasil tetapi gagal mendapatkan ID pengguna."))

            // Ambil role dari tabel profiles
            var roleStr = "Masyarakat"
            val profilesById = supabase.from("profiles")
                .select { filter { eq("id", userId) }; limit(1) }
                .decodeList<JsonObject>()

            if (profilesById.isNotEmpty()) {
                roleStr = profilesById.first()["role"]?.jsonPrimitive?.contentOrNull ?: "Masyarakat"
            } else {
                val profilesByEmail = supabase.from("profiles")
                    .select { filter { eq("email", email) }; limit(1) }
                    .decodeList<JsonObject>()
                if (profilesByEmail.isNotEmpty()) {
                    roleStr = profilesByEmail.first()["role"]?.jsonPrimitive?.contentOrNull ?: "Masyarakat"
                } else {
                    return Result.failure(Exception("Profil pengguna tidak ditemukan di database."))
                }
            }

            // Jika masih Masyarakat, cek tabel volunteers —
            // kalau ada record APPROVED maka upgrade ke Relawan
            val resolvedRole = if (UserRole.fromString(roleStr) == UserRole.MASYARAKAT) {
                try {
                    val volunteers = supabase.from("volunteers")
                        .select { filter { eq("user_id", userId) }; limit(1) }
                        .decodeList<JsonObject>()
                    val volunteerStatus = volunteers.firstOrNull()
                        ?.get("status")?.jsonPrimitive?.contentOrNull ?: ""
                    if (volunteerStatus.equals("APPROVED", ignoreCase = true)) {
                        // Sinkronkan role di profiles agar konsisten
                        runCatching {
                            supabase.from("profiles").update(
                                buildJsonObject { put("role", "Relawan") }
                            ) { filter { eq("id", userId) } }
                        }
                        UserRole.RELAWAN
                    } else {
                        UserRole.fromString(roleStr)
                    }
                } catch (_: Exception) {
                    UserRole.fromString(roleStr)
                }
            } else {
                UserRole.fromString(roleStr)
            }

            Result.success(resolvedRole)
        } catch (e: AuthRestException) {
            val message = when {
                e.message?.contains("Invalid login credentials", ignoreCase = true) == true ->
                    "Email atau password salah. Periksa kembali kredensial Anda."
                e.message?.contains("Email not confirmed", ignoreCase = true) == true ->
                    "Email belum diverifikasi. Silakan cek kotak masuk email Anda."
                e.message?.contains("too many requests", ignoreCase = true) == true ->
                    "Terlalu banyak percobaan login. Coba lagi beberapa saat kemudian."
                else -> "Login gagal: ${e.message}"
            }
            Result.failure(Exception(message))
        } catch (e: RestException) {
            Result.failure(Exception("Gagal mengambil data profil: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update kolom role di tabel profiles berdasarkan userId.
     * Dipanggil saat admin approve volunteer.
     */
    suspend fun updateUserRole(userId: String, newRole: UserRole): Result<Unit> {
        return try {
            supabase.from("profiles").update(
                buildJsonObject { put("role", newRole.displayName) }
            ) {
                filter { eq("id", userId) }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal update role: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun restoreSession(): UserRole? {
        return try {
            val session = supabase.auth.currentSessionOrNull() ?: return null

            val userId = session.user?.id ?: return null

            val profiles = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<JsonObject>()

            val profile = profiles.firstOrNull() ?: return null
            val roleStr = profile["role"]?.jsonPrimitive?.contentOrNull ?: "MASYARAKAT"
            UserRole.fromString(roleStr)
        } catch (e: AuthSessionMissingException) {

            null
        } catch (e: HttpRequestTimeoutException) {

            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun logout() {
        supabase.auth.signOut()
    }

    fun getCurrentUserId(): String? {
        return supabase.auth.currentUserOrNull()?.id
    }

    suspend fun getUserName(): String {
        val userId = getCurrentUserId()
            ?: return ""

        return try {
            val profiles = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeList<JsonObject>()

            profiles.firstOrNull()
                ?.get("full_name")
                ?.jsonPrimitive
                ?.contentOrNull
                ?: ""
        } catch (e: HttpRequestTimeoutException) {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    fun isSessionValid(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun updateProfile(newName: String, newEmail: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Tidak ada pengguna yang sedang login."))

            val updates = buildJsonObject {
                put("full_name", newName)
                if (newEmail.isNotBlank()) {
                    put("email", newEmail)
                }
            }

            supabase.from("profiles").update(updates) {
                filter {
                    eq("id", userId)
                }
            }

            if (newEmail.isNotBlank()) {
                supabase.auth.updateUser {
                    email = newEmail
                }
            }

            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal memperbarui profil: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Upload bitmap ke Supabase Storage bucket "Picture Profile"
     * dan kembalikan public URL-nya.
     */
    suspend fun uploadProfilePhoto(bitmap: Bitmap): Result<String> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Tidak ada pengguna yang sedang login."))

            // Kompres bitmap ke JPEG bytes
            val bytes = withContext(Dispatchers.Default) {
                ByteArrayOutputStream().use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                    stream.toByteArray()
                }
            }

            // Path file di bucket: avatars/<userId>.jpg
            val filePath = "avatars/$userId.jpg"

            // Upload ke bucket "Picture Profile" dengan upsert agar overwrite jika sudah ada
            supabase.storage.from("Picture Profile").upload(
                path = filePath,
                data = bytes,
                options = {
                    upsert = true
                    contentType = io.ktor.http.ContentType.Image.JPEG
                }
            )

            // Ambil public URL
            val publicUrl = supabase.storage.from("Picture Profile").publicUrl(filePath)

            Result.success(publicUrl)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal mengupload foto: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal mengupload foto: ${e.message}"))
        }
    }

    /**
     * Simpan photo_url ke kolom photo_url di tabel profiles.
     */
    suspend fun updateProfilePhotoUrl(photoUrl: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Tidak ada pengguna yang sedang login."))

            supabase.from("profiles").update(
                buildJsonObject { put("photo_url", photoUrl) }
            ) {
                filter {
                    eq("id", userId)
                }
            }

            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal menyimpan URL foto: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(Exception("Gagal menyimpan URL foto: ${e.message}"))
        }
    }

    /**
     * Ambil photo_url milik user yang sedang login dari tabel profiles.
     */
    suspend fun getProfilePhotoUrl(): String? {
        val userId = getCurrentUserId() ?: return null
        return try {
            val profiles = supabase.from("profiles")
                .select {
                    filter { eq("id", userId) }
                    limit(1)
                }
                .decodeList<JsonObject>()

            profiles.firstOrNull()
                ?.get("photo_url")
                ?.jsonPrimitive
                ?.contentOrNull
        } catch (e: Exception) {
            null
        }
    }
}
