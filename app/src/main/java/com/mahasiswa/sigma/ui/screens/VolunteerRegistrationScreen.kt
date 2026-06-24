package com.mahasiswa.sigma.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.data.model.VolunteerRegistrationData
import com.mahasiswa.sigma.ui.viewmodel.VolunteerRegistrationViewModel

// ── Skill metadata ────────────────────────────────────────────────────────────
private data class SkillInfo(
    val skill: SkillsVolunteer,
    val label: String,
    val description: String,
    val icon: ImageVector,
    val color: Color
)

private val skillInfoList = listOf(
    SkillInfo(SkillsVolunteer.MEDIS,       "Medis",       "Pertolongan pertama & kesehatan", Icons.Default.Favorite,   Color(0xFFE53935)),
    SkillInfo(SkillsVolunteer.SAR,         "SAR",         "Pencarian & penyelamatan korban", Icons.Default.Shield,     Color(0xFF1E88E5)),
    SkillInfo(SkillsVolunteer.LOGISTIK,    "Logistik",    "Distribusi bantuan & kebutuhan",  Icons.Default.Inventory,  Color(0xFFFB8C00)),
    SkillInfo(SkillsVolunteer.KONSUMSI,    "Konsumsi",    "Penyediaan makanan & minuman",    Icons.Default.Restaurant, Color(0xFF43A047)),
    SkillInfo(SkillsVolunteer.PSIKOSOSIAL, "Psikososial", "Dukungan mental & konseling",     Icons.Default.Psychology, Color(0xFF8E24AA)),
    SkillInfo(SkillsVolunteer.PENDIDIKAN,  "Pendidikan",  "Edukasi & penyuluhan bencana",    Icons.Default.MenuBook,   Color(0xFF00897B)),
)

// ── Main Screen ───────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VolunteerRegistrationScreen(
    userEmail: String,
    userName: String = "",
    onBack: () -> Unit,
    onRelogin: () -> Unit = {},
    viewModel: VolunteerRegistrationViewModel = hiltViewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.loadRegistrationData(userEmail, userName)
    }

    // Saat needsRelogin = true (user terima penugasan), trigger force relogin
    LaunchedEffect(viewModel.needsRelogin) {
        if (viewModel.needsRelogin) {
            viewModel.dismissRelogin()
            onRelogin()
        }
    }

    val currentStep    = viewModel.currentStep
    val isRegistered   = viewModel.isRegistered
    val registeredData = viewModel.registeredData

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Daftar Relawan", fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentStep > 1 && !isRegistered) viewModel.goToPreviousStep()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (isRegistered && registeredData != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                RegistrationStatusBox(
                    data = registeredData,
                    onReRegister = { viewModel.resetRegistration() },
                    onAcceptAssignment = { viewModel.confirmAssignment(true) },
                    onRejectAssignment = { viewModel.confirmAssignment(false) },
                    isConfirmingAssignment = viewModel.isConfirmingAssignment
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))
                StepperIndicator(currentStep = currentStep)
                Spacer(Modifier.height(4.dp))
                StepContent(viewModel = viewModel, currentStep = currentStep)
                AboutVolunteerPanel()
                Spacer(Modifier.height(24.dp))
            }
        }
    }

    // Dialogs
    if (viewModel.showIncompleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showIncompleteDialog = false },
            icon = {
                Icon(Icons.Default.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            },
            title = { Text("Data Belum Lengkap", fontWeight = FontWeight.Bold) },
            text = { Text("Pastikan semua kolom terisi dan nomor telepon minimal 10 digit angka.") },
            confirmButton = {
                Button(
                    onClick = { viewModel.showIncompleteDialog = false },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Mengerti") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }

    if (viewModel.submitError != null) {
        AlertDialog(
            onDismissRequest = { viewModel.submitError = null },
            icon = {
                Icon(Icons.Default.ErrorOutline, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(36.dp))
            },
            title = { Text("Pendaftaran Gagal", fontWeight = FontWeight.Bold) },
            text = { Text(viewModel.submitError ?: "") },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitError = null },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Tutup") }
            },
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Stepper ───────────────────────────────────────────────────────────────────
@Composable
private fun StepperIndicator(currentStep: Int) {
    val steps = listOf("Data Diri", "Keahlian", "Konfirmasi")
    val primary = MaterialTheme.colorScheme.primary

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val num      = index + 1
            val isDone   = num < currentStep
            val isActive = num == currentStep

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isDone || isActive -> primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isDone) {
                        Icon(Icons.Default.Check, contentDescription = null,
                            tint = Color.White, modifier = Modifier.size(17.dp))
                    } else {
                        Text("$num", color = if (isActive) Color.White
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isActive || isDone) primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (index < steps.lastIndex) {
                HorizontalDivider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .padding(bottom = 18.dp),
                    color = if (isDone) primary else MaterialTheme.colorScheme.outlineVariant,
                    thickness = 1.5.dp
                )
            }
        }
    }
}

