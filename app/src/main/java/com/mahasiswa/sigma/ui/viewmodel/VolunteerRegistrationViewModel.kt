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
import com.mahasiswa.sigma.data.model.UpdateVolunteerRequest
import com.mahasiswa.sigma.data.repository.VolunteerRepositoryRetrofit
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class VolunteerRegistrationViewModel @Inject constructor(
    private val volunteerRepository: VolunteerRepositoryRetrofit,
    private val authManager: AuthManager
) : ViewModel() {

    private var currentUserEmail: String = ""
    private var registeredVolunteerId: Long? = null

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

    // State konfirmasi penugasan
    var isConfirmingAssignment by mutableStateOf(false)
    var confirmAssignmentError by mutableStateOf<String?>(null)
    var confirmAssignmentSuccess by mutableStateOf(false)

    private fun nowTimestamp(): String =
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault()).format(Date())

    fun loadRegistrationData(email: String, userName: String = "") {
        currentUserEmail = email
        if (name.isBlank() && userName.isNotBlank()) {
            name = userName
        }
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    registeredVolunteerId = volunteerDto.id
                    registeredData = VolunteerRegistrationData(
                        name = volunteerDto.name,
                        skill = try {
                            SkillsVolunteer.valueOf(volunteerDto.skill.uppercase())
                        } catch (_: Exception) {
                            SkillsVolunteer.MEDIS
                        },
                        address = volunteerDto.address,
                        phoneNumber = volunteerDto.phoneNumber,
                        status = volunteerDto.status,
                        assignment = volunteerDto.assignment,
                        assignmentStatus = volunteerDto.assignmentStatus,
                        disasterId = volunteerDto.disasterId,
                        volunteerId = volunteerDto.id
                    )
                    isRegistered = true
                }
            }
        }
    }

    fun refreshStatus() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                if (volunteerDto != null) {
                    registeredVolunteerId = volunteerDto.id
                    registeredData = registeredData?.copy(
                        status = volunteerDto.status,
                        assignment = volunteerDto.assignment,
                        assignmentStatus = volunteerDto.assignmentStatus,
                        disasterId = volunteerDto.disasterId
                    ) ?: VolunteerRegistrationData(
                        name = volunteerDto.name,
                        skill = try { SkillsVolunteer.valueOf(volunteerDto.skill.uppercase()) }
                                catch (_: Exception) { SkillsVolunteer.MEDIS },
                        address = volunteerDto.address,
                        phoneNumber = volunteerDto.phoneNumber,
                        status = volunteerDto.status,
                        assignment = volunteerDto.assignment,
                        assignmentStatus = volunteerDto.assignmentStatus,
                        disasterId = volunteerDto.disasterId,
                        volunteerId = volunteerDto.id
                    )
                }
            }
        }
    }

    fun onNameChange(newValue: String) { name = newValue }
    fun onAddressChange(newValue: String) { address = newValue }
    fun onPhoneNumberChange(newValue: String) { phoneNumber = newValue }
    fun onSkillSelected(skill: SkillsVolunteer) { selectedSkill = skill }

    fun goToNextStep() {
        when (currentStep) {
            1 -> if (isStep1Valid()) currentStep = 2 else showIncompleteDialog = true
            2 -> currentStep = 3
        }
    }

    fun goToPreviousStep() { if (currentStep > 1) currentStep-- }

    private fun isStep1Valid(): Boolean =
        name.isNotBlank() && address.isNotBlank() &&
        phoneNumber.isNotBlank() && phoneNumber.length >= 10 &&
        phoneNumber.all { it.isDigit() }

    fun submitRegistration() {
        viewModelScope.launch {
            isSubmitting = true
            submitError = null
            val userId = authManager.getCurrentUserId() ?: run {
                submitError = "Sesi tidak ditemukan, silakan login ulang."
                isSubmitting = false
                return@launch
            }

            val now = nowTimestamp()
            val request = CreateVolunteerRequest(
                userId = userId,
                name = name,
                skill = selectedSkill.name,
                address = address,
                phoneNumber = phoneNumber,
                availability = "available",
                status = "PENDING",
                createdAt = now,
                updatedAt = now
            )

            val result = volunteerRepository.createVolunteer(request)
            result.onSuccess { volunteerDto ->
                registeredVolunteerId = volunteerDto.id
                registeredData = VolunteerRegistrationData(
                    name = volunteerDto.name,
                    skill = selectedSkill,
                    address = volunteerDto.address,
                    phoneNumber = volunteerDto.phoneNumber,
                    status = volunteerDto.status,
                    volunteerId = volunteerDto.id
                )
                isRegistered = true
                name = ""; address = ""; phoneNumber = ""
                currentStep = 1
            }
            result.onFailure { e ->
                submitError = e.message ?: "Pendaftaran gagal, coba lagi."
            }
            isSubmitting = false
        }
    }

    /**
     * Relawan konfirmasi bersedia / tidak bersedia setelah admin menugaskan.
     * assignment_status: "accepted" = bersedia, "rejected" = tidak bersedia
     */
    fun confirmAssignment(accept: Boolean) {
        val vid = registeredVolunteerId ?: return
        viewModelScope.launch {
            isConfirmingAssignment = true
            confirmAssignmentError = null
            val request = UpdateVolunteerRequest(
                assignmentStatus = if (accept) "accepted" else "rejected",
                updatedAt = nowTimestamp()
            )
            val result = volunteerRepository.updateVolunteer(vid.toString(), request)
            result.onSuccess {
                confirmAssignmentSuccess = true
                refreshStatus()
            }
            result.onFailure { e ->
                confirmAssignmentError = e.message ?: "Gagal mengonfirmasi. Coba lagi."
            }
            isConfirmingAssignment = false
        }
    }

    fun dismissConfirmSuccess() { confirmAssignmentSuccess = false }
    fun dismissConfirmError() { confirmAssignmentError = null }

    fun resetRegistration() {
        viewModelScope.launch {
            val userId = authManager.getCurrentUserId() ?: return@launch
            val result = volunteerRepository.getVolunteerByUserId(userId)
            result.onSuccess { volunteerDto ->
                volunteerDto?.id?.let { volunteerRepository.deleteVolunteer(it.toString()) }
            }
            isRegistered = false
            registeredData = null
            registeredVolunteerId = null
            currentStep = 1
        }
    }
}
