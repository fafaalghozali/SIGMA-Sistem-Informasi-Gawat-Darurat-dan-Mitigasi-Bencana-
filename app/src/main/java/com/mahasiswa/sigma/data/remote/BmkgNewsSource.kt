package com.mahasiswa.sigma.data.remote

import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.model.RawRssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL












object BmkgNewsSource {

    private const val AUTOGEMPA_URL =
        "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
    private const val GEMPATERKINI_URL =
        "https://data.bmkg.go.id/DataMKG/TEWS/gempaterkini.json"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000

    




    suspend fun fetchBmkgNews(): List<RawRssItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawRssItem>()

        
        try {
            val json = fetchJson(AUTOGEMPA_URL)
            parseAutogempa(json)?.let { results.add(it) }
        } catch (_: Exception) {}

        
        try {
            val json = fetchJson(GEMPATERKINI_URL)
            results.addAll(parseGempaterkini(json))
        } catch (_: Exception) {}

        results
    }

    

    





    private fun parseAutogempa(json: String): RawRssItem? {
        return try {
            val gempa = JSONObject(json)
                .getJSONObject("Infogempa")
                .getJSONObject("gempa")

            val mag = gempa.optString("Magnitude", "")
            val wilayah = gempa.optString("Wilayah", "")
            val tanggal = gempa.optString("Tanggal", "")
            val jam = gempa.optString("Jam", "")
            val kedalaman = gempa.optString("Kedalaman", "")
            val dirasakan = gempa.optString("Dirasakan", "")
            val shakemap = gempa.optString("Shakemap", "")

            val magDouble = mag.toDoubleOrNull() ?: 0.0
            val severity = bmkgMagnitudeToSeverityLabel(magDouble)

            val title = "Gempa M $mag – $wilayah"
            val description = buildString {
                append("Gempa bumi berkekuatan M $mag mengguncang $wilayah ")
                append("pada kedalaman $kedalaman. ")
                if (dirasakan.isNotBlank() && dirasakan != "Tidak dirasakan") {
                    append("Dirasakan: $dirasakan. ")
                }
                append("Waktu: $tanggal $jam WIB.")
            }

            
            val imageUrl = if (shakemap.isNotBlank()) {
                "https://data.bmkg.go.id/DataMKG/TEWS/$shakemap"
            } else null

            RawRssItem(
                guid = "bmkg-autogempa-${tanggal}-${jam}".replace(" ", "-"),
                title = title,
                description = description,
                link = "https://www.bmkg.go.id/gempabumi/gempabumi-terkini.bmkg",
                pubDate = "$tanggal $jam",
                imageUrl = imageUrl,
                sourceName = "BMKG",
                isOfficial = true
            )
        } catch (_: Exception) {
            null
        }
    }

    




    private fun parseGempaterkini(json: String): List<RawRssItem> {
        return try {
            val arr = JSONObject(json)
                .getJSONObject("Infogempa")
                .getJSONArray("gempa")

            val result = mutableListOf<RawRssItem>()
            
            for (i in 1 until minOf(arr.length(), 6)) {
                val g = arr.getJSONObject(i)
                val mag = g.optString("Magnitude", "0").toDoubleOrNull() ?: 0.0
                if (mag < 5.0) continue   

                val wilayah = g.optString("Wilayah", "--")
                val tanggal = g.optString("Tanggal", "")
                val jam = g.optString("Jam", "")
                val kedalaman = g.optString("Kedalaman", "--")
                val dirasakan = g.optString("Dirasakan", "")
                val severity = bmkgMagnitudeToSeverityLabel(mag)

                val title = "Gempa M ${"%.1f".format(mag)} – $wilayah"
                val description = buildString {
                    append("Gempa M ${"%.1f".format(mag)} mengguncang $wilayah ")
                    append("kedalaman $kedalaman. ")
                    if (dirasakan.isNotBlank() && dirasakan != "Tidak dirasakan") {
                        append("Dirasakan: $dirasakan.")
                    }
                }

                result.add(
                    RawRssItem(
                        guid = "bmkg-terkini-${i}-${tanggal}-${jam}".replace(" ", "-"),
                        title = title,
                        description = description,
                        link = "https://www.bmkg.go.id/gempabumi/gempabumi-terkini.bmkg",
                        pubDate = "$tanggal $jam",
                        imageUrl = null,
                        sourceName = "BMKG",
                        isOfficial = true
                    )
                )
            }
            result
        } catch (_: Exception) {
            emptyList()
        }
    }

    

    private fun bmkgMagnitudeToSeverityLabel(mag: Double): String = when {
        mag >= 7.0 -> "DARURAT"
        mag >= 5.5 -> "WASPADA"
        else -> "INFO"
    }

    private fun fetchJson(urlString: String): String {
        val conn = URL(urlString).openConnection() as HttpURLConnection
        return try {
            conn.connectTimeout = CONNECT_TIMEOUT_MS
            conn.readTimeout = READ_TIMEOUT_MS
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
}
