package com.mahasiswa.sigma.ui.viewmodel

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.BmkgWarning
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.EarthquakeInfo
import com.mahasiswa.sigma.data.model.NewsDto
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.NewsSeverity
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.WeatherInfo
import com.mahasiswa.sigma.data.repository.DashboardRepository
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import com.mahasiswa.sigma.data.repository.NewsRepositoryRetrofit
import com.mahasiswa.sigma.data.repository.WeatherRepository
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.data.model.UpdateVolunteerRequest
import com.mahasiswa.sigma.data.model.VolunteerDto
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

data class DashboardUiState(
    val menuItems: List<DashboardMenuModel> = emptyList(),
    val newsItems: List<NewsItem> = emptyList(),
    val weatherInfo: WeatherInfo? = null,
    val earthquakeInfo: EarthquakeInfo? = null,
    val bmkgWarnings: List<BmkgWarning> = emptyList(),
    val localDisasterAlert: DisasterReportDto? = null,
    val showNotification: Boolean = true,
    val isLoading: Boolean = false,
    val isWeatherLoading: Boolean = false,
    val isNewsLoading: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val weatherError: String? = null,
    val newsError: String? = null,
    val lastUpdated: Long? = null,
    val newsLastUpdated: Long? = null,
    val userCityName: String = "",
    val isAwaitingPermission: Boolean = false,
    val volunteerStatus: String = "available",
    val volunteerSkill: SkillsVolunteer? = null,
    val allReports: List<DisasterReportDto> = emptyList(),
    val allVolunteers: List<VolunteerDto> = emptyList(),
    val isAdminDataLoading: Boolean = false,
    // Notifikasi penugasan relawan
    val pendingAssignment: VolunteerDto? = null,
    val showAssignmentNotification: Boolean = false,
    val isConfirmingAssignment: Boolean = false,
    val assignmentConfirmError: String? = null,
    val needsForceRelogin: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dashboardRepo: DashboardRepository,
    private val weatherRepo: WeatherRepository,
    private val newsRepo: NewsRepositoryRetrofit,
    private val disasterRepo: DisasterReportRepositoryRetrofit,
    private val volunteerRepo: VolunteerRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    fun loadVolunteerStatus(email: String) {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepo.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    _uiState.value = _uiState.value.copy(
                        // Baca kolom availability, default "available" jika null
                        volunteerStatus = volunteerDto.availability ?: "available",
                        volunteerSkill = try {
                            SkillsVolunteer.valueOf(volunteerDto.skill.uppercase())
                        } catch (_: Exception) {
                            SkillsVolunteer.MEDIS
                        }
                    )
                }
            }
        }
    }

    fun updateVolunteerAvailability(email: String, newStatus: String) {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepo.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                volunteerDto?.id?.let { volunteerId ->
                    val request = com.mahasiswa.sigma.data.model.UpdateVolunteerRequest(
                        availability = newStatus   // FIX: update kolom availability, bukan status
                    )
                    volunteerRepo.updateVolunteer(volunteerId.toString(), request)
                    _uiState.value = _uiState.value.copy(volunteerStatus = newStatus)
                }
            }
        }
    }

    fun loadDashboardData(userRole: UserRole, @Suppress("UNUSED_PARAMETER") isDark: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isNewsLoading = true)

            val menu = dashboardRepo.getMenuItems(userRole)
            _uiState.value = _uiState.value.copy(
                menuItems = menu,
                isLoading = false
            )

            loadEarthquake()
            loadBmkgWarnings()
            loadLocalDisasterAlerts()

            loadFreshNews()

            if (userRole == UserRole.BNPB) {
                loadAdminStatistics()
            }
        }
    }

    fun loadAdminStatistics() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isAdminDataLoading = true)
            val reportsResult = disasterRepo.getAllDisasterReports()
            val volunteersResult = volunteerRepo.getAllVolunteers()

            val reports = reportsResult.getOrDefault(emptyList())
            val volunteers = volunteersResult.getOrDefault(emptyList())

            _uiState.value = _uiState.value.copy(
                allReports = reports,
                allVolunteers = volunteers,
                isAdminDataLoading = false
            )
        }
    }

    fun onPermissionRequested() {
        _uiState.value = _uiState.value.copy(
            isWeatherLoading = true,
            isAwaitingPermission = true,
            weatherError = null,
            locationPermissionDenied = false
        )
    }

    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            locationPermissionDenied = false,
            isAwaitingPermission = false
        )
        loadWeatherFromRealLocation()
        startAutoRefresh()
    }

    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            locationPermissionDenied = true,
            isAwaitingPermission = false,
            isWeatherLoading = false
        )
    }

    private fun loadWeatherFromRealLocation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isWeatherLoading = true,
                weatherError = null,
                locationPermissionDenied = false
            )
            try {
                val weather = weatherRepo.getWeatherForCurrentLocation()
                val cityName = weather.location
                    .removePrefix("(Default)")
                    .trim()

                _uiState.value = _uiState.value.copy(
                    weatherInfo = weather,
                    isWeatherLoading = false,
                    weatherError = null,
                    lastUpdated = System.currentTimeMillis(),
                    userCityName = cityName
                )

                loadEarthquake()
                loadBmkgWarnings()
            } catch (e: WeatherRepository.LocationUnavailableException) {
                try {
                    val fallback = weatherRepo.getWeatherForFallbackLocation()
                    _uiState.value = _uiState.value.copy(
                        weatherInfo = fallback,
                        isWeatherLoading = false,
                        weatherError = null,
                        lastUpdated = System.currentTimeMillis(),
                        userCityName = "Surakarta"
                    )
                } catch (_: Exception) {
                    _uiState.value = _uiState.value.copy(
                        isWeatherLoading = false,
                        weatherError = "Gagal mengambil data cuaca"
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isWeatherLoading = false,
                    weatherError = "Gagal mengambil data cuaca"
                )
            }
        }
    }

    private fun loadFreshNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNewsLoading = true, newsError = null)
            newsRepo.getAllNews()
                .onSuccess { newsList ->
                    val items = newsList.map { it.toNewsItem() }
                    _uiState.value = _uiState.value.copy(
                        newsItems = items,
                        isNewsLoading = false,
                        newsError = null,
                        newsLastUpdated = System.currentTimeMillis()
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        isNewsLoading = false,
                        newsError = if (_uiState.value.newsItems.isEmpty()) 
                            (error.message ?: "Gagal memuat berita") else null
                    )
                }
        }
    }

    private fun NewsDto.toNewsItem(): NewsItem {
        val timeStr = publishedAt?.let { ts ->
            try {
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                val date = sdf.parse(ts)
                if (date != null) {
                    val diff = System.currentTimeMillis() - date.time
                    when {
                        diff < 60_000 -> "Baru saja"
                        diff < 3_600_000 -> "${diff / 60_000} mnt lalu"
                        diff < 86_400_000 -> "${diff / 3_600_000} jam lalu"
                        else -> SimpleDateFormat("dd MMM", Locale("id")).format(date)
                    }
                } else ts
            } catch (_: Exception) { ts }
        } ?: ""

        val publishedAtMillis = publishedAt?.let { ts ->
            try {
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(ts)?.time ?: 0L
            } catch (_: Exception) { 0L }
        } ?: 0L

        return NewsItem(
            id = id?.toString() ?: title,
            title = title,
            time = timeStr,
            publishedAt = publishedAtMillis,
            category = "BERITA",
            categoryColor = Color(0xFF1565C0),
            imageUrl = imageUrl,
            source = source ?: "SIGMA",
            link = url ?: "",
            severity = NewsSeverity.INFO,
            isOfficial = true,
            region = null
        )
    }

    fun retryNews() {
        loadFreshNews()
    }

    fun loadWeather() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (!hasPermission) {
            _uiState.value = _uiState.value.copy(
                locationPermissionDenied = true,
                isWeatherLoading = false
            )
            return
        }
        loadWeatherFromRealLocation()
        startAutoRefresh()
    }

    fun retryWeather() { loadWeather() }

    private fun loadEarthquake() {
        viewModelScope.launch {
            try {
                val eq = weatherRepo.getLatestEarthquake()
                val userCity = _uiState.value.userCityName

                if (eq != null) {
                    val isRelevant = isEventRelevant(eq.location, userCity) ||
                                     (eq.felt.isNotBlank() && eq.felt != "Tidak dirasakan")
                    if (isRelevant) {
                        _uiState.value = _uiState.value.copy(earthquakeInfo = eq)
                    } else {
                        _uiState.value = _uiState.value.copy(earthquakeInfo = null)
                    }
                } else {
                    _uiState.value = _uiState.value.copy(earthquakeInfo = null)
                }
            } catch (_: Exception) {}
        }
    }

    private fun loadBmkgWarnings() {
        viewModelScope.launch {
            try {
                val warnings = weatherRepo.getRecentBmkgWarnings()
                val userCity = _uiState.value.userCityName

                val localWarnings = warnings.filter {
                    isEventRelevant(it.type, userCity) || isEventRelevant(it.message, userCity)
                }
                _uiState.value = _uiState.value.copy(bmkgWarnings = localWarnings)
            } catch (_: Exception) {}
        }
    }

    private fun loadLocalDisasterAlerts() {
        viewModelScope.launch {
            try {
                disasterRepo.getAllDisasterReports().onSuccess { reports ->
                    val userCity = _uiState.value.userCityName
                    // Find active disaster reports (SIAGA_1 or AWAS) in user's area
                    val activeStatuses = listOf("SIAGA_1", "SIAGA_2", "AWAS", "Siaga 1", "Siaga 2", "Awas")
                    val localAlert = reports
                        .filter { it.status in activeStatuses }
                        .firstOrNull { report ->
                            isEventRelevant(report.location, userCity) ||
                            isEventRelevant(report.title, userCity)
                        }
                    _uiState.value = _uiState.value.copy(localDisasterAlert = localAlert)
                }
            } catch (_: Exception) {}
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(NEWS_REFRESH_INTERVAL_MS)
                tick++

                loadFreshNews()

                if (tick % 2 != 0) {
                    try {
                        val weather = weatherRepo.getWeatherForCurrentLocation()
                        _uiState.value = _uiState.value.copy(
                            weatherInfo = weather,
                            lastUpdated = System.currentTimeMillis()
                        )
                    } catch (_: Exception) {}
                    loadEarthquake()
                    loadBmkgWarnings()
                }
            }
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(showNotification = false)
    }

    // ==================== ASSIGNMENT NOTIFICATION FOR MASYARAKAT ====================

    private var assignmentPollingJob: Job? = null

    /**
     * Mulai polling penugasan relawan untuk user yang login sebagai Masyarakat.
     * Jika admin approve volunteer & beri penugasan, notifikasi muncul di Dashboard.
     */
    fun startAssignmentPolling() {
        assignmentPollingJob?.cancel()
        assignmentPollingJob = viewModelScope.launch {
            while (isActive) {
                checkPendingAssignment()
                delay(ASSIGNMENT_POLL_INTERVAL_MS)
            }
        }
    }

    /**
     * Cek sekali apakah ada penugasan yang perlu dikonfirmasi relawan.
     */
    fun checkPendingAssignment() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepo.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    val isApproved = volunteerDto.status.equals("APPROVED", ignoreCase = true) ||
                            volunteerDto.status.equals("ACCEPTED", ignoreCase = true)
                    val hasPendingAssignment = isApproved &&
                            (volunteerDto.assignmentStatus.isNullOrBlank() ||
                                    volunteerDto.assignmentStatus.equals("pending", ignoreCase = true))
                    if (hasPendingAssignment) {
                        _uiState.value = _uiState.value.copy(
                            pendingAssignment = volunteerDto,
                            showAssignmentNotification = true
                        )
                    }
                }
            }
        }
    }

    /**
     * Relawan konfirmasi bersedia / menolak penugasan dari Dashboard.
     *
     * accept = true → assignment_status = "accepted", role → RELAWAN, force relogin
     * accept = false → status = PENDING, assignment dihapus, role tetap MASYARAKAT
     */
    fun confirmAssignment(accept: Boolean) {
        val volunteer = _uiState.value.pendingAssignment ?: return
        val vid = volunteer.id ?: return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isConfirmingAssignment = true,
                assignmentConfirmError = null
            )

            if (accept) {
                val request = UpdateVolunteerRequest(
                    assignmentStatus = "accepted",
                    updatedAt = nowTimestamp()
                )
                val result = volunteerRepo.updateVolunteer(vid.toString(), request)
                result.onSuccess {
                    // Upgrade role ke RELAWAN
                    val userId = authManager.getCurrentUserId()
                    if (!userId.isNullOrBlank()) {
                        authManager.updateUserRole(userId, UserRole.RELAWAN)
                    }
                    _uiState.value = _uiState.value.copy(
                        isConfirmingAssignment = false,
                        showAssignmentNotification = false,
                        needsForceRelogin = true
                    )
                }
                result.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isConfirmingAssignment = false,
                        assignmentConfirmError = e.message ?: "Gagal mengonfirmasi. Coba lagi."
                    )
                }
            } else {
                // Tolak: reset ke PENDING, hapus penugasan, downgrade role ke MASYARAKAT
                val request = UpdateVolunteerRequest(
                    status = "PENDING",
                    assignment = null,
                    disasterId = null,
                    assignmentStatus = null,
                    updatedAt = nowTimestamp()
                )
                val result = volunteerRepo.updateVolunteer(vid.toString(), request)
                result.onSuccess {
                    // Downgrade role kembali ke MASYARAKAT
                    val userId = authManager.getCurrentUserId()
                    if (!userId.isNullOrBlank()) {
                        authManager.updateUserRole(userId, UserRole.MASYARAKAT)
                    }
                    _uiState.value = _uiState.value.copy(
                        isConfirmingAssignment = false,
                        showAssignmentNotification = false,
                        pendingAssignment = null
                    )
                }
                result.onFailure { e ->
                    _uiState.value = _uiState.value.copy(
                        isConfirmingAssignment = false,
                        assignmentConfirmError = e.message ?: "Gagal menolak penugasan. Coba lagi."
                    )
                }
            }
        }
    }

    fun dismissAssignmentNotification() {
        _uiState.value = _uiState.value.copy(showAssignmentNotification = false)
    }

    fun dismissAssignmentError() {
        _uiState.value = _uiState.value.copy(assignmentConfirmError = null)
    }

    fun consumeForceRelogin() {
        _uiState.value = _uiState.value.copy(needsForceRelogin = false)
    }

    private fun nowTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
        assignmentPollingJob?.cancel()
    }

    companion object {
        private const val NEWS_REFRESH_INTERVAL_MS = 10 * 60 * 1_000L
        private const val ASSIGNMENT_POLL_INTERVAL_MS = 15_000L // 15 detik
    }

    private fun isEventRelevant(eventLocation: String, userCity: String): Boolean {
        if (userCity.isBlank()) return false
        val loc = eventLocation.lowercase()
        val city = userCity.lowercase()

        if (loc.contains(city)) return true

        when (city) {
            "surakarta", "solo" -> if (loc.contains("jawa tengah") || loc.contains("solo") || loc.contains("sukoharjo") || loc.contains("karanganyar") || loc.contains("boyolali") || loc.contains("sragen") || loc.contains("klaten") || loc.contains("wonogiri")) return true
            "jakarta" -> if (loc.contains("dki") || loc.contains("banten") || loc.contains("jawa barat")) return true
            "bandung" -> if (loc.contains("jawa barat") || loc.contains("cimahi") || loc.contains("jabar")) return true
            "semarang" -> if (loc.contains("jawa tengah") || loc.contains("jateng") || loc.contains("demak") || loc.contains("kendal")) return true
            "surabaya" -> if (loc.contains("jawa timur") || loc.contains("jatim") || loc.contains("sidoarjo") || loc.contains("gresik")) return true
            "medan" -> if (loc.contains("sumatera utara") || loc.contains("sumut") || loc.contains("deli")) return true
            "makassar" -> if (loc.contains("sulawesi selatan") || loc.contains("sulsel") || loc.contains("gowa")) return true
            "yogyakarta", "jogja" -> if (loc.contains("yogyakarta") || loc.contains("diy") || loc.contains("sleman") || loc.contains("bantul") || loc.contains("gunungkidul")) return true
        }
        return false
    }
}
