package com.mahasiswa.sigma.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
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
    val backStack = remember { mutableStateListOf<Route>(Route.Splash) }
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
                is Route.DisasterReport -> DisasterReportScreen(onBack = { backStack.removeLastOrNull() })
                is Route.ReportDetail -> { }
                is Route.ShelterInfo -> ShelterInfoScreen(onBack = { backStack.removeLastOrNull() })
                is Route.Profile -> {
                    val dashboardData = backStack.filterIsInstance<Route.Dashboard>().lastOrNull()
                    ProfileScreen(
                        userRole = dashboardData?.role ?: UserRole.MASYARAKAT,
                        userName = dashboardData?.name ?: "User",
                        userEmail = dashboardData?.email ?: "",
                        onBack = { backStack.removeLastOrNull() },
                        onLogout = {
                            backStack.clear()
                            backStack.add(Route.Login)
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
