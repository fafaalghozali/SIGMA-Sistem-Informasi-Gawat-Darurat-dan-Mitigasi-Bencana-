package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.CreateShelterRequest
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.data.model.UpdateShelterRequest
import com.mahasiswa.sigma.ui.viewmodel.ShelterViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState
import kotlinx.coroutines.launch

// New imports for visual styling matching ShelterInfoScreen
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import coil.compose.AsyncImage
import coil.request.ImageRequest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageShelterScreen(
    onBack: () -> Unit,
    viewModel: ShelterViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<ShelterDto?>(null) }
    var pendingDelete by remember { mutableStateOf<ShelterDto?>(null) }
    var dialogShelter by remember { mutableStateOf<ShelterDto?>(null) }

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
                title = { Text("Kelola Posko Pengungsian", fontWeight = FontWeight.Bold) },
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
            FloatingActionButton(
                onClick = { editing = null; showEditor = true },
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Posko")
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

                is UiState.Error -> {
                    ManageShelterMessage(
                        title = "Gagal memuat data",
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is UiState.Empty -> {
                    ManageShelterMessage(
                        title = "Belum ada posko",
                        message = "Tekan tombol + untuk menambahkan posko pengungsian baru.",
                        onRetry = { viewModel.refresh() }
                    )
                }

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(state.data, key = { it.id ?: it.name }) { shelter ->
                            ManageShelterCard(
                                shelter = shelter,
                                onEdit = { editing = shelter; showEditor = true },
                                onDelete = { pendingDelete = shelter },
                                onShowLogistics = { dialogShelter = shelter }
                            )
                        }
                    }
                }
            }

            if (isProcessing) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }

    if (showEditor) {
        ShelterEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSubmit = { name, address, lat, lng, capMax, capCurrent, status, logistics, contactPhone, photoBitmap ->
                val current = editing
                if (current?.id != null) {
                    viewModel.updateShelterWithPhoto(
                        current.id.toString(),
                        UpdateShelterRequest(
                            name = name,
                            address = address,
                            capacityCurrent = capCurrent,
                            capacityMax = capMax,
                            status = status,
                            logistics = logistics,
                            contactPhone = contactPhone.ifBlank { null },
                            photoUrl = null
                        ),
                        photoBitmap
                    )
                } else {
                    viewModel.createShelterWithPhoto(
                        CreateShelterRequest(
                            name = name,
                            address = address,
                            latitude = lat,
                            longitude = lng,
                            capacityMax = capMax,
                            capacityCurrent = capCurrent,
                            status = status,
                            logistics = logistics,
                            contactPhone = contactPhone.ifBlank { null },
                            photoUrl = null
                        ),
                        photoBitmap
                    )
                }
                showEditor = false
            }
        )
    }

    pendingDelete?.let { shelter ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus Posko", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus posko \"${shelter.name}\"? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        shelter.id?.let { viewModel.deleteShelter(it.toString()) }
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Batal") }
            }
        )
    }

    // ── Logistics Dialog ────────────────────────────────────────────────
    dialogShelter?.let { shelter ->
        LogisticsDialog(
            shelter = shelter,
            onDismiss = { dialogShelter = null }
        )
    }
}

private fun shelterStatusColor(label: String): Color = when (label) {
    "Tersedia" -> Color(0xFF22C55E)
    "Penuh"    -> Color(0xFFEF4444)
    "Tutup"    -> Color(0xFF64748B)
    else       -> Color(0xFF94A3B8)
}

