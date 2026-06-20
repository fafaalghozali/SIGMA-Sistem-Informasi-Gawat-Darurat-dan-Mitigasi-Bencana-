package com.mahasiswa.sigma.data.model

enum class UserRole(val displayName: String) {
    MASYARAKAT("Masyarakat"),
    RELAWAN("Relawan"),
    BNPB("BNPB");

    companion object {
        fun fromString(value: String?): UserRole {
            if (value == null) return MASYARAKAT
            val trimmed = value.trim()
            return when {
                trimmed.equals("Masyarakat", ignoreCase = true) || trimmed.equals("MASYARAKAT", ignoreCase = true) -> MASYARAKAT
                trimmed.equals("Relawan", ignoreCase = true) || trimmed.equals("RELAWAN", ignoreCase = true) -> RELAWAN
                trimmed.equals("Admin", ignoreCase = true) || trimmed.equals("ADMIN", ignoreCase = true) || trimmed.equals("BNPB", ignoreCase = true) -> BNPB
                else -> entries.find { it.displayName.equals(trimmed, ignoreCase = true) || it.name.equals(trimmed, ignoreCase = true) } ?: MASYARAKAT
            }
        }
    }
}

enum class SkillsVolunteer {
    MEDIS, SAR, LOGISTIK, KONSUMSI, PSIKOSOSIAL, PENDIDIKAN
}
