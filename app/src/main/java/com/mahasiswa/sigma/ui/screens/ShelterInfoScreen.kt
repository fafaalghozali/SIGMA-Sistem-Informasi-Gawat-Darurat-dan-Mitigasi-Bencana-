package com.mahasiswa.sigma.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val longitude: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelterInfoScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val shelters = listOf(
        ShelterMock("Stadion UNS", "1.2 km", "80/100", "Tersedia", -7.556303,110.8580877),
        ShelterMock("Taman Cerdas Jebres", "2.5 km", "50/50", "Penuh", -7.5541321,110.8536159),
        ShelterMock("Solo Techno Park", "3.8 km", "30/200", "Tersedia", -7.5560692,110.8538665),
        ShelterMock("SAR UNS", "5.0 km", "10/40", "Tersedia", -7.5615699,110.8594894)
    )

    Scaffold(
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
                            horizontalArrangement = Arrangement.SpaceBetween
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
                        
                        Button(
                            onClick = {
                                val gmmIntentUri = Uri.parse("google.navigation:q=${shelter.latitude},${shelter.longitude}")
                                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                                mapIntent.setPackage("com.google.android.apps.maps")
                                if (mapIntent.resolveActivity(context.packageManager) != null) {
                                    context.startActivity(mapIntent)
                                } else {
                                    // Fallback if Google Maps is not installed
                                    val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:${shelter.latitude},${shelter.longitude}?q=${shelter.latitude},${shelter.longitude}(${shelter.name})"))
                                    context.startActivity(fallbackIntent)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Petunjuk Arah (Maps)", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
