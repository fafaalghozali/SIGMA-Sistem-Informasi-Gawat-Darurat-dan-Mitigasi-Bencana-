package com.mahasiswa.sigma.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey


/**
 * Room entity for persisting processed disaster news items.
 * Mirrors [com.mahasiswa.sigma.data.model.NewsItem] but uses primitive types
 * that Room can store without custom converters.
 */
@Entity(tableName = "disaster_news")
data class NewsEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val time: String,
    val publishedAt: Long,
    val category: String,
    val categoryColorHex: Long,     // Color stored as ARGB Long (e.g. 0xFF3E7BFA)
    val imageUrl: String?,
    val source: String,
    val link: String,
    val severityName: String,       // NewsSeverity.name()
    val isOfficial: Boolean,
    val region: String?,
    val cachedAt: Long = System.currentTimeMillis()
)
