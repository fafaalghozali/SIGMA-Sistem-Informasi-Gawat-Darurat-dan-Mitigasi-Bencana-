package com.mahasiswa.sigma.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mahasiswa.sigma.data.model.NewsDto
import com.mahasiswa.sigma.data.repository.NewsRepositoryRetrofit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for the internal (Supabase-backed) News feature.
 *
 * Full Retrofit flow: NewsViewModel -> NewsRepositoryRetrofit -> SupabaseApiService.
 * This is intentionally separate from the dashboard's external RSS/BMKG news feed.
 */
@HiltViewModel
class NewsViewModel @Inject constructor(
    private val newsRepository: NewsRepositoryRetrofit
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<NewsDto>>>(UiState.Idle)
    val uiState: StateFlow<UiState<List<NewsDto>>> = _uiState.asStateFlow()

    private val _detailState = MutableStateFlow<UiState<NewsDto>>(UiState.Idle)
    val detailState: StateFlow<UiState<NewsDto>> = _detailState.asStateFlow()

    private val _operationMessage = MutableStateFlow<String?>(null)
    val operationMessage: StateFlow<String?> = _operationMessage.asStateFlow()

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    init {
        loadNews()
    }

    fun loadNews() {
        viewModelScope.launch {
            _uiState.value = UiState.Loading
            newsRepository.getAllNews()
                .onSuccess { news ->
                    _uiState.value = if (news.isEmpty()) UiState.Empty else UiState.Success(news)
                }
                .onFailure { error ->
                    _uiState.value = UiState.Error(error.message ?: "Gagal memuat berita")
                }
        }
    }

    fun refresh() = loadNews()

    fun loadNewsDetail(id: String) {
        viewModelScope.launch {
            _detailState.value = UiState.Loading
            newsRepository.getNewsById(id)
                .onSuccess { item ->
                    _detailState.value = if (item != null) UiState.Success(item)
                    else UiState.Error("Berita tidak ditemukan")
                }
                .onFailure { error ->
                    _detailState.value = UiState.Error(error.message ?: "Gagal memuat berita")
                }
        }
    }

    fun createNews(title: String, summary: String, source: String, url: String, imageUrl: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val dto = NewsDto(
                id = null,
                title = title,
                summary = summary.ifBlank { null },
                imageUrl = imageUrl.ifBlank { null },
                source = source.ifBlank { null },
                url = url.ifBlank { null },
                publishedAt = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .format(java.util.Date())
            )
            newsRepository.createNews(dto)
                .onSuccess {
                    _operationMessage.value = "Berita berhasil ditambahkan"
                    loadNews()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menambahkan berita"
                }
            _isProcessing.value = false
        }
    }

    fun updateNews(id: String, title: String, summary: String, source: String, url: String, imageUrl: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val updates = mapOf(
                "title" to title,
                "summary" to summary.ifBlank { null },
                "source" to source.ifBlank { null },
                "url" to url.ifBlank { null },
                "image_url" to imageUrl.ifBlank { null }
            )
            newsRepository.updateNews(id, updates)
                .onSuccess {
                    _operationMessage.value = "Berita berhasil diperbarui"
                    loadNews()
                    loadNewsDetail(id)
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal memperbarui berita"
                }
            _isProcessing.value = false
        }
    }

    fun deleteNews(id: String, onDeleted: () -> Unit = {}) {
        viewModelScope.launch {
            _isProcessing.value = true
            newsRepository.deleteNews(id)
                .onSuccess {
                    _operationMessage.value = "Berita berhasil dihapus"
                    loadNews()
                    onDeleted()
                }
                .onFailure { error ->
                    _operationMessage.value = error.message ?: "Gagal menghapus berita"
                }
            _isProcessing.value = false
        }
    }

    fun clearOperationMessage() {
        _operationMessage.value = null
    }
}
