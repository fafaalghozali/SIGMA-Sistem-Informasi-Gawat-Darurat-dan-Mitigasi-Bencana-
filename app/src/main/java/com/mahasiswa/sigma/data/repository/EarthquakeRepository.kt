package com.mahasiswa.sigma.data.repository

import com.mahasiswa.sigma.data.remote.api.BmkgApiService
import com.mahasiswa.sigma.data.remote.api.GempaTerkiniItem
import com.mahasiswa.sigma.data.remote.api.GempaDetail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class EarthquakeData(
    val tanggal: String,
    val jam: String,
    val magnitude: String,
    val kedalaman: String,
    val wilayah: String,
    val coordinates: String,
    val lintang: String,
    val bujur: String,
    val potensi: String,
    val dirasakan: String
)

@Singleton
class EarthquakeRepository @Inject constructor(
    private val bmkgApiService: BmkgApiService
) {

    suspend fun getLatestEarthquake(): EarthquakeData = withContext(Dispatchers.IO) {
        try {
            val response = bmkgApiService.getAutoGempa()
            val gempa = response.infogempa.gempa
            gempa.toEarthquakeData()
        } catch (e: IOException) {
            throw IOException("Tidak dapat terhubung ke server BMKG. Periksa koneksi internet Anda.", e)
        } catch (e: HttpException) {
            throw HttpException(e.response()!!)
        } catch (e: Exception) {
            throw Exception("Gagal memuat data gempa terbaru: ${e.message}", e)
        }
    }

    suspend fun getRecentEarthquakes(): List<EarthquakeData> = withContext(Dispatchers.IO) {
        try {
            val response = bmkgApiService.getGempaTerkini()
            response.infogempa.gempa.map { it.toEarthquakeData() }
        } catch (e: IOException) {
            throw IOException("Tidak dapat terhubung ke server BMKG. Periksa koneksi internet Anda.", e)
        } catch (e: HttpException) {
            throw HttpException(e.response()!!)
        } catch (e: Exception) {
            throw Exception("Gagal memuat data gempa terkini: ${e.message}", e)
        }
    }

    suspend fun searchEarthquakes(query: String): List<EarthquakeData> = withContext(Dispatchers.IO) {
        try {
            val response = bmkgApiService.getGempaTerkini()
            response.infogempa.gempa
                .map { it.toEarthquakeData() }
                .filter { earthquake ->
                    earthquake.wilayah.contains(query, ignoreCase = true) ||
                    earthquake.magnitude.contains(query, ignoreCase = true) ||
                    earthquake.tanggal.contains(query, ignoreCase = true)
                }
        } catch (e: IOException) {
            throw IOException("Tidak dapat terhubung ke server BMKG. Periksa koneksi internet Anda.", e)
        } catch (e: HttpException) {
            throw HttpException(e.response()!!)
        } catch (e: Exception) {
            throw Exception("Gagal mencari data gempa: ${e.message}", e)
        }
    }

    private fun GempaDetail.toEarthquakeData(): EarthquakeData {
        return EarthquakeData(
            tanggal = tanggal,
            jam = jam,
            magnitude = magnitude,
            kedalaman = kedalaman,
            wilayah = wilayah,
            coordinates = coordinates,
            lintang = lintang,
            bujur = bujur,
            potensi = potensi,
            dirasakan = dirasakan
        )
    }

    private fun GempaTerkiniItem.toEarthquakeData(): EarthquakeData {
        return EarthquakeData(
            tanggal = tanggal,
            jam = jam,
            magnitude = magnitude,
            kedalaman = kedalaman,
            wilayah = wilayah,
            coordinates = coordinates,
            lintang = lintang,
            bujur = bujur,
            potensi = potensi,
            dirasakan = dirasakan
        )
    }
}
