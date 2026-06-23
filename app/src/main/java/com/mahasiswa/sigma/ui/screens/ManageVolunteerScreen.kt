package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.window.Dialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.VolunteerDto
import com.mahasiswa.sigma.ui.viewmodel.ManageVolunteerViewModel
import kotlinx.coroutines.launch

// ── Warna per status ──────────────────────────────────────────────────────────
private fun volunteerStatusColor(status: String): Color = when (status.uppercase()) {
    "APPROVED"  -> Color(0xFF16A34A)
    "DECLINED", "REJECTED" -> Color(0xFFDC2626)
    "PENDING"   -> Color(0xFFCA8A04)
    else        -> Color(0xFF6B7280)
}

private fun volunteerStatusLabel(status: String): String = when (status.uppercase()) {
    "APPROVED" -> "Approved"
    "DECLINED", "REJECTED" -> "Rejected"
    "PENDING"  -> "Pending"
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

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // State dialog detail dan assign
    var selectedVolunteer by remember { mutableStateOf<VolunteerDto?>(null) }
    var assignTarget by remember { mutableStateOf<VolunteerDto?>(null) }
    var volunteerToDelete by remember { mutableStateOf<VolunteerDto?>(null) }

    // Search & Filter State
    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf("ALL") }

    // Hitung statistik (relawan ditolak/rejected/declined tidak dimasukkan ke daftar aktif maupun hitungan statistik)
    val activeRegistrations = remember(registrations) {
        registrations.filter { it.status.uppercase() != "REJECTED" && it.status.uppercase() != "DECLINED" }
    }
    val totalCount = activeRegistrations.size
    val pendingCount = activeRegistrations.count { it.status.uppercase() == "PENDING" || it.status.isBlank() }
    val approvedCount = activeRegistrations.count { it.status.uppercase() == "APPROVED" || it.status.uppercase() == "ACCEPTED" }
    val unassignedCount = activeRegistrations.count { (it.status.uppercase() == "APPROVED" || it.status.uppercase() == "ACCEPTED") && it.assignment.isNullOrBlank() }
    val rejectedCount = remember(registrations) {
        registrations.count { it.status.uppercase() == "REJECTED" || it.status.uppercase() == "DECLINED" }
    }

    // Tampilkan snackbar saat assign selesai
    LaunchedEffect(assignResult) {
        assignResult?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearAssignResult()
        }
    }

    // Filter daftar relawan berdasarkan statistik card yang aktif
    val filteredList = when (activeFilter) {
        "PENDING" -> activeRegistrations.filter { it.status.uppercase() == "PENDING" || it.status.isBlank() }
        "APPROVED" -> activeRegistrations.filter { it.status.uppercase() == "APPROVED" || it.status.uppercase() == "ACCEPTED" }
        "UNASSIGNED" -> activeRegistrations.filter { (it.status.uppercase() == "APPROVED" || it.status.uppercase() == "ACCEPTED") && it.assignment.isNullOrBlank() }
        "REJECTED" -> registrations.filter { it.status.uppercase() == "REJECTED" || it.status.uppercase() == "DECLINED" }
        else -> activeRegistrations
    }

    // Filter berdasarkan kolom pencarian
    val displayList = filteredList.filter {
        it.name.contains(searchQuery, ignoreCase = true)
    }

    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) Color(0xFF0F172A) else Color(0xFFF5F7FA)

    Scaffold(
        snackbarHost = { 
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.padding(bottom = 100.dp)
            )
        },
        containerColor = backgroundColor
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Custom Header Row ──────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Daftar Relawan",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Kelola pendaftaran dan data relawan.",
                            fontSize = 13.sp,
                            color = if (isDark) Color.Gray else Color(0xFF64748B)
                        )
                    }
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = if (isDark) Color.White else Color(0xFF475569)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Kembali", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }

            // ── Statistik Cards Row (2x2 Grid for Mobile) ──────────────────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "TOTAL RELAWAN",
                            value = totalCount.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF3B82F6),
                            bgColor = Color(0xFFEFF6FF),
                            selected = activeFilter == "ALL",
                            onClick = { activeFilter = "ALL" },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "VERIFIKASI PENDING",
                            value = pendingCount.toString(),
                            icon = Icons.Default.Info,
                            color = Color(0xFFF59E0B),
                            bgColor = Color(0xFFFFFBEB),
                            selected = activeFilter == "PENDING",
                            onClick = { activeFilter = "PENDING" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            label = "DISETUJUI",
                            value = approvedCount.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF10B981),
                            bgColor = Color(0xFFD1FAE5),
                            selected = activeFilter == "APPROVED",
                            onClick = { activeFilter = "APPROVED" },
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            label = "BELUM TUGAS",
                            value = unassignedCount.toString(),
                            icon = Icons.Default.PersonAdd,
                            color = Color(0xFF8B5CF6),
                            bgColor = Color(0xFFEDE9FE),
                            selected = activeFilter == "UNASSIGNED",
                            onClick = { activeFilter = "UNASSIGNED" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (activeFilter == "REJECTED") "Daftar Relawan Ditolak" else "Registrasi Relawan",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (activeFilter == "REJECTED") Color(0xFFDC2626) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    TextButton(
                        onClick = {
                            activeFilter = if (activeFilter == "REJECTED") "ALL" else "REJECTED"
                        },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (activeFilter == "REJECTED") "Lihat Aktif" else "Lihat Ditolak (${rejectedCount})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (activeFilter == "REJECTED") MaterialTheme.colorScheme.primary else Color(0xFFDC2626)
                        )
                    }
                }
            }

            // ── Search & Header Section (Stacked vertically for Mobile) ────
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Cari nama relawan...", fontSize = 14.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, modifier = Modifier.size(18.dp), tint = Color.Gray) },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0),
                            focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        )
                    )
                    
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Menampilkan ${displayList.size} dari ${filteredList.size} relawan",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.Gray else Color(0xFF64748B)
                    )
                }
            }

            // ── Mobile-Friendly Card List ──────────────────────────────────
            if (isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (displayList.isEmpty()) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.People,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = if (isDark) Color.DarkGray else Color.LightGray
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                "Tidak ada data relawan ditemukan",
                                fontSize = 14.sp,
                                color = if (isDark) Color.Gray else Color(0xFF64748B)
                            )
                        }
                    }
                }
            } else {
                items(displayList, key = { it.id ?: 0 }) { volunteer ->
                    Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                        VolunteerMobileCardItem(
                            volunteer = volunteer,
                            disasters = disasters,
                            onClick = { selectedVolunteer = volunteer }
                        )
                    }
                }
            }
        }
    }

    // ── Dialog Detail Relawan ──────────────────────────────────────────────────
    selectedVolunteer?.let { volunteer ->
        VolunteerDetailDialog(
            volunteer = volunteer,
            disasters = disasters,
            onDismiss = { selectedVolunteer = null },
            onApprove = {
                val id = volunteer.id?.toString()
                if (id != null) {
                    viewModel.approveVolunteer(id)
                    selectedVolunteer = null
                }
            },
            onReject = {
                val id = volunteer.id?.toString()
                if (id != null) {
                    viewModel.rejectVolunteer(id)
                    selectedVolunteer = null
                }
            },
            onAssignClick = {
                assignTarget = volunteer
                selectedVolunteer = null
            },
            onCancelAssignment = {
                val id = volunteer.id?.toString()
                if (id != null) {
                    viewModel.cancelAssignment(id)
                    selectedVolunteer = null
                }
            },
            onDelete = {
                volunteerToDelete = volunteer
                selectedVolunteer = null
            },
            onResetToPending = {
                val id = volunteer.id?.toString()
                if (id != null) {
                    viewModel.resetVolunteerToPending(id)
                    selectedVolunteer = null
                }
            }
        )
    }

    // ── Dialog Assign ─────────────────────────────────────────────────────────
    assignTarget?.let { volunteer ->
        AssignVolunteerDialog(
            volunteer = volunteer,
            disasters = disasters,
            onDismiss = { assignTarget = null },
            onConfirm = { disasterId, location ->
                val id = volunteer.id?.toString()
                if (id != null) {
                    viewModel.assignVolunteer(id, disasterId, location)
                    assignTarget = null
                }
            }
        )
    }

    // ── Dialog Hapus Konfirmasi ────────────────────────────────────────────────
    volunteerToDelete?.let { volunteer ->
        AlertDialog(
            onDismissRequest = { volunteerToDelete = null },
            title = { Text("Hapus Relawan", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus data pendaftaran relawan '${volunteer.name}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        val id = volunteer.id?.toString()
                        if (id != null) {
                            viewModel.deleteVolunteer(id)
                            volunteerToDelete = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Ya, Hapus", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { volunteerToDelete = null },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Batal")
                }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Composable VolunteerMobileCardItem (Native Mobile Card) ───────────────────
@Composable
private fun VolunteerMobileCardItem(
    volunteer: VolunteerDto,
    disasters: List<DisasterReportDto>,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val statusColor = volunteerStatusColor(volunteer.status)
    val cardBg = if (isDark) Color(0xFF1E293B) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color(0xFFE2E8F0))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: Avatar + Nama + Status/Skill Badges
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar Circle
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = volunteer.name.firstOrNull()?.uppercase() ?: "R",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.width(12.dp))
                
                // Name & Domisili
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = volunteer.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (isDark) Color.White else Color(0xFF1E293B),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = volunteer.address,
                        fontSize = 12.sp,
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Badges Column
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    SkillBadge(skill = volunteer.skill)
                    
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = volunteerStatusLabel(volunteer.status),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusColor
                        )
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)
            )

            // Phone Info Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Phone,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = volunteer.phoneNumber.ifBlank { "-" },
                    fontSize = 13.sp,
                    color = if (isDark) Color.LightGray else Color(0xFF475569)
                )
            }

            // Penugasan section
            val statusUpper = volunteer.status.uppercase()
            if (statusUpper == "APPROVED" || statusUpper == "ACCEPTED") {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "Penugasan:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.Gray else Color(0xFF64748B)
                )
                Spacer(Modifier.height(4.dp))

                if (volunteer.assignment.isNullOrBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)
                    ) {
                        Text(
                            text = "Belum Ditugaskan",
                            fontSize = 11.sp,
                            color = if (isDark) Color.LightGray else Color(0xFF475569),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Disaster tag
                        val disaster = disasters.find { it.id?.toLong() == volunteer.disasterId }
                        val disasterTitle = disaster?.title ?: "Bencana #${volunteer.disasterId}"
                        TagItem(
                            icon = Icons.Default.LocationOn,
                            label = disasterTitle,
                            bgColor = if (isDark) Color(0xFF1D4ED8).copy(alpha = 0.2f) else Color(0xFFEFF6FF),
                            contentColor = if (isDark) Color(0xFF60A5FA) else Color(0xFF1D4ED8)
                        )

                        // Location/Posko tag
                        TagItem(
                            icon = Icons.Default.Home,
                            label = volunteer.assignment ?: "",
                            bgColor = if (isDark) Color(0xFF7E22CE).copy(alpha = 0.2f) else Color(0xFFF3E8FF),
                            contentColor = if (isDark) Color(0xFFC084FC) else Color(0xFF7E22CE)
                        )

                        // Assignment status tag
                        val aStatus = volunteer.assignmentStatus ?: "pending"
                        val aColor = when (aStatus.lowercase()) {
                            "accepted" -> if (isDark) Color(0xFF4ADE80) else Color(0xFF15803D)
                            "rejected" -> if (isDark) Color(0xFFF87171) else Color(0xFFB91C1C)
                            else -> if (isDark) Color(0xFFFBBF24) else Color(0xFFB45309)
                        }
                        val aBg = when (aStatus.lowercase()) {
                            "accepted" -> if (isDark) Color(0xFF15803D).copy(alpha = 0.2f) else Color(0xFFF0FDF4)
                            "rejected" -> if (isDark) Color(0xFFB91C1C).copy(alpha = 0.2f) else Color(0xFFFEF2F2)
                            else -> if (isDark) Color(0xFFB45309).copy(alpha = 0.2f) else Color(0xFFFFFBEB)
                        }
                        val aLabel = when (aStatus.lowercase()) {
                            "accepted" -> "Bersedia"
                            "rejected" -> "Tidak Bersedia"
                            else -> "Menunggu Konfirmasi"
                        }
                        TagItem(
                            icon = Icons.Default.HourglassTop,
                            label = aLabel,
                            bgColor = aBg,
                            contentColor = aColor
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            
            // Detail Trigger Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                OutlinedButton(
                    onClick = onClick,
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    modifier = Modifier.height(34.dp),
                    border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.15f) else Color(0xFFE2E8F0))
                ) {
                    Icon(
                        imageVector = Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (isDark) Color.LightGray else Color.Gray
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Detail",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF475569)
                    )
                }
            }
        }
    }
}

