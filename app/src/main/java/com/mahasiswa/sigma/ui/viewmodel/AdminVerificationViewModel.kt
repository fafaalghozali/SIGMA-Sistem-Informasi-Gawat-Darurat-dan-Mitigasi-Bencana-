package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.PendingReport
import com.mahasiswa.sigma.data.repository.AdminRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AdminVerificationViewModel(
    private val repository: AdminRepository = AdminRepository()
) : ViewModel() {

    private val _pendingReports = MutableStateFlow<List<PendingReport>>(emptyList())
    val pendingReports: StateFlow<List<PendingReport>> = _pendingReports.asStateFlow()

    init {
        loadPendingReports()
    }

    private fun loadPendingReports() {
        viewModelScope.launch {
            val data = repository.getPendingReports()
            _pendingReports.value = data
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
