package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.repository.ReportRepository
import com.mahasiswa.sigma.data.repository.VolunteerRepository
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisasterReportViewModel @Inject constructor(
    private val repository: ReportRepository,
    private val volunteerRepository: VolunteerRepository
) : ViewModel() {

    private val _reports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val reports: StateFlow<List<LocalDisasterReport>> = _reports.asStateFlow()

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var locationAddress by mutableStateOf("Mendeteksi lokasi...")
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    var currentLatitude by mutableStateOf(0.0)
    var currentLongitude by mutableStateOf(0.0)
    
    var volunteerSkill by mutableStateOf<SkillsVolunteer?>(null)
    var volunteerName by mutableStateOf("")
    
    fun loadVolunteerSkill(email: String) {
        viewModelScope.launch {
            val reg = volunteerRepository.getRegistration(email)
            if (reg != null) {
                volunteerSkill = reg.skill
                volunteerName = reg.name
            }
        }
    }
    
    var showIncompleteDialog by mutableStateOf(false)
    var showPhotoSourceSheet by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var saveErrorMessage by mutableStateOf<String?>(null)
    var saveSuccess by mutableStateOf(false)

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = repository.getAllReports()
        }
    }

    fun onTitleChange(newValue: String) { title = newValue }
    fun onDescriptionChange(newValue: String) { description = newValue }
    fun onLocationReceived(address: String, lat: Double = 0.0, lng: Double = 0.0) {
        locationAddress = address
        if (lat != 0.0 && lng != 0.0) {
            currentLatitude = lat
            currentLongitude = lng
        }
    }
    fun onImageSelected(bitmap: Bitmap?) { 
        imageBitmap = bitmap 
        showPhotoSourceSheet = false
    }

    fun sendReport() {
        if (title.isBlank() || description.isBlank() || imageBitmap == null) {
            showIncompleteDialog = true
            return
        }

        viewModelScope.launch {
            isLoading = true
            saveErrorMessage = null
            saveSuccess = false
            val newReport = LocalDisasterReport(
                title = title,
                description = description,
                location = locationAddress,
                latitude = currentLatitude,
                longitude = currentLongitude
            )
            val result = repository.saveReport(newReport)
            if (result.isSuccess) {
                title = ""
                description = ""
                imageBitmap = null
                saveSuccess = true
                loadReports()
            } else {
                saveErrorMessage = result.exceptionOrNull()?.message
                    ?: "Gagal mengirim laporan. Coba lagi."
            }
            isLoading = false
        }
    }

    fun sendVolunteerReport(disasterTitle: String, dataLaporan: String, catatanTambahan: String) {
        viewModelScope.launch {
            isLoading = true
            saveErrorMessage = null
            saveSuccess = false
            val formattedTitle = "[LAPORAN TUGAS - ${volunteerSkill?.name ?: "UMUM"}] Terkait: $disasterTitle"
            val formattedDescription = """
                DATA LAPORAN:
                $dataLaporan

                CATATAN TAMBAHAN:
                $catatanTambahan
            """.trimIndent()

            val newReport = LocalDisasterReport(
                title = formattedTitle,
                description = formattedDescription,
                location = locationAddress,
                reporter = volunteerName.ifBlank { "Relawan" },
                status = "Verified"
            )
            val result = repository.saveReport(newReport)
            if (result.isSuccess) {
                saveSuccess = true
                loadReports()
            } else {
                saveErrorMessage = result.exceptionOrNull()?.message
                    ?: "Gagal mengirim laporan. Coba lagi."
            }
            isLoading = false
        }
    }

    fun clearSaveState() {
        saveErrorMessage = null
        saveSuccess = false
    }


    fun updateReport(report: LocalDisasterReport) {
        viewModelScope.launch {
            repository.updateReport(report)
            loadReports()
        }
    }
}
