package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.ProfileDto
import com.mahasiswa.sigma.data.repository.ProfileRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ProfileListUiState {
    object Idle : ProfileListUiState()
    object Loading : ProfileListUiState()
    data class Success(val profiles: List<ProfileDto>) : ProfileListUiState()
    data class Error(val message: String) : ProfileListUiState()
    object Empty : ProfileListUiState()
}

@HiltViewModel
class ProfileListViewModel @Inject constructor(
    private val profileRepository: ProfileRepositoryRetrofit
) : ViewModel() {

    private val _allProfiles = MutableStateFlow<List<ProfileDto>>(emptyList())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedRoleFilter = MutableStateFlow<String?>("Semua")
    val selectedRoleFilter: StateFlow<String?> = _selectedRoleFilter.asStateFlow()

    private val _pendingDeleteIds = MutableStateFlow<Set<String>>(emptySet())
    private val deleteJobs = mutableMapOf<String, Job>()

    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)

    private val _uiState = MutableStateFlow<ProfileListUiState>(ProfileListUiState.Idle)
    val uiState: StateFlow<ProfileListUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val filteredProfilesFlow = combine(
                _allProfiles,
                _searchQuery,
                _selectedRoleFilter,
                _pendingDeleteIds
            ) { allProfiles, query, roleFilter, pendingDeletes ->
                var filtered = allProfiles
                if (query.isNotBlank()) {
                    filtered = filtered.filter {
                        it.fullName?.contains(query, ignoreCase = true) == true ||
                        it.email?.contains(query, ignoreCase = true) == true
                    }
                }
                if (roleFilter != null && roleFilter != "Semua") {
                    filtered = filtered.filter { it.role == roleFilter }
                }
                
                filtered.filter { it.id !in pendingDeletes }
            }

            combine(
                filteredProfilesFlow,
                _isLoading,
                _errorMessage
            ) { filtered, loading, error ->
                if (loading && filtered.isEmpty()) return@combine ProfileListUiState.Loading
                if (error != null) return@combine ProfileListUiState.Error(error)

                if (filtered.isEmpty() && !loading) {
                    ProfileListUiState.Empty
                } else {
                    ProfileListUiState.Success(filtered)
                }
            }.collect { state ->
                _uiState.value = state
            }
        }
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            profileRepository.getAllProfiles()
                .onSuccess { profiles ->
                    _allProfiles.value = profiles
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to load profiles"
                }
        }
    }

    fun refresh() {
        loadProfiles()
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setRoleFilter(role: String?) {
        _selectedRoleFilter.value = role
    }

    fun deleteProfileWithUndo(profileId: String, onUndoAvailable: () -> Unit) {
        val profile = _allProfiles.value.find { it.id == profileId }
        if (profile?.role == "Admin") {
            // Cannot delete admin
            _errorMessage.value = "Aksi ditolak: Admin tidak dapat dihapus."
            viewModelScope.launch { delay(3000); _errorMessage.value = null }
            return
        }

        _pendingDeleteIds.value = _pendingDeleteIds.value + profileId
        onUndoAvailable()

        val job = viewModelScope.launch {
            delay(5000) // 5 seconds wait for undo
            // Proceed to real delete
            _pendingDeleteIds.value = _pendingDeleteIds.value - profileId
            _isLoading.value = true
            profileRepository.deleteProfile(profileId)
                .onSuccess {
                    val currentList = _allProfiles.value.toMutableList()
                    currentList.removeAll { it.id == profileId }
                    _allProfiles.value = currentList
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to delete profile"
                    viewModelScope.launch { delay(3000); _errorMessage.value = null }
                }
            deleteJobs.remove(profileId)
        }
        deleteJobs[profileId] = job
    }

    fun undoDelete(profileId: String) {
        deleteJobs[profileId]?.cancel()
        deleteJobs.remove(profileId)
        _pendingDeleteIds.value = _pendingDeleteIds.value - profileId
    }

    // Keep old method for backward compatibility if needed, or remove it. We'll just remove it as we use the new one.
    fun deleteProfile(profileId: String) {
        // Fallback or override if needed, but we'll use deleteProfileWithUndo in UI
    }

    fun editProfile(profileId: String, newName: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val request = com.mahasiswa.sigma.data.model.UpdateProfileRequest(fullName = newName)
            profileRepository.updateProfile(profileId, request)
                .onSuccess { updatedProfile ->
                    val updatedList = _allProfiles.value.map {
                        if (it.id == profileId) updatedProfile else it
                    }
                    _allProfiles.value = updatedList
                    _isLoading.value = false
                }
                .onFailure { error ->
                    _isLoading.value = false
                    _errorMessage.value = error.message ?: "Failed to update profile"
                    viewModelScope.launch { delay(3000); _errorMessage.value = null }
                }
        }
    }
}
