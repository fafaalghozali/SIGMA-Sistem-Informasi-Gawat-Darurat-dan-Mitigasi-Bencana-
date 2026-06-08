package com.mahasiswa.sigma.data.repository

import android.content.ContentResolver
import android.net.Uri
import android.util.Log
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.storage.storage
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val supabase: SupabaseClient,
    private val contentResolver: ContentResolver
) {

    companion object {
        private const val TAG = "StorageRepository"
        private const val MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024L
        private const val BUCKET_DISASTER  = "disaster-photos"
        private const val BUCKET_VOLUNTEER = "volunteer-reports"
    }

    suspend fun uploadDisasterPhoto(userId: String, uri: Uri): Result<String> {
        return uploadPhoto(uri, BUCKET_DISASTER, userId)
    }

    suspend fun uploadVolunteerReportPhoto(userId: String, uri: Uri): Result<String> {
        return uploadPhoto(uri, BUCKET_VOLUNTEER, userId)
    }

    private suspend fun uploadPhoto(uri: Uri, bucket: String, userId: String): Result<String> {
        return try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: return Result.failure(Exception("Tidak dapat membaca file gambar."))

            if (bytes.size > MAX_FILE_SIZE_BYTES) {
                return Result.failure(
                    Exception("Ukuran file melebihi batas maksimum 5 MB. Pilih gambar yang lebih kecil.")
                )
            }

            val fileName = "${userId}_${System.currentTimeMillis()}.jpg"

            supabase.storage
                .from(bucket)
                .upload(fileName, bytes)

            val publicUrl = supabase.storage
                .from(bucket)
                .publicUrl(fileName)

            Result.success(publicUrl)
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Log.e(TAG, "uploadPhoto to $bucket failed: ${e.message}")
            Result.failure(Exception("Gagal mengunggah foto: ${e.message}"))
        }
    }
}
