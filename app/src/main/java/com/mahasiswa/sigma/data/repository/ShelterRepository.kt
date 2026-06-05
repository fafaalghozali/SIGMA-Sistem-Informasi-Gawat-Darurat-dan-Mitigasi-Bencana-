package com.mahasiswa.sigma.data.repository

import android.util.Log
import com.mahasiswa.sigma.data.model.Shelter
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.from
import io.ktor.client.plugins.HttpRequestTimeoutException
import javax.inject.Inject
import javax.inject.Singleton

// ShelterDto is defined in AdminRepository.kt (shared between both repositories)

// ---------------------------------------------------------------------------
// Task 7.1–7.3 — ShelterRepository
// ---------------------------------------------------------------------------

@Singleton
class ShelterRepository @Inject constructor(
    private val supabase: SupabaseClient
) {

    companion object {
        private const val TAG = "ShelterRepository"
    }

    // -----------------------------------------------------------------------
    // Task 7.2 — getAllShelters
    // -----------------------------------------------------------------------

    suspend fun getAllShelters(): Result<List<Shelter>> {
        return try {
            val list = supabase.from("shelters")
                .select()
                .decodeList<ShelterDto>()
                .map { it.toDomainModel() }
            Result.success(list)
        } catch (e: RestException) {
            Log.e(TAG, "getAllShelters RestException: ${e.message}")
            Result.failure(Exception("Gagal mengambil data posko: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Log.e(TAG, "getAllShelters error: ${e.message}")
            Result.failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // Task 7.3 — getShelterById
    // -----------------------------------------------------------------------

    suspend fun getShelterById(id: String): Result<Shelter> {
        return try {
            val dto = supabase.from("shelters")
                .select {
                    filter {
                        eq("id", id)
                    }
                }
                .decodeSingle<ShelterDto>()
            Result.success(dto.toDomainModel())
        } catch (e: RestException) {
            Result.failure(Exception("Gagal mengambil data posko: ${e.message}"))
        } catch (e: HttpRequestTimeoutException) {
            Result.failure(Exception("Tidak ada koneksi internet. Periksa jaringan Anda."))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // -----------------------------------------------------------------------
    // Mapping helper — ShelterDto → Shelter domain model
    // availableSpace = capacityMax - capacityCurrent
    // -----------------------------------------------------------------------

    private fun ShelterDto.toDomainModel(): Shelter = Shelter(
        id = id ?: java.util.UUID.randomUUID().toString(),
        name = name,
        address = address,
        location = address,
        capacity = capacityMax,
        availableSpace = (capacityMax - capacityCurrent).coerceAtLeast(0),
        latitude = latitude,
        longitude = longitude
    )
}
