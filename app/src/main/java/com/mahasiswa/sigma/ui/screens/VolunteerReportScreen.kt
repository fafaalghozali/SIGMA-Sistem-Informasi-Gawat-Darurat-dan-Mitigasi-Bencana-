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
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.VolunteerReportDto
import com.mahasiswa.sigma.ui.viewmodel.UiState
import com.mahasiswa.sigma.ui.viewmodel.VolunteerReportViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerReportScreen(
    userRole: UserRole,
    onBack: () -> Unit,
    viewModel: VolunteerReportViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VolunteerReportDto?>(null) }
    var pendingDelete by remember { mutableStateOf<VolunteerReportDto?>(null) }

    LaunchedEffect(Unit) { viewModel.load(userRole) }
    LaunchedEffect(operationMessage) {
        operationMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearOperationMessage()
        }
    }

    val isBnpb = userRole == UserRole.BNPB

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isBnpb) "Laporan Tugas Relawan" else "Riwayat Laporan Tugas", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.load(userRole) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                    }
                }
            )
        },
        floatingActionButton = {
            if (!isBnpb) {
                FloatingActionButton(
                    onClick = { editing = null; showEditor = true },
                    modifier = Modifier.padding(bottom = 80.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Buat Laporan")
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

                is UiState.Error -> VolunteerReportMessage(
                    title = "Gagal memuat laporan",
                    message = state.message,
                    onRetry = { viewModel.load(userRole) }
                )

                is UiState.Empty -> VolunteerReportMessage(
                    title = "Belum ada laporan",
                    message = if (isBnpb) "Belum ada laporan tugas yang dikirim relawan."
                    else "Tekan tombol + untuk membuat laporan tugas.",
                    onRetry = { viewModel.load(userRole) }
                )

                is UiState.Success -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 160.dp)
                    ) {
                        items(state.data, key = { it.id ?: it.hashCode().toString() }) { report ->
                            VolunteerReportCard(
                                report = report,
                                canModify = !isBnpb,
                                onEdit = { editing = report; showEditor = true },
                                onDelete = { pendingDelete = report }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showEditor) {
        VolunteerReportEditorDialog(
            initial = editing,
            onDismiss = { showEditor = false },
            onSubmit = { reportData, notes ->
                val current = editing
                if (current?.id != null) {
                    viewModel.updateReport(userRole, current.id, reportData, notes)
                } else {
                    viewModel.createReport(userRole, current?.skillType, reportData, notes)
                }
                showEditor = false
            }
        )
    }

    pendingDelete?.let { report ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Hapus Laporan", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus laporan ini?") },
            confirmButton = {
                Button(
                    onClick = {
                        report.id?.let { viewModel.deleteReport(userRole, it) }
                        pendingDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Hapus") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Batal") } }
        )
    }
}

@Composable
private fun VolunteerReportCard(
    report: VolunteerReportDto,
    canModify: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        report.skillType ?: "UMUM",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            Text(
                report.reportData ?: "(Tidak ada data laporan)",
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            if (!report.notes.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    "Catatan: ${report.notes}",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (canModify) {
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerReportEditorDialog(
    initial: VolunteerReportDto?,
    onDismiss: () -> Unit,
    onSubmit: (reportData: String, notes: String) -> Unit
) {
    var reportData by remember { mutableStateOf(initial?.reportData ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    val isEdit = initial?.id != null
    val valid = reportData.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEdit) "Edit Laporan" else "Buat Laporan Tugas", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = reportData, onValueChange = { reportData = it },
                    label = { Text("Data Laporan") }, minLines = 3, isError = !valid,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Catatan Tambahan") }, minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = { onSubmit(reportData.trim(), notes.trim()) }) {
                Text(if (isEdit) "Simpan" else "Kirim")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}

@Composable
private fun VolunteerReportMessage(
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
                Icons.Default.Assignment,
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
