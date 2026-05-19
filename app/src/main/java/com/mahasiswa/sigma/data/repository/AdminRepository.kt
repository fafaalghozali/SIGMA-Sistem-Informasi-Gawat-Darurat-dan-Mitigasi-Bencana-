package com.mahasiswa.sigma.data.repository

import androidx.datastore.core.DataStore
import com.mahasiswa.sigma.DisasterReportEntry
import com.mahasiswa.sigma.DisasterReportsData
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import kotlinx.coroutines.flow.first

class AdminRepository(private val dataStore: DataStore<DisasterReportsData>) {

    suspend fun getPendingReports(): List<LocalDisasterReport> {
        val data = dataStore.data.first()
        return data.reportsList
            .filter { it.status == "Pending" }
            .map { it.toDomainModel() }
    }

    suspend fun verifyReport(reportId: String) {
        updateReportStatus(reportId, "Verified")
    }

    suspend fun rejectReport(reportId: String) {
        updateReportStatus(reportId, "Rejected")
    }

    private suspend fun updateReportStatus(reportId: String, newStatus: String) {
        dataStore.updateData { currentData ->
            val index = currentData.reportsList.indexOfFirst { it.id == reportId }
            if (index == -1) return@updateData currentData

            val updatedEntry = currentData.reportsList[index].toBuilder()
                .setStatus(newStatus)
                .build()

            currentData.toBuilder()
                .setReports(index, updatedEntry)
                .build()
        }
    }

    private fun DisasterReportEntry.toDomainModel(): LocalDisasterReport {
        return LocalDisasterReport(
            id = id,
            title = title,
            description = description,
            location = location,
            reporter = reporter,
            status = status,
            timestamp = timestamp
        )
    }
}
