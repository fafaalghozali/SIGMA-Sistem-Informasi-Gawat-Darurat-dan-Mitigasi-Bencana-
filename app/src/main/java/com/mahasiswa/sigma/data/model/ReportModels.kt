package com.mahasiswa.sigma.data.model

import java.io.Serializable
import java.util.UUID

data class LocalDisasterReport(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val location: String,
    val reporter: String = "Masyarakat",
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis(),
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUrl: String? = null,
    val disasterType: String? = null
) : Serializable
