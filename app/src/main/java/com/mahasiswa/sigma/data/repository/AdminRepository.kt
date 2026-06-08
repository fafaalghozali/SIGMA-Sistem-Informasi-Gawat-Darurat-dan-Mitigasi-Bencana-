package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class ShelterDto(
    val id: String? = null,
    val name: String,
    val address: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("capacity_max") val capacityMax: Int = 0,
    @SerialName("capacity_current") val capacityCurrent: Int = 0,
    val status: String = "active"
)

@Singleton
class AdminRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authManager: AuthManager
) {

    companion object {
        private const val TAG = "AdminRepository"
    }

    suspend fun getPendingReports(): List<LocalDisasterReport> {
        return try {
            supabase.from("disasters")
                .select {
                    filter {
                        eq("status", "pending")
                    }
                }
                .decodeList<DisasterDto>()
                .map { it.toDomainModel() }
        } catch (e: RestException) {
            Log.e(TAG, "getPendingReports RestException: ${e.message}")
            emptyList()
        } catch (e: HttpRequestTimeoutException) {
            Log.w(TAG, "getPendingReports timeout")
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "getPendingReports error: ${e.message}")
            emptyList()
        }
    }

    suspend fun verifyReport(reportId: String, newStatus: String = "siaga_1"): Result<Unit> {
        return updateDisasterStatus(reportId, newStatus)
    }

    suspend fun rejectReport(reportId: String): Result<Unit> {
        return updateDisasterStatus(reportId, "decline")
    }

    private suspend fun updateDisasterStatus(reportId: String, status: String): Result<Unit> {
        return try {
            val verifiedBy = authManager.getCurrentUserId()
            supabase.from("disasters").update(
                buildMap {
                    put("status", status)
                    if (verifiedBy != null) put("verified_by", verifiedBy)
                }
            ) {
                filter {
                    eq("id", reportId)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            if (e.message?.contains("403") == true) {
                Result.failure(Exception("Anda tidak memiliki akses untuk melakukan tindakan ini."))
            } else {
                Result.failure(Exception("Gagal memperbarui status laporan: ${e.message}"))
            }
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllVolunteers(): Result<List<VolunteerDto>> {
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

    suspend fun updateVolunteerStatus(id: String, status: String): Result<Unit> {
        return try {
            supabase.from("volunteers").update(
                mapOf("status" to status)
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

    suspend fun assignVolunteer(
        id: String,
        assignment: String,
        disasterId: String
    ): Result<Unit> {
        return try {
            supabase.from("volunteers").update(
                mapOf(
                    "assignment" to assignment,
                    "assignment_status" to "assigned",
                    "disaster_id" to disasterId
                )
            ) {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal menugaskan relawan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun createShelter(shelter: ShelterDto): Result<Unit> {
        return try {
            supabase.from("shelters").insert(shelter)
            Result.success(Unit)
        } catch (e: RestException) {
            shelterError(e)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateShelter(shelter: ShelterDto): Result<Unit> {
        val id = shelter.id ?: return Result.failure(Exception("ID shelter tidak boleh kosong."))
        return try {
            supabase.from("shelters").update(shelter) {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            shelterError(e)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteShelter(id: String): Result<Unit> {
        return try {
            supabase.from("shelters").delete {
                filter {
                    eq("id", id)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            shelterError(e)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun shelterError(e: RestException): Result<Unit> {
        return if (e.message?.contains("403") == true) {
            Result.failure(Exception("Anda tidak memiliki hak akses untuk mengelola posko."))
        } else {
            Result.failure(Exception("Operasi posko gagal: ${e.message}"))
        }
    }

    private fun DisasterDto.toDomainModel(): LocalDisasterReport = LocalDisasterReport(
        id = id ?: java.util.UUID.randomUUID().toString(),
        title = title,
        description = description,
        location = location,
        reporter = reporterName,
        status = status,
        latitude = latitude,
        longitude = longitude
    )
}