// ── Step Content wrapper ──────────────────────────────────────────────────────
@Composable
private fun StepContent(viewModel: VolunteerRegistrationViewModel, currentStep: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            when (currentStep) {
                1 -> Step1DataDiri(viewModel)
                2 -> Step2Keahlian(viewModel)
                3 -> Step3Konfirmasi(viewModel)
            }
        }
    }
}

// ── Tombol navigasi bawah (reusable) ──────────────────────────────────────────
@Composable
private fun NavButtons(
    onBack: (() -> Unit)? = null,
    onNext: () -> Unit,
    nextLabel: String = "Selanjutnya",
    nextIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    isLoading: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        horizontalArrangement = if (onBack != null) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            OutlinedButton(
                onClick = onBack,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(48.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null,
                    modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(6.dp))
                Text("Kembali", fontWeight = FontWeight.Medium)
            }
        }

        Button(
            onClick = onNext,
            enabled = !isLoading,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp),
            contentPadding = PaddingValues(horizontal = 24.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(17.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(Modifier.width(8.dp))
                Text("Mengirim...")
            } else {
                Text(nextLabel, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.width(6.dp))
                Icon(nextIcon, contentDescription = null, modifier = Modifier.size(17.dp))
            }
        }
    }
}

// ── Step 1 — Data Diri ────────────────────────────────────────────────────────
@Composable
private fun Step1DataDiri(viewModel: VolunteerRegistrationViewModel) {
    Column {
        Text("Data Diri", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Lengkapi informasi pribadi Anda", fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = viewModel.name,
            onValueChange = { viewModel.onNameChange(it) },
            label = { Text("Nama Lengkap") },
            placeholder = { Text("Masukkan nama lengkap") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.address,
            onValueChange = { viewModel.onAddressChange(it) },
            label = { Text("Alamat Domisili") },
            placeholder = { Text("Masukkan alamat tempat tinggal") },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 100.dp),
            shape = RoundedCornerShape(12.dp),
            maxLines = 4,
            leadingIcon = {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(top = 16.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        )

        Spacer(Modifier.height(14.dp))

        OutlinedTextField(
            value = viewModel.phoneNumber,
            onValueChange = { viewModel.onPhoneNumberChange(it) },
            label = { Text("Nomor Telepon") },
            placeholder = { Text("08xxxxxxxxxx") },
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        NavButtons(onNext = { viewModel.goToNextStep() })
    }
}

// ── Step 2 — Keahlian ─────────────────────────────────────────────────────────
@Composable
private fun Step2Keahlian(viewModel: VolunteerRegistrationViewModel) {
    Column {
        Text("Pilih Keahlian", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Pilih bidang yang paling sesuai dengan kemampuan Anda",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(16.dp))

        skillInfoList.forEach { info ->
            SkillCard(
                info = info,
                isSelected = viewModel.selectedSkill == info.skill,
                onClick = { viewModel.onSkillSelected(info.skill) }
            )
            Spacer(Modifier.height(8.dp))
        }

        NavButtons(
            onBack = { viewModel.goToPreviousStep() },
            onNext = { viewModel.goToNextStep() }
        )
    }
}

@Composable
private fun SkillCard(info: SkillInfo, isSelected: Boolean, onClick: () -> Unit) {
    val primary = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) primary.copy(alpha = 0.07f)
                else MaterialTheme.colorScheme.surface
            )
            .border(
                BorderStroke(if (isSelected) 2.dp else 1.dp,
                    if (isSelected) primary else MaterialTheme.colorScheme.outlineVariant),
                RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(info.color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(info.icon, contentDescription = null,
                tint = info.color, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(info.label, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(info.description, fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        RadioButton(
            selected = isSelected,
            onClick = { onClick() },
            colors = RadioButtonDefaults.colors(selectedColor = primary)
        )
    }
}

// ── Step 3 — Konfirmasi ───────────────────────────────────────────────────────
@Composable
private fun Step3Konfirmasi(viewModel: VolunteerRegistrationViewModel) {
    Column {
        Text("Konfirmasi", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Periksa kembali data sebelum mengirim",
            fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

        Spacer(Modifier.height(20.dp))

        // Ringkasan data
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ConfirmRow(Icons.Default.Person,    "Nama",      viewModel.name)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ConfirmRow(Icons.Default.Home,      "Alamat",    viewModel.address)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ConfirmRow(Icons.Default.Phone,     "Telepon",   viewModel.phoneNumber)
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ConfirmRow(Icons.Default.Star,      "Keahlian",  viewModel.selectedSkill.name)
            }
        }

        Spacer(Modifier.height(14.dp))

        // Banner peringatan
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
        ) {
            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Default.Info, contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(16.dp).padding(top = 1.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    "Setelah mendaftar, Anda wajib menjalankan tugas sampai selesai bila mendapat penugasan resmi.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.error,
                    lineHeight = 18.sp
                )
            }
        }

        NavButtons(
            onBack = { viewModel.goToPreviousStep() },
            onNext = { viewModel.submitRegistration() },
            nextLabel = "Kirim",
            nextIcon = Icons.Default.Send,
            isLoading = viewModel.isSubmitting
        )
    }
}

@Composable
private fun ConfirmRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(72.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f))
    }
}

