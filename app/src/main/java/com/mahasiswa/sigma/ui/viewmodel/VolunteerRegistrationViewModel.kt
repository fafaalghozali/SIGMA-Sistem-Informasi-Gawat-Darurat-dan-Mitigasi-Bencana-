package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mahasiswa.sigma.data.model.SkillsVolunteer

data class VolunteerRegistrationData(
    val name: String,
    val skill: SkillsVolunteer,
    val address: String,
    val phoneNumber: String,
    val status: String = "Pending Verifikasi"
)

class VolunteerRegistrationViewModel : ViewModel() {
    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var showConfirmDialog by mutableStateOf(false)
    var showIncompleteDialog by mutableStateOf(false)

    val skillOptions = SkillsVolunteer.entries
    var selectedSkill by mutableStateOf(skillOptions[0])
    var skillExpanded by mutableStateOf(false)

    // State to track if user is registered and store their data
    var registeredData by mutableStateOf<VolunteerRegistrationData?>(null)
    var isRegistered by mutableStateOf(false)

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
        // Logic to save registration
        registeredData = VolunteerRegistrationData(
            name = name,
            skill = selectedSkill,
            address = address,
            phoneNumber = phoneNumber
        )
        isRegistered = true
        
        // Clear form
        name = ""
        address = ""
        phoneNumber = ""
        showConfirmDialog = false
    }
}
