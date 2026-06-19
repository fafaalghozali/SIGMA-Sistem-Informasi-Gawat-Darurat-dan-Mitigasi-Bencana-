package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.data.model.UpdateVolunteerRequest
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageVolunteerViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit
) : ViewModel() {

    private val _registrations = MutableStateFlow<List<VolunteerDto>>(emptyList())
    val registrations: StateFlow<List<VolunteerDto>> = _registrations.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadRegistrations()
    }

    fun loadRegistrations() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null
            val result = volunteerRepository.getAllVolunteers()
            result.fold(
                onSuccess = { _registrations.value = it },
                onFailure = { _errorMessage.value = it.message }
            )
            _isLoading.value = false
        }
    }

    fun approveVolunteer(volunteerId: String) {
        viewModelScope.launch {
            val request = UpdateVolunteerRequest(status = "Accepted")
            volunteerRepository.updateVolunteer(volunteerId, request)
            loadRegistrations()
        }
    }

    fun rejectVolunteer(volunteerId: String) {
        viewModelScope.launch {
            val request = UpdateVolunteerRequest(status = "Declined")
            volunteerRepository.updateVolunteer(volunteerId, request)
            loadRegistrations()
        }
    }
}
