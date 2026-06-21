package com.mahasiswa.sigma.data.model

/**
 * Data models for the BNPB admin's volunteer report view.
 * A flat filterable list showing all reports with enriched details.
 */

/**
 * A volunteer report enriched with volunteer name, posko (assignment), and disaster title.
 */
data class VolunteerReportWithDetails(
    val report: VolunteerReportDto,
    val volunteerName: String,
    val poskoName: String,
    val disasterTitle: String
)
