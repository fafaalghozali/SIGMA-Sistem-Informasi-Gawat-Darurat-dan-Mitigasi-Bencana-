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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.mahasiswa.sigma.ui.theme.DarkElevatedSurface
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.CancellationTokenSource
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.data.model.VolunteerReportParser
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
    userRole: UserRole = UserRole.MASYARAKAT,
    userEmail: String = "",
    viewModel: DisasterReportViewModel = hiltViewModel()
) {
    LaunchedEffect(userEmail) {
        if (userRole == UserRole.RELAWAN && userEmail.isNotBlank()) {
            viewModel.loadVolunteerSkill(userEmail)
        }
    }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val reportsList by viewModel.reports.collectAsState()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val sheetState = rememberModalBottomSheetState()

    val settingResultRequest = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            fetchFreshLocation(context, fusedLocationClient, scope) { address, lat, lng ->
                viewModel.onLocationReceived(address, lat, lng)
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
            fetchFreshLocation(context, fusedLocationClient, scope) { address, lat, lng ->
                viewModel.onLocationReceived(address, lat, lng)
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
        userRole = userRole,
        volunteerSkill = viewModel.volunteerSkill,
        volunteerAssignment = viewModel.volunteerAssignment,
        disasterLocation = viewModel.disasterLocation,
        onSendVolunteerReport = { disasterTitle, dataLaporan, catatanTambahan ->
            viewModel.sendVolunteerReport(disasterTitle, dataLaporan, catatanTambahan)
        },
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
    userRole: UserRole = UserRole.MASYARAKAT,
    volunteerSkill: SkillsVolunteer? = null,
    volunteerAssignment: String? = null,
    disasterLocation: String? = null,
    onSendVolunteerReport: (String, String, String) -> Unit = { _, _, _ -> },
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
    var showConfirmDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (showIncompleteDialog || showPhotoSourceSheet || showConfirmDialog) 10.dp else 0.dp),
            topBar = {
                val topBarTitle = if (userRole == UserRole.RELAWAN) "Laporan Tugas Relawan" else "Lapor Kejadian"
                TopAppBar(
                    title = { Text(topBarTitle, fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("Kembali", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
            }
        ) { padding ->
            if (userRole == UserRole.RELAWAN) {
                VolunteerReportLayout(
                    padding = padding,
                    volunteerSkill = volunteerSkill,
                    volunteerAssignment = volunteerAssignment,
                    disasterLocation = disasterLocation,
                    reportsList = reportsList,
                    onSendReport = onSendVolunteerReport,
                    onBack = onBack,
                    onNavigateToDetail = onNavigateToDetail
                )
            } else {
                LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                // Form Card
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Send,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Formulir Laporan Baru", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    Text("Lengkapi semua kolom yang diperlukan", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // Judul Laporan
                            Text("JUDUL LAPORAN *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = title,
                                onValueChange = onTitleChange,
                                placeholder = { Text("Contoh: Banjir bandang di kawasan Perumahan Indah", fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            // Deskripsi
                            Text("DESKRIPSI LENGKAP *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = description,
                                onValueChange = onDescriptionChange,
                                placeholder = { Text("Ceritakan detail kejadian secara kronologis...", fontSize = 14.sp) },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 4,
                                shape = RoundedCornerShape(10.dp)
                            )

                            Spacer(Modifier.height(16.dp))

                            // Lokasi di Peta
                            Text("PILIH LOKASI DI PETA *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))

                            // Gunakan Lokasi Saya button
                            Button(
                                onClick = onRetryLocation,
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                            ) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Gunakan Lokasi Saya", fontWeight = FontWeight.SemiBold)
                            }

                            Spacer(Modifier.height(8.dp))

                            // Location address display
                            if (locationAddress.isNotBlank() && locationAddress != "Mendeteksi lokasi...") {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text(
                                            locationAddress,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 2,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            } else if (locationAddress == "Mendeteksi lokasi...") {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Mendeteksi lokasi...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // Dokumentasi Foto
                            Text("DOKUMENTASI FOTO *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(Modifier.height(6.dp))

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(140.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                                        Icon(
                                            Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(36.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.height(6.dp))
                                        Text("Tambah Foto Kejadian", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                                        Text("Maksimal 3 foto, Total ukuran maks 25MB", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "ⓘ Pilih berkas foto dokumentasi kejadian yang valid",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            Spacer(Modifier.height(20.dp))

                            // Action buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(onClick = onBack) {
                                    Text("Batal", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Button(
                                    onClick = {
                                        if (title.isBlank() || description.isBlank()) {
                                            onSendClick()  // This triggers showIncompleteDialog
                                        } else {
                                            showConfirmDialog = true
                                        }
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    enabled = !isLoading,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                                ) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Kirim Laporan", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // Riwayat Laporan Anda
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.History, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Riwayat Laporan Anda", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
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

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
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

        if (showConfirmDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.3f))
                    .clickable(enabled = false) {}
            )
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = {
                    Text(
                        "Konfirmasi Laporan",
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column {
                        Text("Pastikan data laporan sudah benar sebelum dikirim:", fontSize = 14.sp)
                        Spacer(Modifier.height(12.dp))
                        Text("Judul: $title", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Deskripsi: $description", fontSize = 13.sp, maxLines = 3, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Spacer(Modifier.height(4.dp))
                        Text("Lokasi: $locationAddress", fontSize = 13.sp, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                        Spacer(Modifier.height(12.dp))
                        Text("Apakah data di atas sudah benar?", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            onSendClick()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1565C0))
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ya, Kirim Laporan", fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text("Periksa Lagi", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = MaterialTheme.colorScheme.surface,
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

private fun fetchFreshLocation(
    context: Context,
    fusedLocationClient: FusedLocationProviderClient,
    scope: kotlinx.coroutines.CoroutineScope,
    onResult: (address: String, lat: Double, lng: Double) -> Unit
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
                                scope.launch(Dispatchers.Main) {
                                    onResult(result, location.latitude, location.longitude)
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                            val result = if (!addresses.isNullOrEmpty()) {
                                addresses[0].getAddressLine(0)
                            } else {
                                "${location.latitude}, ${location.longitude}"
                            }
                            scope.launch(Dispatchers.Main) {
                                onResult(result, location.latitude, location.longitude)
                            }
                        }
                    } catch (e: Exception) {
                        scope.launch(Dispatchers.Main) {
                            onResult("${location.latitude}, ${location.longitude}", location.latitude, location.longitude)
                        }
                    }
                }
            } else {
                onResult("Gagal mendapatkan lokasi", 0.0, 0.0)
            }
        }.addOnFailureListener {
            onResult("Gagal mendeteksi lokasi", 0.0, 0.0)
        }
    } catch (e: SecurityException) {
        onResult("Izin lokasi tidak diberikan", 0.0, 0.0)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerReportLayout(
    padding: PaddingValues,
    volunteerSkill: SkillsVolunteer?,
    volunteerAssignment: String? = null,
    disasterLocation: String? = null,
    reportsList: List<LocalDisasterReport>,
    onSendReport: (String, String, String) -> Unit,
    onBack: () -> Unit,
    onNavigateToDetail: (LocalDisasterReport) -> Unit
) {
    val isDark = isSystemInDarkTheme()

    val disasterOptions = remember(reportsList) {
        val list = reportsList.filter { it.status == "Accepted" || it.status == "Verified" || it.status == "Siaga 1" || it.status == "Awas" || it.status == "SIAGA_1" || it.status == "SIAGA_2" || it.status == "AWAS" }.map { it.title }
        if (list.isEmpty()) {
            listOf(
                "Banjir Bandang Surakarta (Mei 2026)",
                "Gempa Bumi Yogyakarta (M 5.6)",
                "Tanah Longsor Karanganyar (Sektor C)"
            )
        } else {
            list
        }
    }
    var selectedDisaster by remember { mutableStateOf("") }
    var disasterExpanded by remember { mutableStateOf(false) }

    var catatanTambahan by remember { mutableStateOf("") }

    var jumlahDidampingi by remember { mutableStateOf("") }
    var kondisiPsikologis by remember { mutableStateOf("Stabil") }
    var psikologisExpanded by remember { mutableStateOf(false) }
    var kasusKhusus by remember { mutableStateOf("") }
    var rekomendasi by remember { mutableStateOf("") }

    var jenisBantuan by remember { mutableStateOf("") }
    var jumlahDisalurkan by remember { mutableStateOf("") }
    var stokTersisa by remember { mutableStateOf("") }
    var kebutuhanMendesakLogistik by remember { mutableStateOf("") }

    var totalKorban by remember { mutableStateOf("") }
    var selamat by remember { mutableStateOf("") }
    var lukaRingan by remember { mutableStateOf("") }
    var lukaBerat by remember { mutableStateOf("") }
    var kritis by remember { mutableStateOf("") }
    var meninggal by remember { mutableStateOf("") }
    var kebutuhanMedis by remember { mutableStateOf("") }

    var totalDievakuasi by remember { mutableStateOf("") }
    var masihDicari by remember { mutableStateOf("") }
    var lokasiEvakuasi by remember { mutableStateOf("") }
    var kendalaDiLapangan by remember { mutableStateOf("") }
    var statusPencarian by remember { mutableStateOf("Sedang Berjalan") }
    var pencarianExpanded by remember { mutableStateOf(false) }

    var jumlahPorsi by remember { mutableStateOf("") }
    var menuHariIni by remember { mutableStateOf("") }
    var pengungsiDilayani by remember { mutableStateOf("") }
    var kebutuhanBahan by remember { mutableStateOf("") }

    var jumlahSiswa by remember { mutableStateOf("") }
    var materiPembelajaran by remember { mutableStateOf("") }
    var kebutuhanEduKits by remember { mutableStateOf("") }

    var showIncompleteDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val skillColor = when (volunteerSkill) {
        SkillsVolunteer.MEDIS -> Color(0xFF2196F3)
        SkillsVolunteer.SAR -> Color(0xFFFF5722)
        SkillsVolunteer.LOGISTIK -> Color(0xFF4CAF50)
        SkillsVolunteer.PSIKOSOSIAL -> Color(0xFF9C27B0)
        SkillsVolunteer.KONSUMSI -> Color(0xFFFFC107)
        SkillsVolunteer.PENDIDIKAN -> Color(0xFF009688)
        else -> Color(0xFF757575)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(padding)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = skillColor.copy(alpha = 0.08f)
            ),
            border = BorderStroke(1.dp, skillColor.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(skillColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.People,
                        contentDescription = null,
                        tint = skillColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Divisi Penugasan",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Text(
                        text = "Relawan ${volunteerSkill?.name ?: "UMUM"}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = Color(0xFFE8F5E9),
                    contentColor = Color(0xFF2E7D32)
                ) {
                    Text(
                        text = "Aktif",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ── Info Lokasi Otomatis ───────────────────────────────────────────
        if (!volunteerAssignment.isNullOrBlank() || !disasterLocation.isNullOrBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Informasi Penugasan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (!volunteerAssignment.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.HomeWork,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Lokasi Posko",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    volunteerAssignment,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    if (!disasterLocation.isNullOrBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Column {
                                Text(
                                    "Lokasi Kejadian",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    disasterLocation,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Form Pelaporan Tugas",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                ExposedDropdownMenuBox(
                    expanded = disasterExpanded,
                    onExpandedChange = { disasterExpanded = !disasterExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = selectedDisaster.ifEmpty { "Pilih Bencana" },
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Terkait Bencana") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disasterExpanded) },
                        modifier = Modifier
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                            focusedLabelColor = MaterialTheme.colorScheme.primary,
                            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            focusedTextColor = if (selectedDisaster.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            unfocusedTextColor = if (selectedDisaster.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = disasterExpanded,
                        onDismissRequest = { disasterExpanded = false }
                    ) {
                        disasterOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    selectedDisaster = option
                                    disasterExpanded = false
                                }
                            )
                        }
                    }
                }

                when (volunteerSkill) {
                    SkillsVolunteer.PSIKOSOSIAL -> {
                        SigmaNumberField(jumlahDidampingi, { jumlahDidampingi = it }, "Jumlah Didampingi (Jiwa)", "Contoh: 15")

                        val psikologisOptions = listOf("Stabil", "Trauma Ringan", "Trauma Sedang", "Trauma Berat")
                        ExposedDropdownMenuBox(
                            expanded = psikologisExpanded,
                            onExpandedChange = { psikologisExpanded = !psikologisExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = kondisiPsikologis,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Kondisi Psikologis Umum") },
                                placeholder = { Text("Pilih Kondisi") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = psikologisExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = psikologisExpanded,
                                onDismissRequest = { psikologisExpanded = false }
                            ) {
                                psikologisOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            kondisiPsikologis = option
                                            psikologisExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        SigmaTextAreaField(kasusKhusus, { kasusKhusus = it }, "Kasus Khusus", "Ceritakan jika ada kasus depresi berat/histeria...")
                        SigmaTextAreaField(rekomendasi, { rekomendasi = it }, "Rekomendasi Penanganan", "Rekomendasi rujukan klinis atau therapy lanjutan...")
                    }

                    SkillsVolunteer.LOGISTIK -> {
                        SigmaTextField(jenisBantuan, { jenisBantuan = it }, "Jenis Bantuan", "Contoh: Selimut, Hygiene Kit")
                        SigmaNumberField(jumlahDisalurkan, { jumlahDisalurkan = it }, "Jumlah Disalurkan (Paket)", "Contoh: 50")
                        SigmaNumberField(stokTersisa, { stokTersisa = it }, "Stok Tersisa di Posko (Paket)", "Contoh: 200")
                        SigmaTextAreaField(kebutuhanMendesakLogistik, { kebutuhanMendesakLogistik = it }, "Kebutuhan Mendesak Gudang", "Barang bantuan yang masih sangat minim...")
                    }

                    SkillsVolunteer.MEDIS -> {
                        SigmaNumberField(totalKorban, { totalKorban = it }, "Total Korban Ditangani (Jiwa)", "Contoh: 24")
                        SigmaNumberField(selamat, { selamat = it }, "Kondisi: Selamat / Sehat", "Contoh: 12")
                        SigmaNumberField(lukaRingan, { lukaRingan = it }, "Kondisi: Luka Ringan", "Contoh: 6")
                        SigmaNumberField(lukaBerat, { lukaBerat = it }, "Kondisi: Luka Berat", "Contoh: 4")
                        SigmaNumberField(kritis, { kritis = it }, "Kondisi: Kritis", "Contoh: 2")
                        SigmaNumberField(meninggal, { meninggal = it }, "Kondisi: Meninggal Dunia", "Contoh: 0")
                        SigmaTextAreaField(kebutuhanMedis, { kebutuhanMedis = it }, "Kebutuhan Medis Mendesak", "Contoh: Oksigen, Vaksin, Perban Steril...")
                    }

                    SkillsVolunteer.SAR -> {
                        SigmaNumberField(totalDievakuasi, { totalDievakuasi = it }, "Total Dievakuasi (Jiwa)", "Contoh: 8")
                        SigmaNumberField(masihDicari, { masihDicari = it }, "Masih Dicari / Hilang (Jiwa)", "Contoh: 2")
                        SigmaTextField(lokasiEvakuasi, { lokasiEvakuasi = it }, "Lokasi Evakuasi (Koordinat/Sektor)", "Contoh: Sektor C, RT 02/03")
                        SigmaTextAreaField(kendalaDiLapangan, { kendalaDiLapangan = it }, "Kendala di Lapangan", "Contoh: Arus deras, jembatan terputus...")

                        val statusOptions = listOf("Sedang Berjalan", "Dihentikan Sementara", "Selesai")
                        ExposedDropdownMenuBox(
                            expanded = pencarianExpanded,
                            onExpandedChange = { pencarianExpanded = !pencarianExpanded },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OutlinedTextField(
                                value = statusPencarian,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Status Pencarian") },
                                placeholder = { Text("Pilih Status") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = pencarianExpanded) },
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                    focusedLabelColor = MaterialTheme.colorScheme.primary,
                                    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                )
                            )
                            ExposedDropdownMenu(
                                expanded = pencarianExpanded,
                                onDismissRequest = { pencarianExpanded = false }
                            ) {
                                statusOptions.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(option) },
                                        onClick = {
                                            statusPencarian = option
                                            pencarianExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    SkillsVolunteer.KONSUMSI -> {
                        SigmaNumberField(jumlahPorsi, { jumlahPorsi = it }, "Jumlah Porsi Didistribusikan", "Contoh: 150")
                        SigmaTextField(menuHariIni, { menuHariIni = it }, "Menu Hari Ini", "Contoh: Nasi Putih + Telur Dadar + Sayur Sop")
                        SigmaNumberField(pengungsiDilayani, { pengungsiDilayani = it }, "Total Pengungsi Dilayani (Jiwa)", "Contoh: 120")
                        SigmaTextAreaField(kebutuhanBahan, { kebutuhanBahan = it }, "Kebutuhan Bahan Masak", "Contoh: Gas LPG 3Kg, Air Bersih, Bumbu Dapur...")
                    }

                    SkillsVolunteer.PENDIDIKAN -> {
                        SigmaNumberField(jumlahSiswa, { jumlahSiswa = it }, "Jumlah Siswa Mengikuti Kelas", "Contoh: 25")
                        SigmaTextField(materiPembelajaran, { materiPembelajaran = it }, "Materi Pembelajaran", "Contoh: Belajar Calistung & Dongeng Pagi")
                        SigmaTextAreaField(kebutuhanEduKits, { kebutuhanEduKits = it }, "Kebutuhan Edu-Kits Mendesak", "Contoh: Buku gambar, pensil warna, papan tulis lipat...")
                    }

                    else -> {
                        SigmaTextAreaField(catatanTambahan, { catatanTambahan = it }, "Data Pelaporan Lapangan", "Ceritakan detail pelaporan tugas Anda...")
                    }
                }

                SigmaTextAreaField(catatanTambahan, { catatanTambahan = it }, "Catatan Tambahan", "Catatan penting di luar formulir wajib...")
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier
                    .weight(1f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text("Batal", fontSize = 14.sp)
            }

            Button(
                onClick = {
                    val map = mutableMapOf<String, String>()
                    var isValid = true

                    if (selectedDisaster.isBlank()) isValid = false

                    when (volunteerSkill) {
                        SkillsVolunteer.PSIKOSOSIAL -> {
                            if (jumlahDidampingi.isBlank()) isValid = false
                            map["jumlah_didampingi"] = jumlahDidampingi
                            map["kondisi_psikologis"] = kondisiPsikologis
                            map["kasus_khusus"] = kasusKhusus
                            map["rekomendasi"] = rekomendasi
                        }
                        SkillsVolunteer.LOGISTIK -> {
                            if (jenisBantuan.isBlank() || jumlahDisalurkan.isBlank()) isValid = false
                            map["jenis_bantuan"] = jenisBantuan
                            map["jumlah_disalurkan"] = jumlahDisalurkan
                            map["stok_tersisa"] = stokTersisa
                            map["kebutuhan_mendesak"] = kebutuhanMendesakLogistik
                        }
                        SkillsVolunteer.MEDIS -> {
                            if (totalKorban.isBlank()) isValid = false
                            map["total_korban"] = totalKorban
                            map["selamat"] = selamat
                            map["luka_ringan"] = lukaRingan
                            map["luka_berat"] = lukaBerat
                            map["kritis"] = kritis
                            map["meninggal"] = meninggal
                            map["kebutuhan_medis"] = kebutuhanMedis
                        }
                        SkillsVolunteer.SAR -> {
                            if (totalDievakuasi.isBlank() || lokasiEvakuasi.isBlank()) isValid = false
                            map["total_dievakuasi"] = totalDievakuasi
                            map["masih_dicari"] = masihDicari
                            map["lokasi_evakuasi"] = lokasiEvakuasi
                            map["kendala_di_lapangan"] = kendalaDiLapangan
                            map["status_pencarian"] = statusPencarian
                        }
                        SkillsVolunteer.KONSUMSI -> {
                            if (jumlahPorsi.isBlank() || menuHariIni.isBlank()) isValid = false
                            map["jumlah_porsi"] = jumlahPorsi
                            map["menu_hari_ini"] = menuHariIni
                            map["pengungsi_dilayani"] = pengungsiDilayani
                            map["kebutuhan_bahan"] = kebutuhanBahan
                        }
                        SkillsVolunteer.PENDIDIKAN -> {
                            if (jumlahSiswa.isBlank() || materiPembelajaran.isBlank()) isValid = false
                            map["jumlah_siswa"] = jumlahSiswa
                            map["materi_pembelajaran"] = materiPembelajaran
                            map["kebutuhan_edu_kits"] = kebutuhanEduKits
                        }
                        else -> {
                            map["catatan_pelaporan_lapangan"] = catatanTambahan
                        }
                    }

                    if (!isValid) {
                        showIncompleteDialog = true
                    } else {
                        val reportJson = VolunteerReportParser.toJson(map)
                        onSendReport(selectedDisaster, reportJson, catatanTambahan)
                        onBack()
                    }
                },
                modifier = Modifier
                    .weight(1.2f)
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Kirim Laporan", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(4.dp))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        Spacer(modifier = Modifier.height(4.dp))

        val taskReports = reportsList.filter { it.title.contains("[LAPORAN TUGAS") }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkElevatedSurface else Color.White
            ),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Statistik Laporan Anda",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Total Kiriman", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${taskReports.size}", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFE8F5E9).copy(alpha = 0.5f))
                            .padding(8.dp)
                    ) {
                        Column {
                            Text("Status Tugas", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            Text("Aktif", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2E7D32))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkElevatedSurface else Color.White
            ),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Riwayat Laporan Tugas",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                if (taskReports.isEmpty()) {
                    Text(
                        text = "Belum ada laporan tugas dikirim.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
                    )
                } else {
                    taskReports.take(3).forEachIndexed { idx, report ->
                        val cleanTitle = report.title.removePrefix("[LAPORAN TUGAS - ").substringAfter("] ")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onNavigateToDetail(report) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cleanTitle, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text("Status: ${report.status}", fontSize = 10.sp, color = Color(0xFF2E7D32))
                            }
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                        }
                        if (idx < taskReports.take(3).size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkElevatedSurface else Color.White
            ),
            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "Panduan Operasional Cepat",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(8.dp))

                val tips = when (volunteerSkill) {
                    SkillsVolunteer.MEDIS -> listOf(
                        "Prioritaskan triase Merah untuk pasien kritis.",
                        "Minta pasokan obat kritis sesegera mungkin.",
                        "Dokumentasikan rujukan ambulance dengan baik."
                    )
                    SkillsVolunteer.SAR -> listOf(
                        "Utamakan keselamatan diri dan tim SAR Anda.",
                        "Tandai koordinat dengan presisi tinggi.",
                        "Segera koordinasikan kendala taktis dengan BNPB."
                    )
                    SkillsVolunteer.LOGISTIK -> listOf(
                        "Selalu scan barcode saat serah-terima bantuan.",
                        "Laporkan segera jika air/selimut menipis (<20%).",
                        "Tanda tangani surat jalan pengiriman secara digital."
                    )
                    SkillsVolunteer.KONSUMSI -> listOf(
                        "Jaga higienitas bahan pangan di dapur umum.",
                        "Hitung porsi berdasarkan headcount shelter resmi.",
                        "Laporkan kekurangan bahan masak 3 jam sebelum masak."
                    )
                    SkillsVolunteer.PSIKOSOSIAL -> listOf(
                        "Gunakan pendekatan trauma healing berbasis bermain.",
                        "Rujuk pengungsi dengan tanda depresi berat ke dokter.",
                        "Jaga privasi warga yang melakukan konseling harian."
                    )
                    SkillsVolunteer.PENDIDIKAN -> listOf(
                        "Kurikulum darurat difokuskan pada belajar yang riang.",
                        "Pilah anak didik sesuai jenjang usianya.",
                        "Ajukan request edu-kits tambahan ke pos logistik."
                    )
                    else -> listOf(
                        "Laporkan kondisi darurat secara akurat.",
                        "Bagikan lokasi GPS Anda untuk mempermudah dispatch.",
                        "Gunakan koordinasi langsung via radio/aplikasi."
                    )
                }

                tips.forEach { tip ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 5.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = tip,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }

    if (showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { showIncompleteDialog = false },
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
                    text = "Data Belum Lengkap",
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Text(
                    text = "Harap isi semua kolom wajib penugasan Anda sebelum mengirim laporan.",
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = { showIncompleteDialog = false },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Mengerti", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
        )
    }
}

@Composable
fun SigmaTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
fun SigmaNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = { input ->
            if (input.all { it.isDigit() }) onValueChange(input)
        },
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
            keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
        ),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}

@Composable
fun SigmaTextAreaField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        placeholder = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(8.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent
        )
    )
}
