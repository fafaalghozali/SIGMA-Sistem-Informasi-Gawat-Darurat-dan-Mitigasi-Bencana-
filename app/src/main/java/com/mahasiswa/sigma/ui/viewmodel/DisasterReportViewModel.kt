package com.mahasiswa.sigma.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DisasterReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(application)

    private val _reports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val reports: StateFlow<List<LocalDisasterReport>> = _reports.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = repository.getAllReports()
        }
    }

    fun sendReport(title: String, description: String, location: String) {
        viewModelScope.launch {
            val newReport = LocalDisasterReport(
                title = title,
                description = description,
                location = location
            )
            repository.saveReport(newReport)
            loadReports()
        }
    }

    fun updateReport(report: LocalDisasterReport) {
        viewModelScope.launch {
            repository.updateReport(report)
            loadReports()
        }
    }
}
