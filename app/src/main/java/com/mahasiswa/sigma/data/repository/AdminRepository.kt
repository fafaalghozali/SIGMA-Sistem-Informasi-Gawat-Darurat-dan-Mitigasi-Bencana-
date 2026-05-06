package com.mahasiswa.sigma.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import java.text.SimpleDateFormat
import java.util.*

class AdminRepository(context: Context) {
    private val sharedPrefs = context.getSharedPreferences("disaster_reports_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun getPendingReports(): List<LocalDisasterReport> {
        val json = sharedPrefs.getString("reports_list", null) ?: return emptyList()
        val type = object : TypeToken<List<LocalDisasterReport>>() {}.type
        val allReports: List<LocalDisasterReport> = gson.fromJson(json, type)
        return allReports.filter { it.status == "Pending" }
    }

    fun verifyReport(reportId: String) {
        updateReportStatus(reportId, "Verified")
    }

    fun rejectReport(reportId: String) {
        updateReportStatus(reportId, "Rejected")
    }

    private fun updateReportStatus(reportId: String, newStatus: String) {
        val json = sharedPrefs.getString("reports_list", null) ?: return
        val type = object : TypeToken<List<LocalDisasterReport>>() {}.type
        val reports: MutableList<LocalDisasterReport> = gson.fromJson(json, type)
        
        val index = reports.indexOfFirst { it.id == reportId }
        if (index != -1) {
            reports[index] = reports[index].copy(status = newStatus)
            sharedPrefs.edit().putString("reports_list", gson.toJson(reports)).apply()
        }
    }
}
