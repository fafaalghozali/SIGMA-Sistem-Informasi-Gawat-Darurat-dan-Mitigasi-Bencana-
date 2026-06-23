package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.CreateVolunteerRequest
import com.mahasiswa.sigma.data.model.CreateVolunteerReportRequest
import com.mahasiswa.sigma.data.model.UpdateVolunteerRequest
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.data.model.VolunteerReportDto
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Volunteer Repository using Retrofit for Supabase REST API
 */
@Singleton
class VolunteerRepositoryRetrofit @Inject constructor(
    private val supabaseApi: SupabaseApiService
) {

    companion object {
        private const val TAG = "VolunteerRepositoryRetrofit"
    }

    // ==================== VOLUNTEERS ====================

    suspend fun getAllVolunteers(): Result<List<VolunteerDto>> {
        return try {
            val volunteers = supabaseApi.getVolunteers()
            Log.d(TAG, "Fetched ${volunteers.size} volunteers")
            Result.success(volunteers)
        } catch (e: HttpException) {
            handleHttpException(e, "getAllVolunteers")
        } catch (e: IOException) {
            handleNetworkError(e, "getAllVolunteers")
        } catch (e: Exception) {
            handleGenericError(e, "getAllVolunteers")
        }
    }

    suspend fun getVolunteerById(id: String): Result<VolunteerDto?> {
        return try {
            val volunteers = supabaseApi.getVolunteerById(id = "eq.$id")
            Result.success(volunteers.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteerById")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteerById")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteerById")
        }
    }

    suspend fun getVolunteerByUserId(userId: String): Result<VolunteerDto?> {
        return try {
            val volunteers = supabaseApi.getVolunteerByUserId(userId = "eq.$userId")
            Result.success(volunteers.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteerByUserId")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteerByUserId")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteerByUserId")
        }
    }

    suspend fun createVolunteer(request: CreateVolunteerRequest): Result<VolunteerDto> {
        return try {
            val volunteers = supabaseApi.createVolunteer(request)
            val volunteer = volunteers.firstOrNull()
                ?: throw Exception("Volunteer creation did not return data")
            Log.d(TAG, "Created volunteer: ${volunteer.name}")
            Result.success(volunteer)
        } catch (e: HttpException) {
            handleHttpException(e, "createVolunteer")
        } catch (e: IOException) {
            handleNetworkError(e, "createVolunteer")
        } catch (e: Exception) {
            handleGenericError(e, "createVolunteer")
        }
    }

    suspend fun updateVolunteer(id: String, request: UpdateVolunteerRequest): Result<VolunteerDto> {
         return try {
             val volunteers = supabaseApi.updateVolunteer(id = "eq.$id", updates = request)
             val volunteer = volunteers.firstOrNull()
                 ?: throw Exception("Volunteer update did not return data")
             Log.d(TAG, "Updated volunteer: ${volunteer.name}")
             Result.success(volunteer)
         } catch (e: HttpException) {
             handleHttpException(e, "updateVolunteer")
         } catch (e: IOException) {
             handleNetworkError(e, "updateVolunteer")
         } catch (e: Exception) {
             handleGenericError(e, "updateVolunteer")
         }
     }

    suspend fun updateVolunteerMap(id: String, updates: Map<String, Any?>): Result<VolunteerDto> {
        return try {
            val volunteers = supabaseApi.updateVolunteerMap(id = "eq.$id", updates = updates)
            val volunteer = volunteers.firstOrNull()
                ?: throw Exception("Volunteer update map did not return data")
            Log.d(TAG, "Updated volunteer map: ${volunteer.name}")
            Result.success(volunteer)
        } catch (e: HttpException) {
            handleHttpException(e, "updateVolunteerMap")
        } catch (e: IOException) {
            handleNetworkError(e, "updateVolunteerMap")
        } catch (e: Exception) {
            handleGenericError(e, "updateVolunteerMap")
        }
    }

    suspend fun deleteVolunteer(id: String): Result<Unit> {
        return try {
            supabaseApi.deleteVolunteer(id = "eq.$id")
            Log.d(TAG, "Deleted volunteer: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            handleHttpException(e, "deleteVolunteer")
        } catch (e: IOException) {
            handleNetworkError(e, "deleteVolunteer")
        } catch (e: Exception) {
            handleGenericError(e, "deleteVolunteer")
        }
    }

    // ==================== VOLUNTEER REPORTS ====================

    suspend fun getAllVolunteerReports(): Result<List<VolunteerReportDto>> {
        return try {
            val reports = supabaseApi.getVolunteerReports()
            Log.d(TAG, "Fetched ${reports.size} volunteer reports")
            Result.success(reports)
        } catch (e: HttpException) {
            handleHttpException(e, "getAllVolunteerReports")
        } catch (e: IOException) {
            handleNetworkError(e, "getAllVolunteerReports")
        } catch (e: Exception) {
            handleGenericError(e, "getAllVolunteerReports")
        }
    }

    suspend fun getVolunteerReportById(id: String): Result<VolunteerReportDto?> {
        return try {
            val reports = supabaseApi.getVolunteerReportById(id = "eq.$id")
            Result.success(reports.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteerReportById")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteerReportById")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteerReportById")
        }
    }

    suspend fun getVolunteersByDisasterId(disasterId: String): Result<List<VolunteerDto>> {
        return try {
            val volunteers = supabaseApi.getVolunteersByDisasterId(disasterId = "eq.$disasterId")
            Log.d(TAG, "Fetched ${volunteers.size} volunteers for disaster: $disasterId")
            Result.success(volunteers)
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteersByDisasterId")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteersByDisasterId")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteersByDisasterId")
        }
    }

    suspend fun getVolunteerReportsByDisasterId(disasterId: String): Result<List<VolunteerReportDto>> {
        return try {
            val reports = supabaseApi.getVolunteerReportsByDisasterId(disasterId = "eq.$disasterId")
            Log.d(TAG, "Fetched ${reports.size} reports for disaster: $disasterId")
            Result.success(reports)
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteerReportsByDisasterId")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteerReportsByDisasterId")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteerReportsByDisasterId")
        }
    }

    suspend fun getVolunteerReportsByVolunteerId(volunteerId: String): Result<List<VolunteerReportDto>> {
        return try {
            val reports = supabaseApi.getVolunteerReportsByVolunteerId(volunteerId = "eq.$volunteerId")
            Log.d(TAG, "Fetched ${reports.size} reports for volunteer: $volunteerId")
            Result.success(reports)
        } catch (e: HttpException) {
            handleHttpException(e, "getVolunteerReportsByVolunteerId")
        } catch (e: IOException) {
            handleNetworkError(e, "getVolunteerReportsByVolunteerId")
        } catch (e: Exception) {
            handleGenericError(e, "getVolunteerReportsByVolunteerId")
        }
    }

    suspend fun createVolunteerReport(request: CreateVolunteerReportRequest): Result<VolunteerReportDto> {
        return try {
            val reports = supabaseApi.createVolunteerReport(request)
            val report = reports.firstOrNull()
                ?: throw Exception("Volunteer report creation did not return data")
            Log.d(TAG, "Created volunteer report: ${report.id}")
            Result.success(report)
        } catch (e: HttpException) {
            handleHttpException(e, "createVolunteerReport")
        } catch (e: IOException) {
            handleNetworkError(e, "createVolunteerReport")
        } catch (e: Exception) {
            handleGenericError(e, "createVolunteerReport")
        }
    }

    suspend fun updateVolunteerReport(id: String, updates: Map<String, Any?>): Result<VolunteerReportDto> {
        return try {
            val reports = supabaseApi.updateVolunteerReport(id = "eq.$id", updates = updates)
            val report = reports.firstOrNull()
                ?: throw Exception("Volunteer report update did not return data")
            Log.d(TAG, "Updated volunteer report: ${report.id}")
            Result.success(report)
        } catch (e: HttpException) {
            handleHttpException(e, "updateVolunteerReport")
        } catch (e: IOException) {
            handleNetworkError(e, "updateVolunteerReport")
        } catch (e: Exception) {
            handleGenericError(e, "updateVolunteerReport")
        }
    }

    suspend fun deleteVolunteerReport(id: String): Result<Unit> {
        return try {
            supabaseApi.deleteVolunteerReport(id = "eq.$id")
            Log.d(TAG, "Deleted volunteer report: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            handleHttpException(e, "deleteVolunteerReport")
        } catch (e: IOException) {
            handleNetworkError(e, "deleteVolunteerReport")
        } catch (e: Exception) {
            handleGenericError(e, "deleteVolunteerReport")
        }
    }

    // ==================== ERROR HANDLING ====================

    private fun <T> handleHttpException(e: HttpException, operation: String): Result<T> {
        // Baca response body asli dari Supabase untuk debug
        val rawBody = try { e.response()?.errorBody()?.string() } catch (_: Exception) { null }
        Log.e(TAG, "$operation HTTP ${e.code()} body: $rawBody", e)

        val errorMessage = when (e.code()) {
            400 -> "Bad request (400): ${rawBody ?: "Invalid data"}"
            401 -> "Unauthorized: Invalid API key or token"
            403 -> "Forbidden: Insufficient permissions"
            404 -> "Not found"
            409 -> "Conflict: ${rawBody ?: "Resource already exists"}"
            422 -> "Unprocessable: ${rawBody ?: "Validation failed"}"
            else -> "HTTP ${e.code()}: ${rawBody ?: e.message()}"
        }
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
