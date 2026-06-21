package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.ui.viewmodel.SearchDisasterViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

private fun formatDisasterDate(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return ""
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timestamp) ?: return timestamp
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(date)
    } catch (_: Exception) { timestamp }
}

private fun statusColor(status: String): Color = when (status.uppercase()) {
    "AWAS" -> Color(0xFFD32F2F)
    "SIAGA_1", "SIAGA 1" -> Color(0xFFF57C00)
    "SIAGA_2", "SIAGA 2" -> Color(0xFFFBC02D)
    "PENDING" -> Color(0xFFFF8F00)
    "RESOLVED" -> Color(0xFF388E3C)
    else -> Color(0xFF78909C)
}

private fun statusLabel(status: String): String = when (status.uppercase()) {
    "AWAS" -> "Awas"
    "SIAGA_1" -> "Siaga 1"
    "SIAGA_2" -> "Siaga 2"
    "PENDING" -> "Pending"
    "RESOLVED" -> "Resolved"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun statusBorderColor(status: String): Color = when (status.uppercase()) {
    "AWAS" -> Color(0xFFD32F2F)
    "SIAGA_1", "SIAGA 1" -> Color(0xFFF57C00)
    "SIAGA_2", "SIAGA 2" -> Color(0xFFFBC02D)
    "PENDING" -> Color(0xFFFF8F00)
    "RESOLVED" -> Color(0xFF388E3C)
    else -> Color(0xFF78909C)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDisasterScreen(
    onBack: () -> Unit,
    initialQuery: String? = null,
    initialStatus: String? = null,
    viewModel: SearchDisasterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()

    val statusFilters = listOf("Semua", "Awas", "Siaga 1", "Siaga 2", "Pending", "Resolved")

    LaunchedEffect(initialQuery, initialStatus) {
        viewModel.onSearchQueryChange(initialQuery ?: "")
        viewModel.onStatusFilterChange(initialStatus ?: "Semua")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.retry() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Header
            Text(
                "Cari & Filter Laporan Bencana",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Temukan laporan berdasarkan lokasi atau jenis bencana.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.onSearchQueryChange(it) },
                placeholder = { Text("Cari lokasi atau jenis bencana...", fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                )
            )
            Spacer(Modifier.height(12.dp))

            // Status filter chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(statusFilters) { status ->
                    val isSelected = selectedStatus == status
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.onStatusFilterChange(status) },
                        label = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                if (status != "Semua") {
                                    Surface(
                                        modifier = Modifier.size(8.dp),
                                        shape = CircleShape,
                                        color = statusColor(status.uppercase().replace(" ", "_"))
                                    ) {}
                                }
                                Text(status, fontSize = 13.sp)
                            }
                        },
                        shape = RoundedCornerShape(20.dp),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF1A237E),
                            selectedLabelColor = Color.White
                        )
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            // Content
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }

                is UiState.Error -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Gagal memuat laporan", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text(state.message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { viewModel.retry() }) { Text("Coba Lagi") }
                        }
                    }
                }

                is UiState.Empty -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Belum ada laporan bencana", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { viewModel.retry() }) { Text("Segarkan") }
                        }
                    }
                }

                is UiState.Success -> {
                    val filtered = state.data.filter { report ->
                        val matchesSearch = searchQuery.isBlank() ||
                            report.title.contains(searchQuery, ignoreCase = true) ||
                            report.location.contains(searchQuery, ignoreCase = true) ||
                            (report.disasterType?.contains(searchQuery, ignoreCase = true) == true) ||
                            report.description.contains(searchQuery, ignoreCase = true)

                        val matchesStatus = selectedStatus == "Semua" ||
                            statusLabel(report.status).equals(selectedStatus, ignoreCase = true)

                        matchesSearch && matchesStatus
                    }

                    Text(
                        "${filtered.size} Laporan ditemukan",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))

                    if (filtered.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("Tidak ada laporan yang sesuai filter.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(bottom = 100.dp)
                        ) {
                            items(filtered, key = { it.id ?: it.title }) { report ->
                                DisasterReportCard(report = report)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisasterReportCard(report: DisasterReportDto) {
    val borderColor = statusBorderColor(report.status)
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(2.dp, borderColor.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Title + Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        report.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (report.description.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            report.description,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor(report.status).copy(alpha = 0.12f)
                ) {
                    Text(
                        statusLabel(report.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor(report.status)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Location, Date, Reporter row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Location
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        report.location.take(35) + if (report.location.length > 35) "..." else "",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Date
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        formatDisasterDate(report.createdAt),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Reporter
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        report.reporterName,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
