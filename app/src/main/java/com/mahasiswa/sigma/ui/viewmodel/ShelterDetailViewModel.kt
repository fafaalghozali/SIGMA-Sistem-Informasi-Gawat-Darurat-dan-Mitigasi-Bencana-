package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.data.repository.ShelterRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ShelterDetailViewModel @Inject constructor(
    private val repository: ShelterRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<ShelterDto>>(UiState.Idle)
    val uiState: StateFlow<UiState<ShelterDto>> = _uiState.asStateFlow()

    fun loadShelter(id: Int) {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            repository.getShelterById(id.toString())
                .onSuccess { shelter ->
                    if (shelter != null) {
                        _uiState.value = UiState.Success(shelter)
                    } else {
                        _uiState.value = UiState.Empty
                    }
                }
                .onFailure { e ->
                    _uiState.value = UiState.Error(e.message ?: "Gagal memuat detail posko")
                }
        }
    }
}
