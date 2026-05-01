package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahasiswa.sigma.data.model.ReportStatus

data class DisasterInfo(
    val type: String,
    val location: String,
    val status: ReportStatus,
    val date: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchDisasterScreen(onBack: () -> Unit) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    
    val allDisasters = listOf(
        DisasterInfo("Banjir", "Sukoharjo", ReportStatus.SIAGA_1, "14 April 2026"),
    )

    var filteredDisasters by remember { mutableStateOf(allDisasters) }

    LaunchedEffect(searchQuery) {
        filteredDisasters = allDisasters.filter { d ->
            d.location.contains(searchQuery, ignoreCase = true) || d.type.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cari & Filter Bencana") },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { 
                    searchQuery = it
                    filteredDisasters = allDisasters.filter { d -> 
                        d.location.contains(it, ignoreCase = true) || d.type.contains(it, ignoreCase = true)
                    }
                },
                placeholder = { Text("Cari lokasi atau jenis bencana...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.FilterList, contentDescription = null)
                Text("Filter Status:", fontWeight = FontWeight.Bold)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = false, onClick = {}, label = { Text("Danger") })
                FilterChip(selected = false, onClick = {}, label = { Text("Siaga") })
                FilterChip(selected = false, onClick = {}, label = { Text("Resolved") })
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(filteredDisasters) { disaster ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(disaster.type, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                Badge(
                                    containerColor = when(disaster.status) {
                                        ReportStatus.AWAS -> Color.Red
                                        ReportStatus.SIAGA_1 -> Color.Blue
                                        ReportStatus.SIAGA_2 -> Color.DarkGray
                                        else -> Color.Green
                                    }
                                ) {
                                    Text(disaster.status.displayName, color = Color.White)
                                }
                            }
                            Text("Lokasi: ${disaster.location}", fontSize = 14.sp)
                            Text("Waktu: ${disaster.date}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        }
                    }
                }
            }
        }
    }
}
