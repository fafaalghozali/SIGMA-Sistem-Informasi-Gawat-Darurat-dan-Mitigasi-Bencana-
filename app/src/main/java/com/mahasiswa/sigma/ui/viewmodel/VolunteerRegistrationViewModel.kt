package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mahasiswa.sigma.data.model.SkillsVolunteer

class VolunteerRegistrationViewModel : ViewModel() {
    var name by mutableStateOf("")
    var address by mutableStateOf("")
    var phoneNumber by mutableStateOf("")
    var showConfirmDialog by mutableStateOf(false)

    val skillOptions = SkillsVolunteer.entries
    var selectedSkill by mutableStateOf(skillOptions[0])
    var skillExpanded by mutableStateOf(false)

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

    fun submitRegistration(onSuccess: () -> Unit) {
        // Logic to save registration can be added here
        showConfirmDialog = false
        onSuccess()
    }
}
