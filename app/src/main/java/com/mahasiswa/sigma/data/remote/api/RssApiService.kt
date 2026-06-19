package com.mahasiswa.sigma.data.remote.api

import okhttp3.ResponseBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Url

interface RssApiService {

    @GET
    suspend fun fetchRssFeed(
        @Url url: String,
        @Header("User-Agent") userAgent: String = "Mozilla/5.0 SIGMA-DisasterApp/1.0 (Android; disaster monitoring)",
        @Header("Accept") accept: String = "application/rss+xml, application/xml, text/xml"
    ): ResponseBody
}
