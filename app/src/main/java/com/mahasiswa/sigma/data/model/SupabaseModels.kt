package com.mahasiswa.sigma.data.model

import com.google.gson.annotations.SerializedName
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    val id: String? = null,
    @SerialName("full_name") @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String,
    val password: String? = null,
    @SerialName("remember_token") @SerializedName("remember_token") val rememberToken: String? = null,
    @SerialName("photo_url") @SerializedName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") @SerializedName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") @SerializedName("updated_at") val updatedAt: String? = null
)

@Serializable
data class VolunteerDto(
    val id: Long? = null,
    @SerialName("user_id") @SerializedName("user_id") val userId: String? = null,
    val name: String,
    val skill: String,
    val address: String,
    @SerialName("phone_number") @SerializedName("phone_number") val phoneNumber: String,
    val availability: String? = null,
    val status: String = "PENDING",
    val assignment: String? = null,
    @SerialName("assignment_status") @SerializedName("assignment_status") val assignmentStatus: String? = null,
    @SerialName("disaster_id") @SerializedName("disaster_id") val disasterId: String? = null
)

@Serializable
data class VolunteerReportDto(
    val id: String? = null,
    @SerialName("volunteer_id") @SerializedName("volunteer_id") val volunteerId: String? = null,
    @SerialName("disaster_id") @SerializedName("disaster_id") val disasterId: String? = null,
    @SerialName("skill_type") @SerializedName("skill_type") val skillType: String? = null,
    @SerialName("report_data") @SerializedName("report_data") val reportData: String? = null,
    val notes: String? = null,
    @SerialName("photo_urls") @SerializedName("photo_urls") val photoUrls: List<String>? = null,
    @SerialName("created_at") @SerializedName("created_at") val createdAt: String? = null
)

@Serializable
data class DisasterReportDto(
    val id: Int? = null,
    @SerialName("user_id") @SerializedName("user_id") val userId: String? = null,
    val title: String = "",
    val description: String = "",
    @SerialName("disaster_type") @SerializedName("disaster_type") val disasterType: String? = null,
    val location: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val status: String = "PENDING",
    @SerialName("reporter_name") @SerializedName("reporter_name") val reporterName: String = "",
    @SerialName("photo_url") @SerializedName("photo_url") val photoUrl: String? = null,
    @SerialName("verified_by") @SerializedName("verified_by") val verifiedBy: String? = null,
    @SerialName("created_at") @SerializedName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") @SerializedName("updated_at") val updatedAt: String? = null
)

@Serializable
data class ShelterDto(
    val id: Int? = null,
    val name: String = "",
    val address: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    @SerialName("capacity_max") @SerializedName("capacity_max") val capacityMax: Int = 0,
    @SerialName("capacity_current") @SerializedName("capacity_current") val capacityCurrent: Int = 0,
    val status: String = "active",
    val logistics: List<String>? = null,
    @SerialName("contact_phone") @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerialName("photo_url") @SerializedName("photo_url") val photoUrl: String? = null,
    @SerialName("created_at") @SerializedName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") @SerializedName("updated_at") val updatedAt: String? = null
)

@Serializable
data class NewsDto(
    val id: Int? = null,
    val title: String = "",
    val summary: String? = null,
    @SerialName("image_url") @SerializedName("image_url") val imageUrl: String? = null,
    val source: String? = null,
    val url: String? = null,
    @SerialName("published_at") @SerializedName("published_at") val publishedAt: String? = null,
    @SerialName("created_at") @SerializedName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") @SerializedName("updated_at") val updatedAt: String? = null
)

// Request models for CREATE operations
data class CreateProfileRequest(
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val role: String,
    val password: String
)

data class CreateVolunteerRequest(
    @SerializedName("user_id") val userId: String?,
    val name: String,
    val skill: String,
    val address: String,
    @SerializedName("phone_number") val phoneNumber: String,
    val availability: String? = null,
    val status: String = "PENDING"
)

data class CreateDisasterReportRequest(
    @SerializedName("user_id") val userId: String?,
    val title: String,
    val description: String,
    @SerializedName("disaster_type") val disasterType: String?,
    val location: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("reporter_name") val reporterName: String,
    @SerializedName("photo_url") val photoUrl: String? = null
)

data class CreateShelterRequest(
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    @SerializedName("capacity_max") val capacityMax: Int,
    @SerializedName("capacity_current") val capacityCurrent: Int = 0,
    val status: String = "active",
    val logistics: List<String> = emptyList(),
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null
)

data class CreateVolunteerReportRequest(
    @SerializedName("volunteer_id") val volunteerId: String,
    @SerializedName("disaster_id") val disasterId: String?,
    @SerializedName("skill_type") val skillType: String?,
    @SerializedName("report_data") val reportData: String?,
    val notes: String?,
    @SerializedName("photo_urls") val photoUrls: List<String>?
)

// Update request models (for PATCH operations with partial updates)
data class UpdateProfileRequest(
    @SerializedName("full_name") val fullName: String? = null,
    val email: String? = null,
    val role: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null
)

data class UpdateVolunteerRequest(
    val status: String? = null,
    val assignment: String? = null,
    @SerializedName("assignment_status") val assignmentStatus: String? = null,
    @SerializedName("disaster_id") val disasterId: String? = null,
    val availability: String? = null
)

data class UpdateDisasterReportRequest(
    val status: String? = null,
    @SerializedName("verified_by") val verifiedBy: String? = null,
    val title: String? = null,
    val description: String? = null,
    val location: String? = null,
    @SerializedName("disaster_type") val disasterType: String? = null
)

data class UpdateShelterRequest(
    val name: String? = null,
    val address: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    @SerializedName("capacity_max") val capacityMax: Int? = null,
    @SerializedName("capacity_current") val capacityCurrent: Int? = null,
    val status: String? = null,
    val logistics: List<String>? = null,
    @SerializedName("contact_phone") val contactPhone: String? = null,
    @SerializedName("photo_url") val photoUrl: String? = null
)
