package com.mahasiswa.sigma.data.model

data class RawRssItem(
    val guid: String,
    val title: String,
    val description: String,
    val link: String,
    val pubDate: String,
    val imageUrl: String?,
    val sourceName: String,
    val isOfficial: Boolean
)

data class RssSource(
    val name: String,
    val url: String,
    val isOfficial: Boolean = false,

    val isBmkgJson: Boolean = false
)
