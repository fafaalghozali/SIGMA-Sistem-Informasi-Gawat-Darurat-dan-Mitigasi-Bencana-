package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageShelterScreen(@Suppress("UNUSED_PARAMETER") onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Kelola Posko Pengungsian", fontWeight = FontWeight.Bold) }
            )
        },
        floatingActionButton = {
            // Adjusting FAB position slightly up to not conflict with bottom bar
            FloatingActionButton(
                onClick = { /* TODO: Add Shelter */ },
                modifier = Modifier.padding(bottom = 80.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Tambah Posko")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.LocationOn,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Fitur Kelola Posko",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Di sini Admin dapat menambah, mengedit, dan menghapus titik posko pengungsian.",
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
            
            // Placeholder for list
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = { /* TODO */ }) {
                Text("Lihat Daftar Posko (Soon)")
            }

            // Spacing for bottom navbar
            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}
