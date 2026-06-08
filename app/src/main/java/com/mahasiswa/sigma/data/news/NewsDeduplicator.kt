package com.mahasiswa.sigma.data.news

import com.mahasiswa.sigma.data.model.NewsItem

object NewsDeduplicator {

    private const val SIMILARITY_THRESHOLD = 0.60

    private val STOP_WORDS = setOf(
        "di", "ke", "dari", "yang", "dan", "dengan", "untuk", "dalam",
        "pada", "oleh", "ini", "itu", "atau", "juga", "sudah", "telah",
        "akan", "bisa", "ada", "tidak", "lebih", "sangat", "baru",
        "saat", "ketika", "karena", "setelah", "sebelum", "akibat",
        "warga", "pihak", "dua", "tiga", "satu", "lima", "puluh"
    )

    fun deduplicate(items: List<NewsItem>): List<NewsItem> {
        if (items.size <= 1) return items

        val accepted = mutableListOf<NewsItem>()

        for (candidate in items) {
            val duplicateIndex = accepted.indexOfFirst { existing ->
                jaccardSimilarity(
                    normalizeTitle(candidate.title),
                    normalizeTitle(existing.title)
                ) >= SIMILARITY_THRESHOLD
            }

            if (duplicateIndex == -1) {

                accepted.add(candidate)
            } else {

                val existing = accepted[duplicateIndex]
                val better = pickBetter(candidate, existing)
                if (better !== existing) {
                    accepted[duplicateIndex] = better
                }
            }
        }

        return accepted
    }

    private fun jaccardSimilarity(a: String, b: String): Double {
        val bigramsA = wordBigrams(a)
        val bigramsB = wordBigrams(b)

        if (bigramsA.isEmpty() && bigramsB.isEmpty()) return 1.0
        if (bigramsA.isEmpty() || bigramsB.isEmpty()) return 0.0

        val intersection = (bigramsA intersect bigramsB).size.toDouble()
        val union = (bigramsA union bigramsB).size.toDouble()

        return if (union == 0.0) 0.0 else intersection / union
    }

    private fun wordBigrams(text: String): Set<Pair<String, String>> {
        val words = text.split(" ").filter { it.length > 2 }
        if (words.size < 2) return emptySet()
        return (0 until words.size - 1).map { Pair(words[it], words[it + 1]) }.toSet()
    }

    private fun normalizeTitle(title: String): String {
        return title
            .lowercase()
            .replace(Regex("[^a-z0-9\\s]"), " ")
            .split("\\s+".toRegex())
            .filter { it.isNotBlank() && it !in STOP_WORDS }
            .joinToString(" ")
    }

    private fun pickBetter(a: NewsItem, b: NewsItem): NewsItem {

        if (a.isOfficial && !b.isOfficial) return a
        if (b.isOfficial && !a.isOfficial) return b

        val severityOrder = mapOf(
            com.mahasiswa.sigma.data.model.NewsSeverity.DARURAT to 2,
            com.mahasiswa.sigma.data.model.NewsSeverity.WASPADA to 1,
            com.mahasiswa.sigma.data.model.NewsSeverity.INFO to 0
        )
        val aScore = severityOrder[a.severity] ?: 0
        val bScore = severityOrder[b.severity] ?: 0
        if (aScore != bScore) return if (aScore > bScore) a else b

        return if (a.publishedAt >= b.publishedAt) a else b
    }
}
