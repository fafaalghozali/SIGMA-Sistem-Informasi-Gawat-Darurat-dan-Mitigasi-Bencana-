package com.mahasiswa.sigma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahasiswa.sigma.ui.theme.SIGMATheme

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prefManager = PreferenceManager(this)

        if (!prefManager.isLoggedIn()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        setContent {
            SIGMATheme {
                DashboardScreen(
                    userName = prefManager.getUserName() ?: "User",
                    userRole = prefManager.getUserRole() ?: "Masyarakat",
                    onLogout = {
                        prefManager.clear()
                        startActivity(Intent(this, LoginActivity::class.java))
                        finish()
                    },
                    onFeatureClick = { menu ->
                        // Explicit Intent logic
                        when (menu.id) {
                            2 -> startActivity(Intent(this, DisasterReportActivity::class.java))
                            10 -> startActivity(Intent(this, GuidanceActivity::class.java))
                            // Implicit Intent example: Call Emergency
                            else -> {
                                if (menu.title == "Call Emergency") {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this, "Fitur ${menu.title} segera hadir", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(userName: String, userRole: String, onLogout: () -> Unit, onFeatureClick: (DashboardMenuModel) -> Unit) {
    val menuItems = getMenuByRole(userRole)

    val roleGreeting = when (userRole) {
        "BNPB" -> "Monitoring dan kontrol sistem bencana"
        "Relawan" -> "Kelola tugas dan koordinasi tim"
        else -> "Pantau informasi bencana di sekitar Anda"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SIGMA Dashboard") },
                actions = {
                    IconButton(onClick = onLogout) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { 
                // Implicit Intent: Dial 112
                onFeatureClick(DashboardMenuModel(99, "Call Emergency", "", Icons.Default.Call))
            }) {
                Icon(Icons.Default.Call, contentDescription = "SOS")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            Text(text = "Welcome, $userName", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(text = roleGreeting, fontSize = 14.sp, color = MaterialTheme.colorScheme.secondary)
            
            Spacer(modifier = Modifier.height(24.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(menuItems) { item ->
                    MenuCard(item, onFeatureClick)
                }
            }
        }
    }
}

@Composable
fun MenuCard(item: DashboardMenuModel, onClick: (DashboardMenuModel) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .clickable { onClick(item) },
        shape = MaterialTheme.shapes.medium
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = item.title,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = item.title, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        }
    }
}

fun getMenuByRole(role: String): List<DashboardMenuModel> {
    val list = mutableListOf<DashboardMenuModel>()
    list.add(DashboardMenuModel(2, "Lapor Bencana", "Kirim laporan", Icons.Default.Warning))
    list.add(DashboardMenuModel(10, "Panduan Bencana", "Tips mitigasi", Icons.Default.MenuBook))

    if (role == "Relawan" || role == "BNPB") {
        list.add(DashboardMenuModel(6, "Manajemen Relawan", "Tugas tim", Icons.Default.Person))
    }
    if (role == "BNPB") {
        list.add(DashboardMenuModel(7, "Verifikasi", "Validasi data", Icons.Default.CheckCircle))
        list.add(DashboardMenuModel(8, "Monitoring", "Statistik", Icons.Default.Assessment))
    }
    return list
}

data class DashboardMenuModel(
    val id: Int,
    val title: String,
    val description: String,
    val icon: ImageVector
)
