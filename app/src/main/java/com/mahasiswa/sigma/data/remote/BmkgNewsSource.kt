package com.mahasiswa.sigma.data.remote

import com.mahasiswa.sigma.data.model.RawRssItem
import com.mahasiswa.sigma.data.remote.api.BmkgApiService
import com.mahasiswa.sigma.data.remote.api.GempaDetail
import com.mahasiswa.sigma.data.remote.api.GempaTerkiniItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BmkgNewsSource @Inject constructor(
    private val bmkgApiService: BmkgApiService
) {

    suspend fun fetchBmkgNews(): List<RawRssItem> = withContext(Dispatchers.IO) {
        val results = mutableListOf<RawRssItem>()

        try {
            val autoGempaResponse = bmkgApiService.getAutoGempa()
            parseAutogempa(autoGempaResponse.infogempa.gempa)?.let { results.add(it) }
        } catch (_: Exception) {}

        try {
            val terkiniResponse = bmkgApiService.getGempaTerkini()
            results.addAll(parseGempaterkini(terkiniResponse.infogempa.gempa))
        } catch (_: Exception) {}

        results
    }

    private fun parseAutogempa(gempa: GempaDetail): RawRssItem? {
        return try {
            val mag = gempa.magnitude
            val wilayah = gempa.wilayah
            val tanggal = gempa.tanggal
            val jam = gempa.jam
            val kedalaman = gempa.kedalaman
            val dirasakan = gempa.dirasakan
            val shakemap = gempa.shakemap

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

    private fun parseGempaterkini(gempaList: List<GempaTerkiniItem>): List<RawRssItem> {
        val result = mutableListOf<RawRssItem>()

        for (i in 1 until minOf(gempaList.size, 6)) {
            val g = gempaList[i]
            val mag = g.magnitude.toDoubleOrNull() ?: 0.0
            if (mag < 5.0) continue

            val wilayah = g.wilayah.ifBlank { "--" }
            val tanggal = g.tanggal
            val jam = g.jam
            val kedalaman = g.kedalaman.ifBlank { "--" }
            val dirasakan = g.dirasakan

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
        return result
    }
}
