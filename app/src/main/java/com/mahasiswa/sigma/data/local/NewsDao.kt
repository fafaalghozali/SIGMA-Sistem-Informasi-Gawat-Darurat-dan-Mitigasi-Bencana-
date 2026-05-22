package com.mahasiswa.sigma.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

/**
 * Room DAO for disaster news cache.
 */
@Dao
interface NewsDao {

    /** Returns all cached news, ordered newest-first (by publish time). */
    @Query("SELECT * FROM disaster_news ORDER BY publishedAt DESC")
    suspend fun getAll(): List<NewsEntity>

    /** Returns cached news newer than [cutoffMs] epoch. */
    @Query("SELECT * FROM disaster_news WHERE publishedAt > :cutoffMs ORDER BY publishedAt DESC")
    suspend fun getNewerThan(cutoffMs: Long): List<NewsEntity>

    /**
     * Upserts a batch of news items.
     * If an item with the same primary key exists, it is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<NewsEntity>)

    /**
     * Deletes news cached more than [maxAgeMs] ago.
     * Called after a successful network refresh to keep the cache lean.
     */
    @Query("DELETE FROM disaster_news WHERE cachedAt < :cutoffMs")
    suspend fun deleteOlderThan(cutoffMs: Long)

    /** Deletes all cached news. */
    @Query("DELETE FROM disaster_news")
    suspend fun deleteAll()

    /** Returns the number of cached items. */
    @Query("SELECT COUNT(*) FROM disaster_news")
    suspend fun count(): Int
}
