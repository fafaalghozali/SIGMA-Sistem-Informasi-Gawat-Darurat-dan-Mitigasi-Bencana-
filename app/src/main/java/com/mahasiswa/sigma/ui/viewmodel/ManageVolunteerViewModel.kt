package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.VolunteerEntry
import com.mahasiswa.sigma.data.repository.VolunteerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageVolunteerViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepository
) : ViewModel() {

    private val _registrations = MutableStateFlow<List<VolunteerEntry>>(emptyList())
    val registrations: StateFlow<List<VolunteerEntry>> = _registrations.asStateFlow()

    init {
        loadRegistrations()
    }

    fun loadRegistrations() {
        viewModelScope.launch {
            _registrations.value = volunteerRepository.getAllRegistrations()
        }
    }

    fun approveVolunteer(username: String) {
        viewModelScope.launch {
            volunteerRepository.updateVolunteerStatus(username, "Accepted")
            loadRegistrations()
        }
    }

    fun rejectVolunteer(username: String) {
        viewModelScope.launch {
            volunteerRepository.updateVolunteerStatus(username, "Declined")
            loadRegistrations()
        }
    }
}
