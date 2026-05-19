package com.mahasiswa.sigma.ui.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.datastore.disasterReportsDataStore
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DisasterReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(application.disasterReportsDataStore)

    private val _reports = MutableStateFlow<List<LocalDisasterReport>>(emptyList())
    val reports: StateFlow<List<LocalDisasterReport>> = _reports.asStateFlow()

    var title by mutableStateOf("")
    var description by mutableStateOf("")
    var locationAddress by mutableStateOf("Mendeteksi lokasi...")
    var imageBitmap by mutableStateOf<Bitmap?>(null)
    
    var showIncompleteDialog by mutableStateOf(false)
    var showPhotoSourceSheet by mutableStateOf(false)
    var isLoading by mutableStateOf(false)

    init {
        loadReports()
    }

    fun loadReports() {
        viewModelScope.launch {
            _reports.value = repository.getAllReports()
        }
    }

    fun onTitleChange(newValue: String) { title = newValue }
    fun onDescriptionChange(newValue: String) { description = newValue }
    fun onLocationReceived(address: String) { locationAddress = address }
    fun onImageSelected(bitmap: Bitmap?) { 
        imageBitmap = bitmap 
        showPhotoSourceSheet = false
    }

    fun sendReport() {
        if (title.isBlank() || description.isBlank() || imageBitmap == null) {
            showIncompleteDialog = true
            return
        }

        viewModelScope.launch {
            isLoading = true
            val newReport = LocalDisasterReport(
                title = title,
                description = description,
                location = locationAddress
            )
            repository.saveReport(newReport)
            
            title = ""
            description = ""
            imageBitmap = null
            
            loadReports()
            isLoading = false
        }
    }

    fun updateReport(report: LocalDisasterReport) {
        viewModelScope.launch {
            repository.updateReport(report)
            loadReports()
        }
    }
}
