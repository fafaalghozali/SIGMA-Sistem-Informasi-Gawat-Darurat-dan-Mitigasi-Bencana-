package com.mahasiswa.sigma.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.ui.viewmodel.DisasterReportViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisasterReportScreen(
    onBack: () -> Unit,
    onNavigateToDetail: (LocalDisasterReport) -> Unit,
    viewModel: DisasterReportViewModel = viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reportsList by viewModel.reports.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val sheetState = rememberModalBottomSheetState()

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchFreshLocation(context, fusedLocationClient, scope) { address ->
                viewModel.onLocationReceived(address)
            }
        } else {
            viewModel.onLocationReceived("Lokasi tidak aktif. Klik untuk aktifkan.")
        }
    }

    fun checkLocationSettings() {
        viewModel.onLocationReceived("Mendeteksi lokasi...")
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(context)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            fetchFreshLocation(context, fusedLocationClient, scope) { address ->
                viewModel.onLocationReceived(address)
            }
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution.intentSender).build()
                    settingResultRequest.launch(intentSenderRequest)
                } catch (sendEx: IntentSender.SendIntentException) {
                    viewModel.onLocationReceived("Gagal mengaktifkan lokasi")
                }
            } else {
                viewModel.onLocationReceived("GPS tidak tersedia")
            }
        }
    }

    val openSettingsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        checkLocationSettings()
    }

    fun openLocationSettings() {
        val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
        openSettingsLauncher.launch(intent)
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            checkLocationSettings()
        } else {
            viewModel.onLocationReceived("Izin lokasi ditolak. Klik untuk buka Pengaturan.")
        }
    }

    LaunchedEffect(Unit) {
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettings()
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    DisasterReportContent(
        title = viewModel.title,
        description = viewModel.description,
        locationAddress = viewModel.locationAddress,
        imageBitmap = viewModel.imageBitmap,
        reportsList = reportsList,
        showIncompleteDialog = viewModel.showIncompleteDialog,
        showPhotoSourceSheet = viewModel.showPhotoSourceSheet,
        isLoading = viewModel.isLoading,
        sheetState = sheetState,
        onTitleChange = viewModel::onTitleChange,
        onDescriptionChange = viewModel::onDescriptionChange,
        onImageClick = { viewModel.showPhotoSourceSheet = true },
        onSendClick = viewModel::sendReport,
        onBack = onBack,
        onNavigateToDetail = onNavigateToDetail,
        onDismissIncompleteDialog = { viewModel.showIncompleteDialog = false },
        onDismissPhotoSheet = { viewModel.showPhotoSourceSheet = false },
        onImageSelected = viewModel::onImageSelected,
        onStatusUpdate = viewModel::updateReport,
        onRetryLocation = {
            if (viewModel.locationAddress.contains("ditolak") || viewModel.locationAddress.contains("Pengaturan")) {
                openLocationSettings()
            } else {
                checkLocationSettings()
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisasterReportContent(
    title: String,
    description: String,
    locationAddress: String,
    imageBitmap: android.graphics.Bitmap?,
    reportsList: List<LocalDisasterReport>,
    showIncompleteDialog: Boolean,
    showPhotoSourceSheet: Boolean,
    isLoading: Boolean,
    sheetState: SheetState,
    onTitleChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onImageClick: () -> Unit,
    onSendClick: () -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (LocalDisasterReport) -> Unit,
    onDismissIncompleteDialog: () -> Unit,
    onDismissPhotoSheet: () -> Unit,
    onImageSelected: (android.graphics.Bitmap?) -> Unit,
    onStatusUpdate: (LocalDisasterReport) -> Unit,
    onRetryLocation: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (showIncompleteDialog || showPhotoSourceSheet) 10.dp else 0.dp),
            topBar = {
                TopAppBar(
                    title = { Text("Lapor Kejadian", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("Kembali", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 24.dp)
            ) {
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onRetryLocation() },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                        )
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Lokasi Terdeteksi", fontSize = 11.sp, color = MaterialTheme.colorScheme.primary)
                                Text(
                                    text = locationAddress,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                            if (locationAddress == "Mendeteksi lokasi...") {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    OutlinedTextField(
                        value = title,
                        onValueChange = onTitleChange,
                        label = { Text("Jenis Bencana / Judul") },
                        placeholder = { Text("Contoh: Banjir Bandang") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = onDescriptionChange,
                        label = { Text("Deskripsi Kejadian") },
                        placeholder = { Text("Ceritakan detail kejadian...") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3,
                        shape = RoundedCornerShape(12.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { onImageClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageBitmap != null) {
                            Image(
                                bitmap = imageBitmap.asImageBitmap(),
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Default.CameraAlt, null, modifier = Modifier.size(32.dp))
                                Text("Tambah Foto Kejadian", fontSize = 12.sp)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                item {
                    Button(
                        onClick = onSendClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Kirim Laporan", fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Riwayat Laporan Anda", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                if (reportsList.isEmpty()) {
                    item {
                        Text(
                            "Belum ada laporan yang dikirim.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    items(reportsList, key = { it.id }) { report ->
                        ReportItemCard(
                            report = report,
                            onStatusUpdate = onStatusUpdate,
                            onClick = { onNavigateToDetail(report) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }

                item { Spacer(modifier = Modifier.height(32.dp)) }
            }
        }

        if (showIncompleteDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {}
            )
            AlertDialog(
                onDismissRequest = onDismissIncompleteDialog,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.errorContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Laporan Belum Lengkap",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Text(
                        text = "Harap isi semua bidang (Judul, Deskripsi, dan Foto) sebelum mengirim laporan.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(
                        onClick = onDismissIncompleteDialog,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Mengerti", fontWeight = FontWeight.Bold)
                    }
                },
                shape = RoundedCornerShape(24.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 8.dp
            )
        }

        if (showPhotoSourceSheet) {
            ImagePickerBottomSheet(
                sheetState = sheetState,
                onDismiss = onDismissPhotoSheet,
                onImageSelected = onImageSelected
            )
        }
    }
}

/**
 * Mendapatkan lokasi terbaru dengan tingkat akurasi tinggi dan mengubahnya
 * menjadi alamat yang dapat dibaca menggunakan proses background untuk mencegah lag.
 */
private fun fetchFreshLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (String) -> Unit
) {
    try {
        val cts = CancellationTokenSource()
        fusedLocationClient.getCurrentLocation(
            Priority.PRIORITY_HIGH_ACCURACY,
            cts.token
        ).addOnSuccessListener { location ->
            if (location != null) {
                val geocoder = Geocoder(context, Locale.getDefault())

                scope.launch(Dispatchers.IO) {
                    try {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                            geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                val result = if (addresses.isNotEmpty()) {
                                    addresses[0].getAddressLine(0)
                                } else {
                                    "${location.latitude}, ${location.longitude}"
                                }
                                scope.launch(Dispatchers.Main) { onResult(result) }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val result = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0)
                            } else {
                                "${location.latitude}, ${location.longitude}"
                            }
                            scope.launch(Dispatchers.Main) { onResult(result) }
                        }
                    } catch (e: Exception) {
                        scope.launch(Dispatchers.Main) { 
                            onResult("${location.latitude}, ${location.longitude}") 
                        }
                    }
                }
            } else {
                onResult("Gagal mendapatkan lokasi")
            }
        }.addOnFailureListener {
            onResult("Gagal mendeteksi lokasi")
        }
    } catch (e: SecurityException) {
        onResult("Izin lokasi tidak diberikan")
    }
}

@Composable
fun ReportItemCard(
    report: LocalDisasterReport,
    onStatusUpdate: (LocalDisasterReport) -> Unit,
    onClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(report.timestamp))

    LaunchedEffect(report.id) {
        if (report.status == "Pending") {
            delay(15000)
            onStatusUpdate(
                report.copy(
                    status = "Accepted"
                )
            )
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(report.title, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (report.status == "Accepted") {
                        Surface(
                            color = Color(0xFFB71C1C),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Siaga 1",
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Surface(
                        color = if (report.status == "Pending") Color(0xFFFFF3E0) else Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = report.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (report.status == "Pending") Color(0xFFE65100) else Color(0xFF2E7D32)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(report.description, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(4.dp))
                Text(report.location, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
