package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.datastore.UserPreferencesRepository
import com.mahasiswa.sigma.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String = "",
    val showSuccessDialog: Boolean = false,
    val loggedInName: String = "",
    val loggedInRole: UserRole = UserRole.MASYARAKAT
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        loadSavedCredentials()
    }

    private fun loadSavedCredentials() {
        viewModelScope.launch {
            val rememberMe = userPreferencesRepository.rememberMe.first()
            if (rememberMe) {
                val email = userPreferencesRepository.savedEmail.first()
                val password = userPreferencesRepository.savedPassword.first()
                _uiState.update {
                    it.copy(
                        email = email,
                        password = password,
                        rememberMe = true
                    )
                }
            }
        }
    }

    fun onEmailChange(newValue: String) {
        _uiState.update { it.copy(email = newValue) }
    }

    fun onPasswordChange(newValue: String) {
        if (!newValue.contains("\n")) {
            _uiState.update { it.copy(password = newValue) }
        }
    }

    fun onPasswordVisibilityToggle() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeToggle() {
        _uiState.update { it.copy(rememberMe = !it.rememberMe) }
    }

    fun onDismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    fun resetLoginState() {
        _uiState.update { current ->
            LoginUiState(
                email = if (current.rememberMe) current.email else "",
                password = if (current.rememberMe) current.password else "",
                rememberMe = current.rememberMe
            )
        }
    }

    fun login() {
        val currentState = _uiState.value
        if (currentState.email.isEmpty() || currentState.password.isEmpty()) {
            _uiState.update {
                it.copy(
                    showErrorDialog = true,
                    errorMessage = "Mohon isi Email dan Password Anda."
                )
            }
            return
        }

        viewModelScope.launch {
            val result = authManager.loginUser(currentState.email, currentState.password)
            if (result.isSuccess) {
                // Simpan atau hapus kredensial berdasarkan pilihan "Ingat Saya"
                if (currentState.rememberMe) {
                    userPreferencesRepository.saveCredentials(
                        email = currentState.email,
                        password = currentState.password
                    )
                } else {
                    userPreferencesRepository.clearCredentials()
                }

                val role = result.getOrDefault(UserRole.MASYARAKAT)
                val name = authManager.getUserName()
                _uiState.update {
                    it.copy(
                        showSuccessDialog = true,
                        loggedInName = name,
                        loggedInRole = role
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        showErrorDialog = true,
                        errorMessage = result.exceptionOrNull()?.message
                            ?: "Email atau Password salah. Silakan periksa kembali."
                    )
                }
            }
        }
    }
}