// ── Composable StatCard ──────────────────────────────────────────────────────
@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: ImageVector,
    color: Color,
    bgColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val cardBg = if (isDark) {
        if (selected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surface
    } else {
        if (selected) bgColor else Color.White
    }
    val borderColor = if (selected) color else {
        if (isDark) Color.White.copy(alpha = 0.1f) else Color(0xFFE2E8F0)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(if (selected) 2.dp else 1.dp, borderColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(if (isDark) color.copy(alpha = 0.15f) else color.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.LightGray else Color(0xFF64748B),
                    letterSpacing = 0.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = value,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color(0xFF1E293B)
                )
            }
        }
    }
}

// ── Composable SkillBadge ─────────────────────────────────────────────────────
@Composable
private fun SkillBadge(skill: String) {
    val skillColor = when (skill.uppercase()) {
        "MEDIS"       -> Color(0xFF2196F3)
        "SAR"         -> Color(0xFFFF5722)
        "LOGISTIK"    -> Color(0xFF4CAF50)
        "PSIKOSOSIAL" -> Color(0xFF9C27B0)
        "KONSUMSI"    -> Color(0xFFFFC107)
        "PENDIDIKAN"  -> Color(0xFF009688)
        else          -> Color(0xFF757575)
    }
    Surface(
        shape = RoundedCornerShape(32.dp),
        color = skillColor.copy(alpha = 0.12f)
    ) {
        Text(
            text = skill.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = skillColor
        )
    }
}

