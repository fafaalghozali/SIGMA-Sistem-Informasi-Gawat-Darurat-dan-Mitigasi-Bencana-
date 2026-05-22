package com.mahasiswa.sigma.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class DashboardMenuModel(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector,
    val category: MenuCategory = MenuCategory.OTHERS
)

enum class MenuCategory {
    EMERGENCY, VOLUNTEER, MITIGATION, SEARCH, OTHERS
}

data class NewsItem(
    val id: String,                // guid or link-based stable ID
    val title: String,
    val time: String,              // human-relative: "10 mnt lalu"
    val publishedAt: Long = 0L,    // epoch ms for sorting/dedup
    val category: String,          // "DARURAT" / "WASPADA" / "INFO"
    val categoryColor: Color = Color.Gray,
    val imageUrl: String? = null,
    val source: String = "",       // "CNN Indonesia", "BMKG", etc.
    val link: String = "",         // article URL
    val severity: NewsSeverity = NewsSeverity.INFO,
    val isOfficial: Boolean = false,  // BMKG/BNPB = true
    val region: String? = null     // detected Indonesian region
)

enum class NewsSeverity { INFO, WASPADA, DARURAT }

data class WeatherInfo(
    val location: String,
    val condition: String,
    val temperature: String,
    val riskStatus: String,
    val riskColor: Color,
    val weatherCode: Int = 0,        // WMO weather code (Open-Meteo)
    val humidity: String = "--",      // e.g. "78%"
    val windSpeed: String = "--",     // e.g. "12 km/h"
    val lastUpdated: Long = System.currentTimeMillis()
)

data class EarthquakeInfo(
    val magnitude: String,
    val location: String,
    val depth: String,
    val time: String,
    val felt: String        // "Dirasakan" field from BMKG
)

/**
 * Represents an active BMKG disaster warning
 * (recent significant earthquakes, tsunami, extreme weather).
 */
data class BmkgWarning(
    val type: String,               // "Gempa", "Tsunami", "Cuaca Ekstrem"
    val message: String,
    val severity: WarningSeverity,
    val time: String
)

enum class WarningSeverity { INFO, WARNING, DANGER }
