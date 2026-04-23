package com.mahasiswa.sigma.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class ReportRepository(context: Context) {
    private val sharedPrefs: SharedPreferences =
        context.getSharedPreferences("disaster_reports_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveReport(report: LocalDisasterReport) {
        val currentReports = getAllReports().toMutableList()
        currentReports.add(0, report)
        val json = gson.toJson(currentReports)
        sharedPrefs.edit().putString("reports_list", json).apply()
    }

    fun getAllReports(): List<LocalDisasterReport> {
        val json = sharedPrefs.getString("reports_list", null) ?: return emptyList()
        val type = object : TypeToken<List<LocalDisasterReport>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updateReport(updatedReport: LocalDisasterReport) {
        val currentReports = getAllReports().toMutableList()
        val index = currentReports.indexOfFirst { it.id == updatedReport.id }
        if (index != -1) {
            currentReports[index] = updatedReport
            val json = gson.toJson(currentReports)
            sharedPrefs.edit().putString("reports_list", json).apply()
        }
    }
}
