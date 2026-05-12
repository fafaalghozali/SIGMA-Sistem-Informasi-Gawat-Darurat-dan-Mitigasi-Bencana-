package com.mahasiswa.sigma.data.model

data class DisasterInfo(
    val type: String,
    val location: String,
    val status: ReportStatus,
    val date: String
)

data class ShelterMock(
    val name: String,
    val distance: String,
    val capacity: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val logisticsNeeded: List<String>
)
