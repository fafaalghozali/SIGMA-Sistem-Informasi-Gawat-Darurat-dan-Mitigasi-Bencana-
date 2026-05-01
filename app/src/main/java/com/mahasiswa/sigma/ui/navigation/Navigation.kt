package com.mahasiswa.sigma.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Box
import com.mahasiswa.sigma.PdfUtils
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.ui.screens.*

@Composable
fun NavDisplay(
    modifier: Modifier = Modifier,
    backStack: MutableList<Route>,
    content: @Composable (Route) -> Unit
) {
    val current = backStack.lastOrNull()
    if (current != null) {
        Box(modifier) {
            content(current)
        }
    }
    androidx.activity.compose.BackHandler(enabled = backStack.size > 1) {
        backStack.removeLastOrNull()
    }
}

@Composable
fun SigmaNavigation() {
    val backStack = rememberSaveable(
        saver = listSaver<SnapshotStateList<Route>, Route>(
            save = { it.toList() },
            restore = { it.toMutableStateList() }
        )
    ) { mutableStateListOf<Route>(Route.Splash) }

    val context = LocalContext.current
    val authManager = remember { AuthManager(context) }

    CompositionLocalProvider(LocalBackStack provides backStack) {
        NavDisplay(backStack = backStack) { currentRoute ->
            when (currentRoute) {
                is Route.Splash -> {
                    SplashScreen {
                        backStack.removeLastOrNull()
                        backStack.add(Route.Login)
                    }
                }
                is Route.Login -> {
                    LoginScreen(
                        onNavigateToDashboard = { role, email ->
                            val name = authManager.getUserName(email)
                            backStack.add(Route.Dashboard(role, email, name))
                        },
                        onNavigateToRegister = {
                            backStack.add(Route.Register)
                        }
                    )
                }
                is Route.Register -> {
                    RegisterScreen(
                        onNavigateToDashboard = { },
                        onNavigateToLogin = {
                            backStack.removeLastOrNull()
                        }
                    )
                }
                is Route.Dashboard -> {
                    DashboardScreen(
                        userRole = currentRoute.role,
                        userName = currentRoute.name,
                        onFeatureClick = { id ->
                            when (id) {
                                1 -> backStack.add(Route.Map)
                                2 -> backStack.add(Route.DisasterReport)
                                3 -> backStack.add(Route.ShelterInfo)
                                7 -> backStack.add(Route.SearchDisaster)
                                10 -> PdfUtils.openPdfFromAssets(context)
                                5 -> backStack.add(Route.VolunteerRegistration)
                                6 -> backStack.add(Route.AdminVerification)
                                99 -> {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    context.startActivity(intent)
                                }
                            }
                        },
                        onNavigateToProfile = {
                            backStack.add(Route.Profile)
                        }
                    )
                }
                is Route.Map -> MapScreen(onBack = { backStack.removeLastOrNull() })
                is Route.DisasterReport -> DisasterReportScreen(
                    onBack = { backStack.removeLastOrNull() },
                    onNavigateToDetail = { report ->
                        backStack.add(Route.ReportDetail(report))
                    }
                )
                is Route.ReportDetail -> ReportDetailScreen(
                    report = currentRoute.report,
                    onBack = { backStack.removeLastOrNull() }
                )
                is Route.ShelterInfo -> ShelterInfoScreen(onBack = { backStack.removeLastOrNull() })
                is Route.Profile -> {
                    val dashboardData = backStack.filterIsInstance<Route.Dashboard>().lastOrNull()
                    var showLogoutDialog by remember { mutableStateOf(false) }
                    if (showLogoutDialog) {
                        AlertDialog(
                            onDismissRequest = { showLogoutDialog = false },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Logout,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            },
                            title = {
                                Text(
                                    text = "Keluar Aplikasi",
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            text = {
                                Text(
                                    text = "Apakah Anda yakin ingin keluar dari akun ini?",
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        showLogoutDialog = false
                                        backStack.clear()
                                        backStack.add(Route.Login)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Text("Ya, Keluar", fontWeight = FontWeight.Bold)
                                }
                            },
                            dismissButton = {
                                TextButton(
                                    onClick = { showLogoutDialog = false },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Batal", textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
                                }
                            },
                            shape = RoundedCornerShape(24.dp)
                        )
                    }

                    ProfileScreen(
                        userRole = dashboardData?.role ?: UserRole.MASYARAKAT,
                        userName = dashboardData?.name ?: "User",
                        userEmail = dashboardData?.email ?: "",
                        onBack = { backStack.removeLastOrNull() },
                        onLogout = {
                            showLogoutDialog = true
                        }
                    )
                }
                is Route.SearchDisaster -> SearchDisasterScreen(onBack = { backStack.removeLastOrNull() })
                is Route.VolunteerRegistration -> VolunteerRegistrationScreen(onBack = { backStack.removeLastOrNull() })
                is Route.AdminVerification -> AdminVerificationScreen(onBack = { backStack.removeLastOrNull() })
            }
        }
    }
}
