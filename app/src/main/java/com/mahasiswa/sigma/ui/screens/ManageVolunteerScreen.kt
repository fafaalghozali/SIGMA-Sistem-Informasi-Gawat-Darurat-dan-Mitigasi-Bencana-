package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.ui.viewmodel.ManageVolunteerViewModel
import kotlinx.coroutines.launch

// ── Warna per status ──────────────────────────────────────────────────────────
private fun volunteerStatusColor(status: String): Color = when (status.uppercase()) {
    "APPROVED"  -> Color(0xFF16A34A)
    "REJECTED"  -> Color(0xFFDC2626)
    "PENDING"   -> Color(0xFFCA8A04)
    else        -> Color(0xFF6B7280)
}
private fun volunteerStatusLabel(status: String): String = when (status.uppercase()) {
    "APPROVED" -> "Disetujui"
    "REJECTED" -> "Ditolak"
    "PENDING"  -> "Menunggu"
    else       -> status
}

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageVolunteerScreen(
    onBack: () -> Unit,
    viewModel: ManageVolunteerViewModel = hiltViewModel()
) {
    val registrations by viewModel.registrations.collectAsState()
    val disasters     by viewModel.disasters.collectAsState()
    val isLoading     by viewModel.isLoading.collectAsState()
    val assignResult  by viewModel.assignResult.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State dialog assign
    var assignTarget by remember { mutableStateOf<VolunteerDto?>(null) }

    val pendingList  = registrations.filter { it.status.equals("PENDING",  ignoreCase = true) || it.status.isBlank() }
    val approvedList = registrations.filter { it.status.equals("APPROVED", ignoreCase = true) }
    val rejectedList = registrations.filter { it.status.equals("REJECTED", ignoreCase = true) }

    // Tampilkan snackbar saat assign selesai
    LaunchedEffect(assignResult) {
        assignResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAssignResult()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Kelola Relawan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadRegistrations() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Tab Row ───────────────────────────────────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            ) {
                listOf(
                    Triple("Tertunda",  pendingList.size,  MaterialTheme.colorScheme.error),
                    Triple("Disetujui", approvedList.size, MaterialTheme.colorScheme.primary),
                    Triple("Ditolak",   rejectedList.size, MaterialTheme.colorScheme.outline)
                ).forEachIndexed { i, (label, count, badgeColor) ->
                    Tab(
                        selected = selectedTab == i,
                        onClick = { selectedTab = i },
                        text = {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(label, fontWeight = FontWeight.Bold)
                                if (count > 0) {
                                    Spacer(Modifier.width(6.dp))
                                    Badge(containerColor = badgeColor) {
                                        Text(count.toString(), color = Color.White)
                                    }
                                }
                            }
                        }
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            val currentList = when (selectedTab) { 0 -> pendingList; 1 -> approvedList; else -> rejectedList }

            if (currentList.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.People, null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            when (selectedTab) {
                                0 -> "Tidak ada pendaftaran tertunda"
                                1 -> "Belum ada relawan yang disetujui"
                                else -> "Tidak ada yang ditolak"
                            },
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(currentList, key = { it.id?.toString() ?: it.name }) { volunteer ->
                        VolunteerCardItem(
                            volunteer = volunteer,
                            showAssignButton = selectedTab == 1,  // tab Disetujui
                            onApprove = {
                                val id = volunteer.id?.toString() ?: return@VolunteerCardItem
                                viewModel.approveVolunteer(id)
                                scope.launch { snackbarHostState.showSnackbar("${volunteer.name} disetujui.") }
                            },
                            onReject = {
                                val id = volunteer.id?.toString() ?: return@VolunteerCardItem
                                viewModel.rejectVolunteer(id)
                                scope.launch { snackbarHostState.showSnackbar("${volunteer.name} ditolak.") }
                            },
                            onAssign = { assignTarget = volunteer }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Assign ─────────────────────────────────────────────────────────
    assignTarget?.let { volunteer ->
        AssignVolunteerDialog(
            volunteer = volunteer,
            disasters = disasters,
            onDismiss = { assignTarget = null },
            onConfirm = { disasterId, location ->
                val id = volunteer.id?.toString() ?: return@AssignVolunteerDialog
                viewModel.assignVolunteer(id, disasterId, location)
                assignTarget = null
            }
        )
    }
}

// ── Dialog Assign ke Bencana ──────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignVolunteerDialog(
    volunteer: VolunteerDto,
    disasters: List<DisasterReportDto>,
    onDismiss: () -> Unit,
    onConfirm: (disasterId: Long, location: String) -> Unit
) {
    var selectedDisaster by remember { mutableStateOf<DisasterReportDto?>(null) }
    var assignmentLocation by remember { mutableStateOf("") }
    var disasterExpanded by remember { mutableStateOf(false) }
    var locationError by remember { mutableStateOf(false) }
    var disasterError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier.size(36.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AssignmentInd, null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text("Tugaskan Relawan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(volunteer.name, fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info relawan ringkas
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Keahlian: ${volunteer.skill}", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Spacer(Modifier.width(16.dp))
                        Icon(Icons.Default.LocationOn, null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(volunteer.address.take(20) + if (volunteer.address.length > 20) "…" else "",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Pilih bencana
                Text("Pilih Bencana *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(
                    expanded = disasterExpanded,
                    onExpandedChange = { disasterExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedDisaster?.let { "[${it.id}] ${it.title}" } ?: "Pilih bencana...",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = disasterExpanded) },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        isError = disasterError,
                        supportingText = if (disasterError) {{ Text("Wajib dipilih") }} else null
                    )
                    ExposedDropdownMenu(
                        expanded = disasterExpanded,
                        onDismissRequest = { disasterExpanded = false }
                    ) {
                        if (disasters.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Tidak ada data bencana", color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                onClick = {}
                            )
                        } else {
                            disasters.forEach { disaster ->
                                DropdownMenuItem(
                                    text = {
                                        Column {
                                            Text("[${disaster.id}] ${disaster.title}",
                                                fontWeight = FontWeight.Medium, maxLines = 1,
                                                overflow = TextOverflow.Ellipsis)
                                            Text(disaster.location, fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    },
                                    onClick = {
                                        selectedDisaster = disaster
                                        // Auto-fill lokasi dari bencana
                                        if (assignmentLocation.isBlank()) {
                                            assignmentLocation = disaster.location
                                        }
                                        disasterExpanded = false
                                        disasterError = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Lokasi penugasan / posko
                Text("Lokasi Penugasan / Posko *", fontSize = 12.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = assignmentLocation,
                    onValueChange = {
                        assignmentLocation = it
                        locationError = false
                    },
                    placeholder = { Text("Contoh: Stadion UNS, GOR Manahan") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    isError = locationError,
                    supportingText = if (locationError) {{ Text("Lokasi tidak boleh kosong") }} else null,
                    singleLine = true
                )

                // Catatan
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFFCD34D))
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Icon(Icons.Default.Info, null,
                            tint = Color(0xFFD97706), modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Setelah ditugaskan, relawan akan mendapat notifikasi dan diminta konfirmasi kesediaan.",
                            fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 16.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var valid = true
                    if (selectedDisaster == null) { disasterError = true; valid = false }
                    if (assignmentLocation.isBlank()) { locationError = true; valid = false }
                    if (valid) {
                        onConfirm(selectedDisaster!!.id!!.toLong(), assignmentLocation.trim())
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) {
                Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Tugaskan Sekarang", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth().height(46.dp)
            ) { Text("Batal") }
        }
    )
}

// ── Card item relawan ─────────────────────────────────────────────────────────
@Composable
fun VolunteerCardItem(
    volunteer: VolunteerDto,
    showAssignButton: Boolean = false,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAssign: () -> Unit = {}
) {
    val skillColor = when (volunteer.skill.uppercase()) {
        "MEDIS"       -> Color(0xFF2196F3)
        "SAR"         -> Color(0xFFFF5722)
        "LOGISTIK"    -> Color(0xFF4CAF50)
        "PSIKOSOSIAL" -> Color(0xFF9C27B0)
        "KONSUMSI"    -> Color(0xFFFFC107)
        "PENDIDIKAN"  -> Color(0xFF009688)
        else          -> Color(0xFF757575)
    }
    val statusColor = volunteerStatusColor(volunteer.status)

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {

            // ── Header ──────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(skillColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        volunteer.name.firstOrNull()?.uppercase() ?: "R",
                        color = skillColor, fontWeight = FontWeight.Bold, fontSize = 20.sp
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(volunteer.name, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                        maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        "Terdaftar sebagai relawan",
                        fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Badge skill
                Surface(
                    shape = RoundedCornerShape(32.dp),
                    color = skillColor.copy(alpha = 0.12f)
                ) {
                    Text(volunteer.skill, modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = skillColor)
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Info ─────────────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Phone, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(volunteer.phoneNumber.ifBlank { "-" }, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(6.dp))
                Text(volunteer.address.ifBlank { "-" }, fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
            }

            // ── Badge status ─────────────────────────────────────────────
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.12f)
                ) {
                    Text(
                        volunteerStatusLabel(volunteer.status),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        fontSize = 11.sp, fontWeight = FontWeight.Bold, color = statusColor
                    )
                }
                // Badge assignment_status jika sudah ditugaskan
                volunteer.assignmentStatus?.let { aStatus ->
                    val aColor = when (aStatus.lowercase()) {
                        "accepted" -> Color(0xFF16A34A)
                        "rejected" -> Color(0xFFDC2626)
                        else       -> Color(0xFFCA8A04)
                    }
                    val aLabel = when (aStatus.lowercase()) {
                        "accepted" -> "Konfirmasi: Bersedia"
                        "rejected" -> "Konfirmasi: Tidak Bersedia"
                        else       -> "Menunggu Konfirmasi"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = aColor.copy(alpha = 0.1f)
                    ) {
                        Text(aLabel, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium, color = aColor)
                    }
                }
            }

            // ── Info penugasan jika sudah ada ────────────────────────────
            if (!volunteer.assignment.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                ) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, null,
                            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Ditugaskan ke: ${volunteer.assignment}",
                            fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(Modifier.height(12.dp))

            // ── Tombol aksi ──────────────────────────────────────────────
            when {
                // Tab PENDING: Setujui + Tolak
                volunteer.status.equals("PENDING", ignoreCase = true) || volunteer.status.isBlank() -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onApprove,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Setujui", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onReject,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFDC2626)),
                            border = BorderStroke(1.dp, Color(0xFFDC2626).copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f).height(44.dp)
                        ) {
                            Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tolak", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                // Tab DECLINED: Setujui ulang
                volunteer.status.equals("REJECTED", ignoreCase = true) -> {
                    Button(
                        onClick = onApprove,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Setujui Ulang", fontWeight = FontWeight.Bold)
                    }
                }
                // Tab APPROVED: Tugaskan ke bencana (jika belum punya assignment)
                showAssignButton -> {
                    if (volunteer.assignment.isNullOrBlank()) {
                        Button(
                            onClick = onAssign,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.fillMaxWidth().height(44.dp)
                        ) {
                            Icon(Icons.Default.AssignmentInd, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tugaskan ke Bencana", fontWeight = FontWeight.Bold)
                        }
                    } else {
                        // Sudah ditugaskan — tampilkan tombol ubah penugasan
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(
                                onClick = onAssign,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(15.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Ubah Tugas", fontWeight = FontWeight.Medium)
                            }
                            OutlinedButton(
                                onClick = onReject,
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error),
                                border = BorderStroke(1.dp,
                                    MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f).height(44.dp)
                            ) {
                                Text("Batalkan", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
        }
    }
}
