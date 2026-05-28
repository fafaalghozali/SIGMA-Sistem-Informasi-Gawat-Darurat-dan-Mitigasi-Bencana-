package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authManager: AuthManager
) : ViewModel() {

    private var originalEmail: String = ""
    
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    var showImageSheet by mutableStateOf(false)
    
    var isUpdateSuccess by mutableStateOf(false)
    var isUpdateError by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    fun initData(initialName: String, initialEmail: String) {
        if (originalEmail.isEmpty()) {
            originalEmail = initialEmail
            name = initialName
            email = initialEmail
        }
    }

    fun onImageSelected(bitmap: Bitmap) {
        imageBitmap = bitmap
        showImageSheet = false
    }

    fun updateProfile() {
        if (name.isBlank() || email.isBlank()) {
            errorMessage = "Nama dan Email tidak boleh kosong"
            isUpdateError = true
            return
        }

        viewModelScope.launch {
            val success = authManager.updateProfile(originalEmail, name, email)
            if (success) {
                originalEmail = email
                isUpdateSuccess = true
            } else {
                errorMessage = "Gagal memperbarui profil"
                isUpdateError = true
            }
        }
    }
    
    fun dismissDialogs() {
        isUpdateSuccess = false
        isUpdateError = false
    }
}
