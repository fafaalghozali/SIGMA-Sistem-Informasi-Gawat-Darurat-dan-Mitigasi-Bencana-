package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ShelterMock(
    val name: String,
    val distance: String,
    val capacity: String,
    val status: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelterInfoScreen(onBack: () -> Unit) {
    val shelters = listOf(
        ShelterMock("Posko GOR Jakarta", "1.2 km", "80/100", "Tersedia"),
        ShelterMock("Masjid Al-Barkah", "2.5 km", "50/50", "Penuh"),
        ShelterMock("Sekolah Dasar 01", "3.8 km", "30/200", "Tersedia"),
        ShelterMock("Kantor Kelurahan", "5.0 km", "10/40", "Tersedia")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Informasi Posko & Pengungsian") },
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
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(shelter.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                            Text(
                                shelter.status,
                                color = if (shelter.status == "Penuh") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Jarak: ${shelter.distance}", fontSize = 14.sp)
                        Text("Kapasitas: ${shelter.capacity}", fontSize = 14.sp)
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Button(
                            onClick = { /* Navigasi ke Maps */ },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Petunjuk Arah")
                        }
                    }
                }
            }
        }
    }
}
