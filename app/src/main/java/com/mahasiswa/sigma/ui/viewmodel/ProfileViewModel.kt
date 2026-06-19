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

    // Bitmap yang baru dipilih user (belum/sudah diupload)
    var imageBitmap by mutableStateOf<Bitmap?>(null)

    // URL foto yang tersimpan di Supabase Storage
    var photoUrl by mutableStateOf<String?>(null)
        private set

    var showImageSheet by mutableStateOf(false)

    // State upload foto
    var isUploadingPhoto by mutableStateOf(false)
        private set
    var isUploadPhotoSuccess by mutableStateOf(false)
    var isUploadPhotoError by mutableStateOf(false)

    // State update profil (nama/email)
    var isUpdateSuccess by mutableStateOf(false)
    var isUpdateError by mutableStateOf(false)
    var errorMessage by mutableStateOf("")

    fun initData(initialName: String, initialEmail: String) {
        if (originalEmail != initialEmail) {
            originalEmail = initialEmail
            name = initialName
            email = initialEmail
            // Muat foto profil dari Supabase saat pertama kali init
            loadProfilePhoto()
        }
    }

    private fun loadProfilePhoto() {
        viewModelScope.launch {
            photoUrl = authManager.getProfilePhotoUrl()
        }
    }

    /**
     * Dipanggil saat user memilih gambar dari galeri/kamera.
     * Langsung trigger upload ke Supabase Storage.
     */
    fun onImageSelected(bitmap: Bitmap) {
        imageBitmap = bitmap
        showImageSheet = false
        uploadPhoto(bitmap)
    }

    private fun uploadPhoto(bitmap: Bitmap) {
        viewModelScope.launch {
            isUploadingPhoto = true
            isUploadPhotoError = false

            // 1. Upload ke Storage, dapat public URL
            val uploadResult = authManager.uploadProfilePhoto(bitmap)
            if (uploadResult.isFailure) {
                errorMessage = uploadResult.exceptionOrNull()?.message
                    ?: "Gagal mengupload foto"
                isUploadPhotoError = true
                isUploadingPhoto = false
                return@launch
            }

            val url = uploadResult.getOrNull()!!

            // 2. Simpan URL ke kolom photo_url di tabel profiles
            val saveResult = authManager.updateProfilePhotoUrl(url)
            if (saveResult.isFailure) {
                errorMessage = saveResult.exceptionOrNull()?.message
                    ?: "Gagal menyimpan URL foto"
                isUploadPhotoError = true
                isUploadingPhoto = false
                return@launch
            }

            // 3. Update state lokal
            photoUrl = url
            isUploadPhotoSuccess = true
            isUploadingPhoto = false
        }
    }

    fun updateProfile() {
        if (name.isBlank() || email.isBlank()) {
            errorMessage = "Nama dan Email tidak boleh kosong"
            isUpdateError = true
            return
        }

        viewModelScope.launch {
            val result = authManager.updateProfile(name, email)
            if (result.isSuccess) {
                originalEmail = email
                isUpdateSuccess = true
            } else {
                errorMessage = result.exceptionOrNull()?.message
                    ?: "Gagal memperbarui profil"
                isUpdateError = true
            }
        }
    }

    fun dismissDialogs() {
        isUpdateSuccess = false
        isUpdateError = false
        isUploadPhotoSuccess = false
        isUploadPhotoError = false
    }
}
