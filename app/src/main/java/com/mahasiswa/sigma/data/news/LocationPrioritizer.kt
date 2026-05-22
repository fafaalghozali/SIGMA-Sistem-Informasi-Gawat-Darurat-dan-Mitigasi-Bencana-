package com.mahasiswa.sigma.data.news

import com.mahasiswa.sigma.data.model.NewsItem

/**
 * Boosts the sort priority of news articles that mention regions
 * near the user's current location.
 *
 * The user's location is derived from the already-computed reverse-geocoded
 * city name (from WeatherRepository) — no additional API calls needed.
 *
 * Strategy:
 *   - Each city/regency has a set of nearby region aliases
 *   - News titles/regions mentioning a nearby place get a priority boost
 *   - Boosted items bubble to the top of the list
 *   - Items with the same priority maintain their original order (stable sort)
 */
object LocationPrioritizer {

    /**
     * Map of canonical city name (lowercase) → set of regional aliases that
     * should be considered "nearby". Covers major Indonesian disaster-prone areas.
     */
    private val NEARBY_REGIONS: Map<String, Set<String>> = mapOf(
        // Solo / Surakarta metro
        "surakarta" to setOf(
            "surakarta", "solo", "sukoharjo", "klaten", "boyolali",
            "karanganyar", "sragen", "wonogiri", "soloraya", "jawa tengah",
            "eks karesidenan surakarta"
        ),
        "solo" to setOf(
            "surakarta", "solo", "sukoharjo", "klaten", "boyolali",
            "karanganyar", "sragen", "wonogiri", "soloraya", "jawa tengah"
        ),
        // Semarang
        "semarang" to setOf(
            "semarang", "demak", "kendal", "ungaran", "salatiga",
            "jawa tengah", "pantura"
        ),
        // Yogyakarta
        "yogyakarta" to setOf(
            "yogyakarta", "jogja", "sleman", "bantul", "gunung kidul",
            "kulon progo", "jawa tengah", "merapi"
        ),
        // Jakarta
        "jakarta" to setOf(
            "jakarta", "dki", "bogor", "depok", "tangerang", "bekasi",
            "jabodetabek", "banten"
        ),
        // Bandung
        "bandung" to setOf(
            "bandung", "cimahi", "sumedang", "garut", "jawa barat"
        ),
        // Surabaya
        "surabaya" to setOf(
            "surabaya", "sidoarjo", "gresik", "mojokerto", "jawa timur"
        ),
        // Medan
        "medan" to setOf(
            "medan", "deli serdang", "binjai", "sumatera utara"
        ),
        // Makassar
        "makassar" to setOf(
            "makassar", "gowa", "maros", "sulawesi selatan"
        ),
        // Denpasar / Bali
        "denpasar" to setOf(
            "denpasar", "bali", "badung", "gianyar", "tabanan"
        )
    )

    /**
     * Sorts [items] so that articles mentioning nearby regions appear first.
     *
     * Items are sorted into two tiers:
     *   1. Nearby region match → priority 1 (appears first)
     *   2. No region match → priority 0 (appears after)
     *
     * Within each tier, the original order (newest-first from DisasterFilter) is preserved.
     *
     * @param items Already-filtered disaster news items
     * @param userCityName The city name from reverse geocoding (e.g. "Surakarta")
     */
    fun prioritize(items: List<NewsItem>, userCityName: String): List<NewsItem> {
        val cityKey = userCityName.lowercase().trim()
        val nearbyAliases = NEARBY_REGIONS[cityKey]
            ?: inferNearbyFromCityName(cityKey)

        if (nearbyAliases.isEmpty()) return items

        // Stable sort: nearby items first, rest follow in original order
        return items.sortedWith(
            compareByDescending { item ->
                isNearby(item, nearbyAliases)
            }
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /**
     * Returns true if the news item's title or region mentions any nearby alias.
     */
    private fun isNearby(item: NewsItem, nearbyAliases: Set<String>): Boolean {
        val searchText = buildString {
            append(item.title.lowercase())
            append(" ")
            append(item.region?.lowercase() ?: "")
        }
        return nearbyAliases.any { alias -> searchText.contains(alias) }
    }

    /**
     * Fallback: infer nearby regions from a city name not in the explicit map.
     * Extracts the province from common Indonesian city name patterns.
     */
    private fun inferNearbyFromCityName(cityKey: String): Set<String> {
        // Try to match province-level keywords
        val provinceHints = mapOf(
            "jawa"     to setOf("jawa tengah", "jawa barat", "jawa timur"),
            "sumatera" to setOf("sumatera utara", "sumatera barat", "sumatera selatan"),
            "sulawesi" to setOf("sulawesi selatan", "sulawesi tenggara", "sulawesi utara"),
            "kalimantan" to setOf("kalimantan timur", "kalimantan barat"),
            "papua"    to setOf("papua", "papua barat")
        )
        for ((hint, provinces) in provinceHints) {
            if (cityKey.contains(hint)) return provinces + setOf(cityKey)
        }
        // Return just the city itself as a self-match
        return setOf(cityKey)
    }
}
