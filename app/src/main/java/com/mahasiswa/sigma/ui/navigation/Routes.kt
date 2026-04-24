package com.mahasiswa.sigma.ui.navigation

import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.LocalDisasterReport

sealed class Route {
    object Splash : Route()
    object Login : Route()
    object Register : Route()
    data class Dashboard(val role: UserRole, val email: String, val name: String) : Route()
    object Map : Route()
    object DisasterReport : Route()
    data class ReportDetail(val report: LocalDisasterReport) : Route()
    object ShelterInfo : Route()
    object Profile : Route()
    object SearchDisaster : Route()
    object VolunteerRegistration : Route()
    object AdminVerification : Route()
}
