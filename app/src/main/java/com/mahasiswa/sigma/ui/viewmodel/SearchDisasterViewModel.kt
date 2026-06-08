package com.mahasiswa.sigma.ui.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.mahasiswa.sigma.data.model.ReportStatus
import com.mahasiswa.sigma.data.model.DisasterInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SearchDisasterViewModel @Inject constructor() : ViewModel() {
    var searchQuery by mutableStateOf("")
        private set

    private val allDisasters = listOf(
        DisasterInfo("Banjir", "Sukoharjo", ReportStatus.SIAGA_1, "14 April 2026"),
    )

    var filteredDisasters by mutableStateOf(allDisasters)
        private set

    fun onSearchQueryChange(newQuery: String) {
        searchQuery = newQuery
        filterDisasters()
    }

    private fun filterDisasters() {
        filteredDisasters = allDisasters.filter { d ->
            d.location.contains(searchQuery, ignoreCase = true) ||
            d.type.contains(searchQuery, ignoreCase = true)
        }
    }
}
