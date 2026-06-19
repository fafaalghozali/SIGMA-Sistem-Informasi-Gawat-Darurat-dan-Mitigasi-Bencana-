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
    val volunteerStatus: String = "Pending",
    val volunteerSkill: SkillsVolunteer? = null
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
                        volunteerStatus = volunteerDto.status,
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
                        status = newStatus
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

    override fun onCleared() {
        super.onCleared()
        autoRefreshJob?.cancel()
    }

    companion object {
        private const val NEWS_REFRESH_INTERVAL_MS = 10 * 60 * 1_000L
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
