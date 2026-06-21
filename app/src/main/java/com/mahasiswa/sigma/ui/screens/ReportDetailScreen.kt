package com.mahasiswa.sigma.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.ui.viewmodel.ManageReportViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportDetailScreen(
    report: LocalDisasterReport,
    userRole: UserRole,
    onBack: () -> Unit,
    viewModel: ManageReportViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val timeAgo = formatTimeAgo(report.timestamp)
    val dateString = formatDate(report.timestamp)

    val actualPhotoUrl = report.photoUrl?.let { raw ->
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            trimmed.removePrefix("[").removeSuffix("]")
                .replace("\"", "").trim()
        } else {
            trimmed
        }
    }

    var selectedStatus by remember(report.id) { mutableStateOf(report.status) }
    var selectedDisasterType by remember(report.id) { mutableStateOf(report.disasterType ?: "Lainnya") }
    var showStatusDropdown by remember { mutableStateOf(false) }
    var showTypeDropdown by remember { mutableStateOf(false) }
    var showSaveConfirmDialog by remember { mutableStateOf(false) }
    var showCompleteConfirmDialog by remember { mutableStateOf(false) }

    val actionMessage by viewModel.actionMessage.collectAsState()

    LaunchedEffect(actionMessage) {
        actionMessage?.let {
            if (it.contains("berhasil", ignoreCase = true) || it.contains("selesai", ignoreCase = true)) {
                onBack()
            }
        }
    }

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
                    if (selectedStatus.uppercase() != report.status.uppercase()) {
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
                    if (selectedStatus.uppercase() == report.status.uppercase() && selectedDisasterType == (report.disasterType ?: "Lainnya")) {
                        Text("Tidak ada perubahan yang terdeteksi.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showSaveConfirmDialog = false
                        viewModel.updateReportStatus(report.id, selectedStatus, selectedDisasterType)
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
                        viewModel.markAsCompleted(report.id)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Detail Laporan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Tinjau detail dan status kejadian bencana",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Photo Header Section
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (!actualPhotoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
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
                            imageVector = Icons.Default.ImageNotSupported,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Tidak ada foto",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                val statusColor = getStatusColor(report.status)
                val statusLabel = getStatusLabel(report.status)

                // Title & status badge row
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

                // Detail Information items
                DetailInfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Lokasi",
                    value = report.location.ifBlank { "-" }
                )

                if (report.latitude != 0.0 || report.longitude != 0.0) {
                    DetailInfoRow(
                        icon = Icons.Default.MyLocation,
                        label = "Koordinat",
                        value = "${report.latitude}, ${report.longitude}"
                    )
                }

                DetailInfoRow(
                    icon = Icons.Default.Person,
                    label = "Pelapor",
                    value = report.reporter.ifBlank { "-" }
                )

                DetailInfoRow(
                    icon = Icons.Default.CalendarToday,
                    label = "Tanggal Lapor",
                    value = dateString
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Description section
                Text(
                    text = "Deskripsi",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = report.description.ifBlank { "-" },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // BNPB actions
                if (userRole == UserRole.BNPB) {
                    Spacer(modifier = Modifier.height(24.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Tindakan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Dropdown selector
                    Text("Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showStatusDropdown = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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

                    // Disaster type dropdown selector
                    Text("Jenis Bencana", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box {
                        OutlinedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showTypeDropdown = true },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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

                    Button(
                        onClick = { showSaveConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Text("Simpan Perubahan", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = { showCompleteConfirmDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF2E7D32)),
                        border = BorderStroke(1.5.dp, Color(0xFF2E7D32))
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Tandai Selesai", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }

                // Map Section
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
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
}

@Composable
private fun DetailInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun getStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "PENDING" -> Color(0xFFE65100)
        "AWAS" -> Color(0xFFD32F2F)
        "SIAGA_1", "SIAGA 1" -> Color(0xFFE65100)
        "SIAGA_2", "SIAGA 2" -> Color(0xFFF9A825)
        "RESOLVED" -> Color(0xFF2E7D32)
        "DECLINE", "DITOLAK" -> Color(0xFF757575)
        else -> Color(0xFF1976D2)
    }
}

private fun getStatusLabel(status: String): String {
    return when (status.uppercase()) {
        "PENDING" -> "Pending"
        "AWAS" -> "Awas"
        "SIAGA_1", "SIAGA 1" -> "Siaga 1"
        "SIAGA_2", "SIAGA 2" -> "Siaga 2"
        "RESOLVED" -> "Selesai"
        "DECLINE", "DITOLAK" -> "Ditolak"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun formatTimeAgo(timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val minutes = diff / (1000 * 60)
    val hours = diff / (1000 * 60 * 60)
    val days = diff / (1000 * 60 * 60 * 24)
    val weeks = days / 7
    val months = days / 30

    return when {
        minutes < 1 -> "Baru saja"
        minutes < 60 -> "$minutes menit yang lalu"
        hours < 24 -> "$hours jam yang lalu"
        days < 7 -> "$days hari yang lalu"
        weeks < 4 -> "$weeks minggu yang lalu"
        months < 12 -> "$months bulan yang lalu"
        else -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date(timestamp))
    }
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(Date(timestamp))
}
