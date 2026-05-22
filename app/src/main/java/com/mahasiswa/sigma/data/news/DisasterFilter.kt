package com.mahasiswa.sigma.data.news

import androidx.compose.ui.graphics.Color
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.model.RawRssItem
import com.mahasiswa.sigma.ui.theme.EmergencyRed
import com.mahasiswa.sigma.ui.theme.MitigationBlue
import com.mahasiswa.sigma.ui.theme.WarningOrange
import java.text.SimpleDateFormat
import java.util.Locale











object DisasterFilter {

    

    private val DISASTER_KEYWORDS = setOf(
        "gempa bumi", "banjir bandang", "tanah longsor", "angin puting beliung", "evakuasi warga",
        "banjir", "gempa", "longsor", "tsunami", "kebakaran", "gunung meletus",
        "cuaca ekstrem", "bmkg", "bnpb", "hujan lebat", "angin kencang", "pohon tumbang",
        "erupsi", "letusan", "tornado", "puting beliung", "siklon", "badai",
        "abrasi", "likuifaksi", "lahar", "kekeringan", "darurat", "evakuasi", "pengungsi",
        "korban", "waspada", "siaga", "peringatan", "tanggap", "rob", "gelombang tinggi",
        "kebocoran", "ledakan", "kecelakaan massal"
    )

    private val EXCLUSION_KEYWORDS = setOf(
        "sepak bola", "transfer pemain", "olahraga", "artis", "hiburan", 
        "konser", "liga", "pertandingan", "viral", "selebriti",
        "transfer", "pemain", "gol", "bola basket", "tenis", "bulutangkis",
        "turnamen", "juara", "piala", "klasemen", "degradasi",
        "gosip", "film", "musik", "sinetron", "drakor", "drama korea", "serial",
        "saham", "investasi", "bursa", "kurs", "dolar", "pilkada", "pemilu",
        "kampanye", "esports", "gaming", "divonis", "jambret", "wisata", 
        "waisak", "kriminal", "pencurian", "sidak", "lapas", "ditjenpas",
        "politik umum", "apel", "polisi", "polda", "polres", "kapolres", 
        "kapolda", "mabes", "gas", "tabung gas", "ledakan gas", "ledakan tabung",
        "disekap", "penculikan", "pembunuhan", "narkoba", "penganiayaan"
    )

    

    private val DARURAT_TRIGGERS = setOf(
        "tsunami", "likuifaksi", "lahar", "erupsi", "letusan", "meletus",
        "meninggal", "korban jiwa", "tewas", "wafat",
        "darurat nasional", "banjir besar", "banjir bandang",
        "gempa besar", "gempa dahsyat", "gempa kuat", "gempa destruktif",
        "longsor besar", "gunung meletus"
    )

    private val WASPADA_TRIGGERS = setOf(
        "banjir", "longsor", "gempa", "hujan lebat", "angin kencang",
        "gelombang tinggi", "siaga", "waspada", "peringatan dini",
        "evakuasi", "pengungsi"
    )

    

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
        
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")),
        SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")),
    )

    

    








    fun filter(rawItems: List<RawRssItem>): List<NewsItem> {
        val now = System.currentTimeMillis()
        val maxAgeMs = 7L * 24 * 60 * 60 * 1000

        return rawItems.mapNotNull { raw ->
            val title = raw.title.lowercase()
            val fullText = "$title ${raw.description.lowercase()}"

            
            if (EXCLUSION_KEYWORDS.any { fullText.contains(it) }) {
                return@mapNotNull null
            }

            
            val hasDisasterInTitle = DISASTER_KEYWORDS.any { title.contains(it) }
            if (!hasDisasterInTitle && !raw.isOfficial) {
                return@mapNotNull null
            }

            val publishedAt = parsePubDate(raw.pubDate)
            
            
            if ((now - publishedAt) > maxAgeMs) {
                return@mapNotNull null
            }

            val severity = classifySeverity(fullText, raw.isOfficial)
            val region = detectRegion(fullText)
            val relativeTime = relativeTimeString(publishedAt)

            NewsItem(
                id = raw.guid,
                title = raw.title,
                time = relativeTime,
                publishedAt = publishedAt,
                category = severity.name,
                categoryColor = severity.toColor(),
                imageUrl = raw.imageUrl,
                source = raw.sourceName,
                link = raw.link,
                severity = severity,
                isOfficial = raw.isOfficial,
                region = region
            )
        }.sortedByDescending { it.publishedAt }
    }

    

    private fun classifySeverity(text: String, isOfficial: Boolean): NewsSeverity {
        
        for (kw in DARURAT_TRIGGERS) {
            if (text.contains(kw)) return NewsSeverity.DARURAT
        }

        
        if (isOfficial) {
            val magMatch = Regex("""m\s*([\d.]+)""").find(text)
            val mag = magMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            if (mag >= 7.0) return NewsSeverity.DARURAT
            if (mag >= 5.5) return NewsSeverity.WASPADA
        }

        
        for (kw in WASPADA_TRIGGERS) {
            if (text.contains(kw)) return NewsSeverity.WASPADA
        }

        return NewsSeverity.INFO
    }

    

    
    private fun detectRegion(text: String): String? {
        val regionMap = mapOf(
            "jawa tengah" to "Jawa Tengah",
            "jawa barat" to "Jawa Barat",
            "jawa timur" to "Jawa Timur",
            "jakarta" to "DKI Jakarta",
            "dki" to "DKI Jakarta",
            "sumatera" to "Sumatera",
            "sulawesi" to "Sulawesi",
            "kalimantan" to "Kalimantan",
            "papua" to "Papua",
            "ntt" to "NTT",
            "ntb" to "NTB",
            "bali" to "Bali",
            "surakarta" to "Surakarta",
            "solo" to "Surakarta",
            "yogyakarta" to "Yogyakarta",
            "jogja" to "Yogyakarta",
            "semarang" to "Semarang",
            "bandung" to "Bandung",
            "medan" to "Medan",
            "makassar" to "Makassar",
            "surabaya" to "Surabaya",
            "maluku" to "Maluku",
            "aceh" to "Aceh",
            "riau" to "Riau",
            "lombok" to "Lombok",
            "flores" to "Flores",
            "ternate" to "Ternate",
            "ambon" to "Ambon"
        )
        for ((key, display) in regionMap) {
            if (text.contains(key)) return display
        }
        return null
    }

    

    fun parsePubDate(pubDate: String): Long {
        if (pubDate.isBlank()) return System.currentTimeMillis()
        for (fmt in DATE_FORMATS) {
            try {
                val date = fmt.parse(pubDate.trim())
                if (date != null) return date.time
            } catch (_: Exception) {}
        }
        return System.currentTimeMillis()
    }

    fun relativeTimeString(epochMs: Long): String {
        val diff = System.currentTimeMillis() - epochMs
        return when {
            diff < 60_000L               -> "Baru saja"
            diff < 3_600_000L            -> "${diff / 60_000} mnt lalu"
            diff < 86_400_000L           -> "${diff / 3_600_000} jam lalu"
            diff < 2 * 86_400_000L       -> "Kemarin"
            else                         -> "${diff / 86_400_000} hari lalu"
        }
    }

}

fun NewsSeverity.toColor(): Color = when (this) {
    NewsSeverity.DARURAT -> EmergencyRed
    NewsSeverity.WASPADA -> WarningOrange
    NewsSeverity.INFO    -> MitigationBlue
}
