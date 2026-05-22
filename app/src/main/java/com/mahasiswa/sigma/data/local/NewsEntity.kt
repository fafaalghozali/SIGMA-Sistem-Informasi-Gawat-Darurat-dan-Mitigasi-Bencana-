package com.mahasiswa.sigma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey







@Entity(tableName = "disaster_news")
data class NewsEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val time: String,
    val publishedAt: Long,
    val category: String,
    val categoryColorHex: Long,     
    val imageUrl: String?,
    val source: String,
    val link: String,
    val severityName: String,       
    val isOfficial: Boolean,
    val region: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
