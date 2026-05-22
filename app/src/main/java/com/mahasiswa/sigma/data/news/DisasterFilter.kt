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

/**
 * Disaster content filter and severity classifier.
 *
 * Uses a strict boolean filtering logic:
 * - Articles MUST contain at least one disaster keyword (or be an official source)
 * - Articles MUST NOT contain any false-positive exclusion keywords
 *
 * Severity is classified independently based on the
 * highest-severity trigger word found in the content.
 */
object DisasterFilter {

    // ── Disaster & Exclusion Keywords ─────────────────────────────────────────

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
        "politik umum"
    )

    // ── Severity trigger words ────────────────────────────────────────────────

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

    // ── Date parsers for pubDate → epoch ms ──────────────────────────────────

    private val DATE_FORMATS = listOf(
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.ENGLISH),
        SimpleDateFormat("dd MMM yyyy HH:mm:ss Z", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.ENGLISH),
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.ENGLISH),
        // BMKG format: "21 Mei 2026" / "08:32:00 WIB"
        SimpleDateFormat("dd MMM yyyy HH:mm:ss", Locale("id", "ID")),
        SimpleDateFormat("dd MMMM yyyy HH:mm:ss", Locale("id", "ID")),
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Filters a list of raw RSS items, returning only disaster-relevant [NewsItem]s.
     *
     * Items are displayed ONLY if:
     * - A disaster keyword exists
     * - AND an unrelated/exclusion keyword does NOT exist.
     *
     * Items are returned newest-first.
     */
    fun filter(rawItems: List<RawRssItem>): List<NewsItem> {
        val now = System.currentTimeMillis()
        val maxAgeMs = 7L * 24 * 60 * 60 * 1000

        return rawItems.mapNotNull { raw ->
            val title = raw.title.lowercase()
            val fullText = "$title ${raw.description.lowercase()}"

            // 1. Strict False-Positive Filtering: Exclude unrelated contexts from full text
            if (EXCLUSION_KEYWORDS.any { fullText.contains(it) }) {
                return@mapNotNull null
            }

            // 2. Title-First Disaster Filtering: Include only if disaster keywords exist in TITLE (or if strictly official)
            val hasDisasterInTitle = DISASTER_KEYWORDS.any { title.contains(it) }
            if (!hasDisasterInTitle && !raw.isOfficial) {
                return@mapNotNull null
            }

            val publishedAt = parsePubDate(raw.pubDate)
            
            // 3. Time Filtering: Only show news from the last 7 days
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

    // ── Severity classification ───────────────────────────────────────────────

    private fun classifySeverity(text: String, isOfficial: Boolean): NewsSeverity {
        // Check for DARURAT triggers first
        for (kw in DARURAT_TRIGGERS) {
            if (text.contains(kw)) return NewsSeverity.DARURAT
        }

        // Check magnitude in BMKG official text
        if (isOfficial) {
            val magMatch = Regex("""m\s*([\d.]+)""").find(text)
            val mag = magMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull() ?: 0.0
            if (mag >= 6.0) return NewsSeverity.DARURAT
            if (mag >= 5.0) return NewsSeverity.WASPADA
        }

        // Check for WASPADA triggers
        for (kw in WASPADA_TRIGGERS) {
            if (text.contains(kw)) return NewsSeverity.WASPADA
        }

        return NewsSeverity.INFO
    }

    // ── Region detection ─────────────────────────────────────────────────────

    /** Detect the first major Indonesian region mentioned in the text. */
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

    // ── Date helpers ─────────────────────────────────────────────────────────

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

    private fun NewsSeverity.toColor(): Color = when (this) {
        NewsSeverity.DARURAT -> EmergencyRed
        NewsSeverity.WASPADA -> WarningOrange
        NewsSeverity.INFO    -> MitigationBlue
    }
}
