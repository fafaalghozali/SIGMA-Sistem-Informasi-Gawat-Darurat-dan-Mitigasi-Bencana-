package com.mahasiswa.sigma.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: String,
    onFeatureClick: (Int) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var showNotification by remember { mutableStateOf(true) }

    val menuItems = mutableListOf(
        DashboardMenuModel(1, "Peta Bencana", "Zona bahaya", Icons.Default.Map),
        DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan", Icons.Default.Report),
        DashboardMenuModel(3, "Info Posko", "Titik pengungsian", Icons.Default.HomeWork),
        DashboardMenuModel(10, "Panduan Bencana", "Tips mitigasi PDF", Icons.AutoMirrored.Filled.MenuBook),
        DashboardMenuModel(5, "Registrasi Relawan", "Daftar relawan", Icons.Default.PersonAdd),
        DashboardMenuModel(7, "Cari Bencana", "Search & Filter", Icons.Default.Search)
    )

    if (userRole == "BNPB") {
        menuItems.add(DashboardMenuModel(6, "Verifikasi Laporan", "Validasi data", Icons.Default.VerifiedUser))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIGMA Dashboard") },
                actions = {
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "Profile", modifier = Modifier.size(32.dp))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onFeatureClick(99) },
                icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                text = { Text("DARURAT 112") },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Emergency Notification Alert
            AnimatedVisibility(visible = showNotification) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = Color.Red)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("PERINGATAN DARURAT", fontWeight = FontWeight.Bold, color = Color.Red, fontSize = 12.sp)
                            Text("Potensi banjir rob di pesisir Jakarta Utara. Tetap waspada!", fontSize = 14.sp)
                        }
                        IconButton(onClick = { showNotification = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Text(text = "Halo, $userRole", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = "Sistem Informasi Gawat Darurat", fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(menuItems) { item ->
                    MenuCard(item) { onFeatureClick(item.id) }
                }
            }
        }
    }
}

@Composable
fun MenuCard(item: DashboardMenuModel, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon, 
                contentDescription = null, 
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title, 
                fontWeight = FontWeight.Bold, 
                fontSize = 14.sp, 
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Text(
                text = item.description,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.secondary
            )
        }
    }
}

data class DashboardMenuModel(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)
