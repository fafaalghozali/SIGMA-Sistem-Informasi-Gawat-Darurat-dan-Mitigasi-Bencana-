package com.mahasiswa.sigma.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface NewsDao {

    @Query("SELECT * FROM disaster_news ORDER BY publishedAt DESC")
    suspend fun getAll(): List<NewsEntity>

    @Query("SELECT * FROM disaster_news WHERE publishedAt > :cutoffMs ORDER BY publishedAt DESC")
    suspend fun getNewerThan(cutoffMs: Long): List<NewsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NewsEntity>)

    @Query("DELETE FROM disaster_news WHERE cachedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    @Query("DELETE FROM disaster_news")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM disaster_news")
    suspend fun count(): Int
}
