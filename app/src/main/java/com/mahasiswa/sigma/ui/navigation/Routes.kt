package com.mahasiswa.sigma.ui.navigation

import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import java.io.Serializable

sealed class Route : Serializable {
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
    data class VolunteerRegistration(val email: String, val userName: String = "") : Route()
    object AdminVerification : Route()
    object ManageShelter : Route()
    object ManageVolunteer : Route()
    object NewsList : Route()
    data class NewsDetail(val newsId: String) : Route()
    object VolunteerReport : Route()
    object ProfileList : Route()
}