// ── Composable TagItem ────────────────────────────────────────────────────────
@Composable
private fun TagItem(
    icon: ImageVector,
    label: String,
    bgColor: Color,
    contentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = bgColor
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, modifier = Modifier.size(12.dp), tint = contentColor)
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ── Composable DetailItem ─────────────────────────────────────────────────────
@Composable
private fun DetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    iconColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ── Composable Detail Dialog (Native Mobile Dialog) ──────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerDetailDialog(
    volunteer: VolunteerDto,
    disasters: List<DisasterReportDto>,
    onDismiss: () -> Unit,
    onApprove: () -> Unit,
    onReject: () -> Unit,
    onAssignClick: () -> Unit,
    onCancelAssignment: () -> Unit,
    onDelete: () -> Unit,
    onResetToPending: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (Title & Close Button)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Detail Relawan",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )
                        Text(
                            text = "Kode: ${volunteer.volunteerCode ?: "-"}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable Content including Action Buttons
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Info
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = volunteer.name.firstOrNull()?.uppercase() ?: "R",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                text = volunteer.name,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                SkillBadge(skill = volunteer.skill)
                                Spacer(Modifier.width(8.dp))
                                // Status badge
                                val sColor = volunteerStatusColor(volunteer.status)
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = sColor.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = volunteerStatusLabel(volunteer.status),
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = sColor
                                    )
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.3f))

                    // Detail data diri
                    DetailItem(label = "Alamat Domisili", value = volunteer.address, icon = Icons.Default.Home)
                    DetailItem(label = "Nomor Telepon", value = volunteer.phoneNumber, icon = Icons.Default.Phone)
                    DetailItem(label = "Status Ketersediaan", value = if (volunteer.availability == "available") "Tersedia" else "Tidak Tersedia", icon = Icons.Default.CheckCircle)

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.3f))

                    // Detail penugasan
                    Text("Informasi Penugasan", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = if (isDark) Color.White else Color.Black)
                    val statusUpper = volunteer.status.uppercase()
                    if (statusUpper != "APPROVED" && statusUpper != "ACCEPTED") {
                        Text("Pendaftaran relawan ini belum disetujui.", color = Color.Gray, fontSize = 13.sp)
                    } else if (volunteer.assignment.isNullOrBlank()) {
                        Text("Relawan ini belum ditugaskan ke bencana manapun.", color = Color.Gray, fontSize = 13.sp)
                    } else {
                        val disaster = disasters.find { it.id?.toLong() == volunteer.disasterId }
                        val disasterTitle = disaster?.title ?: "Bencana #${volunteer.disasterId}"

                        DetailItem(label = "Bencana", value = disasterTitle, icon = Icons.Default.LocationOn)
                        DetailItem(label = "Posko / Lokasi Tugas", value = volunteer.assignment ?: "-", icon = Icons.Default.Place)

                        val aStatus = volunteer.assignmentStatus ?: "pending"
                        val aLabel = when (aStatus.lowercase()) {
                            "accepted" -> "Bersedia (Diterima)"
                            "rejected" -> "Tidak Bersedia (Ditolak)"
                            else -> "Menunggu Konfirmasi"
                        }
                        DetailItem(label = "Status Konfirmasi", value = aLabel, icon = Icons.Default.HourglassTop)

                        if (aStatus.lowercase() == "rejected" && !volunteer.assignmentRejectionReason.isNullOrBlank()) {
                            DetailItem(label = "Alasan Menolak", value = volunteer.assignmentRejectionReason, icon = Icons.Default.Cancel, iconColor = Color.Red)
                        }

                        if (!volunteer.assignmentNotifiedAt.isNullOrBlank()) {
                            DetailItem(label = "Waktu Penugasan", value = volunteer.assignmentNotifiedAt, icon = Icons.Default.DateRange)
                        }
                    }

                    HorizontalDivider(color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.3f))

                    // Action Buttons (inside scrollable to prevent cut-off)
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (statusUpper == "PENDING" || volunteer.status.isBlank()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onApprove,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Setujui", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onReject,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tolak", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                Spacer(Modifier.width(6.dp))
                                Text("Hapus Pendaftaran", fontWeight = FontWeight.Medium, color = Color.Red)
                            }
                        } else if (statusUpper == "APPROVED" || statusUpper == "ACCEPTED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (volunteer.assignment.isNullOrBlank()) {
                                    Button(
                                        onClick = onAssignClick,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.AssignmentInd, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Tugaskan", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = onAssignClick,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Ubah Tugas", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = onCancelAssignment,
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.6f)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f).height(44.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp)
                                    ) {
                                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Batal Tugas", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                    }
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick = onReject,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444)),
                                    border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Block, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Tolak Relawan", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onDelete,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                    border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.4f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Hapus", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                        } else if (statusUpper == "DECLINED" || statusUpper == "REJECTED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Button(
                                    onClick = onApprove,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Setujui", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onResetToPending,
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.weight(1f).height(44.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text("Reset Pending", fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = onDelete,
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                                border = BorderStroke(1.dp, Color.Red.copy(alpha = 0.3f)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp)
                            ) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = Color.Red)
                                Spacer(Modifier.width(6.dp))
                                Text("Hapus Pendaftaran", fontWeight = FontWeight.Medium, color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Dialog Assign ke Bencana (Native Mobile Dialog) ──────────────────────────
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

    val isDark = isSystemInDarkTheme()
    val dialogBg = if (isDark) Color(0xFF1E293B) else Color.White

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp)
                .wrapContentHeight(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = dialogBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AssignmentInd,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "Tugaskan Relawan",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Text(
                                volunteer.name,
                                fontSize = 12.sp,
                                color = if (isDark) Color.LightGray else Color.Gray,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Tutup",
                            tint = if (isDark) Color.White else Color.Black
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color(0xFFF1F5F9)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.Star,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Keahlian: ${volunteer.skill}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (isDark) Color.White else Color.Black
                            )
                            Spacer(Modifier.weight(1f))
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = if (isDark) Color.LightGray else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                volunteer.address.take(15) + if (volunteer.address.length > 15) "…" else "",
                                fontSize = 12.sp,
                                color = if (isDark) Color.LightGray else Color.Gray
                            )
                        }
                    }

                    Text(
                        "Pilih Bencana *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.LightGray else Color(0xFF475569)
                    )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(),
                            isError = disasterError,
                            supportingText = if (disasterError) {
                                { Text("Wajib dipilih") }
                            } else null,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = disasterExpanded,
                            onDismissRequest = { disasterExpanded = false }
                        ) {
                            if (disasters.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("Tidak ada data bencana", color = if (isDark) Color.LightGray else Color.Gray) },
                                    onClick = {}
                                )
                            } else {
                                disasters.forEach { disaster ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(
                                                    "[${disaster.id}] ${disaster.title}",
                                                    fontWeight = FontWeight.Medium,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                                Text(
                                                    disaster.location,
                                                    fontSize = 11.sp,
                                                    color = if (isDark) Color.LightGray else Color.Gray
                                                )
                                            }
                                        },
                                        onClick = {
                                            selectedDisaster = disaster
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

                    Text(
                        "Lokasi Penugasan / Posko *",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) Color.LightGray else Color(0xFF475569)
                    )
                    OutlinedTextField(
                        value = assignmentLocation,
                        onValueChange = {
                            assignmentLocation = it
                            locationError = false
                        },
                        placeholder = { Text("Contoh: Stadion UNS, GOR Manahan") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.LocationOn,
                                null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                        isError = locationError,
                        supportingText = if (locationError) {
                            { Text("Lokasi tidak boleh kosong") }
                        } else null,
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White,
                            unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color.White
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isDark) Color(0xFFD97706).copy(alpha = 0.15f) else Color(0xFFFFFBEB),
                        border = BorderStroke(1.dp, if (isDark) Color(0xFFD97706).copy(alpha = 0.3f) else Color(0xFFFCD34D))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                Icons.Default.Info,
                                null,
                                tint = if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706),
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Setelah ditugaskan, relawan akan mendapat notifikasi dan diminta konfirmasi kesediaan.",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFFBBF24) else Color(0xFF92400E),
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(4.dp))

                    Button(
                        onClick = {
                            var valid = true
                            if (selectedDisaster == null) {
                                disasterError = true
                                valid = false
                            }
                            if (assignmentLocation.isBlank()) {
                                locationError = true
                                valid = false
                            }
                            if (valid) {
                                onConfirm(selectedDisaster!!.id!!.toLong(), assignmentLocation.trim())
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                    ) {
                        Icon(Icons.Default.Send, null, modifier = Modifier.size(17.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Tugaskan Sekarang", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        border = BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.2f) else Color(0xFFE2E8F0))
                    ) {
                        Text("Batal", color = if (isDark) Color.White else Color(0xFF475569))
                    }
                }
            }
        }
    }
}
