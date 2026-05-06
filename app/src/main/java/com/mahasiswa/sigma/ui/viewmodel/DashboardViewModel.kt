package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.DashboardMenuModel
import com.mahasiswa.sigma.data.model.NewsItem
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.repository.DashboardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class DashboardUiState(
    val menuItems: List<DashboardMenuModel> = emptyList(),
    val newsItems: List<NewsItem> = emptyList(),
    val showNotification: Boolean = true
)

class DashboardViewModel(
    private val repository: DashboardRepository = DashboardRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    fun loadDashboardData(userRole: UserRole, isDark: Boolean) {
        viewModelScope.launch {
            val menu = repository.getMenuItems(userRole)
            val news = repository.getNewsItems(isDark)
            _uiState.value = _uiState.value.copy(
                menuItems = menu,
                newsItems = news
            )
        }
    }

    fun dismissNotification() {
        _uiState.value = _uiState.value.copy(showNotification = false)
    }
}
