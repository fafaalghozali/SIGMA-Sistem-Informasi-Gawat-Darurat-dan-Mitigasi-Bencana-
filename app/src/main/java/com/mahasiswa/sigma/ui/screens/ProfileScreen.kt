package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.model.SkillsVolunteer
import com.mahasiswa.sigma.ui.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    userRole: UserRole,
    userName: String,
    userEmail: String,
    @Suppress("UNUSED_PARAMETER") onBack: () -> Unit,
    onLogout: () -> Unit,
    onProfileUpdated: (String, String) -> Unit = { _, _ -> },
    viewModel: ProfileViewModel = hiltViewModel()
) {
    LaunchedEffect(userName, userEmail, userRole) {
        viewModel.initData(userName, userEmail, userRole)
    }

    val name = viewModel.name
    val email = viewModel.email
    val imageBitmap = viewModel.imageBitmap
    val photoUrl = viewModel.photoUrl
    val showImageSheet = viewModel.showImageSheet
    val isUploadingPhoto = viewModel.isUploadingPhoto
    val sheetState = rememberModalBottomSheetState()
    val scrollState = rememberScrollState()

    // Dialog sukses upload foto
    if (viewModel.isUploadPhotoSuccess) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Berhasil") },
            text = { Text("Foto profil berhasil diperbarui.") },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog error upload foto
    if (viewModel.isUploadPhotoError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Gagal Upload Foto") },
            text = { Text(viewModel.errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog sukses update profil
    if (viewModel.isUpdateSuccess) {
        AlertDialog(
            onDismissRequest = {
                viewModel.dismissDialogs()
                onProfileUpdated(viewModel.name, viewModel.email)
            },
            title = { Text("Berhasil") },
            text = { Text("Profil berhasil diperbarui.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissDialogs()
                    onProfileUpdated(viewModel.name, viewModel.email)
                }) {
                    Text("OK")
                }
            }
        )
    }

    // Dialog error update profil
    if (viewModel.isUpdateError) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDialogs() },
            title = { Text("Gagal") },
            text = { Text(viewModel.errorMessage) },
            confirmButton = {
                TextButton(onClick = { viewModel.dismissDialogs() }) {
                    Text("OK")
                }
            }
        )
    }

    if (showImageSheet) {
        ImagePickerBottomSheet(
            sheetState = sheetState,
            onDismiss = { viewModel.showImageSheet = false },
            onImageSelected = { bitmap ->
                viewModel.onImageSelected(bitmap)
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil Pengguna", fontWeight = FontWeight.Bold) }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable(enabled = !isUploadingPhoto) { viewModel.showImageSheet = true },
                contentAlignment = Alignment.Center
            ) {
                when {
                    // Prioritas 1: bitmap baru yang baru dipilih (sudah/sedang diupload)
                    imageBitmap != null -> {
                        Image(
                            bitmap = imageBitmap.asImageBitmap(),
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Prioritas 2: URL dari Supabase Storage
                    !photoUrl.isNullOrBlank() -> {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Profile Picture",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    // Fallback: ikon default
                    else -> {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Default Profile",
                            modifier = Modifier.size(80.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Overlay loading saat upload
                if (isUploadingPhoto) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 3.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = if (isUploadingPhoto) "Mengupload foto..." else "Ubah Foto Profil",
                style = MaterialTheme.typography.labelLarge,
                color = if (isUploadingPhoto)
                    MaterialTheme.colorScheme.outline
                else
                    MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(enabled = !isUploadingPhoto) {
                    viewModel.showImageSheet = true
                }
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { viewModel.name = it },
                label = { Text("Nama Lengkap") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { viewModel.email = it },
                label = { Text("Email") },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium
            )

            if (userRole == UserRole.RELAWAN) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.address,
                    onValueChange = { viewModel.address = it },
                    label = { Text("Alamat Domisili") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.phoneNumber,
                    onValueChange = { viewModel.phoneNumber = it },
                    label = { Text("Nomor Telepon") },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                )

                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.selectedSkill.name,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Keahlian / Skill") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        disabledLabelColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f)
                ),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            "Status Akun",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            userRole.displayName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = { viewModel.updateProfile() },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(16.dp)
            ) {
                Text("Simpan Perubahan", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Keluar (Logout)", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ProfileScreenPreview() {
    ProfileScreen(
        userRole = UserRole.MASYARAKAT,
        userName = "Budi Santoso",
        userEmail = "budi@sigma.com",
        onBack = {},
        onLogout = {}
    )
}
