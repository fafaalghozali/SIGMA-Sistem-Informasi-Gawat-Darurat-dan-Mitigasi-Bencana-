package com.mahasiswa.sigma.data.model

enum class UserRole(val displayName: String) {
    MASYARAKAT("Masyarakat"),
    RELAWAN("Relawan"),
    BNPB("BNPB");

    companion object {
        fun fromString(value: String?): UserRole {
            return entries.find { it.displayName == value || it.name == value } ?: MASYARAKAT
        }
    }
}

enum class SkillsVolunteer {
    MEDIS, SAR, LOGISTIK, KONSUMSI, PSIKOSOSIAL, PENDIDIKAN
}
