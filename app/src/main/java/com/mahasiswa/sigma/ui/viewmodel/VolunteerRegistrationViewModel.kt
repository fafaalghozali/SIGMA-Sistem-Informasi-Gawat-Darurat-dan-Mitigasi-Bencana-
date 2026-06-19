package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
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

    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var showConfirmDialog by mutableStateOf(false)
    var showIncompleteDialog by mutableStateOf(false)

    val skillOptions = SkillsVolunteer.entries
    var selectedSkill by mutableStateOf(skillOptions[0])
    var skillExpanded by mutableStateOf(false)

    var registeredData by mutableStateOf<VolunteerRegistrationData?>(null)
    var isRegistered by mutableStateOf(false)

    fun loadRegistrationData(email: String) {
        currentUserEmail = email
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

    fun onNameChange(newValue: String) {
        name = newValue
    }

    fun onAddressChange(newValue: String) {
        address = newValue
    }

    fun onPhoneNumberChange(newValue: String) {
        phoneNumber = newValue
    }

    fun onSkillSelected(skill: SkillsVolunteer) {
        selectedSkill = skill
        skillExpanded = false
    }

    fun toggleSkillExpanded() {
        skillExpanded = !skillExpanded
    }

    fun onRegisterClick() {
        if (isFormValid()) {
            showConfirmDialog = true
        } else {
            showIncompleteDialog = true
        }
    }

    private fun isFormValid(): Boolean {
        return name.isNotBlank() &&
               address.isNotBlank() &&
               phoneNumber.isNotBlank() &&
               phoneNumber.length >= 10 &&
               phoneNumber.all { it.isDigit() }
    }

    fun submitRegistration() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            
            val request = CreateVolunteerRequest(
                userId = userId,
                name = name,
                skill = selectedSkill.name,
                address = address,
                phoneNumber = phoneNumber,
                status = "pending"
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
                
                // Clear form
                name = ""
                address = ""
                phoneNumber = ""
                showConfirmDialog = false
            }
            result.onFailure {
                // Handle error - could add error state here
                showConfirmDialog = false
            }
        }
    }

    fun resetRegistration() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                volunteerDto?.id?.let { volunteerId ->
                    volunteerRepository.deleteVolunteer(volunteerId)
                }
            }
            isRegistered = false
            registeredData = null
        }
    }
}
