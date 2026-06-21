package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.data.model.ShelterMapItem
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import com.mahasiswa.sigma.data.repository.ShelterRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val verifiedReports: List<LocalDisasterReport> = emptyList(),
    val shelters: List<ShelterMapItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedReport: LocalDisasterReport? = null,
    val selectedShelter: ShelterMapItem? = null,
    val showReportLayer: Boolean = true,
    val showShelterLayer: Boolean = true,
    val cameraTarget: LatLng = LatLng(-7.5569, 110.8581)
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val reportRepository: DisasterReportRepositoryRetrofit,
    private val shelterRepository: ShelterRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private fun ShelterDto.toMapItem(): ShelterMapItem = ShelterMapItem(
        id = id,
        name = name,
        address = address,
        capacity = "$capacityCurrent/$capacityMax",
        status = when (status.lowercase()) {
            "active", "tersedia", "available" -> "Tersedia"
            "full", "penuh" -> "Penuh"
            else -> status.replaceFirstChar { it.uppercase() }
        },
        latitude = latitude,
        longitude = longitude,
        logistics = logistics ?: emptyList(),
        contactPhone = contactPhone,
        photoUrl = photoUrl
    )

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val shelters = shelterRepository.getAllShelters()
                .map { list -> list.map { it.toMapItem() } }
                .getOrDefault(emptyList())

            val result = reportRepository.getAllDisasterReports()
            result.onSuccess { reports ->
                val verified = reports
                    .filter { report ->
                        report.latitude != 0.0 && report.longitude != 0.0
                    }
                    .map { dto ->
                        val timestamp = try {
                            dto.createdAt?.let {
                                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                                sdf.parse(it)?.time
                            } ?: System.currentTimeMillis()
                        } catch (_: Exception) {
                            System.currentTimeMillis()
                        }
                        LocalDisasterReport(
                            id = dto.id?.toString() ?: "",
                            title = dto.title,
                            description = dto.description,
                            location = dto.location,
                            reporter = dto.reporterName,
                            status = dto.status,
                            latitude = dto.latitude,
                            longitude = dto.longitude,
                            timestamp = timestamp
                        )
                    }
                _uiState.value = _uiState.value.copy(
                    verifiedReports = verified,
                    shelters = shelters,
                    isLoading = false
                )
            }
            result.onFailure {
                _uiState.value = _uiState.value.copy(
                    verifiedReports = emptyList(),
                    shelters = shelters,
                    isLoading = false
                )
            }
        }
    }

    fun selectReport(report: LocalDisasterReport?) {
        _uiState.value = _uiState.value.copy(selectedReport = report, selectedShelter = null)
    }

    fun selectShelter(shelter: ShelterMapItem?) {
        _uiState.value = _uiState.value.copy(selectedShelter = shelter, selectedReport = null)
    }

    fun toggleReportLayer() {
        _uiState.value = _uiState.value.copy(
            showReportLayer = !_uiState.value.showReportLayer,
            selectedReport = if (_uiState.value.showReportLayer) null else _uiState.value.selectedReport
        )
    }

    fun toggleShelterLayer() {
        _uiState.value = _uiState.value.copy(
            showShelterLayer = !_uiState.value.showShelterLayer,
            selectedShelter = if (_uiState.value.showShelterLayer) null else _uiState.value.selectedShelter
        )
    }

    fun dismissBottomSheet() {
        _uiState.value = _uiState.value.copy(selectedReport = null, selectedShelter = null)
    }
}
