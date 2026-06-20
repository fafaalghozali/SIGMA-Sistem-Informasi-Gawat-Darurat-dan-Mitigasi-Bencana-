package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.CreateVolunteerReportRequest
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.VolunteerReportDto
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the Volunteer Report feature.
 *
 * Full Retrofit flow: VolunteerReportViewModel -> VolunteerRepositoryRetrofit
 * -> SupabaseApiService (volunteer_reports table).
 *
 * BNPB sees all reports; a volunteer sees only their own.
 */
@HiltViewModel
class VolunteerReportViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<VolunteerReportDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<VolunteerReportDto>>> = _uiState.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /** Resolved volunteer id for the current user (null if not a registered volunteer). */
    private var currentVolunteerId: String? = null
    val canCreate: Boolean get() = currentVolunteerId != null

    private suspend fun resolveVolunteerId(): String? {
        if (currentVolunteerId != null) return currentVolunteerId
        val userId = authManager.getCurrentUserId() ?: return null
        volunteerRepository.getVolunteerByUserId(userId).onSuccess {
            currentVolunteerId = it?.id?.toString()
        }
        return currentVolunteerId
    }

    fun load(role: UserRole) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            val result = if (role == UserRole.BNPB) {
                volunteerRepository.getAllVolunteerReports()
            } else {
                val volunteerId = resolveVolunteerId()
                if (volunteerId == null) {
                    _uiState.value = UiState.Error("Anda belum terdaftar sebagai relawan.")
                    return@launch
                }
                volunteerRepository.getVolunteerReportsByVolunteerId(volunteerId)
            }
            result
                .onSuccess { reports ->
                    _uiState.value = if (reports.isEmpty()) UiState.Empty else UiState.Success(reports)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Gagal memuat laporan")
                }
        }
    }

    fun createReport(role: UserRole, skillType: String?, reportData: String, notes: String, disasterId: String? = null) {
        viewModelScope.launch {
            _isProcessing.value = true
            val volunteerId = resolveVolunteerId()
            if (volunteerId == null) {
                _operationMessage.value = "Anda belum terdaftar sebagai relawan."
                _isProcessing.value = false
                return@launch
            }
            val request = CreateVolunteerReportRequest(
                volunteerId = volunteerId,
                disasterId = disasterId,
                skillType = skillType,
                reportData = reportData,
                notes = notes.ifBlank { null },
                photoUrls = null
            )
            volunteerRepository.createVolunteerReport(request)
                .onSuccess {
                    _operationMessage.value = "Laporan berhasil dikirim"
                    load(role)
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal mengirim laporan"
                }
            _isProcessing.value = false
        }
    }

    fun updateReport(role: UserRole, id: String, reportData: String, notes: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val updates = mapOf(
                "report_data" to reportData,
                "notes" to notes.ifBlank { null }
            )
            volunteerRepository.updateVolunteerReport(id, updates)
                .onSuccess {
                    _operationMessage.value = "Laporan berhasil diperbarui"
                    load(role)
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal memperbarui laporan"
                }
            _isProcessing.value = false
        }
    }

    fun deleteReport(role: UserRole, id: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            volunteerRepository.deleteVolunteerReport(id)
                .onSuccess {
                    _operationMessage.value = "Laporan berhasil dihapus"
                    load(role)
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menghapus laporan"
                }
            _isProcessing.value = false
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
