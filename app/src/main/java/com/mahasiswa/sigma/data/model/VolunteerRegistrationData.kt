package com.mahasiswa.sigma.data.model

import java.io.Serializable

data class VolunteerRegistrationData(
    val name: String,
    val skill: SkillsVolunteer,
    val address: String,
    val phoneNumber: String,
    val status: String = "PENDING",
    // Info penugasan dari admin
    val assignment: String? = null,
    val assignmentStatus: String? = null,  // null | "pending" | "accepted" | "rejected"
    val disasterId: Long? = null,
    val volunteerId: Long? = null
) : Serializable
