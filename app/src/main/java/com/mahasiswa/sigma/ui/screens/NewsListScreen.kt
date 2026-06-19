package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.NewsDto
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.ui.viewmodel.NewsViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun formatNewsDate(epochMillis: Long?): String {
    if (epochMillis == null || epochMillis <= 0L) return ""
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id", "ID")).format(Date(epochMillis))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsListScreen(
    userRole: UserRole,
    onBack: () -> Unit,
    onOpenDetail: (String) -> Unit,
    viewModel: NewsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val isAdmin = userRole == UserRole.BNPB

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }

    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Berita Resmi", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = { showEditor = true },
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Tambah Berita")
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> NewsMessageState(
                    title = "Gagal memuat berita",
                    message = state.message,
                    onRetry = { viewModel.refresh() }
                )

                is UiState.Empty -> NewsMessageState(
                    title = "Belum ada berita",
                    message = if (isAdmin) "Tekan tombol + untuk menambahkan berita resmi." else "Belum ada berita resmi yang dipublikasikan.",
                    onRetry = { viewModel.refresh() }
                )

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(state.data, key = { it.id }) { news ->
                            NewsCard(news = news, onClick = { onOpenDetail(news.id) })
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        NewsEditorDialog(
            initial = null,
            onDismiss = { showEditor = false },
            onSubmit = { title, summary, source, url, imageUrl ->
                viewModel.createNews(title, summary, source, url, imageUrl)
                showEditor = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsCard(news: NewsDto, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                news.title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (!news.summary.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    news.summary,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    news.source ?: "SIGMA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatNewsDate(news.publishedAt),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
internal fun NewsMessageState(
    title: String,
    message: String,
    onRetry: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onRetry) { Text("Segarkan") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NewsEditorDialog(
    initial: NewsDto?,
    onDismiss: () -> Unit,
    onSubmit: (title: String, summary: String, source: String, url: String, imageUrl: String) -> Unit
) {
    var title by remember { mutableStateOf(initial?.title ?: "") }
    var summary by remember { mutableStateOf(initial?.summary ?: "") }
    var source by remember { mutableStateOf(initial?.source ?: "") }
    var url by remember { mutableStateOf(initial?.url ?: "") }
    var imageUrl by remember { mutableStateOf(initial?.imageUrl ?: "") }

    val isEdit = initial != null
    val titleValid = title.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Berita" else "Tambah Berita", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = title, onValueChange = { title = it },
                    label = { Text("Judul") }, isError = !titleValid,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = summary, onValueChange = { summary = it },
                    label = { Text("Ringkasan") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = source, onValueChange = { source = it },
                    label = { Text("Sumber") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = url, onValueChange = { url = it },
                    label = { Text("Tautan (URL)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = imageUrl, onValueChange = { imageUrl = it },
                    label = { Text("URL Gambar") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = titleValid,
                onClick = { onSubmit(title.trim(), summary.trim(), source.trim(), url.trim(), imageUrl.trim()) }
            ) { Text(if (isEdit) "Simpan" else "Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
