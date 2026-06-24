package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.ProfileDto
import com.mahasiswa.sigma.data.repository.ProfileRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileListUiState {
    object Idle : ProfileListUiState()
    object Loading : ProfileListUiState()
    data class Success(val profiles: List<ProfileDto>) : ProfileListUiState()
    data class Error(val message: String) : ProfileListUiState()
    object Empty : ProfileListUiState()
}

/**
 * ViewModel for the Profile List screen (Retrofit + StateFlow + Hilt).
 */
@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow<ProfileListUiState>(ProfileListUiState.Idle)
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = ProfileListUiState.Loading

            profileRepository.getAllProfiles()
                .onSuccess { profiles ->
                    _uiState.value = if (profiles.isEmpty()) {
                        ProfileListUiState.Empty
                    } else {
                        ProfileListUiState.Success(profiles)
                    }
                }
                .onFailure { error ->
                    _uiState.value = ProfileListUiState.Error(
                        error.message ?: "Failed to load profiles"
                    )
                }
        }
    }

    fun refresh() {
        loadProfiles()
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            _uiState.value = ProfileListUiState.Loading

            profileRepository.deleteProfile(profileId)
                .onSuccess {
                    loadProfiles()
                }
                .onFailure { error ->
                    _uiState.value = ProfileListUiState.Error(
                        error.message ?: "Failed to delete profile"
                    )
                }
        }
    }

    fun editProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            _uiState.value = ProfileListUiState.Loading

            val request = com.mahasiswa.sigma.data.model.UpdateProfileRequest(fullName = newName)
            profileRepository.updateProfile(profileId, request)
                .onSuccess {
                    loadProfiles()
                }
                .onFailure { error ->
                    _uiState.value = ProfileListUiState.Error(
                        error.message ?: "Failed to update profile"
                    )
                }
        }
    }
}
