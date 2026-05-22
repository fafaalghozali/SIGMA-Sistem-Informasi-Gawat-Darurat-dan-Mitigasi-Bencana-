package com.mahasiswa.sigma.data.repository

import android.content.Context
import android.util.Log
import androidx.compose.ui.graphics.Color
import com.mahasiswa.sigma.data.local.NewsDao
import com.mahasiswa.sigma.data.local.NewsEntity
import com.mahasiswa.sigma.data.local.SigmaDatabase
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.news.DisasterFilter
import com.mahasiswa.sigma.data.news.LocationPrioritizer
import com.mahasiswa.sigma.data.news.NewsDeduplicator
import com.mahasiswa.sigma.data.remote.BmkgNewsSource
import com.mahasiswa.sigma.data.remote.RssNewsSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * Orchestrates the full disaster news pipeline:
 *
 *   [RssNewsSource] + [BmkgNewsSource]
 *           ↓
 *    [DisasterFilter]  (keyword scoring + severity)
 *           ↓
 *   [NewsDeduplicator] (Jaccard bigram similarity)
 *           ↓
 *   [LocationPrioritizer] (nearby region boost)
 *           ↓
 *       [NewsDao]  (Room cache upsert)
 *           ↓
 *    List<NewsItem>
 *
 * Cache strategy:
 *   - Return cached items immediately (fast first render)
 *   - Fetch fresh data in background
 *   - Cache eviction: items older than [CACHE_MAX_AGE_MS] are deleted
 */
class NewsRepository(context: Context) {

    private val dao: NewsDao = SigmaDatabase.getInstance(context).newsDao()

    companion object {
        private const val TAG = "NewsRepository"
        /** 30 minutes — cache is considered stale after this */
        private const val CACHE_MAX_AGE_MS = 30 * 60 * 1_000L
        /** Max news items to show in the dashboard carousel */
        const val MAX_ITEMS = 15
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns cached news immediately (may be empty on first launch),
     * then fetches fresh data from the network.
     *
     * Call [fetchFreshNews] separately for the background update.
     */
    suspend fun getCachedNews(userCity: String = ""): List<NewsItem> =
        withContext(Dispatchers.IO) {
            dao.getAll()
                .map { it.toNewsItem() }
                .let { LocationPrioritizer.prioritize(it, userCity) }
                .take(MAX_ITEMS)
        }

    /**
     * Fetches fresh disaster news from all sources, runs the full pipeline,
     * persists to cache, and returns the processed list.
     *
     * Throws on total failure (all sources unreachable).
     * Partial source failures are handled gracefully (sources are fetched
     * concurrently; individual failures are silently skipped).
     */
    suspend fun fetchFreshNews(userCity: String = ""): List<NewsItem> =
        withContext(Dispatchers.IO) {
            // Fetch from all sources concurrently
            val (rssItems, bmkgItems) = coroutineScope {
                val rss = async { 
                    try { RssNewsSource.fetchAll() } 
                    catch (e: Exception) {
                        Log.w(TAG, "RSS fetch failed: ${e.message}")
                        emptyList()
                    }
                }
                val bmkg = async { 
                    try { BmkgNewsSource.fetchBmkgNews() } 
                    catch (e: Exception) {
                        Log.w(TAG, "BMKG fetch failed: ${e.message}")
                        emptyList()
                    }
                }
                Pair(rss.await(), bmkg.await())
            }

            val allRaw = bmkgItems + rssItems // BMKG items go first (higher priority base)

            if (allRaw.isEmpty()) {
                // If network is completely unreachable, return whatever is in cache
                return@withContext getCachedNews(userCity)
            }

            // Run the processing pipeline
            val filtered    = DisasterFilter.filter(allRaw)
            val deduped     = NewsDeduplicator.deduplicate(filtered)
            val prioritized = LocationPrioritizer.prioritize(deduped, userCity)
            val result      = prioritized.take(MAX_ITEMS)

            // Persist to cache
            dao.upsertAll(result.map { it.toEntity() })
            // Evict stale cache entries
            val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
            dao.deleteOlderThan(cutoff)

            Log.d(TAG, "News pipeline: ${allRaw.size} raw → ${filtered.size} filtered " +
                    "→ ${deduped.size} deduped → ${result.size} shown")

            result
        }

    // ── Entity ↔ NewsItem conversion ──────────────────────────────────────────

    private fun NewsEntity.toNewsItem(): NewsItem = NewsItem(
        id = id,
        title = title,
        time = DisasterFilter.relativeTimeString(publishedAt),
        publishedAt = publishedAt,
        category = category,
        categoryColor = Color(categoryColorHex),
        imageUrl = imageUrl,
        source = source,
        link = link,
        severity = try { NewsSeverity.valueOf(severityName) } catch (_: Exception) { NewsSeverity.INFO },
        isOfficial = isOfficial,
        region = region
    )

    private fun NewsItem.toEntity(): NewsEntity = NewsEntity(
        id = id,
        title = title,
        time = time,
        publishedAt = publishedAt,
        category = category,
        categoryColorHex = categoryColor.value.toLong(),
        imageUrl = imageUrl,
        source = source,
        link = link,
        severityName = severity.name,
        isOfficial = isOfficial,
        region = region
    )
}
