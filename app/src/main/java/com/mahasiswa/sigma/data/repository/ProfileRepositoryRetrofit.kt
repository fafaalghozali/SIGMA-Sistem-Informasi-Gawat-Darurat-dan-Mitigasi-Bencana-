package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.CreateProfileRequest
import com.mahasiswa.sigma.data.model.ProfileDto
import com.mahasiswa.sigma.data.model.UpdateProfileRequest
import com.mahasiswa.sigma.data.remote.api.SupabaseApiService
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest

/**
 * Profile Repository using Retrofit for Supabase REST API
 * 
 * This repository manages user profile data through Supabase PostgREST API
 * using Retrofit HTTP client instead of Supabase Kotlin SDK.
 */
@Singleton
class ProfileRepositoryRetrofit @Inject constructor(
    private val supabaseApi: SupabaseApiService,
    private val supabaseClient: SupabaseClient
) {

    companion object {
        private const val TAG = "ProfileRepositoryRetrofit"
    }

    /**
     * Get all profiles
     */
    suspend fun getAllProfiles(): Result<List<ProfileDto>> {
        return try {
            val profiles = supabaseApi.getProfiles()
            Log.d(TAG, "Fetched ${profiles.size} profiles")
            Result.success(profiles)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized: Invalid API key or token"
                403 -> "Forbidden: Insufficient permissions"
                404 -> "Profiles not found"
                else -> "HTTP error: ${e.message()}"
            }
            Log.e(TAG, "getAllProfiles HttpException: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Log.e(TAG, "getAllProfiles IOException: Network error", e)
            Result.failure(Exception("Network error: Please check your internet connection"))
        } catch (e: Exception) {
            Log.e(TAG, "getAllProfiles Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get profile by ID
     */
    suspend fun getProfileById(id: String): Result<ProfileDto?> {
        return try {
            val profiles = supabaseApi.getProfileById(id = "eq.$id")
            val profile = profiles.firstOrNull()
            if (profile != null) {
                Log.d(TAG, "Fetched profile: ${profile.email}")
            } else {
                Log.w(TAG, "Profile not found for id: $id")
            }
            Result.success(profile)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized: Invalid API key or token"
                403 -> "Forbidden: Insufficient permissions"
                404 -> "Profile not found"
                else -> "HTTP error: ${e.message()}"
            }
            Log.e(TAG, "getProfileById HttpException: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Log.e(TAG, "getProfileById IOException: Network error", e)
            Result.failure(Exception("Network error: Please check your internet connection"))
        } catch (e: Exception) {
            Log.e(TAG, "getProfileById Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Get profile by email
     */
    suspend fun getProfileByEmail(email: String): Result<ProfileDto?> {
        return try {
            val profiles = supabaseApi.getProfileByEmail(email = "eq.$email")
            val profile = profiles.firstOrNull()
            if (profile != null) {
                Log.d(TAG, "Fetched profile by email: ${profile.email}")
            } else {
                Log.w(TAG, "Profile not found for email: $email")
            }
            Result.success(profile)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                401 -> "Unauthorized: Invalid API key or token"
                403 -> "Forbidden: Insufficient permissions"
                404 -> "Profile not found"
                else -> "HTTP error: ${e.message()}"
            }
            Log.e(TAG, "getProfileByEmail HttpException: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Log.e(TAG, "getProfileByEmail IOException: Network error", e)
            Result.failure(Exception("Network error: Please check your internet connection"))
        } catch (e: Exception) {
            Log.e(TAG, "getProfileByEmail Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Create a new profile
     */
    suspend fun createProfile(request: CreateProfileRequest): Result<ProfileDto> {
        return try {
            val profiles = supabaseApi.createProfile(request)
            val profile = profiles.firstOrNull()
                ?: throw Exception("Profile creation did not return data")
            Log.d(TAG, "Created profile: ${profile.email}")
            Result.success(profile)
        } catch (e: HttpException) {
            val errorMessage = when (e.code()) {
                400 -> "Bad request: Invalid profile data"
                401 -> "Unauthorized: Invalid API key or token"
                403 -> "Forbidden: Insufficient permissions"
                409 -> "Conflict: Profile already exists"
                422 -> "Unprocessable entity: Validation failed"
                else -> "HTTP error: ${e.message()}"
            }
            Log.e(TAG, "createProfile HttpException: $errorMessage", e)
            Result.failure(Exception(errorMessage))
        } catch (e: IOException) {
            Log.e(TAG, "createProfile IOException: Network error", e)
            Result.failure(Exception("Network error: Please check your internet connection"))
        } catch (e: Exception) {
            Log.e(TAG, "createProfile Exception: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Update an existing profile
     */
    suspend fun updateProfile(id: String, request: UpdateProfileRequest): Result<ProfileDto> {
        return try {
            val profiles = supabaseClient.postgrest["profiles"].update(
                {
                    request.fullName?.let { set("full_name", it) }
                    request.photoUrl?.let { set("photo_url", it) }
                }
            ) {
                filter { eq("id", id) }
                select()
            }.decodeList<ProfileDto>()
            
            val profile = profiles.firstOrNull()
                ?: throw Exception("Profile update did not return data")
            Log.d(TAG, "Updated profile: ${profile.email}")
            Result.success(profile)
        } catch (e: Exception) {
            Log.e(TAG, "updateProfile Exception: ${e.message}", e)
            Result.failure(Exception("Gagal mengedit profil: ${e.message}"))
        }
    }

    /**
     * Delete a profile
     */
    suspend fun deleteProfile(id: String): Result<Unit> {
        return try {
            // Menggunakan SupabaseClient agar JWT Token dari user yang sedang login ikut terkirim,
            // sehingga RLS (Row Level Security) bisa mengenali bahwa ini adalah aksi dari Admin.
            supabaseClient.postgrest["profiles"].delete {
                filter { eq("id", id) }
            }
            Log.d(TAG, "Deleted profile: $id")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "deleteProfile Exception: ${e.message}", e)
            Result.failure(Exception("Gagal menghapus profil: ${e.message}"))
        }
    }
}
