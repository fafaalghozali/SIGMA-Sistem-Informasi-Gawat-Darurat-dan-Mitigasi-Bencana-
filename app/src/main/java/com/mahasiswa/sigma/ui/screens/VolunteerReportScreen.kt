package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.mahasiswa.sigma.data.model.DisasterReportDto
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.VolunteerReportDto
import com.mahasiswa.sigma.data.model.VolunteerReportParser
import com.mahasiswa.sigma.data.model.VolunteerReportWithDetails
import com.mahasiswa.sigma.ui.theme.DarkElevatedSurface
import com.mahasiswa.sigma.ui.viewmodel.UiState
import com.mahasiswa.sigma.ui.viewmodel.VolunteerReportViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerReportScreen(
    userRole: UserRole,
    onBack: () -> Unit,
    viewModel: VolunteerReportViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) { viewModel.load(userRole) }
    if (userRole == UserRole.BNPB) {
        BnpbVolunteerReportScreen(viewModel = viewModel, onBack = onBack)
    } else {
        RelawanVolunteerReportScreen(userRole = userRole, viewModel = viewModel, onBack = onBack)
    }
}

// ==================== BNPB VIEW ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BnpbVolunteerReportScreen(viewModel: VolunteerReportViewModel, onBack: () -> Unit) {
    val bnpbReports by viewModel.bnpbReports.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSkillFilter by viewModel.selectedSkillFilter.collectAsState()
    val selectedDisasterFilter by viewModel.selectedDisasterFilter.collectAsState()
    val disasters by viewModel.disasters.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("Laporan Tugas Relawan", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Monitoring laporan yang dikirim relawan di lapangan", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali") } },
                actions = { IconButton(onClick = { viewModel.load(UserRole.BNPB) }) { Icon(Icons.Default.Refresh, contentDescription = "Segarkan") } }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            BnpbFilterSection(searchQuery, selectedSkillFilter, selectedDisasterFilter, disasters,
                onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                onSkillFilterChange = { viewModel.updateSkillFilter(it) },
                onDisasterFilterChange = { viewModel.updateDisasterFilter(it) }
            )
            when (val state = bnpbReports) {
                is UiState.Idle, is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> VolunteerReportMessage("Gagal memuat laporan", state.message) { viewModel.load(UserRole.BNPB) }
                is UiState.Empty -> VolunteerReportMessage("Belum ada laporan", "Belum ada laporan tugas yang dikirim relawan.") { viewModel.load(UserRole.BNPB) }
                is UiState.Success -> {
                    if (state.data.isEmpty()) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Tidak ada laporan sesuai filter", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    } else {
                        LazyColumn(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 4.dp, bottom = 80.dp)) {
                            items(state.data, key = { it.report.id ?: it.hashCode().toString() }) { BnpbReportCard(it) }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BnpbFilterSection(
    searchQuery: String, selectedSkillFilter: String?, selectedDisasterFilter: String?,
    disasters: List<DisasterReportDto>, onSearchQueryChange: (String) -> Unit,
    onSkillFilterChange: (String?) -> Unit, onDisasterFilterChange: (String?) -> Unit
) {
    val skillOptions = listOf("MEDIS", "SAR", "LOGISTIK", "KONSUMSI", "PSIKOSOSIAL", "PENDIDIKAN")
    var skillExpanded by remember { mutableStateOf(false) }
    var disasterExpanded by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OutlinedTextField(
            value = searchQuery, onValueChange = onSearchQueryChange, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama relawan, bencana, catatan...", fontSize = 14.sp) },
            leadingIcon = { Icon(Icons.Default.Search, null, Modifier.size(20.dp)) },
            trailingIcon = { if (searchQuery.isNotEmpty()) IconButton(onClick = { onSearchQueryChange("") }) { Icon(Icons.Default.Clear, "Hapus") } },
            singleLine = true, shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ExposedDropdownMenuBox(expanded = skillExpanded, onExpandedChange = { skillExpanded = !skillExpanded }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = selectedSkillFilter ?: "Semua", onValueChange = {}, readOnly = true,
                    label = { Text("Keahlian", fontSize = 12.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(skillExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                ExposedDropdownMenu(expanded = skillExpanded, onDismissRequest = { skillExpanded = false }) {
                    DropdownMenuItem(text = { Text("Semua") }, onClick = { onSkillFilterChange(null); skillExpanded = false })
                    skillOptions.forEach { s -> DropdownMenuItem(text = { Text(s) }, onClick = { onSkillFilterChange(s); skillExpanded = false }) }
                }
            }
            ExposedDropdownMenuBox(expanded = disasterExpanded, onExpandedChange = { disasterExpanded = !disasterExpanded }, modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = selectedDisasterFilter ?: "Semua", onValueChange = {}, readOnly = true,
                    label = { Text("Bencana", fontSize = 12.sp) }, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(disasterExpanded) },
                    modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(10.dp),
                    textStyle = androidx.compose.ui.text.TextStyle(fontSize = 13.sp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)))
                ExposedDropdownMenu(expanded = disasterExpanded, onDismissRequest = { disasterExpanded = false }) {
                    DropdownMenuItem(text = { Text("Semua") }, onClick = { onDisasterFilterChange(null); disasterExpanded = false })
                    disasters.forEach { d -> DropdownMenuItem(text = { Text(d.title, maxLines = 1) }, onClick = { onDisasterFilterChange(d.title); disasterExpanded = false }) }
                }
            }
        }
    }
}

@Composable
private fun BnpbReportCard(r: VolunteerReportWithDetails) {
    val report = r.report
    val reportMap = remember(report.reportData) { VolunteerReportParser.parse(report.reportData) }
    val skillEnum = remember(report.skillType) { try { SkillsVolunteer.valueOf(report.skillType?.uppercase() ?: "") } catch (_: Exception) { null } }
    val skillColor = when (skillEnum) {
        SkillsVolunteer.MEDIS -> Color(0xFF2196F3); SkillsVolunteer.SAR -> Color(0xFFFF5722)
        SkillsVolunteer.LOGISTIK -> Color(0xFF4CAF50); SkillsVolunteer.PSIKOSOSIAL -> Color(0xFF9C27B0)
        SkillsVolunteer.KONSUMSI -> Color(0xFFFFC107); SkillsVolunteer.PENDIDIKAN -> Color(0xFF009688)
        else -> Color(0xFF757575)
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) DarkElevatedSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp)) {
            // Header Row: Avatar & Name on Left, Badges (Posko & Disaster) on Right
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                // Left: Avatar
                Box(Modifier.size(40.dp).clip(CircleShape).background(skillColor.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Text(r.volunteerName.firstOrNull()?.uppercase() ?: "G", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = skillColor)
                }
                Spacer(Modifier.width(10.dp))

                // Middle: Name & Skill details (expandable width)
                Column(Modifier.weight(1f)) {
                    Text(r.volunteerName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(report.skillType ?: "UMUM", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = skillColor)
                        Text(" · ${formatTimestamp(report.createdAt)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Spacer(Modifier.width(8.dp))

                // Right: Posko & Disaster Badges (limited to max 130.dp to avoid squeezing name columns)
                Column(
                    modifier = Modifier.widthIn(max = 130.dp),
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (r.poskoName != "-") {
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Home,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    r.poskoName,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                    if (r.disasterTitle != "-") {
                        Surface(
                            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    r.disasterTitle,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            BnpbStatsGrid(skillEnum, reportMap)
            if (!report.notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)); Spacer(Modifier.height(6.dp))
                Text("Catatan: ${report.notes}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun BnpbStatsGrid(skillEnum: SkillsVolunteer?, m: Map<String, String>) {
    when (skillEnum) {
        SkillsVolunteer.MEDIS -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("TOTAL KORBAN", m["total_korban"] ?: "-", Modifier.weight(1f)); MetricItem("SELAMAT", m["selamat"] ?: "-", Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("LUKA RINGAN", m["luka_ringan"] ?: "-", Modifier.weight(1f)); MetricItem("LUKA BERAT", m["luka_berat"] ?: "-", Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("KRITIS", m["kritis"] ?: "-", Modifier.weight(1f)); MetricItem("MENINGGAL", m["meninggal"] ?: "-", Modifier.weight(1f)) }
            val k = m["kebutuhan_medis"]; if (!k.isNullOrBlank() && k != "-") MetricItem("KEBUTUHAN MEDIS", k)
        }
        SkillsVolunteer.SAR -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("DIEVAKUASI", m["total_dievakuasi"] ?: "-", Modifier.weight(1f)); MetricItem("MASIH DICARI", m["masih_dicari"] ?: "-", Modifier.weight(1f)) }
            MetricItem("LOKASI EVAKUASI", m["lokasi_evakuasi"] ?: "-"); MetricItem("STATUS", m["status_pencarian"] ?: "-")
            val k = m["kendala_di_lapangan"]; if (!k.isNullOrBlank() && k != "-") MetricItem("KENDALA", k)
        }
        SkillsVolunteer.LOGISTIK -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("JENIS BANTUAN", m["jenis_bantuan"] ?: "-", Modifier.weight(1f)); MetricItem("DISALURKAN", m["jumlah_disalurkan"] ?: "-", Modifier.weight(1f)) }
            MetricItem("STOK TERSISA", m["stok_tersisa"] ?: "-")
            val k = m["kebutuhan_mendesak"]; if (!k.isNullOrBlank() && k != "-") MetricItem("KEBUTUHAN", k)
        }
        SkillsVolunteer.KONSUMSI -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("PORSI", m["jumlah_porsi"] ?: "-", Modifier.weight(1f)); MetricItem("DILAYANI", m["pengungsi_dilayani"] ?: "-", Modifier.weight(1f)) }
            MetricItem("MENU", m["menu_hari_ini"] ?: "-")
            val k = m["kebutuhan_bahan"]; if (!k.isNullOrBlank() && k != "-") MetricItem("KEBUTUHAN BAHAN", k)
        }
        SkillsVolunteer.PSIKOSOSIAL -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("DIDAMPINGI", m["jumlah_didampingi"] ?: "-", Modifier.weight(1f)); MetricItem("KONDISI", m["kondisi_psikologis"] ?: "-", Modifier.weight(1f)) }
            val k = m["kasus_khusus"]; if (!k.isNullOrBlank() && k != "-") MetricItem("KASUS KHUSUS", k)
            val r = m["rekomendasi"]; if (!r.isNullOrBlank() && r != "-") MetricItem("REKOMENDASI", r)
        }
        SkillsVolunteer.PENDIDIKAN -> Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) { MetricItem("SISWA", m["jumlah_siswa"] ?: "-", Modifier.weight(1f)); MetricItem("MATERI", m["materi_pembelajaran"] ?: "-", Modifier.weight(1f)) }
            val k = m["kebutuhan_edu_kits"]; if (!k.isNullOrBlank() && k != "-") MetricItem("EDU-KITS", k)
        }
        else -> {}
    }
}

