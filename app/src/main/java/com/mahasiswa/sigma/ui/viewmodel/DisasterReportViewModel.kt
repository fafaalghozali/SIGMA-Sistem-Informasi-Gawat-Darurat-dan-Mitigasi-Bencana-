package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.CreateDisasterReportRequest
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.remote.api.SupabaseStorageService
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DisasterReportViewModel @Inject constructor(
    private val repository: DisasterReportRepositoryRetrofit,
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val storageService: SupabaseStorageService,
    private val authManager: AuthManager
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
    private var volunteerId: String? = null

    var showIncompleteDialog by mutableStateOf(false)
    var showPhotoSourceSheet by mutableStateOf(false)
    var isLoading by mutableStateOf(false)
    var saveErrorMessage by mutableStateOf<String?>(null)
    var saveSuccess by mutableStateOf(false)

    fun loadVolunteerSkill(email: String) {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    volunteerId = volunteerDto.id?.toString()
                    volunteerSkill = try {
                        SkillsVolunteer.valueOf(volunteerDto.skill.uppercase())
                    } catch (_: Exception) {
                        SkillsVolunteer.MEDIS
                    }
                    volunteerName = volunteerDto.name
                }
            }
        }
    }

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId()
            val result = repository.getAllDisasterReports()
            result.onSuccess { reports ->
                // Only show reports belonging to current user
                _reports.value = reports
                    .filter { it.userId == userId }
                    .map { dto ->
                        LocalDisasterReport(
                            id = dto.id?.toString() ?: "",
                            title = dto.title,
                            description = dto.description,
                            location = dto.location,
                            reporter = dto.reporterName,
                            status = dto.status,
                            latitude = dto.latitude,
                            longitude = dto.longitude
                        )
                    }
            }
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
        if (title.isBlank() || description.isBlank()) {
            showIncompleteDialog = true
            return
        }

        viewModelScope.launch {
            isLoading = true
            saveErrorMessage = null
            saveSuccess = false
            
            val userId = authManager.getCurrentUserId()
            val userName = try { authManager.getUserName() } catch (_: Exception) { "Warga" }

            // Upload photo to Supabase Storage if available
            var photoUrl: String? = null
            if (imageBitmap != null) {
                photoUrl = storageService.uploadImage(imageBitmap!!, "report")
            }

            val photoUrlJson = if (photoUrl != null) "[\"$photoUrl\"]" else null

            val request = CreateDisasterReportRequest(
                userId = userId,
                title = title,
                description = description,
                disasterType = null,
                location = locationAddress,
                latitude = currentLatitude,
                longitude = currentLongitude,
                reporterName = userName.ifBlank { "Warga" },
                photoUrl = photoUrlJson
            )
            
            val result = repository.createDisasterReport(request)
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

            // Ensure we have the volunteer id (resolve lazily if needed).
            if (volunteerId == null) {
                val userId = authManager.getCurrentUserId()
                if (userId != null) {
                    volunteerRepository.getVolunteerByUserId(userId).onSuccess { volunteerId = it?.id?.toString() }
                }
            }

            val resolvedVolunteerId = volunteerId
            if (resolvedVolunteerId == null) {
                saveErrorMessage = "Anda belum terdaftar sebagai relawan."
                isLoading = false
                return@launch
            }

            val reportData = "Terkait: $disasterTitle\n$dataLaporan"
            val request = com.mahasiswa.sigma.data.model.CreateVolunteerReportRequest(
                volunteerId = resolvedVolunteerId,
                disasterId = null,
                skillType = volunteerSkill?.name,
                reportData = reportData,
                notes = catatanTambahan.ifBlank { null },
                photoUrls = null
            )

            val result = volunteerRepository.createVolunteerReport(request)
            if (result.isSuccess) {
                saveSuccess = true
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
            val request = com.mahasiswa.sigma.data.model.UpdateDisasterReportRequest(
                status = report.status,
                title = report.title,
                description = report.description,
                location = report.location
            )
            repository.updateDisasterReport(report.id, request)
            loadReports()
        }
    }
}
