package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.data.model.VolunteerRegistrationData
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class VolunteerDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val name: String,
    val skill: String,
    val address: String,
    @SerialName("phone_number") val phoneNumber: String,
    val availability: String? = null,
    val status: String = "pending",
    val assignment: String? = null,
    @SerialName("assignment_status") val assignmentStatus: String? = null,
    @SerialName("disaster_id") val disasterId: String? = null
)

@Serializable
data class VolunteerReportDto(
    val id: String? = null,
    @SerialName("volunteer_id") val volunteerId: String? = null,
    @SerialName("disaster_id") val disasterId: String? = null,
    @SerialName("skill_type") val skillType: String? = null,
    @SerialName("report_data") val reportData: String? = null,
    val notes: String? = null,
    @SerialName("photo_urls") val photoUrls: List<String>? = null
)

@Singleton
class VolunteerRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authManager: AuthManager
) {

    companion object {
        private const val TAG = "VolunteerRepository"
    }

    suspend fun saveRegistration(userId: String, data: VolunteerRegistrationData): Result<Unit> {
        return try {
            val resolvedUserId = authManager.getCurrentUserId() ?: userId
            val dto = VolunteerDto(
                userId = resolvedUserId,
                name = data.name,
                skill = data.skill.name,
                address = data.address,
                phoneNumber = data.phoneNumber,
                status = "pending"
            )
            supabase.from("volunteers").upsert(dto) {
                filter {
                    eq("user_id", resolvedUserId)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Log.e(TAG, "saveRegistration RestException: ${e.message}")
            Result.failure(Exception("Gagal mendaftar sebagai relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Log.e(TAG, "saveRegistration error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getRegistration(userId: String): VolunteerRegistrationData? {
        return try {
            val resolvedUserId = authManager.getCurrentUserId() ?: userId
            val dto = supabase.from("volunteers")
                .select {
                    filter {
                        eq("user_id", resolvedUserId)
                    }
                }
                .decodeSingleOrNull<VolunteerDto>()
                ?: return null

            VolunteerRegistrationData(
                name = dto.name,
                skill = try { SkillsVolunteer.valueOf(dto.skill.uppercase()) } catch (_: Exception) { SkillsVolunteer.MEDIS },
                address = dto.address,
                phoneNumber = dto.phoneNumber,
                status = dto.status
            )
        } catch (e: HttpRequestTimeoutException) {
            Log.w(TAG, "getRegistration timeout")
            null
        } catch (e: Exception) {
            Log.w(TAG, "getRegistration error: ${e.message}")
            null
        }
    }

    suspend fun getAllRegistrations(): Result<List<VolunteerDto>> {
        return try {
            val list = supabase.from("volunteers")
                .select()
                .decodeList<VolunteerDto>()
            Result.success(list)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal mengambil data relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVolunteerStatus(volunteerId: String, newStatus: String): Result<Unit> {
        return try {

            val resolvedUserId = authManager.getCurrentUserId()
            supabase.from("volunteers").update(
                mapOf("status" to newStatus)
            ) {
                filter {

                    eq("user_id", resolvedUserId ?: volunteerId)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal memperbarui status relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateVolunteerStatusById(id: String, newStatus: String): Result<Unit> {
        return try {
            supabase.from("volunteers").update(
                mapOf("status" to newStatus)
            ) {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal memperbarui status relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveVolunteerReport(report: VolunteerReportDto): Result<Unit> {
        return try {
            supabase.from("volunteer_reports").insert(report)
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal mengirim laporan relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun clearRegistration(userId: String) {
        try {
            val resolvedUserId = authManager.getCurrentUserId() ?: userId
            supabase.from("volunteers").delete {
                filter {
                    eq("user_id", resolvedUserId)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "clearRegistration error: ${e.message}")
        }
    }
}
