package com.mahasiswa.sigma.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mahasiswa.sigma.data.model.ShelterDto
import com.mahasiswa.sigma.ui.viewmodel.ShelterViewModel
import com.mahasiswa.sigma.ui.viewmodel.UiState

/**
 * Map the raw DB status into a user-facing label.
 */
internal fun shelterStatusLabel(status: String): String = when (status.lowercase()) {
    "active", "tersedia", "available" -> "Tersedia"
    "full", "penuh" -> "Penuh"
    "closed", "tutup" -> "Tutup"
    else -> status.replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShelterInfoScreen(
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    viewModel: ShelterViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var selectedShelterName by remember { mutableStateOf("") }
    var selectedShelterLogistics by remember { mutableStateOf<List<String>?>(null) }
    var selectedShelterPhone by remember { mutableStateOf<String?>(null) }
    var showLogisticsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (showLogisticsDialog) 12.dp else 0.dp),
            topBar = {
                TopAppBar(
                    title = { Text("Informasi Posko & Pengungsian", fontWeight = FontWeight.Bold) },
                    actions = {
                        IconButton(onClick = { viewModel.refresh() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Segarkan")
                        }
                    }
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
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    is UiState.Error -> {
                        ShelterMessageState(
                            title = "Gagal memuat data",
                            message = state.message,
                            actionLabel = "Coba Lagi",
                            onAction = { viewModel.refresh() }
                        )
                    }

                    is UiState.Empty -> {
                        ShelterMessageState(
                            title = "Belum ada posko",
                            message = "Data posko pengungsian belum tersedia saat ini.",
                            actionLabel = "Segarkan",
                            onAction = { viewModel.refresh() }
                        )
                    }

                    is UiState.Success -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
                        ) {
                            items(state.data, key = { it.id ?: it.name }) { shelter ->
                                ShelterCard(
                                    shelter = shelter,
                                    onDirections = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${shelter.latitude},${shelter.longitude}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            val fallbackIntent = Intent(
                                                Intent.ACTION_VIEW,
                                                Uri.parse("geo:${shelter.latitude},${shelter.longitude}?q=${shelter.latitude},${shelter.longitude}(${shelter.name})")
                                            )
                                            context.startActivity(fallbackIntent)
                                        }
                                    },
                                    onLogistics = {
                                        selectedShelterName = shelter.name
                                        selectedShelterLogistics = shelter.logistics ?: emptyList()
                                        selectedShelterPhone = shelter.contactPhone
                                        showLogisticsDialog = true
                                    },
                                    onCall = {
                                        shelter.contactPhone?.let { phone ->
                                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
                                            context.startActivity(intent)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showLogisticsDialog && selectedShelterLogistics != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(enabled = false) {}
            )

            AlertDialog(
                onDismissRequest = { showLogisticsDialog = false },
                title = {
                    Column {
                        Text(
                            "Kebutuhan Logistik",
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            selectedShelterName,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                text = {
                    Column {
                        if (selectedShelterLogistics.isNullOrEmpty()) {
                            Text(
                                "Belum ada kebutuhan logistik yang tercatat untuk posko ini.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                "Masyarakat dapat mengirimkan bantuan mendesak berikut:",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                selectedShelterLogistics!!.forEach { item ->
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f),
                                        shape = RoundedCornerShape(12.dp),
                                        tonalElevation = 2.dp
                                    ) {
                                        Text(
                                            text = item,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(20.dp))
                            Text(
                                "*Bantuan Anda sangat berarti bagi warga di pengungsian.",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                        }
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val items = selectedShelterLogistics?.joinToString(", ") ?: "-"
                            val phone = selectedShelterPhone ?: "6285934415914"
                            val cleanPhone = phone.replace(Regex("[^0-9+]"), "").let {
                                if (it.startsWith("0")) "62${it.drop(1)}" else it
                            }
                            val message = "Halo, saya ingin mengirimkan bantuan logistik ke $selectedShelterName berupa: $items"
                            val url = "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.data = Uri.parse(url)
                            context.startActivity(intent)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF25D366)),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Hubungi via WhatsApp", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = { showLogisticsDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Tutup",
                            textAlign = TextAlign.Center,
                            color = Color.Red,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                tonalElevation = 8.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ShelterCard(
    shelter: ShelterDto,
    onDirections: () -> Unit,
    onLogistics: () -> Unit,
    onCall: () -> Unit
) {
    val statusLabel = shelterStatusLabel(shelter.status)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(0.dp)) {
            // Photo from Supabase photo_url
            if (!shelter.photoUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(shelter.photoUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Foto ${shelter.name}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        shelter.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = if (statusLabel == "Penuh") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            statusLabel,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            color = if (statusLabel == "Penuh") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text("Alamat: ${shelter.address}", fontSize = 14.sp)
                Text(
                    "Kapasitas: ${shelter.capacityCurrent} / ${shelter.capacityMax} Orang",
                    fontSize = 14.sp
                )

                // Display contact phone
                if (!shelter.contactPhone.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.Phone,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            "Kontak: ${shelter.contactPhone}",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Display logistics as chips
                if (!shelter.logistics.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Logistik:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        shelter.logistics!!.forEach { item ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                            ) {
                                Text(
                                    item,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onDirections,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Petunjuk Arah", fontSize = 13.sp)
                    }

                    if (!shelter.contactPhone.isNullOrBlank()) {
                        IconButton(
                            onClick = onCall,
                            modifier = Modifier.size(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Phone,
                                contentDescription = "Telepon",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    IconButton(
                        onClick = onLogistics,
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Logistik",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ShelterMessageState(
    title: String,
    message: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}
