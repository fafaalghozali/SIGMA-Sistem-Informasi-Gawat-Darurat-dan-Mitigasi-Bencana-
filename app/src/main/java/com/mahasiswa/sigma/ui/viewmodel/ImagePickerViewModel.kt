package com.mahasiswa.sigma.ui.viewmodel

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.mahasiswa.sigma.data.repository.ImageRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ImagePickerViewModel(
    private val repository: ImageRepository
) : ViewModel() {

    companion object {
        fun provideFactory(repository: ImageRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                ImagePickerViewModel(repository)
            }
        }
    }

    private val _selectedBitmap = MutableStateFlow<Bitmap?>(null)
    val selectedBitmap: StateFlow<Bitmap?> = _selectedBitmap.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun handleImageUri(uri: Uri) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val bitmap = repository.uriToBitmap(uri)
                _selectedBitmap.value = bitmap
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun handleImageBitmap(bitmap: Bitmap) {
        _selectedBitmap.value = bitmap
    }

    fun resetState() {
        _selectedBitmap.value = null
        _isLoading.value = false
    }
}
