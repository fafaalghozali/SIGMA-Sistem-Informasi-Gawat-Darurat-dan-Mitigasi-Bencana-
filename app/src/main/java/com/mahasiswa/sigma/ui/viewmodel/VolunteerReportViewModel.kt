package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.CreateVolunteerReportRequest
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.data.model.VolunteerReportDto
import com.mahasiswa.sigma.data.model.VolunteerReportWithDetails
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
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
 * BNPB sees all reports in a filterable flat list with volunteer details;
 * a volunteer sees only their own reports with CRUD capabilities.
 */
@HiltViewModel
class VolunteerReportViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val disasterRepository: DisasterReportRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    // ==================== RELAWAN STATE (unchanged) ====================

    private val _uiState = MutableStateFlow<UiState<List<VolunteerReportDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<VolunteerReportDto>>> = _uiState.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _currentVolunteer = MutableStateFlow<VolunteerDto?>(null)
    val currentVolunteer: StateFlow<VolunteerDto?> = _currentVolunteer.asStateFlow()

    private val _disasters = MutableStateFlow<List<DisasterReportDto>>(emptyList())
    val disasters: StateFlow<List<DisasterReportDto>> = _disasters.asStateFlow()

    /** Resolved volunteer id for the current user (null if not a registered volunteer). */
    private var currentVolunteerId: String? = null
    val canCreate: Boolean get() = currentVolunteerId != null

    // ==================== BNPB STATE ====================

    private val _bnpbReports = MutableStateFlow<UiState<List<VolunteerReportWithDetails>>>(UiState.Idle)
    val bnpbReports: StateFlow<UiState<List<VolunteerReportWithDetails>>> = _bnpbReports.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedSkillFilter = MutableStateFlow<String?>(null)
    val selectedSkillFilter: StateFlow<String?> = _selectedSkillFilter.asStateFlow()

    private val _selectedDisasterFilter = MutableStateFlow<String?>(null)
    val selectedDisasterFilter: StateFlow<String?> = _selectedDisasterFilter.asStateFlow()

    // All enriched reports (unfiltered cache)
    private var allBnpbReports: List<VolunteerReportWithDetails> = emptyList()

    // ==================== COMMON ====================

    private suspend fun resolveVolunteer(): VolunteerDto? {
        val current = _currentVolunteer.value
        if (current != null) return current
        val userId = authManager.getCurrentUserId() ?: return null
        volunteerRepository.getVolunteerByUserId(userId).onSuccess {
            _currentVolunteer.value = it
            currentVolunteerId = it?.id?.toString()
        }
        return _currentVolunteer.value
    }

    fun loadDisasters() {
        viewModelScope.launch {
            disasterRepository.getAllDisasterReports().onSuccess { list ->
                _disasters.value = list
            }
        }
    }

    fun load(role: UserRole) {
        if (role == UserRole.BNPB) {
            loadBnpbView()
        } else {
            loadRelawanView()
        }
    }

    // ==================== RELAWAN FLOW (unchanged) ====================

    private fun loadRelawanView() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            loadDisasters()

            val volunteer = resolveVolunteer()
            if (volunteer == null) {
                _uiState.value = UiState.Error("Anda belum terdaftar sebagai relawan.")
                return@launch
            }
            volunteerRepository.getVolunteerReportsByVolunteerId(volunteer.id.toString())
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
            val volunteer = resolveVolunteer()
            if (volunteer == null) {
                _operationMessage.value = "Anda belum terdaftar sebagai relawan."
                _isProcessing.value = false
                return@launch
            }
            val request = CreateVolunteerReportRequest(
                volunteerId = volunteer.id.toString(),
                disasterId = disasterId,
                skillType = skillType ?: volunteer.skill,
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

    // ==================== BNPB FLOW ====================

    private fun loadBnpbView() {
        viewModelScope.launch {
            _bnpbReports.value = UiState.Loading

            // Fetch all disasters
            val disastersResult = disasterRepository.getAllDisasterReports()
            val disasterList = disastersResult.getOrDefault(emptyList())
            _disasters.value = disasterList

            // Fetch all volunteers (for name + posko/assignment lookup)
            val volunteersResult = volunteerRepository.getAllVolunteers()
            val allVolunteers = volunteersResult.getOrDefault(emptyList())
            val volunteersMap = allVolunteers.associateBy { it.id.toString() }

            // Fetch all reports
            val reportsResult = volunteerRepository.getAllVolunteerReports()
            if (reportsResult.isFailure) {
                _bnpbReports.value = UiState.Error(
                    reportsResult.exceptionOrNull()?.message ?: "Gagal memuat laporan"
                )
                return@launch
            }
            val reports = reportsResult.getOrDefault(emptyList())

            // Enrich each report with volunteer name, posko, disaster title
            val enriched = reports.map { report ->
                val volunteer = volunteersMap[report.volunteerId]
                val disaster = disasterList.find { it.id?.toString() == report.disasterId }
                VolunteerReportWithDetails(
                    report = report,
                    volunteerName = volunteer?.name ?: "Guest",
                    poskoName = volunteer?.assignment ?: "-",
                    disasterTitle = disaster?.title ?: "-"
                )
            }

            allBnpbReports = enriched

            if (enriched.isEmpty()) {
                _bnpbReports.value = UiState.Empty
            } else {
                _bnpbReports.value = UiState.Success(applyBnpbFilters(enriched))
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
        applyBnpbFiltersAndUpdate()
    }

    fun updateSkillFilter(skill: String?) {
        _selectedSkillFilter.value = skill
        applyBnpbFiltersAndUpdate()
    }

    fun updateDisasterFilter(disasterTitle: String?) {
        _selectedDisasterFilter.value = disasterTitle
        applyBnpbFiltersAndUpdate()
    }

    private fun applyBnpbFiltersAndUpdate() {
        val filtered = applyBnpbFilters(allBnpbReports)
        _bnpbReports.value = if (filtered.isEmpty() && allBnpbReports.isNotEmpty()) {
            UiState.Success(emptyList()) // show "no results" but not the empty-state
        } else if (filtered.isEmpty()) {
            UiState.Empty
        } else {
            UiState.Success(filtered)
        }
    }

    private fun applyBnpbFilters(reports: List<VolunteerReportWithDetails>): List<VolunteerReportWithDetails> {
        var filtered = reports

        val query = _searchQuery.value
        if (query.isNotBlank()) {
            filtered = filtered.filter { r ->
                r.volunteerName.contains(query, ignoreCase = true) ||
                        r.disasterTitle.contains(query, ignoreCase = true) ||
                        r.poskoName.contains(query, ignoreCase = true) ||
                        r.report.notes?.contains(query, ignoreCase = true) == true
            }
        }

        val skillFilter = _selectedSkillFilter.value
        if (!skillFilter.isNullOrBlank()) {
            filtered = filtered.filter { r ->
                r.report.skillType.equals(skillFilter, ignoreCase = true)
            }
        }

        val disasterFilter = _selectedDisasterFilter.value
        if (!disasterFilter.isNullOrBlank()) {
            filtered = filtered.filter { r ->
                r.disasterTitle.equals(disasterFilter, ignoreCase = true)
            }
        }

        return filtered
    }
}
