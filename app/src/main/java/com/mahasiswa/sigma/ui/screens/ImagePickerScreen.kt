package com.mahasiswa.sigma.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.mahasiswa.sigma.ui.viewmodel.ImagePickerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerScreen(
    navController: NavController,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: ImagePickerViewModel = hiltViewModel()

    val selectedBitmap by viewModel.selectedBitmap.collectAsState()

    LaunchedEffect(selectedBitmap) {
        selectedBitmap?.let {
            navController.previousBackStackEntry?.savedStateHandle?.set("image_bitmap", it)
            viewModel.resetState()
            navController.popBackStack()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.handleImageUri(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { viewModel.handleImageBitmap(it) }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.handleImageUri(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pilih Gambar") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Kembali")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentAlignment = Alignment.Center
        ) {
            ImagePickerContent(
                onCameraClick = {
                    if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                        cameraLauncher.launch(null)
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                },
                onGalleryClick = { galleryLauncher.launch("image/*") },
                onFileClick = { fileLauncher.launch(arrayOf("image/*")) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePickerBottomSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onImageSelected: (Bitmap) -> Unit
) {
    val context = LocalContext.current
    val viewModel: ImagePickerViewModel = hiltViewModel()
    val selectedBitmap by viewModel.selectedBitmap.collectAsState()

    LaunchedEffect(selectedBitmap) {
        selectedBitmap?.let {
            onImageSelected(it)
            viewModel.resetState()
            onDismiss()
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        bitmap?.let { viewModel.handleImageBitmap(it) }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.handleImageUri(it) }
    }

    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { viewModel.handleImageUri(it) }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) cameraLauncher.launch(null)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 8.dp,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        ImagePickerContent(
            onCameraClick = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    cameraLauncher.launch(null)
                } else {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
            },
            onGalleryClick = { galleryLauncher.launch("image/*") },
            onFileClick = { fileLauncher.launch(arrayOf("image/*")) }
        )
    }
}

@Composable
fun ImagePickerContent(
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onFileClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Lampirkan Gambar",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Pilih salah satu metode untuk mengambil gambar",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            PhotoOptionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.CameraAlt,
                label = "Kamera",
                accentColor = Color(0xFF4285F4),
                onClick = onCameraClick
            )
            PhotoOptionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.Default.PhotoLibrary,
                label = "Galeri",
                accentColor = Color(0xFF34A853),
                onClick = onGalleryClick
            )
            PhotoOptionItem(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Filled.InsertDriveFile,
                label = "File",
                accentColor = Color(0xFFFBBC05),
                onClick = onFileClick
            )
        }
    }
}

@Composable
fun PhotoOptionItem(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    accentColor: Color,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ImagePickerContentPreview() {
    Surface {
        ImagePickerContent(
            onCameraClick = {},
            onGalleryClick = {},
            onFileClick = {}
        )
    }
}
