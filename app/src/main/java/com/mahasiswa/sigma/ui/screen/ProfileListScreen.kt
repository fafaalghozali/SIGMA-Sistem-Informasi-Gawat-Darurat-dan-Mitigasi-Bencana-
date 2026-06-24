package com.mahasiswa.sigma.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.mahasiswa.sigma.data.model.ProfileDto
import com.mahasiswa.sigma.ui.viewmodel.ProfileListUiState
import com.mahasiswa.sigma.ui.viewmodel.ProfileListViewModel
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Profile List screen — renders Loading / Success / Error / Empty from [ProfileListViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileListScreen(
    onBack: () -> Unit = {},
    viewModel: ProfileListViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Pengguna", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Kembali"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F7FA))
        ) {
            when (val state = uiState) {
                is ProfileListUiState.Idle -> {
                    // Initial state - could show placeholder
                }

                is ProfileListUiState.Loading -> {
                    LoadingState()
                }

                is ProfileListUiState.Success -> {
                    ProfileList(
                        profiles = state.profiles,
                        onRefresh = { viewModel.refresh() },
                        onDeleteProfile = { profileId ->
                            viewModel.deleteProfile(profileId)
                        },
                        onEditProfile = { profileId, newName ->
                            viewModel.editProfile(profileId, newName)
                        }
                    )
                }

                is ProfileListUiState.Error -> {
                    ErrorState(
                        message = state.message,
                        onRetry = { viewModel.refresh() }
                    )
                }

                is ProfileListUiState.Empty -> {
                    EmptyState(
                        message = "Belum ada data pengguna",
                        onRefresh = { viewModel.refresh() }
                    )
                }
            }
        }
    }
}

@Composable
fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text(
                text = "Memuat data pengguna...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ErrorState(
    message: String,
    onRetry: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = "Terjadi Kesalahan",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = onRetry) {
                Text("Coba Lagi")
            }
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(24.dp)
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            TextButton(onClick = onRefresh) {
                Text("Refresh")
            }
        }
    }
}

@Composable
fun ProfileList(
    profiles: List<ProfileDto>,
    onRefresh: () -> Unit,
    onDeleteProfile: (String) -> Unit,
    onEditProfile: (String, String) -> Unit
) {
    var profileToDelete by remember { mutableStateOf<ProfileDto?>(null) }
    var profileToEdit by remember { mutableStateOf<ProfileDto?>(null) }
    var selectedProfile by remember { mutableStateOf<ProfileDto?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Total Pengguna: ${profiles.size}",
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        items(
            items = profiles,
            key = { it.id ?: it.email }
        ) { profile ->
            ProfileCard(
                profile = profile,
                onCardClick = { selectedProfile = profile },
                onEditClick = { profileToEdit = profile },
                onDeleteClick = { profileToDelete = profile }
            )
        }
    }

    if (profileToDelete != null) {
        AlertDialog(
            onDismissRequest = { profileToDelete = null },
            title = { Text("Hapus Pengguna", fontWeight = FontWeight.Bold) },
            text = { Text("Apakah Anda yakin ingin menghapus pengguna '${profileToDelete?.fullName}'? Tindakan ini tidak dapat dibatalkan.") },
            confirmButton = {
                Button(
                    onClick = {
                        profileToDelete?.id?.let { onDeleteProfile(it) }
                        profileToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Hapus")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToDelete = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (profileToEdit != null) {
        var editName by remember { mutableStateOf(profileToEdit?.fullName ?: "") }
        AlertDialog(
            onDismissRequest = { profileToEdit = null },
            title = { Text("Edit Pengguna", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Nama Lengkap") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        profileToEdit?.id?.let { onEditProfile(it, editName) }
                        profileToEdit = null
                    }
                ) {
                    Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { profileToEdit = null }) {
                    Text("Batal")
                }
            }
        )
    }

    if (selectedProfile != null) {
        ProfileDetailDialog(
            profile = selectedProfile!!,
            onDismiss = { selectedProfile = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileCard(
    profile: ProfileDto,
    onCardClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val context = LocalContext.current
    val roleColor = when (profile.role.uppercase()) {
        "ADMIN" -> Color(0xFF1565C0) // Blue
        "RELAWAN" -> Color(0xFF2E7D32) // Green
        else -> Color(0xFF757575) // Grey
    }

    val formattedDate = remember(profile.createdAt) {
        try {
            profile.createdAt?.let {
                val cleanDate = it.replace("T", " ")
                val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(parsedDate)
                date?.let { d -> SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(d) }
            } ?: "-"
        } catch (e: Exception) {
            "-"
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        onClick = onCardClick
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8ECF0))
                    .border(1.dp, Color(0xFFE8ECF0), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (!profile.photoUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(profile.photoUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Profile Photo",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Details
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = profile.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Surface(
                        color = roleColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = profile.role.uppercase(),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                            color = roleColor
                        )
                    }
                    Text(
                        text = "• Mendaftar: $formattedDate",
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                        color = Color.Gray,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Actions
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Hapus",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileDetailDialog(
    profile: ProfileDto,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(profile.createdAt) {
        try {
            profile.createdAt?.let {
                val cleanDate = it.replace("T", " ")
                val parsedDate = if (cleanDate.contains(".")) cleanDate.substringBefore(".") else cleanDate
                val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(parsedDate)
                date?.let { d -> SimpleDateFormat("dd MMM yyyy, HH:mm", Locale("id")).format(d) }
            } ?: "-"
        } catch (e: Exception) {
            "-"
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Large Avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8ECF0)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!profile.photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = ImageRequest.Builder(context)
                                .data(profile.photoUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }

                Text(
                    text = profile.fullName,
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1A1A1A)
                )

                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = profile.role.uppercase(),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DetailRow(label = "Email", value = profile.email)
                    DetailRow(label = "ID Pengguna", value = profile.id ?: "-")
                    DetailRow(label = "Mendaftar Pada", value = formattedDate)
                    DetailRow(label = "Terakhir Diperbarui", value = profile.updatedAt?.let { it.substringBefore("T") } ?: "-")
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Tutup")
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Color(0xFF1A1A1A),
            modifier = Modifier.padding(top = 2.dp)
        )
    }
}
