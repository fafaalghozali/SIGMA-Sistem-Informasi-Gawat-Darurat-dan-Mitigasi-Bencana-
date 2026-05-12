package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

class ProfileViewModel : ViewModel() {
    var name by mutableStateOf("")
    var email by mutableStateOf("")
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    var showImageSheet by mutableStateOf(false)

    fun updateName(newName: String) {
        name = newName
    }

    fun updateEmail(newEmail: String) {
        email = newEmail
    }

    fun onImageSelected(bitmap: Bitmap) {
        imageBitmap = bitmap
        showImageSheet = false
    }

    fun initData(initialName: String, initialEmail: String) {
        if (name.isEmpty() && email.isEmpty()) {
            name = initialName
            email = initialEmail
        }
    }

    fun updateProfile() {
        // TODO: Implement save logic to repository/AuthManager
    }
}
