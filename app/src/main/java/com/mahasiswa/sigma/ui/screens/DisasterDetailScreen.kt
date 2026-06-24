package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.ui.viewmodel.DisasterDetailViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisasterDetailScreen(
    disasterId: Int,
    onBack: () -> Unit,
    viewModel: DisasterDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(disasterId) {
        viewModel.loadDisaster(disasterId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Detail Bencana", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text(
                            "Informasi lengkap laporan bencana",
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
        when (val state = uiState) {
            is UiState.Idle, is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            is UiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Gagal memuat detail", fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            state.message,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(onClick = { viewModel.loadDisaster(disasterId) }) {
                            Text("Coba Lagi")
                        }
                    }
                }
            }

            is UiState.Empty -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Laporan tidak ditemukan", fontWeight = FontWeight.Bold)
                }
            }

            is UiState.Success -> {
                DisasterDetailContent(
                    report = state.data,
                    modifier = Modifier.padding(padding)
                )
            }
        }
    }
}

@Composable
private fun DisasterDetailContent(
    report: DisasterReportDto,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val actualPhotoUrl = report.photoUrl?.let { raw ->
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            trimmed.removePrefix("[").removeSuffix("]")
                .replace("\"", "").trim()
        } else {
            trimmed
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Photo Header
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
                    contentDescription = "Foto Bencana",
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
            val statusColor = disasterDetailStatusColor(report.status)
            val statusText = disasterDetailStatusLabel(report.status)

            // Title & Status badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(report.title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        disasterDetailFormatDate(report.createdAt),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        statusText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Detail information rows
            if (!report.disasterType.isNullOrBlank()) {
                DisasterInfoRow(
                    icon = Icons.Default.Category,
                    label = "Jenis Bencana",
                    value = report.disasterType
                )
            }

            DisasterInfoRow(
                icon = Icons.Default.LocationOn,
                label = "Lokasi",
                value = report.location.ifBlank { "-" }
            )

            if (report.latitude != 0.0 || report.longitude != 0.0) {
                DisasterInfoRow(
                    icon = Icons.Default.MyLocation,
                    label = "Koordinat",
                    value = "${report.latitude}, ${report.longitude}"
                )
            }

            DisasterInfoRow(
                icon = Icons.Default.Person,
                label = "Pelapor",
                value = report.reporterName.ifBlank { "-" }
            )

            DisasterInfoRow(
                icon = Icons.Default.CalendarToday,
                label = "Tanggal Lapor",
                value = disasterDetailFormatDate(report.createdAt)
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

            // Map Section
            if (report.latitude != 0.0 || report.longitude != 0.0) {
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                Text("Lokasi di Peta", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val location = LatLng(report.latitude, report.longitude)
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(location, 15f)
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
                            state = MarkerState(position = location),
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
private fun DisasterInfoRow(
    icon: ImageVector,
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

private fun disasterDetailStatusColor(status: String): Color = when (status.uppercase()) {
    "AWAS" -> Color(0xFFD32F2F)
    "SIAGA_1", "SIAGA 1" -> Color(0xFFF57C00)
    "SIAGA_2", "SIAGA 2" -> Color(0xFFFBC02D)
    "PENDING" -> Color(0xFFFF8F00)
    "RESOLVED" -> Color(0xFF388E3C)
    else -> Color(0xFF78909C)
}

private fun disasterDetailStatusLabel(status: String): String = when (status.uppercase()) {
    "AWAS" -> "Awas"
    "SIAGA_1" -> "Siaga 1"
    "SIAGA_2" -> "Siaga 2"
    "PENDING" -> "Pending"
    "RESOLVED" -> "Selesai"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun disasterDetailFormatDate(timestamp: String?): String {
    if (timestamp.isNullOrBlank()) return "-"
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = sdf.parse(timestamp) ?: return timestamp
        SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(date)
    } catch (_: Exception) {
        timestamp
    }
}
