package com.mahasiswa.sigma.data.remote

import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.model.RawRssItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Converts BMKG's official JSON earthquake endpoints into [RawRssItem] objects
 * so they flow through the same pipeline as RSS news.
 *
 * BMKG endpoints used:
 *   - autogempa.json  → latest single earthquake
 *   - gempaterkini.json → 15 most recent felt earthquakes
 *
 * Official BMKG items always receive isOfficial=true and bypass keyword scoring
 * (they are guaranteed disaster-relevant by definition).
 */
object BmkgNewsSource {

    private const val AUTOGEMPA_URL =
        "https://data.bmkg.go.id/DataMKG/TEWS/autogempa.json"
    private const val GEMPATERKINI_URL =
        "https://data.bmkg.go.id/DataMKG/TEWS/gempaterkini.json"

    private const val CONNECT_TIMEOUT_MS = 8_000
    private const val READ_TIMEOUT_MS = 10_000

    /**
     * Fetches BMKG official earthquake data and returns as [RawRssItem] list.
     * Returns empty list on any failure — earthquake card in the dashboard
     * handles display separately.
     */
    suspend fun fetchBmkgNews(): List<RawRssItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawRssItem>()

        // 1. Latest single earthquake (autogempa)
        try {
            val json = fetchJson(AUTOGEMPA_URL)
            parseAutogempa(json)?.let { results.add(it) }
        } catch (_: Exception) {}

        // 2. Recent significant earthquakes (gempaterkini)
        try {
            val json = fetchJson(GEMPATERKINI_URL)
            results.addAll(parseGempaterkini(json))
        } catch (_: Exception) {}

        results
    }

    // ── Parsers ───────────────────────────────────────────────────────────────

    /**
     * BMKG autogempa.json → single latest earthquake as RawRssItem.
     *
     * Shape: { "Infogempa": { "gempa": { "Tanggal", "Jam", "Magnitude",
     *           "Kedalaman", "Wilayah", "Dirasakan", "Shakemap" } } }
     */
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

            val title = "[$severity] Gempa M $mag – $wilayah"
            val description = buildString {
                append("Gempa bumi berkekuatan M $mag mengguncang $wilayah ")
                append("pada kedalaman $kedalaman. ")
                if (dirasakan.isNotBlank() && dirasakan != "Tidak dirasakan") {
                    append("Dirasakan: $dirasakan. ")
                }
                append("Waktu: $tanggal $jam WIB.")
            }

            // BMKG shakemap image URL
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

    /**
     * BMKG gempaterkini.json → up to 5 recent significant earthquakes (M≥5.0).
     *
     * Shape: { "Infogempa": { "gempa": [ {...}, {...} ] } }
     */
    private fun parseGempaterkini(json: String): List<RawRssItem> {
        return try {
            val arr = JSONObject(json)
                .getJSONObject("Infogempa")
                .getJSONArray("gempa")

            val result = mutableListOf<RawRssItem>()
            // Skip index 0 — same as autogempa (already added above)
            for (i in 1 until minOf(arr.length(), 6)) {
                val g = arr.getJSONObject(i)
                val mag = g.optString("Magnitude", "0").toDoubleOrNull() ?: 0.0
                if (mag < 5.0) continue   // Only include significant quakes

                val wilayah = g.optString("Wilayah", "--")
                val tanggal = g.optString("Tanggal", "")
                val jam = g.optString("Jam", "")
                val kedalaman = g.optString("Kedalaman", "--")
                val dirasakan = g.optString("Dirasakan", "")
                val severity = bmkgMagnitudeToSeverityLabel(mag)

                val title = "[$severity] Gempa M ${"%.1f".format(mag)} – $wilayah"
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

    // ── Helpers ───────────────────────────────────────────────────────────────

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
