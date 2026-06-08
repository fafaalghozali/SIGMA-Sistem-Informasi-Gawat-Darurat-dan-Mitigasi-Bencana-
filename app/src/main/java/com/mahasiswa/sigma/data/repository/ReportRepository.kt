package com.mahasiswa.sigma.data.repository

import android.net.Uri
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.plugins.HttpRequestTimeoutException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
data class DisasterDto(
    val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val title: String,
    val description: String,
    @SerialName("disaster_type") val disasterType: String? = null,
    val location: String,
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "pending",
    @SerialName("reporter_name") val reporterName: String,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Singleton
class ReportRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val authManager: AuthManager,
    private val storageRepository: StorageRepository
) {

    suspend fun saveReport(report: LocalDisasterReport, photoUri: Uri? = null): Result<Unit> {
        return try {
            val userId = authManager.getCurrentUserId()

            val photoUrl: String? = if (photoUri != null && userId != null) {
                storageRepository.uploadDisasterPhoto(userId, photoUri).getOrNull()
            } else null

            val dto = report.toDto(userId = userId, status = "pending", photoUrl = photoUrl)
            supabase.from("disasters").insert(dto)
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal menyimpan laporan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllReports(): List<LocalDisasterReport> {
        return try {
            supabase.from("disasters")
                .select {
                    order("created_at", Order.DESCENDING)
                }
                .decodeList<DisasterDto>()
                .map { it.toDomainModel() }
        } catch (e: RestException) {
            emptyList()
        } catch (e: HttpRequestTimeoutException) {
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getReportById(id: String): LocalDisasterReport? {
        return try {
            supabase.from("disasters")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<DisasterDto>()
                .toDomainModel()
        } catch (e: RestException) {
            null
        } catch (e: HttpRequestTimeoutException) {
            null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun updateReport(report: LocalDisasterReport): Result<Unit> {
        return try {
            val userId = authManager.getCurrentUserId()
            val dto = report.toDto(userId = userId, status = report.status)
            supabase.from("disasters").update(dto) {
                filter {
                    eq("id", report.id)
                }
            }
            Result.success(Unit)
        } catch (e: RestException) {
            Result.failure(Exception("Gagal memperbarui laporan: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun LocalDisasterReport.toDto(
        userId: String?,
        status: String,
        photoUrl: String? = null
    ): DisasterDto = DisasterDto(
        id = id,
        userId = userId,
        title = title,
        description = description,
        location = location,
        latitude = latitude,
        longitude = longitude,
        status = status,
        reporterName = reporter,
        photoUrl = photoUrl
    )

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
