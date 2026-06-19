package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.repository.DisasterReportRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchDisasterViewModel @Inject constructor(
    private val repository: DisasterReportRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<DisasterReportDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<DisasterReportDto>>> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedStatus = MutableStateFlow("Semua")
    val selectedStatus: StateFlow<String> = _selectedStatus.asStateFlow()

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getAllDisasterReports()
                .onSuccess { reports ->
                    _uiState.value = if (reports.isEmpty()) UiState.Empty
                    else UiState.Success(reports)
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Gagal memuat laporan")
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.value = query
    }

    fun onStatusFilterChange(status: String) {
        _selectedStatus.value = status
    }

    fun retry() = loadReports()
}
