package com.mahasiswa.sigma.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
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
import com.mahasiswa.sigma.data.model.UserRole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    userRole: UserRole,
    userName: String,
    onFeatureClick: (Int) -> Unit,
    onNavigateToProfile: () -> Unit
) {
    var showNotification by remember { mutableStateOf(true) }
    val isDark = isSystemInDarkTheme()

    val menuItems = remember(userRole) {
        val baseMenu = mutableListOf(
            DashboardMenuModel(1, "Peta Bencana", "Zona bahaya", Icons.Default.Map),
            DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan", Icons.Default.Report),
            DashboardMenuModel(3, "Info Posko", "Titik pengungsian", Icons.Default.HomeWork),
            DashboardMenuModel(10, "Panduan Bencana", "Tips mitigasi PDF", Icons.AutoMirrored.Filled.MenuBook),
            DashboardMenuModel(5, "Registrasi Relawan", "Daftar relawan", Icons.Default.PersonAdd),
            DashboardMenuModel(7, "Cari Bencana", "Search & Filter", Icons.Default.Search)
        )

        if (userRole == UserRole.BNPB) {
            baseMenu.add(DashboardMenuModel(6, "Verifikasi Laporan", "Validasi data", Icons.Default.VerifiedUser))
        }
        baseMenu
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Halo, $userName",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.5.sp,
                                fontSize = 20.sp
                            ),
                            color = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
                        )
                        Text(
                            text = "Sistem Informasi Gawat Darurat",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            ),
                            color = if (isDark) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onNavigateToProfile,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            modifier = Modifier.size(36.dp),
                            tint = if (isDark) MaterialTheme.colorScheme.primary else Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (isDark) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary,
                    titleContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White,
                    actionIconContentColor = if (isDark) MaterialTheme.colorScheme.onSurface else Color.White
                )
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { onFeatureClick(99) },
                icon = { Icon(Icons.Default.Phone, contentDescription = null) },
                text = { Text("DARURAT") },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                AnimatedVisibility(visible = showNotification) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF422222) else Color(0xFFFFEBEE),
                            contentColor = if (isDark) Color(0xFFFFEBEE) else Color(0xFFB71C1C)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = if (isDark) Color(0xFFFF8A80) else Color.Red)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("PERINGATAN DARURAT", fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
                                Text("Intensitas Hujan tinggi berpotensi menyebabkan banjir di wilayah Surakarta", fontSize = 14.sp)
                            }
                            IconButton(onClick = { showNotification = false }) {
                                Icon(Icons.Default.Close, contentDescription = "Close", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }


            item(span = { GridItemSpan(2) }) {
                NewsSection()
            }

            item(span = { GridItemSpan(2) }) {
                Text(
                    text = "Menu Layanan",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            items(menuItems) { item ->
                MenuCard(item) { onFeatureClick(item.id) }
            }

            item(span = { GridItemSpan(2) }) {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }
}

@Composable
fun NewsSection() {
    val isDark = isSystemInDarkTheme()
    val newsItems = remember(isDark) {
        listOf(
            NewsItem(1, "Banjir bandang melanda wilayah Sukoharjo", "10 min ago", "INFO", if (isDark) Color(0xFF1B2C42) else Color(0xFFE3F2FD)),
            NewsItem(2, "Gempa bumi M 5,0 SR di Daerah Ternate, Maluku Utara hingga Rektorat UNS", "3 hours ago", "DARURAT", if (isDark) Color(0xFF422222) else Color(0xFFFFEBEE)),
            NewsItem(3, "Prakiraan cuaca: Hujan lebat esok hari di Soloraya", "1 hour ago", "WASPADA", if (isDark) Color(0xFF423422) else Color(0xFFFFF3E0)),
            NewsItem(4, "Penyaluran bantuan logistik di posko pengungsian terkenda jembatan terputus", "2 hours ago", "INFO", if (isDark) Color(0xFF1B2C42) else Color(0xFFE3F2FD))
        )
    }

    Column(modifier = Modifier.padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Berita Terkini",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = { }) {
                Text("Lihat Semua", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(newsItems) { item ->
                NewsCard(item)
            }
        }
    }
}

@Composable
fun NewsCard(item: NewsItem) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .width(280.dp)
            .height(130.dp),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = item.backgroundColor,
            contentColor = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.8f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Surface(
                    color = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        text = item.category,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        color = if (isDark) Color.White else MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = item.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    lineHeight = 20.sp,
                    color = if (isDark) Color.White else Color.Black
                )
            }
            Text(
                text = item.time,
                fontSize = 11.sp,
                color = if (isDark) Color.White.copy(alpha = 0.6f) else Color.Black.copy(alpha = 0.6f)
            )
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
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
            contentColor = MaterialTheme.colorScheme.onSurface
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
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = item.description,
                fontSize = 11.sp,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
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

data class NewsItem(
    val id: Int,
    val title: String,
    val time: String,
    val category: String,
    val backgroundColor: Color
)
