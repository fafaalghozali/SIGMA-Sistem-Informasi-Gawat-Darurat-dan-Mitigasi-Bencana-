package com.mahasiswa.sigma.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import com.mahasiswa.sigma.data.repository.VolunteerRepository
import com.mahasiswa.sigma.data.model.SkillsVolunteer

data class VolunteerRegistrationData(
    val name: String,
    val skill: SkillsVolunteer,
    val address: String,
    val phoneNumber: String,
    val status: String = "Pending"
)

class VolunteerRegistrationViewModel(application: Application) : AndroidViewModel(application) {
    private val volunteerRepository = VolunteerRepository(application)
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
        val savedData = volunteerRepository.getRegistration(email)
        if (savedData != null) {
            registeredData = savedData
            isRegistered = true
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
        val data = VolunteerRegistrationData(
            name = name,
            skill = selectedSkill,
            address = address,
            phoneNumber = phoneNumber,
            status = "Pending"
        )
        
        volunteerRepository.saveRegistration(currentUserEmail, data)
        registeredData = data
        isRegistered = true
        
        name = ""
        address = ""
        phoneNumber = ""
        showConfirmDialog = false
    }

    fun resetRegistration() {
        volunteerRepository.clearRegistration(currentUserEmail)
        isRegistered = false
        registeredData = null
    }
}
