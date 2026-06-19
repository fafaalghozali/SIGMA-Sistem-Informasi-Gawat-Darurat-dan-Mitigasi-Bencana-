package com.mahasiswa.sigma.data.remote.api

import com.google.gson.annotations.SerializedName

data class AutoGempaResponse(
    @SerializedName("Infogempa") val infogempa: InfoGempaWrapper
)

data class InfoGempaWrapper(
    @SerializedName("gempa") val gempa: GempaDetail
)

data class GempaDetail(
    @SerializedName("Tanggal") val tanggal: String = "",
    @SerializedName("Jam") val jam: String = "",
    @SerializedName("DateTime") val dateTime: String = "",
    @SerializedName("Coordinates") val coordinates: String = "",
    @SerializedName("Lintang") val lintang: String = "",
    @SerializedName("Bujur") val bujur: String = "",
    @SerializedName("Magnitude") val magnitude: String = "",
    @SerializedName("Kedalaman") val kedalaman: String = "",
    @SerializedName("Wilayah") val wilayah: String = "",
    @SerializedName("Potensi") val potensi: String = "",
    @SerializedName("Dirasakan") val dirasakan: String = "",
    @SerializedName("Shakemap") val shakemap: String = ""
)

data class GempaTerkiniResponse(
    @SerializedName("Infogempa") val infogempa: InfoGempaListWrapper
)

data class InfoGempaListWrapper(
    @SerializedName("gempa") val gempa: List<GempaTerkiniItem>
)

data class GempaTerkiniItem(
    @SerializedName("Tanggal") val tanggal: String = "",
    @SerializedName("Jam") val jam: String = "",
    @SerializedName("DateTime") val dateTime: String = "",
    @SerializedName("Coordinates") val coordinates: String = "",
    @SerializedName("Lintang") val lintang: String = "",
    @SerializedName("Bujur") val bujur: String = "",
    @SerializedName("Magnitude") val magnitude: String = "",
    @SerializedName("Kedalaman") val kedalaman: String = "",
    @SerializedName("Wilayah") val wilayah: String = "",
    @SerializedName("Potensi") val potensi: String = "",
    @SerializedName("Dirasakan") val dirasakan: String = ""
)
