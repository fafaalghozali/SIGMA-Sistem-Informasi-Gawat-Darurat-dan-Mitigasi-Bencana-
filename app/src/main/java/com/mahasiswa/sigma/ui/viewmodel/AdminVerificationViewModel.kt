package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.UpdateDisasterReportRequest
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AdminVerificationViewModel @Inject constructor(
    private val repository: DisasterReportRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private val _pendingReports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val pendingReports: StateFlow<List<LocalDisasterReport>> = _pendingReports.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionError = MutableStateFlow<String?>(null)
    val actionError: StateFlow<String?> = _actionError.asStateFlow()

    init {
        loadPendingReports()
    }

    fun loadPendingReports() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getDisasterReportsByStatus("pending")
            result.onSuccess { reports ->
                _pendingReports.value = reports.map { dto ->
                    LocalDisasterReport(
                        id = dto.id?.toString() ?: "",
                        title = dto.title,
                        description = dto.description,
                        location = dto.location,
                        reporter = dto.reporterName,
                        status = dto.status,
                        latitude = dto.latitude,
                        longitude = dto.longitude,
                        photoUrl = dto.photoUrl
                    )
                }
            }
            result.onFailure {
                _pendingReports.value = emptyList()
            }
            _isLoading.value = false
        }
    }

    fun verifyReport(reportId: String, newStatus: String = "siaga_1") {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            val request = UpdateDisasterReportRequest(
                status = newStatus,
                verifiedBy = userId
            )
            val result = repository.updateDisasterReport(reportId, request)
            result.onFailure { _actionError.value = it.message }
            loadPendingReports()
        }
    }

    fun rejectReport(reportId: String) {
        viewModelScope.launch {
            val request = UpdateDisasterReportRequest(
                status = "decline"
            )
            val result = repository.updateDisasterReport(reportId, request)
            result.onFailure { _actionError.value = it.message }
            loadPendingReports()
        }
    }

    fun clearActionError() {
        _actionError.value = null
    }
}
