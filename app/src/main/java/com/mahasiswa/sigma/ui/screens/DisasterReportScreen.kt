package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import android.content.Intent
import android.provider.MediaStore
import androidx.compose.ui.platform.LocalContext
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.asImageBitmap

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisasterReportScreen(
    onBack: () -> Unit,
    onSubmit: (String, String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("GPS: -6.1754, 106.8272") } // Mock location

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lapor Bencana") },
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
                .verticalScroll(rememberScrollState())
        ) {
            Text("Detail Kejadian", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Judul Bencana") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Kejadian") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Lokasi (Otomatis)", fontWeight = FontWeight.Bold)
                    Text(location)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            val context = LocalContext.current

            var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

            val launcher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.TakePicturePreview()
            ) { bitmap: Bitmap? ->
                imageBitmap = bitmap
            }

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap!!.asImageBitmap(),
                    contentDescription = "Hasil Foto",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .padding(bottom = 16.dp)
                )
            } else {
                Text("Belum ada foto")
                Spacer(modifier = Modifier.height(8.dp))
            }

            Button(
                onClick = {
                    launcher.launch(null)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondary
                )
            ) {
                Text("Ambil Foto")
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { onSubmit(title, description, location) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kirim Laporan")
            }
        }
    }
}
