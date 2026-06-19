package com.mahasiswa.sigma.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.ui.viewmodel.ShelterViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState

// ─── Status helpers ────────────────────────────────────────────────────────────

internal fun shelterStatusLabel(status: String): String = when (status.lowercase()) {
    "active", "tersedia", "available" -> "Tersedia"
    "full", "penuh"                   -> "Penuh"
    "closed", "tutup"                 -> "Tutup"
    else                              -> status.replaceFirstChar { it.uppercase() }
}

private fun shelterStatusColor(label: String): Color = when (label) {
    "Tersedia" -> Color(0xFF22C55E)
    "Penuh"    -> Color(0xFFEF4444)
    "Tutup"    -> Color(0xFF64748B)
    else       -> Color(0xFF94A3B8)
}

// ─── Main Screen ───────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShelterInfoScreen(
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    viewModel: ShelterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var dialogShelter by remember { mutableStateOf<ShelterDto?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (dialogShelter != null) 8.dp else 0.dp),
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Posko & Pengungsian",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                            Text(
                                "Informasi posko bencana terkini",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                when (val state = uiState) {
                    is UiState.Idle, is UiState.Loading -> {
                        ShelterLoadingState()
                    }
                    is UiState.Error -> {
                        ShelterMessageState(
                            icon = Icons.Default.CloudOff,
                            title = "Gagal memuat data",
                            message = state.message,
                            actionLabel = "Coba Lagi",
                            onAction = { viewModel.refresh() }
                        )
                    }
                    is UiState.Empty -> {
                        ShelterMessageState(
                            icon = Icons.Default.HomeWork,
                            title = "Belum ada posko",
                            message = "Data posko pengungsian belum tersedia saat ini.",
                            actionLabel = "Segarkan",
                            onAction = { viewModel.refresh() }
                        )
                    }
                    is UiState.Success -> {
                        val filtered = remember(state.data, searchQuery) {
                            if (searchQuery.isBlank()) state.data
                            else state.data.filter { shelter ->
                                shelter.name.contains(searchQuery, ignoreCase = true) ||
                                shelter.address.contains(searchQuery, ignoreCase = true)
                            }
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp, end = 16.dp,
                                top = 12.dp, bottom = 100.dp
                            ),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            item {
                                ShelterSearchBar(
                                    query = searchQuery,
                                    onQueryChange = { searchQuery = it },
                                    resultCount = filtered.size
                                )
                            }
                            if (filtered.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 40.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                Icons.Default.SearchOff,
                                                contentDescription = null,
                                                modifier = Modifier.size(48.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                            )
                                            Text(
                                                "Tidak ada posko yang cocok",
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                    }
                                }
                            } else {
                                items(filtered, key = { it.id ?: it.name }) { shelter ->
                                    ShelterCard(
                                        shelter = shelter,
                                        onDirections = {
                                            val gmmUri = Uri.parse(
                                                "google.navigation:q=${shelter.latitude},${shelter.longitude}"
                                            )
                                            val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri)
                                                .apply { setPackage("com.google.android.apps.maps") }
                                            if (mapIntent.resolveActivity(context.packageManager) != null) {
                                                context.startActivity(mapIntent)
                                            } else {
                                                context.startActivity(
                                                    Intent(
                                                        Intent.ACTION_VIEW,
                                                        Uri.parse(
                                                            "geo:${shelter.latitude},${shelter.longitude}" +
                                                            "?q=${shelter.latitude},${shelter.longitude}(${shelter.name})"
                                                        )
                                                    )
                                                )
                                            }
                                        },
                                        onShowLogistics = { dialogShelter = shelter },
                                        onCall = {
                                            shelter.contactPhone?.let { phone ->
                                                context.startActivity(
                                                    Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                                )
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // ── Logistics Dialog ────────────────────────────────────────────────
        dialogShelter?.let { shelter ->
            LogisticsDialog(
                shelter = shelter,
                onDismiss = { dialogShelter = null }
            )
        }
    }
}

// ─── Search Bar ────────────────────────────────────────────────────────────────

@Composable
private fun ShelterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    resultCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Cari nama atau alamat posko…") },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Hapus")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
            )
        )
        if (query.isNotBlank()) {
            Text(
                "$resultCount posko ditemukan",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

// ─── Shelter Card ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShelterCard(
    shelter: ShelterDto,
    onDirections: () -> Unit,
    onShowLogistics: () -> Unit,
    onCall: () -> Unit
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
                    // gradient overlay bottom
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
                    // status badge on photo
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
                    // badge only shown when there's no photo
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
                        shelter.logistics!!.take(4).forEach { item ->
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
                    // Directions button
                    Button(
                        onClick = onDirections,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Petunjuk Arah", fontSize = 13.sp)
                    }

                    // Call button
                    if (!shelter.contactPhone.isNullOrBlank()) {
                        OutlinedButton(
                            onClick = onCall,
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Telepon", modifier = Modifier.size(18.dp))
                        }
                    }

                    // Logistics button
                    OutlinedButton(
                        onClick = onShowLogistics,
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Icon(Icons.Default.Inventory2, contentDescription = "Logistik", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
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

// ─── Logistics Dialog ──────────────────────────────────────────────────────────

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
                    // Call-to-action text
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

                    // Items list with icons
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

// ─── Loading State ─────────────────────────────────────────────────────────────

@Composable
private fun ShelterLoadingState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(3) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {}
        }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    }
}

// ─── Message State ─────────────────────────────────────────────────────────────

@Composable
private fun ShelterMessageState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(72.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier.padding(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                title,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )
            Text(
                message,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(actionLabel)
            }
        }
    }
}