// ==================== RELAWAN VIEW (unchanged) ====================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RelawanVolunteerReportScreen(userRole: UserRole, viewModel: VolunteerReportViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val operationMessage by viewModel.operationMessage.collectAsState()
    val currentVolunteer by viewModel.currentVolunteer.collectAsState()
    val disasters by viewModel.disasters.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showEditor by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<VolunteerReportDto?>(null) }
    var pendingDelete by remember { mutableStateOf<VolunteerReportDto?>(null) }

    LaunchedEffect(operationMessage) { operationMessage?.let { snackbarHostState.showSnackbar(it); viewModel.clearOperationMessage() } }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = { TopAppBar(title = { Text("Riwayat Laporan Tugas", fontWeight = FontWeight.Bold) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Kembali") } },
            actions = { IconButton(onClick = { viewModel.load(userRole) }) { Icon(Icons.Default.Refresh, "Segarkan") } }) },
        floatingActionButton = { FloatingActionButton(onClick = { editing = null; showEditor = true }, Modifier.padding(bottom = 80.dp)) { Icon(Icons.Default.Add, "Buat Laporan") } }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (val state = uiState) {
                is UiState.Idle, is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is UiState.Error -> VolunteerReportMessage("Gagal memuat laporan", state.message) { viewModel.load(userRole) }
                is UiState.Empty -> VolunteerReportMessage("Belum ada laporan", "Tekan tombol + untuk membuat laporan tugas.") { viewModel.load(userRole) }
                is UiState.Success -> {
                    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 160.dp)) {
                        items(state.data, key = { it.id ?: it.hashCode().toString() }) { report ->
                            VolunteerReportCard(report, disasters, canModify = true, onEdit = { editing = report; showEditor = true }, onDelete = { pendingDelete = report })
                        }
                    }
                }
            }
        }
    }
    if (showEditor) {
        val skillToUse = editing?.skillType ?: currentVolunteer?.skill
        VolunteerReportEditorDialog(initial = editing, volunteerSkill = skillToUse, disasters = disasters, onDismiss = { showEditor = false },
            onSubmit = { reportData, notes, selectedDisasterId ->
                val current = editing
                if (current?.id != null) viewModel.updateReport(userRole, current.id, reportData, notes)
                else viewModel.createReport(userRole, skillToUse, reportData, notes, selectedDisasterId)
                showEditor = false
            })
    }
    pendingDelete?.let { report ->
        AlertDialog(onDismissRequest = { pendingDelete = null }, title = { Text("Hapus Laporan", fontWeight = FontWeight.Bold) },
            text = { Text("Yakin ingin menghapus laporan ini?") },
            confirmButton = { Button(onClick = { report.id?.let { viewModel.deleteReport(userRole, it) }; pendingDelete = null },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text("Hapus") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Batal") } })
    }
}

@Composable
private fun VolunteerReportCard(report: VolunteerReportDto, disasters: List<DisasterReportDto>, canModify: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val reportMap = remember(report.reportData) { VolunteerReportParser.parse(report.reportData) }
    val skillEnum = remember(report.skillType) { try { SkillsVolunteer.valueOf(report.skillType?.uppercase() ?: "") } catch (_: Exception) { null } }
    val skillColor = when (skillEnum) {
        SkillsVolunteer.MEDIS -> Color(0xFF2196F3); SkillsVolunteer.SAR -> Color(0xFFFF5722); SkillsVolunteer.LOGISTIK -> Color(0xFF4CAF50)
        SkillsVolunteer.PSIKOSOSIAL -> Color(0xFF9C27B0); SkillsVolunteer.KONSUMSI -> Color(0xFFFFC107); SkillsVolunteer.PENDIDIKAN -> Color(0xFF009688)
        else -> Color(0xFF757575)
    }
    val disasterTitle = remember(report.disasterId, disasters) { disasters.find { it.id?.toString() == report.disasterId }?.title }

    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) DarkElevatedSurface else Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Surface(color = skillColor.copy(alpha = 0.12f), shape = RoundedCornerShape(8.dp), border = BorderStroke(0.5.dp, skillColor.copy(alpha = 0.4f))) {
                    Text(report.skillType ?: "UMUM", Modifier.padding(horizontal = 10.dp, vertical = 4.dp), fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = skillColor)
                }
                Text(formatTimestamp(report.createdAt), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            }
            if (disasterTitle != null) {
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(disasterTitle, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                }
            }
            Spacer(Modifier.height(14.dp))
            RelawanStatsSection(skillEnum, reportMap)
            if (!report.notes.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp)); HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)); Spacer(Modifier.height(8.dp))
                Text("Catatan: ${report.notes}", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (canModify) {
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = onEdit, Modifier.weight(1f)) { Icon(Icons.Default.Edit, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Edit") }
                    OutlinedButton(onClick = onDelete, Modifier.weight(1f), colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                        Icon(Icons.Default.Delete, null, Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)); Text("Hapus")
                    }
                }
            }
        }
    }
}

