package com.mahasiswa.sigma.data.model

/**
 * Raw item parsed from an RSS feed before any filtering / enrichment.
 */
data class RawRssItem(
    val guid: String,
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String,       // RFC-2822 date string from the RSS <pubDate> tag
    val imageUrl: String?,
    val sourceName: String,
    val isOfficial: Boolean    // true = BMKG / BNPB government source
)

/**
 * Definition of a single RSS source to fetch from.
 */
data class RssSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean = false,
    /** Set to true for BMKG JSON endpoints that we convert to pseudo-RSS. */
    val isBmkgJson: Boolean = false
)
