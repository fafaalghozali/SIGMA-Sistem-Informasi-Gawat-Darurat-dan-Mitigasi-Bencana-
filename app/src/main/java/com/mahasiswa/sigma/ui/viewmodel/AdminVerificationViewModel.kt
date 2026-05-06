package com.mahasiswa.sigma.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminVerificationViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = AdminRepository(application)

    private val _pendingReports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val pendingReports: StateFlow<List<LocalDisasterReport>> = _pendingReports.asStateFlow()

    init {
        loadPendingReports()
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            _pendingReports.value = repository.getPendingReports()
        }
    }

    fun verifyReport(reportId: String) {
        viewModelScope.launch {
            repository.verifyReport(reportId)
            loadPendingReports()
        }
    }

    fun rejectReport(reportId: String) {
        viewModelScope.launch {
            repository.rejectReport(reportId)
            loadPendingReports()
        }
    }
}
