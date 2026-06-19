package com.mahasiswa.sigma.data.remote.api

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface BmkgApiService {

    @GET("DataMKG/TEWS/autogempa.json")
    suspend fun getAutoGempa(): AutoGempaResponse

    @GET("DataMKG/TEWS/gempaterkini.json")
    suspend fun getGempaTerkini(): GempaTerkiniResponse

    @GET("DataMKG/TEWS/{endpoint}")
    suspend fun getGempaByEndpoint(
        @Path("endpoint") endpoint: String
    ): GempaTerkiniResponse

    @GET("DataMKG/TEWS/gempaterkini.json")
    suspend fun getGempaFiltered(
        @Query("limit") limit: Int? = null
    ): GempaTerkiniResponse

    @POST("api/reports")
    suspend fun createReport(
        @Body request: CreateReportRequest
    ): ApiMessageResponse

    @PUT("api/reports/{id}")
    suspend fun updateReport(
        @Path("id") reportId: String,
        @Body request: UpdateReportRequest
    ): ApiMessageResponse

    @PATCH("api/reports/{id}/status")
    suspend fun patchReportStatus(
        @Path("id") reportId: String,
        @Body request: PatchReportStatusRequest
    ): ApiMessageResponse

    @DELETE("api/reports/{id}")
    suspend fun deleteReport(
        @Path("id") reportId: String
    ): ApiMessageResponse
}
