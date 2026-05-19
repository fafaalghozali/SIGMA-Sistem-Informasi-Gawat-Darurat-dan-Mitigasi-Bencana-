package com.mahasiswa.sigma.data.repository

import androidx.datastore.core.DataStore
import com.mahasiswa.sigma.DisasterReportEntry
import com.mahasiswa.sigma.DisasterReportsData
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import kotlinx.coroutines.flow.first

class ReportRepository(private val dataStore: DataStore<DisasterReportsData>) {

    suspend fun saveReport(report: LocalDisasterReport) {
        dataStore.updateData { currentData ->
            val newEntry = report.toProtoEntry()
            currentData.toBuilder()
                .addReports(0, newEntry)
                .build()
        }
    }

    suspend fun getAllReports(): List<LocalDisasterReport> {
        val data = dataStore.data.first()
        return data.reportsList.map { it.toDomainModel() }
    }

    suspend fun updateReport(updatedReport: LocalDisasterReport) {
        dataStore.updateData { currentData ->
            val index = currentData.reportsList.indexOfFirst { it.id == updatedReport.id }
            if (index == -1) return@updateData currentData

            currentData.toBuilder()
                .setReports(index, updatedReport.toProtoEntry())
                .build()
        }
    }

    private fun LocalDisasterReport.toProtoEntry(): DisasterReportEntry {
        return DisasterReportEntry.newBuilder()
            .setId(id)
            .setTitle(title)
            .setDescription(description)
            .setLocation(location)
            .setReporter(reporter)
            .setStatus(status)
            .setTimestamp(timestamp)
            .build()
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
