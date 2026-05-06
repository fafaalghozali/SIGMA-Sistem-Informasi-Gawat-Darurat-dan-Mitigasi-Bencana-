package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import com.mahasiswa.sigma.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class ImagePickerViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    fun handleImageUri(uri: Uri) {
        val bitmap = repository.uriToBitmap(uri)
        _selectedBitmap.value = bitmap
    }

    fun handleImageBitmap(bitmap: Bitmap) {
        _selectedBitmap.value = bitmap
    }
}
