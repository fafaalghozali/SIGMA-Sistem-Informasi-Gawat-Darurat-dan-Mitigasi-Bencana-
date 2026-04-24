package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class PendingReport(
    val id: String,
    val title: String,
    val reporter: String,
    val description: String,
    val time: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationScreen(onBack: () -> Unit) {
    val pendingReports = listOf(
        PendingReport("1", "Banjir Bandang", "Andi (Masyarakat)", "Air setinggi 1 meter di jalan utama.", "10 menit yang lalu"),
        PendingReport("2", "Kebakaran Hutan", "Budi (Relawan)", "Titik api terlihat di lereng bukit.", "30 menit yang lalu"),
        PendingReport("3", "Tanah Longsor", "Citra (Masyarakat)", "Akses jalan terputus akibat longsoran.", "1 jam yang lalu")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Verifikasi Laporan (BNPB)") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(pendingReports) { report ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(report.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                        Text("Pelapor: ${report.reporter}", fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)
                        Text(report.time, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(report.description)

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {  },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                            ) {
                                Text("Verifikasi")
                            }
                            OutlinedButton(
                                onClick = { },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text("Tolak")
                            }
                        }
                    }
                }
            }
        }
    }
}