@Composable
private fun LogisticsChip(item: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Inventory2,
                contentDescription = null,
                modifier = Modifier.size(12.dp),
                tint = MaterialTheme.colorScheme.secondary
            )
            Text(
                item,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogisticsDialog(
    shelter: ShelterDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val hasLogistics = !shelter.logistics.isNullOrEmpty()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(enabled = false) {}
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Inventory2,
                            contentDescription = null,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(20.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            "Kebutuhan Logistik",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            shelter.name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                if (!hasLogistics) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(14.dp)
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF22C55E),
                            modifier = Modifier.size(22.dp)
                        )
                        Text(
                            "Tidak ada kebutuhan logistik mendesak untuk posko ini.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                } else {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Default.VolunteerActivism,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                "Bantuan Anda sangat berarti. Silakan kirimkan item berikut ke posko:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shelter.logistics!!.forEach { item ->
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Inventory2,
                                        contentDescription = null,
                                        modifier = Modifier.padding(6.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                                Text(
                                    item,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (hasLogistics) {
                    Button(
                        onClick = {
                            val items = shelter.logistics!!.joinToString(", ")
                            val phone = shelter.contactPhone ?: "6285934415914"
                            val cleanPhone = phone.replace(Regex("[^0-9+]"), "").let {
                                if (it.startsWith("0")) "62${it.drop(1)}" else it
                            }
                            val msg = "Halo, saya ingin mengirimkan bantuan logistik ke ${shelter.name} berupa: $items"
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(msg)}"
                            context.startActivity(Intent(Intent.ACTION_VIEW).apply { data = Uri.parse(url) })
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Chat, contentDescription = null, tint = Color.White)
                        Spacer(Modifier.width(8.dp))
                        Text("Hubungi via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Tutup", fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = null
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ManageShelterCard(
    shelter: ShelterDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onShowLogistics: () -> Unit
) {
    val statusLabel = shelterStatusLabel(shelter.status)
    val statusColor = shelterStatusColor(statusLabel)
    val occupancy   = if (shelter.capacityMax > 0)
        (shelter.capacityCurrent.toFloat() / shelter.capacityMax.toFloat()).coerceIn(0f, 1f)
    else 0f
    val animOccupancy by animateFloatAsState(
        targetValue = occupancy,
        animationSpec = tween(800),
        label = "occupancy"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column {
            // ── Photo ──────────────────────────────────────────────────────
            if (!shelter.photoUrl.isNullOrBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(shelter.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Foto ${shelter.name}",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                                    startY = 80f
                                )
                            )
                    )
                    Surface(
                        color = statusColor,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(10.dp)
                    ) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                // ── Header row ─────────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Text(
                        shelter.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (shelter.photoUrl.isNullOrBlank()) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = statusColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
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
                }

                Spacer(Modifier.height(10.dp))

                // ── Address ────────────────────────────────────────────────
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp).padding(top = 1.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        shelter.address,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // ── Contact ────────────────────────────────────────────────
                if (!shelter.contactPhone.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            shelter.contactPhone,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ── Capacity progress ──────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.People,
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Kapasitas", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        "${shelter.capacityCurrent} / ${shelter.capacityMax} orang",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (occupancy >= 1f) Color(0xFFEF4444)
                               else if (occupancy >= 0.8f) Color(0xFFF59E0B)
                               else MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = animOccupancy,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(50)),
                    color = when {
                        occupancy >= 1f   -> Color(0xFFEF4444)
                        occupancy >= 0.8f -> Color(0xFFF59E0B)
                        else              -> Color(0xFF22C55E)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    strokeCap = StrokeCap.Round
                )

                // ── Logistics preview ──────────────────────────────────────
                if (!shelter.logistics.isNullOrEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Kebutuhan",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(
                            onClick = onShowLogistics,
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Lihat semua", fontSize = 11.sp)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        maxItemsInEachRow = 4
                    ) {
                        shelter.logistics.take(4).forEach { item ->
                            LogisticsChip(item = item)
                        }
                        if (shelter.logistics.size > 4) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    "+${shelter.logistics.size - 4}",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                Spacer(Modifier.height(12.dp))

                // ── Action buttons ─────────────────────────────────────────
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Edit", fontSize = 13.sp)
                    }

                    Button(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Hapus", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// Status options – label shown to user and raw value stored to DB
private data class StatusOption(val value: String, val label: String)
private val shelterStatusOptions = listOf(
    StatusOption("active", "Tersedia"),
    StatusOption("full",   "Penuh"),
    StatusOption("closed", "Tutup")
)

// Suggested logistics chips – admin can pick or type freely
private val logisticsSuggestions = listOf(
    "Makanan", "Air Minum", "Obat-obatan", "Pakaian", "Selimut",
    "Tenda", "Peralatan Dapur", "Sanitasi", "Susu Bayi", "P3K"
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShelterEditorDialog(
    initial: ShelterDto?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String, lat: Double, lng: Double, capMax: Int, capCurrent: Int, status: String, logistics: List<String>, contactPhone: String, photoBitmap: android.graphics.Bitmap?) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var capacityMax by remember { mutableStateOf(initial?.capacityMax?.toString() ?: "") }
    var capacityCurrent by remember { mutableStateOf(initial?.capacityCurrent?.toString() ?: "0") }

    // --- Status dropdown ---
    val initialStatus = shelterStatusOptions.find { it.value == initial?.status } ?: shelterStatusOptions[0]
    var selectedStatus by remember { mutableStateOf(initialStatus) }
    var statusExpanded by remember { mutableStateOf(false) }

    // --- Logistics: track as mutable set of selected items + free-text ---
    val initialLogistics = initial?.logistics?.toMutableSet() ?: mutableSetOf()
    val selectedLogistics = remember { mutableStateListOf<String>().apply { addAll(initialLogistics) } }
    var logisticsCustomText by remember { mutableStateOf("") }

    var contactPhone by remember { mutableStateOf(initial?.contactPhone ?: "") }

    // --- Photo picker state ---
    var photoBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var showImagePicker by remember { mutableStateOf(false) }
    val imagePickerSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val isEdit = initial?.id != null
    val nameValid = name.isNotBlank()
    val addressValid = address.isNotBlank()
    val capMaxValid = capacityMax.toIntOrNull() != null && (capacityMax.toIntOrNull() ?: 0) > 0
    val latValid = isEdit || latitude.toDoubleOrNull() != null
    val lngValid = isEdit || longitude.toDoubleOrNull() != null
    val formValid = nameValid && addressValid && capMaxValid && latValid && lngValid

    if (showImagePicker) {
        ImagePickerBottomSheet(
            sheetState = imagePickerSheetState,
            onDismiss = { showImagePicker = false },
            onImageSelected = { bitmap ->
                photoBitmap = bitmap
                showImagePicker = false
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Posko" else "Tambah Posko", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Nama
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nama Posko") }, singleLine = true,
                    isError = !nameValid, modifier = Modifier.fillMaxWidth()
                )
                // Alamat
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Alamat") }, isError = !addressValid,
                    modifier = Modifier.fillMaxWidth()
                )
                // Lat / Lng
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = latitude, onValueChange = { latitude = it },
                        label = { Text("Latitude") }, singleLine = true,
                        enabled = !isEdit, isError = !latValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = longitude, onValueChange = { longitude = it },
                        label = { Text("Longitude") }, singleLine = true,
                        enabled = !isEdit, isError = !lngValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.weight(1f)
                    )
                }
                // Kapasitas
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = capacityCurrent, onValueChange = { capacityCurrent = it },
                        label = { Text("Terisi") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = capacityMax, onValueChange = { capacityMax = it },
                        label = { Text("Kapasitas Maks") }, singleLine = true,
                        isError = !capMaxValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                // --- Status dropdown (no more hardcoded free-text) ---
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedStatus.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Status Posko") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        shelterStatusOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    selectedStatus = option
                                    statusExpanded = false
                                },
                                leadingIcon = {
                                    if (selectedStatus == option) {
                                        Icon(
                                            imageVector = androidx.compose.material.icons.Icons.Default.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            )
                        }
                    }
                }

                // --- Logistics chips (suggestion + custom) ---
                Text(
                    "Kebutuhan Logistik",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    logisticsSuggestions.forEach { item ->
                        val selected = selectedLogistics.contains(item)
                        FilterChip(
                            selected = selected,
                            onClick = {
                                if (selected) selectedLogistics.remove(item)
                                else selectedLogistics.add(item)
                            },
                            label = { Text(item, fontSize = 12.sp) }
                        )
                    }
                }
                // Custom logistics (tambahan selain suggestion)
                val customItems = selectedLogistics.filter { it !in logisticsSuggestions }
                if (customItems.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        customItems.forEach { item ->
                            InputChip(
                                selected = true,
                                onClick = { selectedLogistics.remove(item) },
                                label = { Text(item, fontSize = 12.sp) },
                                trailingIcon = {
                                    Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Close,
                                        contentDescription = "Hapus",
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            )
                        }
                    }
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = logisticsCustomText,
                        onValueChange = { logisticsCustomText = it },
                        label = { Text("Tambah lainnya…") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            val trimmed = logisticsCustomText.trim()
                            if (trimmed.isNotBlank() && !selectedLogistics.contains(trimmed)) {
                                selectedLogistics.add(trimmed)
                            }
                            logisticsCustomText = ""
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Tambah")
                    }
                }

                // Kontak & Foto
                OutlinedTextField(
                    value = contactPhone, onValueChange = { contactPhone = it },
                    label = { Text("Nomor Kontak") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )

                // Photo picker
                Text(
                    "Foto Posko",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (photoBitmap != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showImagePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = photoBitmap,
                            contentDescription = "Foto posko",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    TextButton(onClick = { photoBitmap = null }) {
                        Text("Hapus Foto", color = MaterialTheme.colorScheme.error)
                    }
                } else if (!initial?.photoUrl.isNullOrBlank() && photoBitmap == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { showImagePicker = true },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(initial?.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Foto posko saat ini",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Text(
                        "Ketuk untuk mengganti foto",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    OutlinedButton(
                        onClick = { showImagePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Pilih Foto")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = formValid,
                onClick = {
                    onSubmit(
                        name.trim(),
                        address.trim(),
                        latitude.toDoubleOrNull() ?: (initial?.latitude ?: 0.0),
                        longitude.toDoubleOrNull() ?: (initial?.longitude ?: 0.0),
                        capacityMax.toIntOrNull() ?: 0,
                        capacityCurrent.toIntOrNull() ?: 0,
                        selectedStatus.value,
                        selectedLogistics.toList(),
                        contactPhone.trim(),
                        photoBitmap
                    )
                }
            ) { Text(if (isEdit) "Simpan" else "Tambah") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun ManageShelterMessage(
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
            Icon(
                Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
            )
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