@Composable
private fun RelawanStatsSection(skillEnum: SkillsVolunteer?, reportMap: Map<String, String>) {
    when (skillEnum) {
        SkillsVolunteer.MEDIS -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Total Korban", "${reportMap["total_korban"] ?: "-"} Jiwa", Modifier.weight(1f)); MetricItem("Selamat/Sehat", "${reportMap["selamat"] ?: "-"} Jiwa", Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Luka Ringan", reportMap["luka_ringan"] ?: "-", Modifier.weight(1f)); MetricItem("Luka Berat", reportMap["luka_berat"] ?: "-", Modifier.weight(1f)) }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Kritis", reportMap["kritis"] ?: "-", Modifier.weight(1f)); MetricItem("Meninggal", reportMap["meninggal"] ?: "-", Modifier.weight(1f)) }
            val k = reportMap["kebutuhan_medis"]; if (!k.isNullOrBlank() && k != "-") WarningBox("Kebutuhan Medis Mendesak", k)
        }
        SkillsVolunteer.SAR -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Total Dievakuasi", "${reportMap["total_dievakuasi"] ?: "-"} Jiwa", Modifier.weight(1f)); MetricItem("Masih Dicari", "${reportMap["masih_dicari"] ?: "-"} Jiwa", Modifier.weight(1f)) }
            MetricItem("Lokasi Evakuasi", reportMap["lokasi_evakuasi"] ?: "-"); MetricItem("Status Pencarian", reportMap["status_pencarian"] ?: "-")
            val k = reportMap["kendala_di_lapangan"]; if (!k.isNullOrBlank() && k != "-") WarningBox("Kendala Lapangan", k)
        }
        SkillsVolunteer.LOGISTIK -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Jenis Bantuan", reportMap["jenis_bantuan"] ?: "-", Modifier.weight(1.2f)); MetricItem("Disalurkan", "${reportMap["jumlah_disalurkan"] ?: "-"} Pkt", Modifier.weight(0.8f)) }
            MetricItem("Stok Tersisa di Posko", "${reportMap["stok_tersisa"] ?: "-"} Paket")
            val k = reportMap["kebutuhan_mendesak"]; if (!k.isNullOrBlank() && k != "-") WarningBox("Kebutuhan Logistik Mendesak", k)
        }
        SkillsVolunteer.KONSUMSI -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Porsi Distribusi", "${reportMap["jumlah_porsi"] ?: "-"} Porsi", Modifier.weight(1f)); MetricItem("Warga Dilayani", "${reportMap["pengungsi_dilayani"] ?: "-"} Jiwa", Modifier.weight(1f)) }
            MetricItem("Menu Hari Ini", reportMap["menu_hari_ini"] ?: "-")
            val k = reportMap["kebutuhan_bahan"]; if (!k.isNullOrBlank() && k != "-") WarningBox("Kekurangan Bahan Makanan", k)
        }
        SkillsVolunteer.PSIKOSOSIAL -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Didampingi", "${reportMap["jumlah_didampingi"] ?: "-"} Jiwa", Modifier.weight(1f)); MetricItem("Kondisi Umum", reportMap["kondisi_psikologis"] ?: "-", Modifier.weight(1f)) }
            val k = reportMap["kasus_khusus"]; if (!k.isNullOrBlank() && k != "-") MetricItem("Kasus Khusus", k)
            val r = reportMap["rekomendasi"]; if (!r.isNullOrBlank() && r != "-") WarningBox("Rekomendasi Rujukan/Terapi", r)
        }
        SkillsVolunteer.PENDIDIKAN -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) { MetricItem("Jumlah Siswa", "${reportMap["jumlah_siswa"] ?: "-"} Anak", Modifier.weight(1f)); MetricItem("Materi Pembelajaran", reportMap["materi_pembelajaran"] ?: "-", Modifier.weight(1f)) }
            val k = reportMap["kebutuhan_edu_kits"]; if (!k.isNullOrBlank() && k != "-") WarningBox("Kebutuhan Edu-Kits", k)
        }
        else -> Text(text = "(Tidak ada data laporan)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun MetricItem(label: String, value: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Column {
            Text(
                text = label.uppercase(),
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = value,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WarningBox(label: String, content: String) {
    Surface(color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f), border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f)), shape = RoundedCornerShape(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column { Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold); Text(content, fontSize = 12.sp, color = MaterialTheme.colorScheme.onErrorContainer) }
        }
    }
}

