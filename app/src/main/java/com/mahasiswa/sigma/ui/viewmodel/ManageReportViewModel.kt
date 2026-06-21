package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.UpdateDisasterReportRequest
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageReportViewModel @Inject constructor(
    private val repository: DisasterReportRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private val _allReports = MutableStateFlow<List<DisasterReportDto>>(emptyList())

    private val _selectedFilter = MutableStateFlow<String?>(null)
    val selectedFilter: StateFlow<String?> = _selectedFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _selectedReport = MutableStateFlow<DisasterReportDto?>(null)
    val selectedReport: StateFlow<DisasterReportDto?> = _selectedReport.asStateFlow()

    private val _filteredReports = MutableStateFlow<List<DisasterReportDto>>(emptyList())
    val filteredReports: StateFlow<List<DisasterReportDto>> = _filteredReports.asStateFlow()

    val pendingCount: Int get() = _allReports.value.count { it.status.equals("PENDING", true) }
    val activeCount: Int get() = _allReports.value.count {
        !it.status.equals("PENDING", true) && !it.status.equals("DECLINE", true) && !it.status.equals("RESOLVED", true)
    }

    init {
        loadAllReports()
        viewModelScope.launch {
            combine(_allReports, _selectedFilter, _searchQuery) { reports, filter, query ->
                var result = reports
                if (filter != null) {
                    result = result.filter { it.status.equals(filter, true) }
                }
                if (query.isNotBlank()) {
                    result = result.filter {
                        it.title.contains(query, true) ||
                                it.reporterName.contains(query, true) ||
                                it.location.contains(query, true)
                    }
                }
                result
            }.collect { _filteredReports.value = it }
        }
    }

    fun loadAllReports() {
        viewModelScope.launch {
            _isLoading.value = true
            val result = repository.getAllDisasterReports()
            result.onSuccess { reports ->
                _allReports.value = reports
            }
            result.onFailure {
                _allReports.value = emptyList()
                _actionMessage.value = "Gagal memuat laporan: ${it.message}"
            }
            _isLoading.value = false
        }
    }

    fun setFilter(filter: String?) {
        _selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectReport(report: DisasterReportDto?) {
        _selectedReport.value = report
    }

    fun updateReportStatus(reportId: String, newStatus: String, disasterType: String? = null) {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            // Status must be UPPERCASE to match database convention
            val uppercaseStatus = newStatus.uppercase()
            // Only include verifiedBy if we have a valid userId and status warrants it
            val shouldIncludeVerifier = uppercaseStatus != "PENDING" && uppercaseStatus != "DECLINE" && !userId.isNullOrBlank()
            val request = UpdateDisasterReportRequest(
                status = uppercaseStatus,
                verifiedBy = if (shouldIncludeVerifier) userId else null,
                disasterType = disasterType
            )
            val result = repository.updateDisasterReport(reportId, request)
            result.onSuccess {
                _actionMessage.value = "Status laporan berhasil diperbarui"
                _selectedReport.value = it
                loadAllReports()
            }
            result.onFailure {
                _actionMessage.value = "Gagal memperbarui: ${it.message}"
            }
        }
    }

    fun markAsCompleted(reportId: String) {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            val request = UpdateDisasterReportRequest(
                status = "RESOLVED",
                verifiedBy = if (!userId.isNullOrBlank()) userId else null
            )
            val result = repository.updateDisasterReport(reportId, request)
            result.onSuccess {
                _actionMessage.value = "Laporan ditandai selesai"
                _selectedReport.value = null
                loadAllReports()
            }
            result.onFailure {
                _actionMessage.value = "Gagal: ${it.message}"
            }
        }
    }

    fun clearActionMessage() {
        _actionMessage.value = null
    }

    companion object {
        val STATUS_OPTIONS = listOf("PENDING", "AWAS", "SIAGA_1", "SIAGA_2", "RESOLVED", "DECLINE")
        val DISASTER_TYPE_OPTIONS = listOf("Banjir", "Kebakaran", "Gempa", "Longsor", "Tsunami", "Badai", "Gunung Meletus", "Lainnya")
    }
}
