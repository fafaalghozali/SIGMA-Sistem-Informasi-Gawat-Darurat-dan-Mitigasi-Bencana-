package com.mahasiswa.sigma.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.datastore.authDataStore
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val authManager = AuthManager(application.authDataStore)
    
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
