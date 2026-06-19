package com.mahasiswa.sigma.data.remote.api

import com.google.gson.annotations.SerializedName

data class CreateReportRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("latitude") val latitude: Double,
    @SerializedName("longitude") val longitude: Double,
    @SerializedName("disaster_type") val disasterType: String,
    @SerializedName("reporter_email") val reporterEmail: String
)

data class UpdateReportRequest(
    @SerializedName("title") val title: String,
    @SerializedName("description") val description: String,
    @SerializedName("status") val status: String
)

data class PatchReportStatusRequest(
    @SerializedName("status") val status: String
)

data class ApiMessageResponse(
    @SerializedName("success") val success: Boolean = false,
    @SerializedName("message") val message: String = ""
)
