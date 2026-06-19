package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.CreateShelterRequest
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.data.model.UpdateShelterRequest
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Shelter Repository using Retrofit for Supabase REST API
 */
@Singleton
class ShelterRepositoryRetrofit @Inject constructor(
    private val supabaseApi: SupabaseApiService
) {

    companion object {
        private const val TAG = "ShelterRepositoryRetrofit"
    }

    suspend fun getAllShelters(): Result<List<ShelterDto>> {
        return try {
            val shelters = supabaseApi.getShelters()
            Log.d(TAG, "Fetched ${shelters.size} shelters")
            if (shelters.isNotEmpty()) {
                Log.d(TAG, "First shelter: ${shelters[0].name}, id=${shelters[0].id}")
            }
            Result.success(shelters)
        } catch (e: HttpException) {
            handleHttpException(e, "getAllShelters")
        } catch (e: IOException) {
            handleNetworkError(e, "getAllShelters")
        } catch (e: Exception) {
            Log.e(TAG, "getAllShelters FULL ERROR: ${e.javaClass.simpleName}: ${e.message}", e)
            handleGenericError(e, "getAllShelters")
        }
    }

    suspend fun getShelterById(id: String): Result<ShelterDto?> {
        return try {
            val shelters = supabaseApi.getShelterById(id = "eq.$id")
            Result.success(shelters.firstOrNull())
        } catch (e: HttpException) {
            handleHttpException(e, "getShelterById")
        } catch (e: IOException) {
            handleNetworkError(e, "getShelterById")
        } catch (e: Exception) {
            handleGenericError(e, "getShelterById")
        }
    }

    suspend fun createShelter(request: CreateShelterRequest): Result<ShelterDto> {
        return try {
            val shelters = supabaseApi.createShelter(request)
            val shelter = shelters.firstOrNull()
                ?: throw Exception("Shelter creation did not return data")
            Log.d(TAG, "Created shelter: ${shelter.name}")
            Result.success(shelter)
        } catch (e: HttpException) {
            handleHttpException(e, "createShelter")
        } catch (e: IOException) {
            handleNetworkError(e, "createShelter")
        } catch (e: Exception) {
            handleGenericError(e, "createShelter")
        }
    }

    suspend fun updateShelter(id: String, request: UpdateShelterRequest): Result<ShelterDto> {
        return try {
            val shelters = supabaseApi.updateShelter(id = "eq.$id", updates = request)
            val shelter = shelters.firstOrNull()
                ?: throw Exception("Shelter update did not return data")
            Log.d(TAG, "Updated shelter: ${shelter.name}")
            Result.success(shelter)
        } catch (e: HttpException) {
            handleHttpException(e, "updateShelter")
        } catch (e: IOException) {
            handleNetworkError(e, "updateShelter")
        } catch (e: Exception) {
            handleGenericError(e, "updateShelter")
        }
    }

    suspend fun deleteShelter(id: String): Result<Unit> {
        return try {
            supabaseApi.deleteShelter(id = "eq.$id")
            Log.d(TAG, "Deleted shelter: $id")
            Result.success(Unit)
        } catch (e: HttpException) {
            handleHttpException(e, "deleteShelter")
        } catch (e: IOException) {
            handleNetworkError(e, "deleteShelter")
        } catch (e: Exception) {
            handleGenericError(e, "deleteShel")
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
