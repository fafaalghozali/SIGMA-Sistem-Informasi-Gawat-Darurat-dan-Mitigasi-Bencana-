package com.mahasiswa.sigma.data.model

import java.io.Serializable

data class VolunteerRegistrationData(
    val name: String,
    val skill: SkillsVolunteer,
    val address: String,
    val phoneNumber: String,
    val status: String = "PENDING"
) : Serializable
