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
    val id: String,                
    val title: String,
    val time: String,              
    val publishedAt: Long = 0L,    
    val category: String,          
    val categoryColor: Color = Color.Gray,
    val imageUrl: String? = null,
    val source: String = "",       
    val link: String = "",         
    val severity: NewsSeverity = NewsSeverity.INFO,
    val isOfficial: Boolean = false,  
    val region: String? = null     
)

enum class NewsSeverity { INFO, WASPADA, DARURAT }

data class WeatherInfo(
    val location: String,
    val condition: String,
    val temperature: String,
    val riskStatus: String,
    val riskColor: Color,
    val weatherCode: Int = 0,        
    val humidity: String = "--",      
    val windSpeed: String = "--",     
    val lastUpdated: Long = System.currentTimeMillis()
)

data class EarthquakeInfo(
    val magnitude: String,
    val location: String,
    val depth: String,
    val time: String,
    val felt: String        
)





data class BmkgWarning(
    val type: String,               
    val message: String,
    val severity: WarningSeverity,
    val time: String
)

enum class WarningSeverity { INFO, WARNING, DANGER }
