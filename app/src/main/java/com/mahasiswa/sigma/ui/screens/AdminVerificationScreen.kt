package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahasiswa.sigma.data.model.LocalDisasterReport
import com.mahasiswa.sigma.ui.viewmodel.AdminVerificationViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AdminVerificationScreen(
    onBack: () -> Unit,
    viewModel: AdminVerificationViewModel = viewModel()
) {
    val pendingReports by viewModel.pendingReports.collectAsState()

    AdminVerificationContent(
        pendingReports = pendingReports,
        onBack = onBack,
        onVerify = { reportId -> viewModel.verifyReport(reportId) },
        onReject = { reportId -> viewModel.rejectReport(reportId) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVerificationContent(
    pendingReports: List<LocalDisasterReport>,
    onBack: () -> Unit,
    onVerify: (String) -> Unit,
    onReject: (String) -> Unit
) {
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
        if (pendingReports.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) {
                Text("Tidak ada laporan tertunda")
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(pendingReports) { report ->
                    ReportVerificationItem(
                        report = report,
                        onVerify = { onVerify(report.id) },
                        onReject = { onReject(report.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ReportVerificationItem(
    report: LocalDisasterReport,
    onVerify: () -> Unit,
    onReject: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
    val dateString = sdf.format(Date(report.timestamp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(report.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(
                "Pelapor: ${report.reporter}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.secondary
            )
            Text(dateString, fontSize = 12.sp, color = MaterialTheme.colorScheme.secondary)

            Spacer(modifier = Modifier.height(8.dp))
            Text(report.description)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onVerify,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50))
                ) {
                    Text("Verifikasi")
                }
                OutlinedButton(
                    onClick = onReject,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Tolak")
                }
            }
        }
    }
}
