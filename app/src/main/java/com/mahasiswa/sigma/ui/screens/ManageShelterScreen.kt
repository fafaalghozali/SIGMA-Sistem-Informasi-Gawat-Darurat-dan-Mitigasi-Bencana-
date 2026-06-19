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
                                onDelete = { pendingDelete = shelter }
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
            onSubmit = { name, address, lat, lng, capMax, capCurrent, status, logistics, contactPhone, photoUrl ->
                val current = editing
                if (current?.id != null) {
                    viewModel.updateShelter(
                        current.id.toString(),
                        UpdateShelterRequest(
                            name = name,
                            address = address,
                            capacityCurrent = capCurrent,
                            capacityMax = capMax,
                            status = status,
                            logistics = logistics,
                            contactPhone = contactPhone.ifBlank { null },
                            photoUrl = photoUrl.ifBlank { null }
                        )
                    )
                } else {
                    viewModel.createShelter(
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
                            photoUrl = photoUrl.ifBlank { null }
                        )
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
}

@Composable
private fun ManageShelterCard(
    shelter: ShelterDto,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(shelter.name, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        shelterStatusLabel(shelter.status),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(shelter.address, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("Kapasitas: ${shelter.capacityCurrent}/${shelter.capacityMax}", fontSize = 13.sp)
            if (!shelter.logistics.isNullOrEmpty()) {
                Text("Logistik: ${shelter.logistics.joinToString(", ")}", fontSize = 13.sp)
            }
            if (!shelter.contactPhone.isNullOrBlank()) {
                Text("Kontak: ${shelter.contactPhone}", fontSize = 13.sp)
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onEdit, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Edit")
                }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Hapus")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShelterEditorDialog(
    initial: ShelterDto?,
    onDismiss: () -> Unit,
    onSubmit: (name: String, address: String, lat: Double, lng: Double, capMax: Int, capCurrent: Int, status: String, logistics: List<String>, contactPhone: String, photoUrl: String) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var address by remember { mutableStateOf(initial?.address ?: "") }
    var latitude by remember { mutableStateOf(initial?.latitude?.toString() ?: "") }
    var longitude by remember { mutableStateOf(initial?.longitude?.toString() ?: "") }
    var capacityMax by remember { mutableStateOf(initial?.capacityMax?.toString() ?: "") }
    var capacityCurrent by remember { mutableStateOf(initial?.capacityCurrent?.toString() ?: "0") }
    var status by remember { mutableStateOf(initial?.status ?: "active") }
    var logisticsText by remember { mutableStateOf(initial?.logistics?.joinToString(", ") ?: "") }
    var contactPhone by remember { mutableStateOf(initial?.contactPhone ?: "") }
    var photoUrl by remember { mutableStateOf(initial?.photoUrl ?: "") }

    val isEdit = initial?.id != null
    val nameValid = name.isNotBlank()
    val addressValid = address.isNotBlank()
    val capMaxValid = capacityMax.toIntOrNull() != null && (capacityMax.toIntOrNull() ?: 0) > 0
    val latValid = isEdit || latitude.toDoubleOrNull() != null
    val lngValid = isEdit || longitude.toDoubleOrNull() != null
    val formValid = nameValid && addressValid && capMaxValid && latValid && lngValid

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Posko" else "Tambah Posko", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Nama Posko") }, singleLine = true,
                    isError = !nameValid, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = address, onValueChange = { address = it },
                    label = { Text("Alamat") }, isError = !addressValid,
                    modifier = Modifier.fillMaxWidth()
                )
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
                OutlinedTextField(
                    value = status, onValueChange = { status = it },
                    label = { Text("Status (active/full/closed)") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = logisticsText, onValueChange = { logisticsText = it },
                    label = { Text("Logistik (pisah dengan koma)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contactPhone, onValueChange = { contactPhone = it },
                    label = { Text("Nomor Kontak") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = photoUrl, onValueChange = { photoUrl = it },
                    label = { Text("URL Foto") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = formValid,
                onClick = {
                    val logisticsList = logisticsText.split(",")
                        .map { it.trim() }
                        .filter { it.isNotBlank() }
                    onSubmit(
                        name.trim(),
                        address.trim(),
                        latitude.toDoubleOrNull() ?: (initial?.latitude ?: 0.0),
                        longitude.toDoubleOrNull() ?: (initial?.longitude ?: 0.0),
                        capacityMax.toIntOrNull() ?: 0,
                        capacityCurrent.toIntOrNull() ?: 0,
                        status.trim().ifBlank { "active" },
                        logisticsList,
                        contactPhone.trim(),
                        photoUrl.trim()
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
