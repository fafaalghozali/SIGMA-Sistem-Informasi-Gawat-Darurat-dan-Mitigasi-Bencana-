package com.mahasiswa.sigma.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import androidx.compose.ui.graphics.Color
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.mahasiswa.sigma.data.model.BmkgWarning
import com.mahasiswa.sigma.data.model.EarthquakeInfo
import com.mahasiswa.sigma.data.model.WarningSeverity
import com.mahasiswa.sigma.data.model.WeatherInfo
import com.mahasiswa.sigma.data.remote.api.BmkgApiService
import com.mahasiswa.sigma.data.remote.api.OpenMeteoApiService
import com.mahasiswa.sigma.ui.theme.EmergencyRed
import com.mahasiswa.sigma.ui.theme.MitigationBlue
import com.mahasiswa.sigma.ui.theme.VolunteerGreen
import com.mahasiswa.sigma.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val openMeteoApiService: OpenMeteoApiService,
    private val bmkgApiService: BmkgApiService
) {

    class LocationUnavailableException : Exception("Could not determine device location")

    @SuppressLint("MissingPermission")
    suspend fun getWeatherForCurrentLocation(): WeatherInfo {
        val (lat, lon) = getCurrentLocation()
        val cityName = reverseGeocode(lat, lon)
        return fetchOpenMeteoWeather(lat, lon, cityName)
    }

    suspend fun getWeatherForFallbackLocation(): WeatherInfo {
        val (lat, lon) = SURAKARTA_COORDS
        return fetchOpenMeteoWeather(lat, lon, "Surakarta (Default)")
    }

    suspend fun getLatestEarthquake(): EarthquakeInfo? = withContext(Dispatchers.IO) {
        try {
            val response = bmkgApiService.getAutoGempa()
            val gempa = response.infogempa.gempa
            EarthquakeInfo(
                magnitude = gempa.magnitude.ifBlank { "--" },
                location = gempa.wilayah.ifBlank { "--" },
                depth = gempa.kedalaman.ifBlank { "--" },
                time = "${gempa.tanggal} ${gempa.jam}".trim(),
                felt = gempa.dirasakan.ifBlank { "Tidak dirasakan" }
            )
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getRecentBmkgWarnings(): List<BmkgWarning> = withContext(Dispatchers.IO) {
        try {
            val response = bmkgApiService.getGempaTerkini()
            val warnings = mutableListOf<BmkgWarning>()
            for (item in response.infogempa.gempa.take(5)) {
                val mag = item.magnitude.toDoubleOrNull() ?: 0.0
                if (mag >= 5.0) {
                    val severity = when {
                        mag >= 7.0 -> WarningSeverity.DANGER
                        mag >= 5.5 -> WarningSeverity.WARNING
                        else -> WarningSeverity.INFO
                    }
                    warnings.add(
                        BmkgWarning(
                            type = "Gempa Bumi",
                            message = "M ${"%.1f".format(mag)} – ${item.wilayah.ifBlank { "--" }}",
                            severity = severity,
                            time = "${item.tanggal} ${item.jam}".trim()
                        )
                    )
                }
            }
            warnings
        } catch (_: Exception) {
            emptyList()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Pair<Double, Double> =
        suspendCancellableCoroutine { cont ->
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()

            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, cts.token)
                .addOnSuccessListener { location ->
                    if (location != null) {
                        cont.resume(Pair(location.latitude, location.longitude))
                    } else {
                        client.lastLocation
                            .addOnSuccessListener { last ->
                                if (last != null) {
                                    cont.resume(Pair(last.latitude, last.longitude))
                                } else {
                                    cont.resumeWithException(LocationUnavailableException())
                                }
                            }
                            .addOnFailureListener {
                                cont.resumeWithException(LocationUnavailableException())
                            }
                    }
                }
                .addOnFailureListener { e -> cont.resumeWithException(e) }

            cont.invokeOnCancellation { cts.cancel() }
        }

    private suspend fun reverseGeocode(lat: Double, lon: Double): String =
        withContext(Dispatchers.IO) {
            try {
                @Suppress("DEPRECATION")
                val addresses = Geocoder(context, Locale("id", "ID"))
                    .getFromLocation(lat, lon, 1)
                val addr = addresses?.firstOrNull()
                addr?.subAdminArea
                    ?.removePrefix("Kota ")
                    ?.removePrefix("Kabupaten ")
                    ?: addr?.adminArea
                    ?: "Lokasi Anda"
            } catch (_: Exception) {
                "Lokasi Anda"
            }
        }

    private suspend fun fetchOpenMeteoWeather(
        lat: Double,
        lon: Double,
        cityName: String
    ): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            val response = openMeteoApiService.getCurrentWeather(lat, lon)
            val current = response.current
            if (current != null) {
                val tempC = current.temperature2m.toInt()
                val wmoCode = current.weatherCode
                val humidity = current.relativeHumidity2m
                val windSpeed = current.windSpeed10m

                val condition = wmoCodeToCondition(wmoCode)
                val (riskStatus, riskColor) = wmoCodeToRisk(wmoCode, tempC)

                WeatherInfo(
                    location = cityName,
                    condition = condition,
                    temperature = "${tempC}°C",
                    riskStatus = riskStatus,
                    riskColor = riskColor,
                    weatherCode = wmoCode,
                    humidity = if (humidity >= 0) "${humidity}%" else "--",
                    windSpeed = if (windSpeed >= 0) "${windSpeed.toInt()} km/h" else "--",
                    lastUpdated = System.currentTimeMillis()
                )
            } else {
                buildWeatherFallback(cityName)
            }
        } catch (_: Exception) {
            buildWeatherFallback(cityName)
        }
    }

    fun wmoCodeToCondition(code: Int): String = when (code) {
        0 -> "Cerah"
        1 -> "Cerah Berawan"
        2 -> "Berawan Sebagian"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        51, 53 -> "Gerimis"
        55 -> "Gerimis Lebat"
        61, 63 -> "Hujan Ringan"
        65 -> "Hujan Lebat"
        80, 81 -> "Hujan Lokal"
        82 -> "Hujan Deras"
        95 -> "Badai Petir"
        96, 99 -> "Badai Petir Lebat"
        56, 57, 66, 67, 71, 73, 75, 77, 85, 86 -> "Cuaca Ekstrem"
        else -> "Kondisi Tidak Diketahui"
    }

    fun wmoCodeToEmoji(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55 -> "🌦️"
        61, 63, 65, 80, 81, 82 -> "🌧️"
        95, 96, 99 -> "⛈️"
        56, 57, 66, 67, 71, 73, 75, 77, 85, 86 -> "🌩️"
        else -> "☁️"
    }

    private fun wmoCodeToRisk(code: Int, tempC: Int): Pair<String, Color> = when {
        code in listOf(95, 96, 99) ->
            Pair("Risiko Petir Tinggi", EmergencyRed)
        code == 65 || code == 82 ->
            Pair("Risiko Banjir Tinggi", EmergencyRed)
        tempC >= 38 ->
            Pair("Suhu Ekstrem", EmergencyRed)
        code in listOf(61, 63, 80, 81) ->
            Pair("Waspada Hujan", WarningOrange)
        code in listOf(45, 48) ->
            Pair("Kabut Tebal", WarningOrange)
        tempC >= 35 ->
            Pair("Cuaca Panas", WarningOrange)
        code in listOf(51, 53, 55) ->
            Pair("Gerimis", MitigationBlue)
        code in listOf(56, 57, 66, 67, 71, 73, 75, 77, 85, 86) ->
            Pair("Cuaca Ekstrem", MitigationBlue)
        code in listOf(0, 1, 2, 3) ->
            Pair("Kondisi Normal", VolunteerGreen)
        else ->
            Pair("Kondisi Stabil", VolunteerGreen)
    }

    private fun buildWeatherFallback(city: String) = WeatherInfo(
        location = city,
        condition = "Tidak tersedia",
        temperature = "--°C",
        riskStatus = "Tidak tersedia",
        riskColor = MitigationBlue,
        weatherCode = -1
    )

    companion object {
        private val SURAKARTA_COORDS = Pair(-7.5755, 110.8243)
    }
}
