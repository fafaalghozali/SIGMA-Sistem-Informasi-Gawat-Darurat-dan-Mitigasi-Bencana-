package com.mahasiswa.sigma.data.news

import com.mahasiswa.sigma.data.model.NewsItem














object LocationPrioritizer {

    



    private val NEARBY_REGIONS: Map<String, Set<String>> = mapOf(
        
        "surakarta" to setOf(
            "surakarta", "solo", "sukoharjo", "klaten", "boyolali",
            "karanganyar", "sragen", "wonogiri", "soloraya", "jawa tengah",
            "eks karesidenan surakarta"
        ),
        "solo" to setOf(
            "surakarta", "solo", "sukoharjo", "klaten", "boyolali",
            "karanganyar", "sragen", "wonogiri", "soloraya", "jawa tengah"
        ),
        
        "semarang" to setOf(
            "semarang", "demak", "kendal", "ungaran", "salatiga",
            "jawa tengah", "pantura"
        ),
        
        "yogyakarta" to setOf(
            "yogyakarta", "jogja", "sleman", "bantul", "gunung kidul",
            "kulon progo", "jawa tengah", "merapi"
        ),
        
        "jakarta" to setOf(
            "jakarta", "dki", "bogor", "depok", "tangerang", "bekasi",
            "jabodetabek", "banten"
        ),
        
        "bandung" to setOf(
            "bandung", "cimahi", "sumedang", "garut", "jawa barat"
        ),
        
        "surabaya" to setOf(
            "surabaya", "sidoarjo", "gresik", "mojokerto", "jawa timur"
        ),
        
        "medan" to setOf(
            "medan", "deli serdang", "binjai", "sumatera utara"
        ),
        
        "makassar" to setOf(
            "makassar", "gowa", "maros", "sulawesi selatan"
        ),
        
        "denpasar" to setOf(
            "denpasar", "bali", "badung", "gianyar", "tabanan"
        )
    )

    











    fun prioritize(items: List<NewsItem>, userCityName: String): List<NewsItem> {
        val cityKey = userCityName.lowercase().trim()
        val nearbyAliases = NEARBY_REGIONS[cityKey]
            ?: inferNearbyFromCityName(cityKey)

        if (nearbyAliases.isEmpty()) return items

        
        return items.sortedWith(
            compareByDescending { item ->
                isNearby(item, nearbyAliases)
            }
        )
    }

    

    


    private fun isNearby(item: NewsItem, nearbyAliases: Set<String>): Boolean {
        val searchText = buildString {
            append(item.title.lowercase())
            append(" ")
            append(item.region?.lowercase() ?: "")
        }
        return nearbyAliases.any { alias -> searchText.contains(alias) }
    }

    



    private fun inferNearbyFromCityName(cityKey: String): Set<String> {
        
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
        
        return setOf(cityKey)
    }
}
