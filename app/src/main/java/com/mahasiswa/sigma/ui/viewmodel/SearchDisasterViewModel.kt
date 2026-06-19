package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.repository.EarthquakeData
import com.mahasiswa.sigma.data.repository.EarthquakeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class SearchDisasterViewModel @Inject constructor(
    private val earthquakeRepository: EarthquakeRepository
) : ViewModel() {

    private val _earthquakeListState = MutableStateFlow<UiState<List<EarthquakeData>>>(UiState.Idle)
    val earthquakeListState: StateFlow<UiState<List<EarthquakeData>>> = _earthquakeListState.asStateFlow()

    private val _latestEarthquakeState = MutableStateFlow<UiState<EarthquakeData>>(UiState.Idle)
    val latestEarthquakeState: StateFlow<UiState<EarthquakeData>> = _latestEarthquakeState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        loadRecentEarthquakes()
    }

    fun loadRecentEarthquakes() {
        viewModelScope.launch {
            _earthquakeListState.value = UiState.Loading
            try {
                val data = earthquakeRepository.getRecentEarthquakes()
                if (data.isEmpty()) {
                    _earthquakeListState.value = UiState.Empty
                } else {
                    _earthquakeListState.value = UiState.Success(data)
                }
            } catch (e: IOException) {
                _earthquakeListState.value = UiState.Error(e.message ?: "Kesalahan jaringan")
            } catch (e: HttpException) {
                _earthquakeListState.value = UiState.Error("Server error: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                _earthquakeListState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun loadLatestEarthquake() {
        viewModelScope.launch {
            _latestEarthquakeState.value = UiState.Loading
            try {
                val data = earthquakeRepository.getLatestEarthquake()
                _latestEarthquakeState.value = UiState.Success(data)
            } catch (e: IOException) {
                _latestEarthquakeState.value = UiState.Error(e.message ?: "Kesalahan jaringan")
            } catch (e: HttpException) {
                _latestEarthquakeState.value = UiState.Error("Server error: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                _latestEarthquakeState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun onSearchQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
        if (newQuery.isBlank()) {
            loadRecentEarthquakes()
            return
        }
        viewModelScope.launch {
            _earthquakeListState.value = UiState.Loading
            try {
                val data = earthquakeRepository.searchEarthquakes(newQuery)
                if (data.isEmpty()) {
                    _earthquakeListState.value = UiState.Empty
                } else {
                    _earthquakeListState.value = UiState.Success(data)
                }
            } catch (e: IOException) {
                _earthquakeListState.value = UiState.Error(e.message ?: "Kesalahan jaringan")
            } catch (e: HttpException) {
                _earthquakeListState.value = UiState.Error("Server error: ${e.code()} ${e.message()}")
            } catch (e: Exception) {
                _earthquakeListState.value = UiState.Error(e.message ?: "Terjadi kesalahan")
            }
        }
    }

    fun retry() {
        val currentQuery = _searchQuery.value
        if (currentQuery.isBlank()) {
            loadRecentEarthquakes()
        } else {
            onSearchQueryChange(currentQuery)
        }
    }
}
