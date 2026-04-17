package com.mahasiswa.sigma.ui.screens

import android.graphics.Bitmap
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userRole: String,
    navController: NavController,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    var name by remember { mutableStateOf("User Sigma") }
    var email by remember { mutableStateOf("user@sigma.id") }

    // State untuk menyimpan gambar (bisa Uri atau Bitmap)
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var imageBitmap by remember { mutableStateOf<Bitmap?>(null) }

    // Ambil hasil dari ImagePickerScreen melalui savedStateHandle
    val newImageUri = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Uri>("image_uri")
        ?.observeAsState()

    val newImageBitmap = navController
        .currentBackStackEntry
        ?.savedStateHandle
        ?.getLiveData<Bitmap>("image_bitmap")
        ?.observeAsState()

    // Update state jika ada hasil baru
    LaunchedEffect(newImageUri?.value) {
        newImageUri?.value?.let {
            imageUri = it
            imageBitmap = null // Reset bitmap jika dapat Uri baru
        }
    }

    LaunchedEffect(newImageBitmap?.value) {
        newImageBitmap?.value?.let {
            imageBitmap = it
            imageUri = null // Reset Uri jika dapat Bitmap baru
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Pengguna") },
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
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // FOTO PROFIL (klik untuk navigasi ke ImagePicker)
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .clickable {
                        navController.navigate("image_picker")
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageBitmap != null) {
                    Image(
                        bitmap = imageBitmap!!.asImageBitmap(),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Profile Picture",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Default Profile",
                        modifier = Modifier.size(80.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Ketuk untuk Ubah Foto", style = MaterialTheme.typography.bodySmall)

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Role: ", fontWeight = FontWeight.Bold)
                    Text(userRole)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { /* Simpan (opsional) */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Simpan Perubahan")
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar (Logout)")
            }
        }
    }
}
