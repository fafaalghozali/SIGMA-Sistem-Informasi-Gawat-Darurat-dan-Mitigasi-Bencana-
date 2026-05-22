package com.mahasiswa.sigma.ui.viewmodel

import android.Manifest
import android.app.Application
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.BmkgWarning
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.EarthquakeInfo
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.WeatherInfo
import com.mahasiswa.sigma.data.repository.DashboardRepository
import com.mahasiswa.sigma.data.repository.NewsRepository
import com.mahasiswa.sigma.data.repository.WeatherRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class DashboardUiState(
    val menuItems: List<DashboardMenuModel> = emptyList(),
    val newsItems: List<NewsItem> = emptyList(),
    val weatherInfo: WeatherInfo? = null,
    val earthquakeInfo: EarthquakeInfo? = null,
    val bmkgWarnings: List<BmkgWarning> = emptyList(),
    val showNotification: Boolean = true,
    val isLoading: Boolean = false,
    val isWeatherLoading: Boolean = false,
    val isNewsLoading: Boolean = false,
    val locationPermissionDenied: Boolean = false,
    val weatherError: String? = null,
    val newsError: String? = null,
    val lastUpdated: Long? = null,
    val newsLastUpdated: Long? = null,
    /** The reverse-geocoded city name for location prioritization. */
    val userCityName: String = "",
    /** True while waiting for the user to respond to the permission dialog. */
    val isAwaitingPermission: Boolean = false
)

class DashboardViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val dashboardRepo = DashboardRepository()
    private val weatherRepo   = WeatherRepository(application)
    private val newsRepo      = NewsRepository(application)

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private var autoRefreshJob: Job? = null

    /**
     * Loads menu items immediately (no network) and starts the news pipeline.
     * Weather is loaded only after location permission is confirmed.
     */
    fun loadDashboardData(userRole: UserRole, @Suppress("UNUSED_PARAMETER") isDark: Boolean) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, isNewsLoading = true)

            // Menu is static — load instantly
            val menu = dashboardRepo.getMenuItems(userRole)
            _uiState.value = _uiState.value.copy(
                menuItems = menu,
                isLoading = false
            )

            // Load cached news for instant display
            val cached = newsRepo.getCachedNews(_uiState.value.userCityName)
            if (cached.isNotEmpty()) {
                _uiState.value = _uiState.value.copy(
                    newsItems = cached,
                    isNewsLoading = false
                )
            }

            // BMKG earthquake + warnings (no location permission needed)
            loadEarthquake()
            loadBmkgWarnings()

            // Fetch fresh news in the background
            loadFreshNews()
        }
    }

    /**
     * Called BEFORE the permission dialog is shown.
     * Sets the weather card into a "waiting" state.
     */
    fun onPermissionRequested() {
        _uiState.value = _uiState.value.copy(
            isWeatherLoading = true,
            isAwaitingPermission = true,
            weatherError = null,
            locationPermissionDenied = false
        )
    }

    /**
     * Called after permission is granted.
     * Triggers real-location weather + re-sorts news by user location.
     */
    fun onLocationPermissionGranted() {
        _uiState.value = _uiState.value.copy(
            locationPermissionDenied = false,
            isAwaitingPermission = false
        )
        loadWeatherFromRealLocation()
        startAutoRefresh()
    }

    /** Called when the user denies location permission. */
    fun onLocationPermissionDenied() {
        _uiState.value = _uiState.value.copy(
            locationPermissionDenied = true,
            isAwaitingPermission = false,
            isWeatherLoading = false
        )
    }

    /**
     * Fetches weather using real GPS, then re-prioritizes news using the
     * resolved city name for location-aware sorting.
     */
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

                // Re-sort cached news now that we have a city name
                val resorted = newsRepo.getCachedNews(cityName)

                _uiState.value = _uiState.value.copy(
                    weatherInfo = weather,
                    isWeatherLoading = false,
                    weatherError = null,
                    lastUpdated = System.currentTimeMillis(),
                    userCityName = cityName,
                    // Only update newsItems from cache re-sort if we have data
                    newsItems = if (resorted.isNotEmpty()) resorted else _uiState.value.newsItems
                )
                
                // Re-evaluate local alerts with new city name
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

    /** Loads fresh news from all sources through the full pipeline. */
    private fun loadFreshNews() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isNewsLoading = true, newsError = null)
            try {
                val fresh = newsRepo.fetchFreshNews(_uiState.value.userCityName)
                _uiState.value = _uiState.value.copy(
                    newsItems = fresh,
                    isNewsLoading = false,
                    newsError = null,
                    newsLastUpdated = System.currentTimeMillis()
                )
            } catch (e: Exception) {
                // If we already have cached items, show them silently
                val hasCached = _uiState.value.newsItems.isNotEmpty()
                _uiState.value = _uiState.value.copy(
                    isNewsLoading = false,
                    newsError = if (hasCached) null else "Gagal memuat berita terkini"
                )
            }
        }
    }

    /**
     * User-initiated news refresh (e.g. tapping retry on error state).
     */
    fun retryNews() {
        loadFreshNews()
    }

    /** Re-checks permission then loads weather. */
    fun loadWeather() {
        val ctx = getApplication<Application>()
        val hasPermission =
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) ==
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

    /** Fetches latest BMKG earthquake and filters to user's location. */
    private fun loadEarthquake() {
        viewModelScope.launch {
            try {
                val eq = weatherRepo.getLatestEarthquake()
                val userCity = _uiState.value.userCityName
                
                if (eq != null) {
                    // Only show if it matches user's region or was felt
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

    /** Fetches BMKG recent significant earthquake warnings and filters to user's location. */
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

    /**
     * Auto-refresh: weather every 15 minutes, news every 10 minutes.
     */
    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            var tick = 0
            while (isActive) {
                delay(NEWS_REFRESH_INTERVAL_MS)
                tick++

                // News: every 10 minutes
                try {
                    val fresh = newsRepo.fetchFreshNews(_uiState.value.userCityName)
                    _uiState.value = _uiState.value.copy(
                        newsItems = fresh,
                        newsLastUpdated = System.currentTimeMillis()
                    )
                } catch (_: Exception) {}

                // Weather + earthquake: every 15 min (every 1.5 news cycles)
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
        /** 10 minutes — news refresh interval */
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
