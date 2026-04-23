package com.mahasiswa.sigma.data.model

import java.util.UUID

data class LocalDisasterReport(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val location: String,
    val status: String = "Pending",
    val timestamp: Long = System.currentTimeMillis()
)
