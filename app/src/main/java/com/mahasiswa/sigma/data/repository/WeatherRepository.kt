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
import com.mahasiswa.sigma.ui.theme.EmergencyRed
import com.mahasiswa.sigma.ui.theme.MitigationBlue
import com.mahasiswa.sigma.ui.theme.VolunteerGreen
import com.mahasiswa.sigma.ui.theme.WarningOrange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Hybrid weather + disaster repository.
 *
 * Weather  → Open-Meteo (https://api.open-meteo.com) — stable, free, no API key
 * Disaster → BMKG official data (https://data.bmkg.go.id) — earthquake & warnings
 *
 * Open-Meteo uses WMO weather interpretation codes:
 *   https://open-meteo.com/en/docs#weathervariables
 */
class WeatherRepository(private val context: Context) {

    /** Thrown when real GPS location cannot be determined. */
    class LocationUnavailableException : Exception("Could not determine device location")

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Fetches real GPS location then queries Open-Meteo for current weather.
     * Throws [LocationUnavailableException] if no real location is available,
     * so the caller (ViewModel) can decide whether to fallback.
     */
    @SuppressLint("MissingPermission")
    suspend fun getWeatherForCurrentLocation(): WeatherInfo {
        val (lat, lon) = getCurrentLocation()
        val cityName = reverseGeocode(lat, lon)
        return fetchOpenMeteoWeather(lat, lon, cityName)
    }

    /**
     * Explicit fallback: uses Surakarta coordinates.
     * Only called by the ViewModel when permission is denied or GPS truly fails.
     */
    suspend fun getWeatherForFallbackLocation(): WeatherInfo {
        val (lat, lon) = SURAKARTA_COORDS
        return fetchOpenMeteoWeather(lat, lon, "Surakarta (Default)")
    }

    /** Fetches the latest earthquake from BMKG's official open-data endpoint. */
    suspend fun getLatestEarthquake(): EarthquakeInfo? = withContext(Dispatchers.IO) {
        try {
            val json = fetchJsonWithRetry("https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json")
            parseEarthquake(json)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Fetches recent significant earthquakes from BMKG and converts them
     * into BmkgWarning items for dashboard display.
     * Uses gempaterkini.json which returns the 15 most recent felt earthquakes.
     */
    suspend fun getRecentBmkgWarnings(): List<BmkgWarning> = withContext(Dispatchers.IO) {
        try {
            val json = fetchJsonWithRetry("https://data.bmkg.go.id/DataMKG/TEWS/gempaterkini.json")
            parseRecentQuakeWarnings(json)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Location ──────────────────────────────────────────────────────────────

    /**
     * Obtains real device GPS coordinates.
     * Tries getCurrentLocation first, then lastLocation.
     * Throws [LocationUnavailableException] if neither succeeds —
     * does NOT silently fall back to Surakarta.
     */
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
                        // Try last known location — but do NOT silently use Surakarta
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

    // ── Reverse geocoding ─────────────────────────────────────────────────────

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

    // ── Open-Meteo weather ────────────────────────────────────────────────────

    /**
     * Open-Meteo v1 forecast endpoint.
     * Returns current temperature_2m, weather_code, relative_humidity_2m,
     * and wind_speed_10m (WMO standard).
     *
     * Example response:
     * {
     *   "current": {
     *     "time": "2024-01-01T12:00",
     *     "temperature_2m": 28.4,
     *     "weather_code": 61,
     *     "relative_humidity_2m": 78,
     *     "wind_speed_10m": 12.3
     *   }
     * }
     */
    private suspend fun fetchOpenMeteoWeather(
        lat: Double,
        lon: Double,
        cityName: String
    ): WeatherInfo = withContext(Dispatchers.IO) {
        try {
            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast")
                append("?latitude=$lat")
                append("&longitude=$lon")
                append("&current=temperature_2m,weather_code,relative_humidity_2m,wind_speed_10m")
                append("&timezone=Asia/Jakarta")
            }
            val json = fetchJsonWithRetry(url)
            parseOpenMeteoResponse(json, cityName)
        } catch (e: Exception) {
            buildWeatherFallback(cityName)
        }
    }

    private fun parseOpenMeteoResponse(json: String, cityName: String): WeatherInfo {
        return try {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val tempC = current.getDouble("temperature_2m").toInt()
            val wmoCode = current.getInt("weather_code")
            val humidity = current.optInt("relative_humidity_2m", -1)
            val windSpeed = current.optDouble("wind_speed_10m", -1.0)

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
        } catch (e: Exception) {
            buildWeatherFallback(cityName)
        }
    }

    // ── BMKG earthquake ───────────────────────────────────────────────────────

    /**
     * BMKG autogempa.json structure:
     * {
     *   "Infogempa": {
     *     "gempa": {
     *       "Tanggal": "...", "Jam": "...", "Magnitude": "...",
     *       "Kedalaman": "...", "Wilayah": "...", "Dirasakan": "..."
     *     }
     *   }
     * }
     */
    private fun parseEarthquake(json: String): EarthquakeInfo? {
        return try {
            val gempa = JSONObject(json)
                .getJSONObject("Infogempa")
                .getJSONObject("gempa")

            EarthquakeInfo(
                magnitude = gempa.optString("Magnitude", "--"),
                location = gempa.optString("Wilayah", "--"),
                depth = gempa.optString("Kedalaman", "--"),
                time = "${gempa.optString("Tanggal", "")} ${gempa.optString("Jam", "")}".trim(),
                felt = gempa.optString("Dirasakan", "Tidak dirasakan")
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * BMKG gempaterkini.json structure:
     * {
     *   "Infogempa": {
     *     "gempa": [
     *       { "Magnitude": "5.2", "Wilayah": "...", "Tanggal": "...", "Jam": "...", ... }
     *     ]
     *   }
     * }
     *
     * Filters to only significant quakes (M >= 5.0) and returns as BmkgWarning list.
     */
    private fun parseRecentQuakeWarnings(json: String): List<BmkgWarning> {
        return try {
            val gempaArray = JSONObject(json)
                .getJSONObject("Infogempa")
                .getJSONArray("gempa")

            val warnings = mutableListOf<BmkgWarning>()
            for (i in 0 until minOf(gempaArray.length(), 5)) {
                val g = gempaArray.getJSONObject(i)
                val mag = g.optString("Magnitude", "0").toDoubleOrNull() ?: 0.0
                if (mag >= 5.0) {
                    val severity = when {
                        mag >= 7.0 -> WarningSeverity.DANGER
                        mag >= 5.5 -> WarningSeverity.WARNING
                        else -> WarningSeverity.INFO
                    }
                    warnings.add(
                        BmkgWarning(
                            type = "Gempa Bumi",
                            message = "M ${"%.1f".format(mag)} – ${g.optString("Wilayah", "--")}",
                            severity = severity,
                            time = "${g.optString("Tanggal", "")} ${g.optString("Jam", "")}".trim()
                        )
                    )
                }
            }
            warnings
        } catch (_: Exception) {
            emptyList()
        }
    }

    // ── HTTP helper with retry ────────────────────────────────────────────────

    /**
     * Fetches JSON with exponential backoff retry.
     * 3 attempts: immediate → 1s delay → 2s delay.
     */
    private suspend fun fetchJsonWithRetry(
        urlString: String,
        maxRetries: Int = 3
    ): String {
        var lastException: Exception? = null
        repeat(maxRetries) { attempt ->
            try {
                return fetchJson(urlString)
            } catch (e: Exception) {
                lastException = e
                if (attempt < maxRetries - 1) {
                    delay(1000L * (1 shl attempt)) // 1s, 2s
                }
            }
        }
        throw lastException ?: Exception("Failed after $maxRetries attempts")
    }

    private fun fetchJson(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = 10_000
            conn.readTimeout = 10_000
            conn.requestMethod = "GET"
            conn.setRequestProperty("Accept", "application/json")
            conn.connect()
            if (conn.responseCode != HttpURLConnection.HTTP_OK) {
                throw Exception("HTTP ${conn.responseCode}")
            }
            conn.inputStream.bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
    }

    // ── WMO weather code mappings ─────────────────────────────────────────────
    // Reference: https://open-meteo.com/en/docs#weathervariables

    fun wmoCodeToCondition(code: Int): String = when (code) {
        0 -> "Cerah"
        1 -> "Cerah Berawan"
        2 -> "Berawan Sebagian"
        3 -> "Berawan"
        45, 48 -> "Berkabut"
        51, 53 -> "Gerimis"
        55 -> "Gerimis Lebat"
        56, 57 -> "Gerimis Beku"
        61, 63 -> "Hujan Ringan"
        65 -> "Hujan Lebat"
        66, 67 -> "Hujan Beku"
        71, 73 -> "Salju Ringan"
        75, 77 -> "Salju Lebat"
        80, 81 -> "Hujan Lokal"
        82 -> "Hujan Deras"
        85, 86 -> "Hujan Salju"
        95 -> "Badai Petir"
        96, 99 -> "Badai Petir Lebat"
        else -> "Berawan"
    }

    /** Maps WMO code to a weather emoji for compact display. */
    fun wmoCodeToEmoji(code: Int): String = when (code) {
        0 -> "☀️"
        1 -> "🌤️"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        51, 53, 55, 56, 57 -> "🌦️"
        61, 63, 80, 81 -> "🌧️"
        65, 66, 67, 82 -> "🌧️"
        71, 73, 75, 77, 85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "☁️"
    }

    /**
     * Maps WMO weather code + temperature to a realistic risk assessment.
     *
     * Tiers:
     *   🟢 "Kondisi Normal"       — clear, partly cloudy, overcast, light fog
     *   🔵 "Perlu Perhatian"      — drizzle, moderate fog, light snow
     *   🟡 "Waspada Hujan"        — moderate rain, showers
     *   🔴 "Risiko Tinggi"        — heavy rain, thunderstorms, extreme temp
     *
     * The default/else branch is SAFE (green), not warning.
     */
    private fun wmoCodeToRisk(code: Int, tempC: Int): Pair<String, Color> = when {
        // ── DANGER (Red) — genuinely hazardous ──────────────────────────
        code in listOf(95, 96, 99) -> Pair("Risiko Petir Tinggi", EmergencyRed)
        code == 65 || code == 82   -> Pair("Risiko Banjir Tinggi", EmergencyRed)
        tempC >= 40                -> Pair("Suhu Berbahaya", EmergencyRed)

        // ── WARNING (Orange) — needs attention ──────────────────────────
        code in listOf(61, 63, 80, 81) -> Pair("Waspada Hujan", WarningOrange)
        code in listOf(66, 67)         -> Pair("Waspada Hujan Beku", WarningOrange)
        code in listOf(75, 77, 86)     -> Pair("Waspada Salju Lebat", WarningOrange)
        tempC >= 36                    -> Pair("Suhu Panas Ekstrem", WarningOrange)

        // ── ATTENTION (Blue) — mild caution ─────────────────────────────
        code in listOf(51, 53)     -> Pair("Perlu Perhatian", MitigationBlue)
        code == 55                 -> Pair("Gerimis Lebat", MitigationBlue)
        code in listOf(56, 57)     -> Pair("Perlu Perhatian", MitigationBlue)
        code in listOf(45, 48)     -> Pair("Jarak Pandang Terbatas", MitigationBlue)
        code in listOf(71, 73)     -> Pair("Perlu Perhatian", MitigationBlue)
        code == 85                 -> Pair("Perlu Perhatian", MitigationBlue)

        // ── NORMAL (Green) — safe conditions ────────────────────────────
        code in listOf(0, 1, 2, 3) -> Pair("Kondisi Normal", VolunteerGreen)

        // ── Default: assume normal — NOT "Waspada" ──────────────────────
        else -> Pair("Kondisi Normal", VolunteerGreen)
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
