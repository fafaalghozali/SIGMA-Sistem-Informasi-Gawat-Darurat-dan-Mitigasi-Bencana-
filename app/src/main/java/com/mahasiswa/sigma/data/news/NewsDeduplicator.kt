package com.mahasiswa.sigma.data.news

import com.mahasiswa.sigma.data.model.NewsItem

/**
 * Removes near-duplicate news items using Jaccard similarity on word bigrams.
 *
 * Two articles are considered duplicates if their title bigram similarity
 * exceeds [SIMILARITY_THRESHOLD]. When duplicates are found, the "better"
 * article is kept using the following priority order:
 *   1. isOfficial = true (BMKG/BNPB always preferred)
 *   2. More severe (DARURAT > WASPADA > INFO)
 *   3. Newer publishedAt timestamp
 */
object NewsDeduplicator {

    private const val SIMILARITY_THRESHOLD = 0.60

    /** Indonesian stop words to strip before computing bigrams. */
    private val STOP_WORDS = setOf(
        "di", "ke", "dari", "yang", "dan", "dengan", "untuk", "dalam",
        "pada", "oleh", "ini", "itu", "atau", "juga", "sudah", "telah",
        "akan", "bisa", "ada", "tidak", "lebih", "sangat", "baru",
        "saat", "ketika", "karena", "setelah", "sebelum", "akibat",
        "warga", "pihak", "dua", "tiga", "satu", "lima", "puluh"
    )

    /**
     * Deduplicates a list of [NewsItem]s.
     * Items are processed in order; later items that are "near-duplicates"
     * of an already-accepted item are discarded (or the better one is kept).
     */
    fun deduplicate(items: List<NewsItem>): List<NewsItem> {
        if (items.size <= 1) return items

        // Group items by similarity clusters
        val accepted = mutableListOf<NewsItem>()

        for (candidate in items) {
            val duplicateIndex = accepted.indexOfFirst { existing ->
                jaccardSimilarity(
                    normalizeTitle(candidate.title),
                    normalizeTitle(existing.title)
                ) >= SIMILARITY_THRESHOLD
            }

            if (duplicateIndex == -1) {
                // No duplicate found — accept this item
                accepted.add(candidate)
            } else {
                // Duplicate found — keep the "better" one
                val existing = accepted[duplicateIndex]
                val better = pickBetter(candidate, existing)
                if (better !== existing) {
                    accepted[duplicateIndex] = better
                }
            }
        }

        return accepted
    }

    // ── Bigram Jaccard similarity ─────────────────────────────────────────────

    /**
     * Computes Jaccard similarity between two strings using word bigrams.
     *
     * Jaccard(A, B) = |A ∩ B| / |A ∪ B|
     *
     * Returns 0.0 (no similarity) to 1.0 (identical).
     */
    private fun jaccardSimilarity(a: String, b: String): Double {
        val bigramsA = wordBigrams(a)
        val bigramsB = wordBigrams(b)

        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0

        val intersection = (bigramsA intersect bigramsB).size.toDouble()
        val union = (bigramsA union bigramsB).size.toDouble()

        return if (union == 0.0) 0.0 else intersection / union
    }

    /**
     * Generates a set of consecutive word pairs (bigrams) from a string.
     * e.g. "banjir melanda solo" → {("banjir","melanda"), ("melanda","solo")}
     */
    private fun wordBigrams(text: String): Set<Pair<String, String>> {
        val words = text.split(" ").filter { it.length > 2 }
        if (words.size < 2) return emptySet()
        return (0 until words.size - 1).map { Pair(words[it], words[it + 1]) }.toSet()
    }

    /** Normalizes a title for comparison: lowercase, strip punctuation, remove stops. */
    private fun normalizeTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .joinToString(" ")
    }

    // ── Duplicate resolution ──────────────────────────────────────────────────

    /** Returns the "better" of two duplicate items using priority rules. */
    private fun pickBetter(a: NewsItem, b: NewsItem): NewsItem {
        // 1. Official source wins
        if (a.isOfficial && !b.isOfficial) return a
        if (b.isOfficial && !a.isOfficial) return b

        // 2. Higher severity wins
        val severityOrder = mapOf(
            com.mahasiswa.sigma.data.model.NewsSeverity.DARURAT to 2,
            com.mahasiswa.sigma.data.model.NewsSeverity.WASPADA to 1,
            com.mahasiswa.sigma.data.model.NewsSeverity.INFO to 0
        )
        val aScore = severityOrder[a.severity] ?: 0
        val bScore = severityOrder[b.severity] ?: 0
        if (aScore != bScore) return if (aScore > bScore) a else b

        // 3. Newer wins
        return if (a.publishedAt >= b.publishedAt) a else b
    }
}
