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
class DisasterDetailViewModel @Inject constructor(
    private val repository: DisasterReportRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<DisasterReportDto>>(UiState.Idle)
    val uiState: StateFlow<UiState<DisasterReportDto>> = _uiState.asStateFlow()

    fun loadDisaster(id: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getDisasterReportById(id.toString())
                .onSuccess { report ->
                    if (report != null) {
                        _uiState.value = UiState.Success(report)
                    } else {
                        _uiState.value = UiState.Empty
                    }
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Gagal memuat detail bencana")
                }
        }
    }
}
