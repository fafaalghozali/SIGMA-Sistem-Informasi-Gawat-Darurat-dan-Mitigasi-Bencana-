package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.CreateShelterRequest
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.data.model.UpdateShelterRequest
import com.mahasiswa.sigma.data.remote.api.SupabaseStorageService
import com.mahasiswa.sigma.data.repository.ShelterRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Shelter feature.
 *
 * Full Retrofit flow: ShelterViewModel -> ShelterRepositoryRetrofit -> SupabaseApiService.
 * Exposes a [UiState] for list rendering (Loading / Success / Error / Empty)
 * and an operation message channel for create/update/delete feedback.
 */
@HiltViewModel
class ShelterViewModel @Inject constructor(
    private val shelterRepository: ShelterRepositoryRetrofit,
    private val storageService: SupabaseStorageService
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<ShelterDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<ShelterDto>>> = _uiState.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        loadShelters()
    }

    fun loadShelters() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            shelterRepository.getAllShelters()
                .onSuccess { shelters ->
                    _uiState.value = if (shelters.isEmpty()) {
                        UiState.Empty
                    } else {
                        UiState.Success(shelters)
                    }
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Gagal memuat data posko")
                }
        }
    }

    fun refresh() = loadShelters()

    fun createShelter(request: CreateShelterRequest) {
        viewModelScope.launch {
            _isProcessing.value = true
            shelterRepository.createShelter(request)
                .onSuccess {
                    _operationMessage.value = "Posko \"${it.name}\" berhasil ditambahkan"
                    loadShelters()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menambahkan posko"
                }
            _isProcessing.value = false
        }
    }

    fun createShelterWithPhoto(request: CreateShelterRequest, photoBitmap: Bitmap?) {
        viewModelScope.launch {
            _isProcessing.value = true
            val photoUrl = if (photoBitmap != null) {
                storageService.uploadImage(photoBitmap, "shelter")
            } else null

            val finalRequest = if (photoUrl != null) {
                request.copy(photoUrl = photoUrl)
            } else request

            shelterRepository.createShelter(finalRequest)
                .onSuccess {
                    _operationMessage.value = "Posko \"${it.name}\" berhasil ditambahkan"
                    loadShelters()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menambahkan posko"
                }
            _isProcessing.value = false
        }
    }

    fun updateShelterWithPhoto(id: String, request: UpdateShelterRequest, photoBitmap: Bitmap?) {
        viewModelScope.launch {
            _isProcessing.value = true
            val photoUrl = if (photoBitmap != null) {
                storageService.uploadImage(photoBitmap, "shelter")
            } else null

            val finalRequest = if (photoUrl != null) {
                request.copy(photoUrl = photoUrl)
            } else request

            shelterRepository.updateShelter(id, finalRequest)
                .onSuccess {
                    _operationMessage.value = "Posko \"${it.name}\" berhasil diperbarui"
                    loadShelters()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal memperbarui posko"
                }
            _isProcessing.value = false
        }
    }

    fun updateShelter(id: String, request: UpdateShelterRequest) {
        viewModelScope.launch {
            _isProcessing.value = true
            shelterRepository.updateShelter(id, request)
                .onSuccess {
                    _operationMessage.value = "Posko \"${it.name}\" berhasil diperbarui"
                    loadShelters()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal memperbarui posko"
                }
            _isProcessing.value = false
        }
    }

    fun deleteShelter(id: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            shelterRepository.deleteShelter(id)
                .onSuccess {
                    _operationMessage.value = "Posko berhasil dihapus"
                    loadShelters()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menghapus posko"
                }
            _isProcessing.value = false
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
