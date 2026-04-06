package com.mahasiswa.sigma

data class MenuModel(
    val id: Int,
    val title: String,
    val description: String,
    val iconRes: Int,
    val targetActivity: Class<*>? = null
)
