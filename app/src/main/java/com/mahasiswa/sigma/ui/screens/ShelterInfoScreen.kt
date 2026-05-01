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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShelterMock(
    val name: String,
    val distance: String,
    val capacity: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val logisticsNeeded: List<String>
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShelterInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val shelters = listOf(
        ShelterMock("Stadion UNS", "1.2 km", "80/100", "Tersedia", -7.556303, 110.8580877, listOf("Sembako", "Air Mineral", "Selimut")),
        ShelterMock("Taman Cerdas Jebres", "1.5 km", "50/50", "Penuh", -7.5541321, 110.8536159, listOf("Popok Bayi", "Susu Formula", "Obat-obatan")),
        ShelterMock("Solo Techno Park", "2.2 km", "30/200", "Tersedia", -7.5560692, 110.8538666, listOf("Pakaian Layak Pakai", "Alat Mandi")),
        ShelterMock("SAR UNS", "0.8 km", "10/40", "Tersedia", -7.5615699, 110.8594894, listOf("Makanan Instan", "Tikar")),
        ShelterMock("Javanologi UNS", "0.7 KM", "127/250","Tersedia",-7.556998, 110.8598277, logisticsNeeded = listOf("Makanan Instan", "Alat Mandi", "Pakaian Layak Pakai")),
        ShelterMock("UNS Tower", "0.45 km","45/125","Tersedia",-7.5638533,110.8555975, logisticsNeeded = listOf("Susu Formula", "Obat-obatan", "Selimut")),
        ShelterMock("Asrama Mahasiswa UNS", "2.4 km","300/300","Penuh",-7.554193,110.865799, logisticsNeeded = listOf("Alat Mandi", "Sembako", "Sleeping Bag")),
        ShelterMock("Sekolah Vokasi UNS", "2.6 km","145/340","Tersedia",-7.559502,110.8383739, logisticsNeeded = listOf("Makanan Instan", "Obat-obatan", "Air Mineral"))
    )

    var selectedShelterName by remember { mutableStateOf("") }
    var selectedShelterLogistics by remember { mutableStateOf<List<String>?>(null) }
    var showLogisticsDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.blur(if (showLogisticsDialog) 12.dp else 0.dp),
            topBar = {
                TopAppBar(
                    title = { Text("Informasi Posko & Pengungsian", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        TextButton(onClick = onBack) {
                            Text("Kembali")
                        }
                    }
                )
            }
        ) { padding ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(shelters) { shelter ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(shelter.name, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                                Surface(
                                    color = if (shelter.status == "Penuh") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
                                    shape = MaterialTheme.shapes.small
                                ) {
                                    Text(
                                        shelter.status,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                        color = if (shelter.status == "Penuh") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Jarak: ${shelter.distance}", fontSize = 14.sp)
                            Text("Kapasitas: ${shelter.capacity} orang", fontSize = 14.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = {
                                        val gmmIntentUri = Uri.parse("google.navigation:q=${shelter.latitude},${shelter.longitude}")
                                        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                        mapIntent.setPackage("com.google.android.apps.maps")
                                        if (mapIntent.resolveActivity(context.packageManager) != null) {
                                            context.startActivity(mapIntent)
                                        } else {
                                            val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${shelter.latitude},${shelter.longitude}?q=${shelter.latitude},${shelter.longitude}(${shelter.name})"))
                                            context.startActivity(fallbackIntent)
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    shape = MaterialTheme.shapes.medium
                                ) {
                                    Icon(Icons.Default.Directions, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Petunjuk Arah", fontSize = 13.sp)
                                }

                                IconButton(
                                    onClick = {
                                        selectedShelterName = shelter.name
                                        selectedShelterLogistics = shelter.logisticsNeeded
                                        showLogisticsDialog = true
                                    },
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
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val message = "Halo, saya ingin mengirimkan bantuan logistik ke $selectedShelterName berupa: ${selectedShelterLogistics?.joinToString(", ")}"
                            val url = "https://api.whatsapp.com/send?phone=6285934415914&text=${Uri.encode(message)}"
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
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                            color =Color.Red,
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
