package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.CreateVolunteerRequest
import com.mahasiswa.sigma.data.model.VolunteerRegistrationData
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class VolunteerRegistrationViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private var currentUserEmail: String = ""

    // Multi-step: 1 = Data Diri, 2 = Keahlian, 3 = Konfirmasi
    var currentStep by mutableIntStateOf(1)

    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var showIncompleteDialog by mutableStateOf(false)
    var isSubmitting by mutableStateOf(false)
    var submitError by mutableStateOf<String?>(null)

    val skillOptions = SkillsVolunteer.entries
    var selectedSkill by mutableStateOf(skillOptions[0])

    var registeredData by mutableStateOf<VolunteerRegistrationData?>(null)
    var isRegistered by mutableStateOf(false)

    fun loadRegistrationData(email: String, userName: String = "") {
        currentUserEmail = email
        // Auto-fill nama dari profil yang sudah login jika belum diisi
        if (name.isBlank() && userName.isNotBlank()) {
            name = userName
        }
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    registeredData = VolunteerRegistrationData(
                        name = volunteerDto.name,
                        skill = try {
                            SkillsVolunteer.valueOf(volunteerDto.skill.uppercase())
                        } catch (_: Exception) {
                            SkillsVolunteer.MEDIS
                        },
                        address = volunteerDto.address,
                        phoneNumber = volunteerDto.phoneNumber,
                        status = volunteerDto.status
                    )
                    isRegistered = true
                }
            }
        }
    }

    fun onNameChange(newValue: String) { name = newValue }
    fun onAddressChange(newValue: String) { address = newValue }
    fun onPhoneNumberChange(newValue: String) { phoneNumber = newValue }

    fun onSkillSelected(skill: SkillsVolunteer) {
        selectedSkill = skill
    }

    // Navigasi antar step
    fun goToNextStep() {
        when (currentStep) {
            1 -> {
                if (isStep1Valid()) {
                    currentStep = 2
                } else {
                    showIncompleteDialog = true
                }
            }
            2 -> currentStep = 3
        }
    }

    fun goToPreviousStep() {
        if (currentStep > 1) currentStep--
    }

    private fun isStep1Valid(): Boolean {
        return name.isNotBlank() &&
               address.isNotBlank() &&
               phoneNumber.isNotBlank() &&
               phoneNumber.length >= 10 &&
               phoneNumber.all { it.isDigit() }
    }

    fun submitRegistration() {
        viewModelScope.launch {
            isSubmitting = true
            submitError = null
            val userId = authManager.getCurrentUserId() ?: run {
                submitError = "Sesi tidak ditemukan, silakan login ulang."
                isSubmitting = false
                return@launch
            }

            val request = CreateVolunteerRequest(
                userId = userId,
                name = name,
                skill = selectedSkill.name,
                address = address,
                phoneNumber = phoneNumber,
                status = "PENDING"
            )

            val result = volunteerRepository.createVolunteer(request)
            result.onSuccess { volunteerDto ->
                registeredData = VolunteerRegistrationData(
                    name = volunteerDto.name,
                    skill = selectedSkill,
                    address = volunteerDto.address,
                    phoneNumber = volunteerDto.phoneNumber,
                    status = volunteerDto.status
                )
                isRegistered = true
                // Reset form & step
                name = ""
                address = ""
                phoneNumber = ""
                currentStep = 1
            }
            result.onFailure { e ->
                submitError = e.message ?: "Pendaftaran gagal, coba lagi."
            }
            isSubmitting = false
        }
    }

    fun resetRegistration() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                volunteerDto?.id?.let { volunteerId ->
                    volunteerRepository.deleteVolunteer(volunteerId.toString())
                }
            }
            isRegistered = false
            registeredData = null
            currentStep = 1
        }
    }
}
