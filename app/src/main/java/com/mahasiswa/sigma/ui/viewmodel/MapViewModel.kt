package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.model.LatLng
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.ShelterMock
import com.mahasiswa.sigma.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MapUiState(
    val verifiedReports: List<LocalDisasterReport> = emptyList(),
    val shelters: List<ShelterMock> = emptyList(),
    val isLoading: Boolean = false,
    val selectedReport: LocalDisasterReport? = null,
    val selectedShelter: ShelterMock? = null,
    val showReportLayer: Boolean = true,
    val showShelterLayer: Boolean = true,
    val cameraTarget: LatLng = LatLng(-7.5569, 110.8581)
)

@HiltViewModel
class MapViewModel @Inject constructor(
    private val reportRepository: ReportRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MapUiState())
    val uiState: StateFlow<MapUiState> = _uiState.asStateFlow()

    private val shelterData: List<ShelterMock> = listOf(
        ShelterMock("Stadion UNS", "1.2 km", "80/100", "Tersedia", -7.556303, 110.8580877, listOf("Sembako", "Air Mineral", "Selimut")),
        ShelterMock("Taman Cerdas Jebres", "1.5 km", "50/50", "Penuh", -7.5541321, 110.8536159, listOf("Popok Bayi", "Susu Formula", "Obat-obatan")),
        ShelterMock("Solo Techno Park", "2.2 km", "30/200", "Tersedia", -7.5560692, 110.8538666, listOf("Pakaian Layak Pakai", "Alat Mandi")),
        ShelterMock("SAR UNS", "0.8 km", "10/40", "Tersedia", -7.5615699, 110.8594894, listOf("Makanan Instan", "Tikar")),
        ShelterMock("Javanologi UNS", "0.7 km", "127/250", "Tersedia", -7.556998, 110.8598277, listOf("Makanan Instan", "Alat Mandi", "Pakaian Layak Pakai")),
        ShelterMock("UNS Tower", "0.45 km", "45/125", "Tersedia", -7.5638533, 110.8555975, listOf("Susu Formula", "Obat-obatan", "Selimut")),
        ShelterMock("Asrama Mahasiswa UNS", "2.4 km", "300/300", "Penuh", -7.554193, 110.865799, listOf("Alat Mandi", "Sembako", "Sleeping Bag")),
        ShelterMock("Sekolah Vokasi UNS", "2.6 km", "145/340", "Tersedia", -7.559502, 110.8383739, listOf("Makanan Instan", "Obat-obatan", "Air Mineral"))
    )

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val allReports = reportRepository.getAllReports()
            val verified = allReports.filter { report ->
                report.status in listOf("Verified", "Siaga 1", "Siaga 2", "Awas", "Resolved")
                    && report.latitude != 0.0 && report.longitude != 0.0
            }
            _uiState.value = _uiState.value.copy(
                verifiedReports = verified,
                shelters = shelterData,
                isLoading = false
            )
        }
    }

    fun selectReport(report: LocalDisasterReport?) {
        _uiState.value = _uiState.value.copy(selectedReport = report, selectedShelter = null)
    }

    fun selectShelter(shelter: ShelterMock?) {
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
