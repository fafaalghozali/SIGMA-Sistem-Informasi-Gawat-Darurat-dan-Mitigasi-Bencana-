package com.mahasiswa.sigma.data.model

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

data class DashboardMenuModel(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)

data class NewsItem(
    val id: Int,
    val title: String,
    val time: String,
    val category: String,
    val backgroundColor: Color
)
