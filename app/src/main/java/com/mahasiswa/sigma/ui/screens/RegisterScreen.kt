package com.mahasiswa.sigma.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.datastore.authDataStore
import com.mahasiswa.sigma.ui.theme.*
import com.mahasiswa.sigma.ui.viewmodel.RegisterViewModel
import com.mahasiswa.sigma.ui.viewmodel.RegisterViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    onNavigateToDashboard: (UserRole) -> Unit,
    onNavigateToLogin: () -> Unit
) {
    val context = LocalContext.current
    val authManager = remember { AuthManager(context.authDataStore) }
    val viewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(authManager)
    )

    val isDark = isSystemInDarkTheme()
    val backgroundColor = if (isDark) DarkBackground else Color(0xFFF8F9FA)
    val cardColor = if (isDark) DarkSurface else Color.White

    val name = viewModel.name
    val email = viewModel.email
    val password = viewModel.password
    val passwordVisible = viewModel.passwordVisible
    val selectedRole = viewModel.selectedRole
    val showDialog = viewModel.showDialog
    val registrationSuccess = viewModel.registrationSuccess
    val dialogMessage = viewModel.dialogMessage

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .blur(if (showDialog) 12.dp else 0.dp)
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ── Branding ──────────────────────────────────────────────────
            Text(
                text = "SIGMA",
                style = MaterialTheme.typography.displaySmall.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                ),
                color = if (isDark) Color.White else MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Buat akun baru untuk mulai\nmenggunakan layanan SIGMA",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                color = if (isDark) Color.Gray else Color.DarkGray.copy(alpha = 0.7f),
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(40.dp))

            // ── Input Group ───────────────────────────────────────────────
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { viewModel.name = it },
                    label = { Text("Nama Lengkap") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                    )
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = { viewModel.email = it },
                    label = { Text("Email") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Email,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    placeholder = { Text("contoh@email.com") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                    )
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = { if (!it.contains("\n")) viewModel.password = it },
                    label = { Text("Password") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    visualTransformation = if (passwordVisible)
                        VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { viewModel.passwordVisible = !viewModel.passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible)
                                    Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible)
                                    "Sembunyikan password" else "Tampilkan password",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                    )
                )

                // Role field — read-only, styled consistently
                OutlinedTextField(
                    value = selectedRole.displayName,
                    onValueChange = {},
                    readOnly = true,
                    enabled = false,
                    label = { Text("Role Anda") },
                    leadingIcon = {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        disabledTextColor = if (isDark) Color.LightGray
                            else MaterialTheme.colorScheme.onSurface,
                        disabledBorderColor = if (isDark) Color.White.copy(alpha = 0.12f)
                            else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        disabledLabelColor = if (isDark) Color.Gray
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledLeadingIconColor = if (isDark) Color.Gray
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledContainerColor = if (isDark)
                            DarkElevatedSurface else Color(0xFFF5F5F5),
                    )
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Primary Button ────────────────────────────────────────────
            Button(
                onClick = { viewModel.register(onNavigateToLogin) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Daftar Sekarang",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ── Footer Link ───────────────────────────────────────────────
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sudah punya akun? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (isDark) Color.Gray else Color.DarkGray
                )
                Text(
                    text = "Masuk di sini",
                    modifier = Modifier.clickable { onNavigateToLogin() },
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                )
            }
        }

        // ── Result Dialog ─────────────────────────────────────────────────
        if (showDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) {},
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier
                        .padding(32.dp)
                        .fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) DarkSurface else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .background(
                                    color = if (registrationSuccess)
                                        VolunteerGreen.copy(alpha = 0.1f)
                                    else
                                        EmergencyRed.copy(alpha = 0.1f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (registrationSuccess)
                                    Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
                                contentDescription = null,
                                tint = if (registrationSuccess) VolunteerGreen else EmergencyRed,
                                modifier = Modifier.size(32.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (registrationSuccess) "Registrasi Berhasil" else "Pendaftaran Gagal",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = dialogMessage,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isDark) Color.Gray else Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        Button(
                            onClick = { viewModel.onDialogConfirm(onNavigateToLogin) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (registrationSuccess)
                                    MaterialTheme.colorScheme.primary else EmergencyRed
                            )
                        ) {
                            Text(
                                text = if (registrationSuccess) "Kembali ke Login" else "Tutup",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun RegisterScreenPreview() {
    RegisterScreen(onNavigateToDashboard = {}, onNavigateToLogin = {})
}
