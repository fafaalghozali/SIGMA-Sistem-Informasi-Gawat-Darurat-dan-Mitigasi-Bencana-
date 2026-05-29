package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.repository.AdminRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminVerificationViewModel @Inject constructor(
    private val repository: AdminRepository
) : ViewModel() {

    private val _pendingReports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val pendingReports: StateFlow<List<LocalDisasterReport>> = _pendingReports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadPendingReports()
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            _isLoading.value = true
            _pendingReports.value = repository.getPendingReports()
            _isLoading.value = false
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
