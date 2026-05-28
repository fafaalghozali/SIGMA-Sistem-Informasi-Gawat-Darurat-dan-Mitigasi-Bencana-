package com.mahasiswa.sigma.data.repository

import android.util.Log
import androidx.compose.ui.graphics.Color
import com.mahasiswa.sigma.data.local.NewsDao
import com.mahasiswa.sigma.data.local.NewsEntity
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.news.DisasterFilter
import com.mahasiswa.sigma.data.news.LocationPrioritizer
import com.mahasiswa.sigma.data.news.NewsDeduplicator
import com.mahasiswa.sigma.data.news.toColor
import com.mahasiswa.sigma.data.remote.BmkgNewsSource
import com.mahasiswa.sigma.data.remote.RssNewsSource

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsRepository @Inject constructor(
    private val dao: NewsDao
) {

    companion object {
        private const val TAG = "NewsRepository"
        
        private const val CACHE_MAX_AGE_MS = 30 * 60 * 1_000L
        
        const val MAX_ITEMS = 15
    }

    

    





    suspend fun getCachedNews(userCity: String = ""): List<NewsItem> =
        withContext(Dispatchers.IO) {
            dao.getAll()
                .map { it.toNewsItem() }
                .let { LocationPrioritizer.prioritize(it, userCity) }
                .take(MAX_ITEMS)
        }

    







    suspend fun fetchFreshNews(userCity: String = ""): List<NewsItem> =
        withContext(Dispatchers.IO) {
            
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

            val allRaw = bmkgItems + rssItems 

            if (allRaw.isEmpty()) {
                
                return@withContext getCachedNews(userCity)
            }

            
            val filtered    = DisasterFilter.filter(allRaw)
            val deduped     = NewsDeduplicator.deduplicate(filtered)
            val prioritized = LocationPrioritizer.prioritize(deduped, userCity)
            val result      = prioritized.take(MAX_ITEMS)

            
            dao.upsertAll(result.map { it.toEntity() })
            
            val cutoff = System.currentTimeMillis() - CACHE_MAX_AGE_MS
            dao.deleteOlderThan(cutoff)

            Log.d(TAG, "News pipeline: ${allRaw.size} raw → ${filtered.size} filtered " +
                    "→ ${deduped.size} deduped → ${result.size} shown")

            result
        }

    

    private fun NewsEntity.toNewsItem(): NewsItem {
        val parsedSeverity = try { NewsSeverity.valueOf(severityName) } catch (_: Exception) { NewsSeverity.INFO }
        return NewsItem(
            id = id,
            title = title,
            time = DisasterFilter.relativeTimeString(publishedAt),
            publishedAt = publishedAt,
            category = category,
            categoryColor = parsedSeverity.toColor(),
            imageUrl = imageUrl,
            source = source,
            link = link,
            severity = parsedSeverity,
            isOfficial = isOfficial,
            region = region
        )
    }

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
