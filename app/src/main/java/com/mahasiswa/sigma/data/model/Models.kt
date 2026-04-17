package com.mahasiswa.sigma.data.model

enum class UserRole {
    MASYARAKAT, RELAWAN, BNPB
}

enum class SkillsVolunteer {
    MEDIS, SAR, LOGISTIK, KONSUMSI, PSIKOSOSIAL
}

enum class ReportStatus(val displayName: String) {
    PENDING("Pending"),
    DECLINE("Decline"),
    RESOLVED("Resolved"),
    SIAGA_1("Siaga 1"),
    SIAGA_2("Siaga 2"),
    AWAS("Awas");

    override fun toString(): String = displayName
}

data class User(
    val id: String,
    val name: String,
    val email: String,
    val role: UserRole
)

data class DisasterReport(
    val id: String,
    val title: String,
    val description: String,
    val photoUrl: String,
    val latitude: Double,
    val longitude: Double,
    val status: ReportStatus,
    val reporterName: String,
    val timestamp: Long
)

data class Shelter(
    val id: String,
    val name: String,
    val location: String,
    val capacity: Int,
    val availableSpace: Int,
    val latitude: Double,
    val longitude: Double
)

data class Volunteer(
    val id: String,
    val userId: String,
    val name: String,
    val skills: List<SkillsVolunteer>,
    val status: String,
    val address: String,
    val NIK: String? = null,
    val phoneNumber: String? = null,
    val assignment: String? = null
)

data class DisasterGuidance(
    val id: String,
    val type: String,
    val before: List<String>,
    val during: List<String>,
    val after: List<String>
)
