package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var password by mutableStateOf("")
    var passwordVisible by mutableStateOf(false)
    val selectedRole = UserRole.MASYARAKAT
    
    var showDialog by mutableStateOf(false)
    var registrationSuccess by mutableStateOf(false)
    var dialogMessage by mutableStateOf("")

    private fun isEmailValid(email: String): Boolean {
        val emailParts = email.split("@")
        if (emailParts.size != 2) return false
        val localPart = emailParts[0]
        val domainPart = emailParts[1]
        return localPart.length >= 5 && domainPart.contains(".")
    }

    fun register(onNavigateToLogin: () -> Unit) {
        if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
            if (isEmailValid(email)) {
                viewModelScope.launch {
                    val result = authManager.registerUser(email, password, selectedRole, name)
                    if (result.isSuccess) {
                        registrationSuccess = true
                        dialogMessage = "Akun Anda telah berhasil didaftarkan ke sistem SIGMA. Silakan masuk untuk melanjutkan."
                        showDialog = true
                    } else {
                        registrationSuccess = false
                        dialogMessage = result.exceptionOrNull()?.message
                            ?: "Terjadi kesalahan saat menyimpan data. Silakan coba lagi."
                        showDialog = true
                    }
                }
            } else {
                registrationSuccess = false
                dialogMessage = "Email tidak valid. Pastikan ada '@', '.', dan minimal 5 karakter sebelum '@'."
                showDialog = true
            }
        } else {
            registrationSuccess = false
            dialogMessage = "Mohon lengkapi semua data sebelum mendaftar."
            showDialog = true
        }
    }
    
    fun onDialogConfirm(onNavigateToLogin: () -> Unit) {
        showDialog = false
        if (registrationSuccess) {
            onNavigateToLogin()
        }
    }
}
