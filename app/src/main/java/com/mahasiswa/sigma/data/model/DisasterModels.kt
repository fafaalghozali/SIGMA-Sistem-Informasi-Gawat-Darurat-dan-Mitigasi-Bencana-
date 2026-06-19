package com.mahasiswa.sigma.data.model

/**
 * Lightweight UI model for rendering shelters on the map.
 * Populated from [ShelterDto] via the Retrofit shelter flow.
 */
data class ShelterMapItem(
    val id: Int?,
    val name: String,
    val address: String,
    val capacity: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val logistics: List<String>,
    val contactPhone: String?,
    val photoUrl: String?
)
