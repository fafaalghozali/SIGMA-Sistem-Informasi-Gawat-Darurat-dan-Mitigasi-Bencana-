package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.CreateDisasterReportRequest
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.UpdateDisasterReportRequest
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Disaster Report Repository using Retrofit for Supabase REST API
 */
@Singleton
class DisasterReportRepositoryRetrofit @Inject constructor(
    private val supabaseApi: SupabaseApiService
) {

    companion object {
        private const val TAG = "DisasterReportRepositoryRetrofit"
    }

    suspend fun getAllDisasterReports(): Result<List<DisasterReportDto>> {
        return try {
            val reports = supabaseApi.getDisasterReports()
            Log.d(TAG, "Fetched ${reports.size} disaster reports")
            Result.success(reports)
        } catch (e: HttpException) {
            handleHttpException(e, "getAllDisasterReports")
        } catch (e: IOException) {
            handleNetworkError(e, "getAllDisasterReports")
        } catch (e: Exception) {
            handleGenericError(e, "getAllDisasterReports")
        }
    }

    suspend fun getDisasterReportById(id: String): Result<DisasterReportDto?> {
        return try {
            val reports = supabaseApi.getDisasterReportById(id = "eq.$id")
            Result.success(reports.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getDisasterReportById")
        } catch (e: IOException) {
            handleNetworkError(e, "getDisasterReportById")
        } catch (e: Exception) {
            handleGenericError(e, "getDisasterReportById")
        }
    }

    suspend fun getDisasterReportsByStatus(status: String): Result<List<DisasterReportDto>> {
        return try {
            val reports = supabaseApi.getDisasterReportsByStatus(status = "eq.$status")
            Log.d(TAG, "Fetched ${reports.size} disaster reports with status: $status")
            Result.success(reports)
        } catch (e: HttpException) {
            handleHttpException(e, "getDisasterReportsByStatus")
        } catch (e: IOException) {
            handleNetworkError(e, "getDisasterReportsByStatus")
        } catch (e: Exception) {
            handleGenericError(e, "getDisasterReportsByStatus")
        }
    }

    suspend fun createDisasterReport(request: CreateDisasterReportRequest): Result<DisasterReportDto> {
        return try {
            val reports = supabaseApi.createDisasterReport(request)
            val report = reports.firstOrNull()
                ?: throw Exception("Disaster report creation did not return data")
            Log.d(TAG, "Created disaster report: ${report.title}")
            Result.success(report)
        } catch (e: HttpException) {
            handleHttpException(e, "createDisasterReport")
        } catch (e: IOException) {
            handleNetworkError(e, "createDisasterReport")
        } catch (e: Exception) {
            handleGenericError(e, "createDisasterReport")
        }
    }

    suspend fun updateDisasterReport(id: String, request: UpdateDisasterReportRequest): Result<DisasterReportDto> {
        return try {
            val reports = supabaseApi.updateDisasterReport(id = "eq.$id", updates = request)
            val report = reports.firstOrNull()
                ?: throw Exception("Disaster report update did not return data")
            Log.d(TAG, "Updated disaster report: ${report.title}")
            Result.success(report)
        } catch (e: HttpException) {
            handleHttpException(e, "updateDisasterReport")
        } catch (e: IOException) {
            handleNetworkError(e, "updateDisasterReport")
        } catch (e: Exception) {
            handleGenericError(e, "updateDisasterReport")
        }
    }

    suspend fun deleteDisasterReport(id: String): Result<Unit> {
        return try {
            supabaseApi.deleteDisasterReport(id = "eq.$id")
            Log.d(TAG, "Deleted disaster report: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            handleHttpException(e, "deleteDisasterReport")
        } catch (e: IOException) {
            handleNetworkError(e, "deleteDisasterReport")
        } catch (e: Exception) {
            handleGenericError(e, "deleteDisasterReport")
        }
    }

    // ==================== ERROR HANDLING ====================

    private fun <T> handleHttpException(e: HttpException, operation: String): Result<T> {
        val errorMessage = when (e.code()) {
            400 -> "Bad request: Invalid data"
            401 -> "Unauthorized: Invalid API key or token"
            403 -> "Forbidden: Insufficient permissions"
            404 -> "Not found"
            409 -> "Conflict: Resource already exists"
            422 -> "Unprocessable entity: Validation failed"
            else -> "HTTP error: ${e.message()}"
        }
        Log.e(TAG, "$operation HttpException: $errorMessage", e)
        return Result.failure(Exception(errorMessage))
    }

    private fun <T> handleNetworkError(e: IOException, operation: String): Result<T> {
        Log.e(TAG, "$operation IOException: Network error", e)
        return Result.failure(Exception("Network error: Please check your internet connection"))
    }

    private fun <T> handleGenericError(e: Exception, operation: String): Result<T> {
        Log.e(TAG, "$operation Exception: ${e.message}", e)
        return Result.failure(e)
    }
}
