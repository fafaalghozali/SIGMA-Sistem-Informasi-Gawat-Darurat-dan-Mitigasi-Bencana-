package com.mahasiswa.sigma.data.remote

import android.util.Log
import android.util.Xml
import com.mahasiswa.sigma.data.model.RawRssItem
import com.mahasiswa.sigma.data.model.RssSource
import com.mahasiswa.sigma.data.remote.api.RssApiService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeoutOrNull
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RssNewsSource @Inject constructor(
    private val rssApiService: RssApiService
) {

    companion object {
        private const val TAG = "RssNewsSource"
        private const val PER_SOURCE_TIMEOUT_MS = 15_000L

        val SOURCES = listOf(
            RssSource(
                name = "Antara News",
                url = "https://www.antaranews.com/rss/terkini.rss",
                isOfficial = false
            ),
            RssSource(
                name = "Republika",
                url = "https://www.republika.co.id/rss/nasional/umum",
                isOfficial = false
            ),
            RssSource(
                name = "Sindonews Nasional",
                url = "https://nasional.sindonews.com/rss",
                isOfficial = false
            ),
            RssSource(
                name = "Liputan6",
                url = "https://www.liputan6.com/rss/news",
                isOfficial = false
            ),
            RssSource(
                name = "Detik News",
                url = "https://news.detik.com/rss",
                isOfficial = false
            ),
        )
    }

    suspend fun fetchAll(): List<RawRssItem> = coroutineScope {
        SOURCES.map { source ->
            async {
                withTimeoutOrNull(PER_SOURCE_TIMEOUT_MS) {
                    try {
                        fetchSource(source)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to fetch ${source.name}: ${e.message}")
                        emptyList()
                    }
                } ?: emptyList()
            }
        }.awaitAll().flatten()
    }

    private suspend fun fetchSource(source: RssSource): List<RawRssItem> {
        val responseBody = rssApiService.fetchRssFeed(source.url)
        val xml = responseBody.string()
        return parseRss(xml, source)
    }

    private fun parseRss(xml: String, source: RssSource): List<RawRssItem> {
        val items = mutableListOf<RawRssItem>()
        val parser: XmlPullParser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var inItem = false
        var guid = ""
        var title = ""
        var description = ""
        var link = ""
        var pubDate = ""
        var imageUrl: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.lowercase() ?: ""

            when (eventType) {
                XmlPullParser.START_TAG -> {
                    when (tagName) {
                        "item" -> {
                            inItem = true
                            guid = ""; title = ""; description = ""
                            link = ""; pubDate = ""; imageUrl = null
                        }
                        "title"       -> if (inItem) title = readText(parser)
                        "description" -> if (inItem) description = readText(parser)
                        "link"        -> if (inItem) link = readText(parser)
                        "guid"        -> if (inItem) guid = readText(parser)
                        "pubdate"     -> if (inItem) pubDate = readText(parser)

                        "media:content", "media:thumbnail" -> {
                            if (inItem && imageUrl == null) {
                                val url = parser.getAttributeValue(null, "url")
                                if (!url.isNullOrBlank()) imageUrl = url
                            }
                        }

                        "enclosure" -> {
                            if (inItem && imageUrl == null) {
                                val type = parser.getAttributeValue(null, "type") ?: ""
                                if (type.startsWith("image")) {
                                    imageUrl = parser.getAttributeValue(null, "url")
                                }
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName == "item" && inItem) {
                        inItem = false
                        val stableId = guid.ifBlank { link }.ifBlank {
                            "${source.name}-${title.hashCode()}"
                        }
                        if (title.isNotBlank()) {
                            val extractedImage = imageUrl
                                ?: extractImageFromHtml(description)

                            items.add(
                                RawRssItem(
                                    guid = stableId,
                                    title = title.cleanHtml(),
                                    description = description.cleanHtml(),
                                    link = link,
                                    pubDate = pubDate,
                                    imageUrl = extractedImage,
                                    sourceName = source.name,
                                    isOfficial = source.isOfficial
                                )
                            )
                        }
                    }
                }
            }
            eventType = parser.next()
        }
        return items
    }

    private fun readText(parser: XmlPullParser): String {
        var result = ""
        if (parser.next() == XmlPullParser.TEXT) {
            result = parser.text ?: ""
            parser.nextTag()
        }
        return result.trim()
    }

    private fun String.cleanHtml(): String =
        this.replace(Regex("<[^>]*>"), "")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .trim()

    private fun extractImageFromHtml(html: String): String? {
        val regex = Regex("""<img[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        return regex.find(html)?.groupValues?.getOrNull(1)
    }
}
