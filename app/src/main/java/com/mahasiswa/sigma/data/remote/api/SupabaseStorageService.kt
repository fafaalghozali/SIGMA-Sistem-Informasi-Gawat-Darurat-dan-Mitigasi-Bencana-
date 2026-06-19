package com.mahasiswa.sigma.data.remote.api

import android.graphics.Bitmap
import android.util.Log
import com.mahasiswa.sigma.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

/**
 * Service for uploading files to Supabase Storage.
 * Uses the "laporan" bucket for disaster report photos.
 */
@Singleton
class SupabaseStorageService @Inject constructor(
    @Named("supabase") private val okHttpClient: OkHttpClient
) {
    companion object {
        private const val TAG = "SupabaseStorageService"
        private const val BUCKET = "laporan"
    }

    /**
     * Upload a bitmap to Supabase Storage and return the public URL.
     * Returns null if upload fails.
     */
    suspend fun uploadImage(bitmap: Bitmap, fileNamePrefix: String = "report"): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Convert bitmap to JPEG bytes
                val baos = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos)
                val imageBytes = baos.toByteArray()

                // Generate unique filename
                val fileName = "${fileNamePrefix}_${UUID.randomUUID()}.jpg"

                // Build upload URL
                val uploadUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/$BUCKET/$fileName"

                val requestBody = imageBytes.toRequestBody("image/jpeg".toMediaType())

                val request = Request.Builder()
                    .url(uploadUrl)
                    .post(requestBody)
                    .header("Content-Type", "image/jpeg")
                    .build()

                val response = okHttpClient.newCall(request).execute()

                if (response.isSuccessful) {
                    // Return public URL
                    val publicUrl = "${BuildConfig.SUPABASE_URL}/storage/v1/object/public/$BUCKET/$fileName"
                    Log.d(TAG, "Upload successful: $publicUrl")
                    publicUrl
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Upload failed: ${response.code} - $errorBody")
                    null
                }
            } catch (e: Exception) {
                Log.e(TAG, "Upload exception: ${e.message}", e)
                null
            }
        }
    }
}
