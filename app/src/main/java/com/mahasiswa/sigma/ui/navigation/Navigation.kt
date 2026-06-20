package com.mahasiswa.sigma.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.ui.components.SigmaBottomBar
import com.mahasiswa.sigma.ui.screen.ProfileListScreen
import com.mahasiswa.sigma.ui.screens.*
import kotlinx.coroutines.launch

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
        backStack.removeAt(backStack.lastIndex)
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

    val currentRoute = backStack.lastOrNull()
    val dashboardRoute = backStack.filterIsInstance<Route.Dashboard>().lastOrNull()
    val userRole = dashboardRoute?.role ?: UserRole.MASYARAKAT
    val userEmail = dashboardRoute?.email ?: ""

    CompositionLocalProvider(LocalBackStack provides backStack) {

        Box(modifier = Modifier.fillMaxSize()) {
            NavDisplay(
                modifier = Modifier.fillMaxSize(),
                backStack = backStack
            ) { route ->
                when (route) {
                    is Route.Splash -> {
                        SplashScreen {
                            backStack.removeAt(backStack.lastIndex)
                            backStack.add(Route.Login)
                        }
                    }
                    is Route.Login -> {
                        LoginScreen(
                            onNavigateToDashboard = { role, email, name ->
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
                                backStack.removeAt(backStack.lastIndex)
                            }
                        )
                    }
                    is Route.Dashboard -> {
                        DashboardScreen(
                            userRole = route.role,
                            userName = route.name,
                            userEmail = route.email,
                            onFeatureClick = { id ->
                                when (id) {
                                    1 -> backStack.add(Route.Map)
                                    2 -> backStack.add(Route.DisasterReport)
                                    3 -> backStack.add(Route.ShelterInfo)
                                    7 -> backStack.add(Route.SearchDisaster)
                                    10 -> PdfUtils.openPdfFromAssets(context)
                                    5 -> backStack.add(Route.VolunteerRegistration(route.email))
                                    6 -> backStack.add(Route.ManageReport)
                                    11 -> backStack.add(Route.ManageShelter)
                                    12 -> backStack.add(Route.ManageVolunteer)
                                    13 -> backStack.add(Route.NewsList)
                                    14 -> backStack.add(Route.VolunteerReport)
                                    15 -> backStack.add(Route.ProfileList)
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
                    is Route.Map -> MapScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.DisasterReport -> DisasterReportScreen(
                        userRole = userRole,
                        userEmail = userEmail,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onNavigateToDetail = { report ->
                            backStack.add(Route.ReportDetail(report))
                        }
                    )
                    is Route.ReportDetail -> ReportDetailScreen(
                        report = route.report,
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                    is Route.ShelterInfo -> ShelterInfoScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.Profile -> {
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
                            userRole = dashboardRoute?.role ?: UserRole.MASYARAKAT,
                            userName = dashboardRoute?.name ?: "User",
                            userEmail = dashboardRoute?.email ?: "",
                            onBack = { backStack.removeAt(backStack.lastIndex) },
                            onLogout = {
                                showLogoutDialog = true
                            }
                        )
                    }
                    is Route.SearchDisaster -> SearchDisasterScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.VolunteerRegistration -> VolunteerRegistrationScreen(
                        userEmail = route.email,
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                    is Route.ManageReport -> ManageReportScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.ManageShelter -> ManageShelterScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.ManageVolunteer -> ManageVolunteerScreen(onBack = { backStack.removeAt(backStack.lastIndex) })
                    is Route.NewsList -> NewsListScreen(
                        userRole = userRole,
                        onBack = { backStack.removeAt(backStack.lastIndex) },
                        onOpenDetail = { newsId -> backStack.add(Route.NewsDetail(newsId)) }
                    )
                    is Route.NewsDetail -> NewsDetailScreen(
                        newsId = route.newsId,
                        userRole = userRole,
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                    is Route.VolunteerReport -> VolunteerReportScreen(
                        userRole = userRole,
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                    is Route.ProfileList -> ProfileListScreen(
                        onBack = { backStack.removeAt(backStack.lastIndex) }
                    )
                }
            }

            Box(
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                SigmaBottomBar(
                    currentRoute = currentRoute,
                    userRole = userRole,
                    onNavigateToHome = {
                        if (currentRoute !is Route.Dashboard && dashboardRoute != null) {
                            while (backStack.isNotEmpty() && backStack.last() !is Route.Dashboard) {
                                backStack.removeAt(backStack.lastIndex)
                            }
                        }
                    },
                    onNavigateToMap = {
                        if (currentRoute !is Route.Map) backStack.add(Route.Map)
                    },
                    onNavigateToPosko = {
                        if (userRole == UserRole.BNPB) {
                            if (currentRoute !is Route.ManageShelter) backStack.add(Route.ManageShelter)
                        } else {
                            if (currentRoute !is Route.ShelterInfo) backStack.add(Route.ShelterInfo)
                        }
                    },
                    onNavigateToProfile = {
                        if (currentRoute !is Route.Profile) backStack.add(Route.Profile)
                    },
                    onNavigateToManageVolunteer = {
                        if (currentRoute !is Route.ManageVolunteer) backStack.add(Route.ManageVolunteer)
                    }
                )
            }
        }
    }
}
