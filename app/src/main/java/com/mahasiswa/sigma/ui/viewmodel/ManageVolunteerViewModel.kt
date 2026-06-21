package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.data.model.UpdateVolunteerRequest
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ManageVolunteerViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val disasterRepository: DisasterReportRepositoryRetrofit
) : ViewModel() {

    private val _registrations = MutableStateFlow<List<VolunteerDto>>(emptyList())
    val registrations: StateFlow<List<VolunteerDto>> = _registrations.asStateFlow()

    private val _disasters = MutableStateFlow<List<DisasterReportDto>>(emptyList())
    val disasters: StateFlow<List<DisasterReportDto>> = _disasters.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // State untuk dialog assign
    private val _assignResult = MutableStateFlow<String?>(null)
    val assignResult: StateFlow<String?> = _assignResult.asStateFlow()

    init {
        loadRegistrations()
        loadDisasters()
    }

    fun loadRegistrations() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = volunteerRepository.getAllVolunteers()
            result.fold(
                onSuccess = { _registrations.value = it },
                onFailure = { _errorMessage.value = it.message }
            )
            _isLoading.value = false
        }
    }

    private fun loadDisasters() {
        viewModelScope.launch {
            val result = disasterRepository.getAllDisasterReports()
            result.onSuccess { _disasters.value = it }
        }
    }

    private fun nowTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    fun approveVolunteer(volunteerId: String) {
        viewModelScope.launch {
            val request = UpdateVolunteerRequest(
                status = "APPROVED",
                updatedAt = nowTimestamp()
            )
            volunteerRepository.updateVolunteer(volunteerId, request)
            loadRegistrations()
        }
    }

    fun rejectVolunteer(volunteerId: String) {
        viewModelScope.launch {
            val request = UpdateVolunteerRequest(
                status = "DECLINED",
                updatedAt = nowTimestamp()
            )
            volunteerRepository.updateVolunteer(volunteerId, request)
            loadRegistrations()
        }
    }

    /**
     * Admin assign relawan ke bencana + lokasi posko.
     * Status relawan → APPROVED, assignment_status → "pending" (menunggu konfirmasi dari relawan).
     */
    fun assignVolunteer(
        volunteerId: String,
        disasterId: Long,
        assignmentLocation: String
    ) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = UpdateVolunteerRequest(
                status = "APPROVED",
                disasterId = disasterId,
                assignment = assignmentLocation,
                assignmentStatus = "pending",   // menunggu konfirmasi relawan
                updatedAt = nowTimestamp()
            )
            val result = volunteerRepository.updateVolunteer(volunteerId, request)
            result.fold(
                onSuccess = {
                    _assignResult.value = "Relawan berhasil ditugaskan. Menunggu konfirmasi."
                    loadRegistrations()
                },
                onFailure = {
                    _assignResult.value = "Gagal menugaskan: ${it.message}"
                    _isLoading.value = false
                }
            )
        }
    }

    fun clearAssignResult() { _assignResult.value = null }
}