@Composable
private fun VolunteerReportMessage(title: String, message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.Assignment, null, Modifier.size(56.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
            Text(title, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
            Button(onClick = onRetry) { Text("Segarkan") }
        }
    }
}

private fun formatTimestamp(isoString: String?): String {
    if (isoString.isNullOrBlank()) return ""
    return try {
        val cleanStr = isoString.replace("T", " ").substringBefore(".")
        val parser = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val date = parser.parse(cleanStr)
        if (date != null) SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(date) else isoString
    } catch (_: Exception) { isoString }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolunteerReportEditorDialog(
    initial: VolunteerReportDto?, volunteerSkill: String?, disasters: List<DisasterReportDto>,
    onDismiss: () -> Unit, onSubmit: (reportData: String, notes: String, disasterId: String?) -> Unit
) {
    val initialMap = remember(initial) { VolunteerReportParser.parse(initial?.reportData) }
    val skillEnum = remember(volunteerSkill) { try { SkillsVolunteer.valueOf(volunteerSkill?.uppercase() ?: "") } catch (_: Exception) { null } }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var selectedDisasterId by remember { mutableStateOf(initial?.disasterId ?: "") }
    var disasterExpanded by remember { mutableStateOf(false) }
    val selectedDisasterTitle = remember(selectedDisasterId, disasters) { disasters.find { it.id?.toString() == selectedDisasterId }?.title ?: "Pilih Bencana Terkait" }

    // MEDIS
    var totalKorban by remember { mutableStateOf(initialMap["total_korban"] ?: "") }
    var selamat by remember { mutableStateOf(initialMap["selamat"] ?: "") }
    var lukaRingan by remember { mutableStateOf(initialMap["luka_ringan"] ?: "") }
    var lukaBerat by remember { mutableStateOf(initialMap["luka_berat"] ?: "") }
    var kritis by remember { mutableStateOf(initialMap["kritis"] ?: "") }
    var meninggal by remember { mutableStateOf(initialMap["meninggal"] ?: "") }
    var kebutuhanMedis by remember { mutableStateOf(initialMap["kebutuhan_medis"] ?: "") }
    // SAR
    var totalDievakuasi by remember { mutableStateOf(initialMap["total_dievakuasi"] ?: "") }
    var masihDicari by remember { mutableStateOf(initialMap["masih_dicari"] ?: "") }
    var lokasiEvakuasi by remember { mutableStateOf(initialMap["lokasi_evakuasi"] ?: "") }
    var kendalaDiLapangan by remember { mutableStateOf(initialMap["kendala_di_lapangan"] ?: "") }
    var statusPencarian by remember { mutableStateOf(initialMap["status_pencarian"] ?: "Sedang Berjalan") }
    // LOGISTIK
    var jenisBantuan by remember { mutableStateOf(initialMap["jenis_bantuan"] ?: "") }
    var jumlahDisalurkan by remember { mutableStateOf(initialMap["jumlah_disalurkan"] ?: "") }
    var stokTersisa by remember { mutableStateOf(initialMap["stok_tersisa"] ?: "") }
    var kebutuhanMendesakLogistik by remember { mutableStateOf(initialMap["kebutuhan_mendesak"] ?: "") }
    // KONSUMSI
    var jumlahPorsi by remember { mutableStateOf(initialMap["jumlah_porsi"] ?: "") }
    var menuHariIni by remember { mutableStateOf(initialMap["menu_hari_ini"] ?: "") }
    var pengungsiDilayani by remember { mutableStateOf(initialMap["pengungsi_dilayani"] ?: "") }
    var kebutuhanBahan by remember { mutableStateOf(initialMap["kebutuhan_bahan"] ?: "") }
    // PSIKOSOSIAL
    var jumlahDidampingi by remember { mutableStateOf(initialMap["jumlah_didampingi"] ?: "") }
    var kondisiPsikologis by remember { mutableStateOf(initialMap["kondisi_psikologis"] ?: "Stabil") }
    var kasusKhusus by remember { mutableStateOf(initialMap["kasus_khusus"] ?: "") }
    var rekomendasi by remember { mutableStateOf(initialMap["rekomendasi"] ?: "") }
    // PENDIDIKAN
    var jumlahSiswa by remember { mutableStateOf(initialMap["jumlah_siswa"] ?: "") }
    var materiPembelajaran by remember { mutableStateOf(initialMap["materi_pembelajaran"] ?: "") }
    var kebutuhanEduKits by remember { mutableStateOf(initialMap["kebutuhan_edu_kits"] ?: "") }
    // Fallback
    var reportDescription by remember { mutableStateOf(initial?.reportData ?: "") }

    val isEdit = initial?.id != null
    val valid = when (skillEnum) {
        SkillsVolunteer.MEDIS -> totalKorban.isNotBlank()
        SkillsVolunteer.SAR -> totalDievakuasi.isNotBlank() && lokasiEvakuasi.isNotBlank()
        SkillsVolunteer.LOGISTIK -> jenisBantuan.isNotBlank() && jumlahDisalurkan.isNotBlank()
        SkillsVolunteer.KONSUMSI -> jumlahPorsi.isNotBlank() && menuHariIni.isNotBlank()
        SkillsVolunteer.PSIKOSOSIAL -> jumlahDidampingi.isNotBlank()
        SkillsVolunteer.PENDIDIKAN -> jumlahSiswa.isNotBlank() && materiPembelajaran.isNotBlank()
        else -> reportDescription.isNotBlank()
    } && selectedDisasterId.isNotBlank()

    AlertDialog(onDismissRequest = onDismiss, title = { Text(if (isEdit) "Edit Laporan" else "Buat Laporan Tugas", fontWeight = FontWeight.Bold) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Terkait Bencana *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                ExposedDropdownMenuBox(expanded = disasterExpanded, onExpandedChange = { disasterExpanded = !disasterExpanded }, modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(value = selectedDisasterTitle, onValueChange = {}, readOnly = true, trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(disasterExpanded) },
                        modifier = Modifier.menuAnchor(MenuAnchorType.PrimaryNotEditable).fillMaxWidth(), shape = RoundedCornerShape(8.dp))
                    ExposedDropdownMenu(expanded = disasterExpanded, onDismissRequest = { disasterExpanded = false }) {
                        disasters.forEach { d -> DropdownMenuItem(text = { Text(d.title) }, onClick = { selectedDisasterId = d.id?.toString() ?: ""; disasterExpanded = false }) }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text("Detail Tugas Divisi (${volunteerSkill ?: "UMUM"})", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                when (skillEnum) {
                    SkillsVolunteer.MEDIS -> { SigmaNumberField(totalKorban, { totalKorban = it }, "Total Korban (Jiwa)", "151"); SigmaNumberField(selamat, { selamat = it }, "Selamat/Sehat", "125"); SigmaNumberField(lukaRingan, { lukaRingan = it }, "Luka Ringan", "25"); SigmaNumberField(lukaBerat, { lukaBerat = it }, "Luka Berat", "35"); SigmaNumberField(kritis, { kritis = it }, "Kritis", "12"); SigmaNumberField(meninggal, { meninggal = it }, "Meninggal", "26"); SigmaTextAreaField(kebutuhanMedis, { kebutuhanMedis = it }, "Kebutuhan Medis Mendesak", "Kekurangan kain perban") }
                    SkillsVolunteer.SAR -> { SigmaNumberField(totalDievakuasi, { totalDievakuasi = it }, "Total Dievakuasi (Jiwa)", "8"); SigmaNumberField(masihDicari, { masihDicari = it }, "Masih Dicari (Jiwa)", "2"); SigmaTextField(lokasiEvakuasi, { lokasiEvakuasi = it }, "Lokasi Evakuasi", "Sektor C"); SigmaTextAreaField(kendalaDiLapangan, { kendalaDiLapangan = it }, "Kendala di Lapangan", "Arus deras") }
                    SkillsVolunteer.LOGISTIK -> { SigmaTextField(jenisBantuan, { jenisBantuan = it }, "Jenis Bantuan", "Selimut"); SigmaNumberField(jumlahDisalurkan, { jumlahDisalurkan = it }, "Jumlah Disalurkan (Paket)", "50"); SigmaNumberField(stokTersisa, { stokTersisa = it }, "Stok Tersisa (Paket)", "200"); SigmaTextAreaField(kebutuhanMendesakLogistik, { kebutuhanMendesakLogistik = it }, "Kebutuhan Mendesak", "Barang minim") }
                    SkillsVolunteer.KONSUMSI -> { SigmaNumberField(jumlahPorsi, { jumlahPorsi = it }, "Jumlah Porsi", "150"); SigmaTextField(menuHariIni, { menuHariIni = it }, "Menu Hari Ini", "Nasi + Telur"); SigmaNumberField(pengungsiDilayani, { pengungsiDilayani = it }, "Pengungsi Dilayani (Jiwa)", "120"); SigmaTextAreaField(kebutuhanBahan, { kebutuhanBahan = it }, "Kebutuhan Bahan", "Gas LPG") }
                    SkillsVolunteer.PSIKOSOSIAL -> { SigmaNumberField(jumlahDidampingi, { jumlahDidampingi = it }, "Jumlah Didampingi (Jiwa)", "15"); SigmaTextAreaField(kasusKhusus, { kasusKhusus = it }, "Kasus Khusus", "Depresi berat"); SigmaTextAreaField(rekomendasi, { rekomendasi = it }, "Rekomendasi", "Rujukan klinis") }
                    SkillsVolunteer.PENDIDIKAN -> { SigmaNumberField(jumlahSiswa, { jumlahSiswa = it }, "Jumlah Siswa", "25"); SigmaTextField(materiPembelajaran, { materiPembelajaran = it }, "Materi Pembelajaran", "Calistung"); SigmaTextAreaField(kebutuhanEduKits, { kebutuhanEduKits = it }, "Kebutuhan Edu-Kits", "Buku gambar") }
                    else -> { OutlinedTextField(value = reportDescription, onValueChange = { reportDescription = it }, label = { Text("Data Laporan") }, minLines = 3, modifier = Modifier.fillMaxWidth()) }
                }
                OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Catatan Tambahan") }, minLines = 2, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = valid, onClick = {
                val map = mutableMapOf<String, String>()
                when (skillEnum) {
                    SkillsVolunteer.MEDIS -> { map["total_korban"] = totalKorban; map["selamat"] = selamat; map["luka_ringan"] = lukaRingan; map["luka_berat"] = lukaBerat; map["kritis"] = kritis; map["meninggal"] = meninggal; map["kebutuhan_medis"] = kebutuhanMedis }
                    SkillsVolunteer.SAR -> { map["total_dievakuasi"] = totalDievakuasi; map["masih_dicari"] = masihDicari; map["lokasi_evakuasi"] = lokasiEvakuasi; map["kendala_di_lapangan"] = kendalaDiLapangan; map["status_pencarian"] = statusPencarian }
                    SkillsVolunteer.LOGISTIK -> { map["jenis_bantuan"] = jenisBantuan; map["jumlah_disalurkan"] = jumlahDisalurkan; map["stok_tersisa"] = stokTersisa; map["kebutuhan_mendesak"] = kebutuhanMendesakLogistik }
                    SkillsVolunteer.KONSUMSI -> { map["jumlah_porsi"] = jumlahPorsi; map["menu_hari_ini"] = menuHariIni; map["pengungsi_dilayani"] = pengungsiDilayani; map["kebutuhan_bahan"] = kebutuhanBahan }
                    SkillsVolunteer.PSIKOSOSIAL -> { map["jumlah_didampingi"] = jumlahDidampingi; map["kondisi_psikologis"] = kondisiPsikologis; map["kasus_khusus"] = kasusKhusus; map["rekomendasi"] = rekomendasi }
                    SkillsVolunteer.PENDIDIKAN -> { map["jumlah_siswa"] = jumlahSiswa; map["materi_pembelajaran"] = materiPembelajaran; map["kebutuhan_edu_kits"] = kebutuhanEduKits }
                    else -> { map["catatan_pelaporan_lapangan"] = reportDescription }
                }
                onSubmit(VolunteerReportParser.toJson(map), notes.trim(), selectedDisasterId.ifBlank { null })
            }) { Text(if (isEdit) "Simpan" else "Kirim") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Batal") } }
    )
}
