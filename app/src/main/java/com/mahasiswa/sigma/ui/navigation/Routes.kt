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
    data class DisasterDetail(val disasterId: Int) : Route()
    data class ShelterDetail(val shelterId: Int) : Route()
    object ShelterInfo : Route()
    object Profile : Route()
    data class SearchDisaster(val query: String? = null, val status: String? = null) : Route()
    data class VolunteerRegistration(val email: String, val userName: String) : Route()
    object ManageReport : Route()
    object ManageShelter : Route()
    object ManageVolunteer : Route()
    object NewsList : Route()
    data class NewsDetail(val newsId: String) : Route()
    object VolunteerReport : Route()
    object ProfileList : Route()
}
