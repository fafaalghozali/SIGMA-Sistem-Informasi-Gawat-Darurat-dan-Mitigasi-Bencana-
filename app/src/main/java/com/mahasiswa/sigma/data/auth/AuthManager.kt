package com.mahasiswa.sigma.data.auth

import com.mahasiswa.sigma.data.model.UserRole
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.exception.AuthRestException
import io.github.jan.supabase.auth.exception.AuthSessionMissingException
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class ProfileDto(
    val id: String,
    @SerialName("full_name") val fullName: String,
    val role: String
)

@Singleton
class AuthManager @Inject constructor(
    private val supabase: SupabaseClient
) {

    suspend fun registerUser(email: String, pass: String, role: UserRole, name: String): Result<Unit> {
        return try {
            // Step 1: Register user with Supabase Auth
            supabase.auth.signUpWith(Email) {
                this.email = email
                this.password = pass
            }

            // Step 2: Get the newly created user's ID
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Registrasi berhasil tetapi gagal mendapatkan ID pengguna."))

            // Step 3: Insert profile data into the profiles table
            supabase.from("profiles").insert(
                mapOf(
                    "id" to userId,
                    "full_name" to name,
                    "role" to role.name
                )
            )

            Result.success(Unit)
        } catch (e: RestException) {
            val message = when {
                e.message?.contains("already registered", ignoreCase = true) == true ||
                e.message?.contains("already been registered", ignoreCase = true) == true ||
                e.message?.contains("duplicate", ignoreCase = true) == true ||
                e.message?.contains("unique", ignoreCase = true) == true ->
                    "Email sudah terdaftar. Gunakan email lain atau login dengan email tersebut."
                else -> "Registrasi gagal: ${e.message}"
            }
            Result.failure(Exception(message))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun loginUser(email: String, pass: String): Result<UserRole> {
        return try {
            // Step 1: Authenticate with Supabase Auth
            supabase.auth.signInWith(Email) {
                this.email = email
                this.password = pass
            }

            // Step 2: Get the authenticated user's ID
            val userId = supabase.auth.currentUserOrNull()?.id
                ?: return Result.failure(Exception("Login berhasil tetapi gagal mendapatkan ID pengguna."))

            // Step 3: Query profiles table to get role and full_name
            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()

            Result.success(UserRole.fromString(profile.role))
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

    suspend fun restoreSession(): UserRole? {
        return try {
            val session = supabase.auth.currentSessionOrNull() ?: return null

            val userId = session.user?.id ?: return null

            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()

            UserRole.fromString(profile.role)
        } catch (e: AuthSessionMissingException) {
            // Session expired or missing — user must log in again
            null
        } catch (e: HttpRequestTimeoutException) {
            // Network unavailable — treat as no session to avoid crash
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
            val profile = supabase.from("profiles")
                .select {
                    filter {
                        eq("id", userId)
                    }
                }
                .decodeSingle<ProfileDto>()

            profile.fullName
        } catch (e: HttpRequestTimeoutException) {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Returns true if there is a valid active session.
     * Used to determine whether the user needs to be redirected to the login screen.
     * Requirements: 9.3, 9.4
     */
    fun isSessionValid(): Boolean {
        return supabase.auth.currentSessionOrNull() != null
    }

    suspend fun updateProfile(newName: String, newEmail: String): Result<Unit> {
        return try {
            val userId = getCurrentUserId()
                ?: return Result.failure(Exception("Tidak ada pengguna yang sedang login."))

            // Update full_name in profiles table
            supabase.from("profiles").update(
                mapOf("full_name" to newName)
            ) {
                filter {
                    eq("id", userId)
                }
            }

            // Update email in Supabase Auth if provided
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
}
