package com.mahasiswa.sigma.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.ui.viewmodel.ManageReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageReportScreen(
    onBack: () -> Unit,
    viewModel: ManageReportViewModel = hiltViewModel()
) {
    val filteredReports by viewModel.filteredReports.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val selectedReport by viewModel.selectedReport.collectAsState()
    val actionMessage by viewModel.actionMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearActionMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Kelola Laporan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Verifikasi, tinjau, dan kelola seluruh laporan",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (selectedReport != null) viewModel.selectReport(null)
                        else onBack()
                    }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadAllReports() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Main list view
            AnimatedVisibility(
                visible = selectedReport == null,
                enter = slideInHorizontally { -it },
                exit = slideOutHorizontally { -it }
            ) {
                ReportListContent(
                    reports = filteredReports,
                    isLoading = isLoading,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    pendingCount = viewModel.pendingCount,
                    activeCount = viewModel.activeCount,
                    onSearchQueryChange = { viewModel.setSearchQuery(it) },
                    onFilterChange = { viewModel.setFilter(it) },
                    onReportClick = { viewModel.selectReport(it) }
                )
            }

            // Detail view
            AnimatedVisibility(
                visible = selectedReport != null,
                enter = slideInHorizontally { it },
                exit = slideOutHorizontally { it }
            ) {
                selectedReport?.let { report ->
                    ReportDetailContent(
                        report = report,
                        onStatusChange = { newStatus ->
                            viewModel.updateReportStatus(report.id.toString(), newStatus)
                        },
                        onMarkCompleted = {
                            viewModel.markAsCompleted(report.id.toString())
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportListContent(
    reports: List<DisasterReportDto>,
    isLoading: Boolean,
    searchQuery: String,
    selectedFilter: String?,
    pendingCount: Int,
    activeCount: Int,
    onSearchQueryChange: (String) -> Unit,
    onFilterChange: (String?) -> Unit,
    onReportClick: (DisasterReportDto) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Stats row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFFE53935)))
                Text("Laporan Aktif", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$activeCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFFE53935))
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(0xFF1976D2)))
                Text("Butuh Verifikasi", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("$pendingCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1976D2))
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            placeholder = { Text("Cari laporan...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Hapus", modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )

        // Filter chips
        LazyRow(
            modifier = Modifier.padding(vertical = 8.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val filters = listOf(
                null to "Semua",
                "PENDING" to "Pending",
                "AWAS" to "Awas",
                "SIAGA_1" to "Siaga 1",
                "SIAGA_2" to "Siaga 2",
                "RESOLVED" to "Selesai"
            )
            items(filters) { (filterValue, label) ->
                FilterChip(
                    selected = selectedFilter == filterValue,
                    onClick = { onFilterChange(filterValue) },
                    label = { Text(label, fontSize = 13.sp) },
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Report list
        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            reports.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(
                            Icons.Default.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(56.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Text("Tidak ada laporan ditemukan", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    items(reports, key = { it.id ?: 0 }) { report ->
                        ReportListItem(report = report, onClick = { onReportClick(report) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportListItem(
    report: DisasterReportDto,
    onClick: () -> Unit
) {
    val statusColor = getStatusColor(report.status)
    val statusLabel = getStatusLabel(report.status)
    val timeAgo = report.createdAt?.let { formatTimeAgo(it) } ?: ""

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier.padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(48.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(statusColor)
            )

            Spacer(modifier = Modifier.width(12.dp))

            // Content
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    report.title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        timeAgo,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (report.reporterName.isNotBlank()) {
                        Text("·", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            report.reporterName,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Status badge
            Surface(
                shape = RoundedCornerShape(6.dp),
                color = statusColor.copy(alpha = 0.12f)
            ) {
                Text(
                    statusLabel,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
}

@Composable
private fun ReportDetailContent(
    report: DisasterReportDto,
    onStatusChange: (String) -> Unit,
    onMarkCompleted: () -> Unit
) {
    var selectedStatus by remember(report.id) { mutableStateOf(report.status) }
    var selectedDisasterType by remember(report.id) { mutableStateOf(report.disasterType ?: "Lainnya") }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }

    val timeAgo = report.createdAt?.let { formatTimeAgo(it) } ?: ""
    val statusColor = getStatusColor(report.status)
    val statusLabel = getStatusLabel(report.status)

    // Confirmation dialog for "Simpan Perubahan"
    if (showSaveConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showSaveConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.Save,
                    contentDescription = null,
                    tint = Color(0xFF1565C0),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Konfirmasi Perubahan", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Anda akan menyimpan perubahan berikut:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (selectedStatus != report.status) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${getStatusLabel(report.status)} → ${getStatusLabel(selectedStatus)}", fontSize = 13.sp, color = Color(0xFF1565C0))
                        }
                    }
                    if (selectedDisasterType != (report.disasterType ?: "Lainnya")) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Jenis:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Text("${report.disasterType ?: "Lainnya"} → $selectedDisasterType", fontSize = 13.sp, color = Color(0xFF1565C0))
                        }
                    }
                    if (selectedStatus == report.status && selectedDisasterType == (report.disasterType ?: "Lainnya")) {
                        Text("Tidak ada perubahan yang terdeteksi.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        onStatusChange(selectedStatus)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Simpan", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveConfirmDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Confirmation dialog for "Tandai Selesai"
    if (showCompleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCompleteConfirmDialog = false },
            icon = {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(32.dp)
                )
            },
            title = { Text("Tandai Selesai?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Laporan ini akan ditandai sebagai selesai:", fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Judul:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(report.title, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Status:", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text("${getStatusLabel(report.status)} → Selesai", fontSize = 13.sp, color = Color(0xFF2E7D32))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCompleteConfirmDialog = false
                        onMarkCompleted()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Tandai Selesai", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCompleteConfirmDialog = false }) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(20.dp)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Photo header
        val actualPhotoUrl = report.photoUrl?.let { raw ->
            // photo_url disimpan sebagai JSON array string: ["url"] — kita parse URL aslinya
            val trimmed = raw.trim()
            if (trimmed.startsWith("[")) {
                trimmed.removePrefix("[").removeSuffix("]")
                    .replace("\"", "").trim()
            } else {
                trimmed
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (!actualPhotoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(actualPhotoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto Laporan",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.ImageNotSupported,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Tidak ada foto", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f))
                }
            }
        }

        Column(modifier = Modifier.padding(16.dp)) {
            // Title & status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(report.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(timeAgo, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusLabel,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Info rows
            DetailInfoRow(icon = Icons.Default.LocationOn, label = "Lokasi", value = report.location.ifBlank { "-" })
            if (report.latitude != 0.0 || report.longitude != 0.0) {
                DetailInfoRow(
                    icon = Icons.Default.MyLocation,
                    label = "Koordinat",
                    value = "${report.latitude}, ${report.longitude}"
                )
            }
            DetailInfoRow(icon = Icons.Default.Person, label = "Pelapor", value = report.reporterName.ifBlank { "-" })
            report.createdAt?.let {
                DetailInfoRow(icon = Icons.Default.CalendarToday, label = "Tanggal Lapor", value = formatDate(it))
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Description
            Text("Deskripsi", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Text(report.description.ifBlank { "-" }, fontSize = 14.sp)

            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            // Tindakan section
            Text("Tindakan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(12.dp))

            // Status dropdown
            Text("Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showStatusDropdown = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(getStatusLabel(selectedStatus), fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showStatusDropdown,
                    onDismissRequest = { showStatusDropdown = false }
                ) {
                    ManageReportViewModel.STATUS_OPTIONS.forEach { status ->
                        DropdownMenuItem(
                            text = { Text(getStatusLabel(status)) },
                            onClick = {
                                selectedStatus = status
                                showStatusDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Disaster type dropdown
            Text("Jenis Bencana", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(4.dp))
            Box {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth().clickable { showTypeDropdown = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(selectedDisasterType, fontSize = 14.sp)
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }
                DropdownMenu(
                    expanded = showTypeDropdown,
                    onDismissRequest = { showTypeDropdown = false }
                ) {
                    ManageReportViewModel.DISASTER_TYPE_OPTIONS.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                selectedDisasterType = type
                                showTypeDropdown = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Save button
            Button(
                onClick = { showSaveConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Mark completed button
            OutlinedButton(
                onClick = { showCompleteConfirmDialog = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Tandai Selesai", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            // Map section - show if coordinates exist
            if (report.latitude != 0.0 || report.longitude != 0.0) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Lokasi Laporan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val reportLocation = LatLng(report.latitude, report.longitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(reportLocation, 15f)
                }

                Card(
                    modifier = Modifier.fillMaxWidth().height(200.dp),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState
                    ) {
                        Marker(
                            state = MarkerState(position = reportLocation),
                            title = report.title,
                            snippet = report.location
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 14.sp)
        }
    }
}

// ==================== HELPER FUNCTIONS ====================

private fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "PENDING" -> Color(0xFFE65100)
        "AWAS" -> Color(0xFFD32F2F)
        "SIAGA_1" -> Color(0xFFE65100)
        "SIAGA_2" -> Color(0xFFF9A825)
        "RESOLVED" -> Color(0xFF2E7D32)
        "DECLINE" -> Color(0xFF757575)
        else -> Color(0xFF1976D2)
    }
}

private fun getStatusLabel(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "Pending"
        "AWAS" -> "Awas"
        "SIAGA_1" -> "Siaga 1"
        "SIAGA_2" -> "Siaga 2"
        "RESOLVED" -> "Selesai"
        "DECLINE" -> "Ditolak"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun formatTimeAgo(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateStr) ?: return dateStr
        val now = System.currentTimeMillis()
        val diff = now - date.time

        val minutes = diff / (1000 * 60)
        val hours = diff / (1000 * 60 * 60)
        val days = diff / (1000 * 60 * 60 * 24)
        val weeks = days / 7
        val months = days / 30

        when {
            minutes < 1 -> "Baru saja"
            minutes < 60 -> "$minutes menit yang lalu"
            hours < 24 -> "$hours jam yang lalu"
            days < 7 -> "$days hari yang lalu"
            weeks < 4 -> "$weeks minggu yang lalu"
            months < 12 -> "$months bulan yang lalu"
            else -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(date)
        }
    } catch (e: Exception) {
        dateStr
    }
}

private fun formatDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        val date = sdf.parse(dateStr) ?: return dateStr
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(date)
    } catch (e: Exception) {
        dateStr
    }
}