// ── Panel info bawah ──────────────────────────────────────────────────────────
@Composable
private fun AboutVolunteerPanel() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Groups,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Tentang Relawan SIGMA",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Spacer(Modifier.height(12.dp))

            Text(
                "Relawan SIGMA membantu masyarakat terdampak bencana di lapangan. " +
                "Data Anda akan diverifikasi Admin sebelum mendapat penugasan.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 20.sp
            )

            Spacer(Modifier.height(16.dp))

            listOf(
                Triple(1, "Daftar",     "Isi formulir pendaftaran"),
                Triple(2, "Verifikasi", "Admin meninjau data Anda"),
                Triple(3, "Penugasan",  "Ditugaskan ke lokasi bencana")
            ).forEachIndexed { index, (num, title, sub) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "$num",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        Text(
                            sub,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (index < 2) {
                    Row(modifier = Modifier.padding(start = 13.dp)) {
                        Box(
                            modifier = Modifier
                                .width(2.dp)
                                .height(16.dp)
                                .background(
                                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                )
                        )
                    }
                }
            }
        }
    }
}

// ── Status setelah terdaftar ──────────────────────────────────────────────────
@Composable
fun RegistrationStatusBox(
    data: VolunteerRegistrationData,
    onReRegister: () -> Unit,
    onAcceptAssignment: () -> Unit = {},
    onRejectAssignment: () -> Unit = {},
    isConfirmingAssignment: Boolean = false
) {

    val isPending  = data.status.uppercase() == "PENDING"
    val isApproved = data.status.uppercase() == "APPROVED" || data.status.uppercase() == "ACCEPTED"
    val isRejected = data.status.uppercase() == "REJECTED" || data.status.uppercase() == "DECLINED"

    val statusColor = when {
        isApproved -> Color(0xFF16A34A)
        isRejected -> Color(0xFFDC2626)
        else       -> Color(0xFFCA8A04)
    }
    val statusBg = when {
        isApproved -> Color(0xFFF0FDF4)
        isRejected -> Color(0xFFFEF2F2)
        else       -> Color(0xFFFFFBEB)
    }
    val gradientColors = when {
        isApproved -> listOf(Color(0xFF16A34A), Color(0xFF15803D))
        isRejected -> listOf(Color(0xFFDC2626), Color(0xFFB91C1C))
        else       -> listOf(Color(0xFFF59E0B), Color(0xFFD97706))
    }

    // Pulse animation untuk pending
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue  = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue  = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Hero card dengan gradient ─────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(colors = gradientColors)
                    )
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    // Ikon utama dengan efek pulse jika pending
                    Box(contentAlignment = Alignment.Center) {
                        if (isPending) {
                            // Ring pulse di belakang ikon
                            Box(
                                modifier = Modifier
                                    .size((80 * pulseScale).dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = pulseAlpha * 0.2f))
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when {
                                    isApproved -> Icons.Default.VerifiedUser
                                    isRejected -> Icons.Default.Cancel
                                    else       -> Icons.Default.HourglassTop
                                },
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = when {
                            isApproved -> "Selamat! Anda Diterima"
                            isRejected -> "Pendaftaran Ditolak"
                            else       -> "Sedang Diproses"
                        },
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = when {
                            isApproved -> "Anda resmi bergabung sebagai relawan SIGMA"
                            isRejected -> "Kualifikasi Anda belum memenuhi syarat"
                            else       -> "Tim Admin sedang meninjau data pendaftaran Anda"
                        },
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(Modifier.height(16.dp))

                    // Badge status
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = Color.White.copy(alpha = 0.2f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isPending) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = pulseAlpha))
                                )
                                Spacer(Modifier.width(7.dp))
                            }
                            Text(
                                text = when {
                                    isApproved -> "✓  DITERIMA"
                                    isRejected -> "✕  DITOLAK"
                                    else       -> "● MENUNGGU VERIFIKASI"
                                },
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // ── Card konfirmasi penugasan — muncul saat APPROVED & belum dikonfirmasi ──
        if (isApproved && (data.assignmentStatus.isNullOrBlank() || data.assignmentStatus.equals("pending", ignoreCase = true))) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                border = BorderStroke(1.5.dp, Color(0xFF16A34A).copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF16A34A).copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.NotificationsActive, null,
                                tint = Color(0xFF16A34A), modifier = Modifier.size(22.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Konfirmasi Penugasan",
                                fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text("Admin telah menugaskan Anda",
                                fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    if (!data.assignment.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.LocationOn, null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text("Lokasi Penugasan", fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(data.assignment, fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))
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
                                "Jika Anda menerima, akun akan diupgrade menjadi Relawan. Jika menolak, status kembali ke pending.",
                                fontSize = 11.sp, color = Color(0xFF92400E), lineHeight = 16.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = onRejectAssignment,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                            modifier = Modifier.weight(1f).height(46.dp),
                            enabled = !isConfirmingAssignment
                        ) {
                            Icon(Icons.Default.Close, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Tolak", fontWeight = FontWeight.SemiBold)
                        }
                        Button(
                            onClick = onAcceptAssignment,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                            modifier = Modifier.weight(1f).height(46.dp),
                            enabled = !isConfirmingAssignment
                        ) {
                            if (isConfirmingAssignment) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp, color = Color.White)
                            } else {
                                Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Terima", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }

        // ── Langkah selanjutnya (hanya untuk pending & accepted yg sudah dikonfirmasi) ─────────
        if (!isRejected) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = statusBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isPending) Icons.Default.NotificationsActive
                                      else Icons.Default.AssignmentTurnedIn,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = if (isPending)
                            "Anda akan mendapat notifikasi setelah Admin memverifikasi data Anda."
                        else
                            "Pantau menu Penugasan secara berkala untuk mendapat penugasan dari Admin.",
                        fontSize = 13.sp,
                        color = statusColor,
                        lineHeight = 19.sp
                    )
                }
            }
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Text(
                    "Detail Pendaftaran",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(14.dp))

                listOf(
                    Triple(Icons.Default.Person,    "Nama",     data.name),
                    Triple(Icons.Default.Star,       "Keahlian", data.skill.name),
                    Triple(Icons.Default.Home,       "Alamat",   data.address),
                    Triple(Icons.Default.Phone,      "Telepon",  data.phoneNumber),
                ).forEachIndexed { index, (icon, label, value) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(statusColor.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null,
                                tint = statusColor,
                                modifier = Modifier.size(16.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (index < 3) {
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = statusColor.copy(alpha = 0.12f)
                        )
                    }
                }
            }
        }

        // ── Tombol daftar ulang jika ditolak ─────────────────────────────
        if (isRejected) {
            Button(
                onClick = onReRegister,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = statusColor)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null,
                    modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Ajukan Pendaftaran Ulang", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }

        Spacer(Modifier.height(8.dp))
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────
@Composable
fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            textAlign = TextAlign.End, modifier = Modifier.weight(1f, fill = false))
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun VolunteerRegistrationScreenPreview() {
    MaterialTheme {
        VolunteerRegistrationScreen(userEmail = "test@gmail.com", userName = "Budi Santoso", onBack = {})
    }
}
