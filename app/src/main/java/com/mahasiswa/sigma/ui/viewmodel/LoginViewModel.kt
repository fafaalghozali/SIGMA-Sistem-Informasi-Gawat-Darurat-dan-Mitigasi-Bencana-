package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.UserRole
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isPasswordVisible: Boolean = false,
    val selectedRole: UserRole = UserRole.MASYARAKAT,
    val isRoleExpanded: Boolean = false,
    val showErrorDialog: Boolean = false,
    val errorMessage: String = "",
    val showSuccessDialog: Boolean = false,
    val loggedInName: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    val availableRoles = UserRole.entries

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

    fun onRoleExpandedChange(expanded: Boolean) {
        _uiState.update { it.copy(isRoleExpanded = expanded) }
    }

    fun onRoleSelected(role: UserRole) {
        _uiState.update { it.copy(selectedRole = role, isRoleExpanded = false) }
    }

    fun onDismissErrorDialog() {
        _uiState.update { it.copy(showErrorDialog = false) }
    }

    fun resetLoginState() {
        _uiState.update { LoginUiState() }
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
            val userRole = authManager.loginUser(currentState.email, currentState.password, currentState.selectedRole)
            if (userRole != null) {
                if (userRole == currentState.selectedRole) {
                    val name = authManager.getUserName(currentState.email)
                    _uiState.update { 
                        it.copy(
                            showSuccessDialog = true, 
                            loggedInName = name 
                        ) 
                    }
                } else {
                    _uiState.update { 
                        it.copy(
                            showErrorDialog = true, 
                            errorMessage = "Role yang Anda pilih tidak sesuai dengan akun ini."
                        ) 
                    }
                }
            } else {
                _uiState.update { 
                    it.copy(
                        showErrorDialog = true, 
                        errorMessage = "Email atau Password salah. Silakan periksa kembali."
                    ) 
                }
            }
        }
    }
}
