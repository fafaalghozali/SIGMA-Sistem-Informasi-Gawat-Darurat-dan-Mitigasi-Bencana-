package com.mahasiswa.sigma

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
    object Dashboard : Screen("dashboard")
    object DisasterReport : Screen("disaster_report")
    object Guidance : Screen("guidance")
    object Volunteer : Screen("volunteer")
    object Verification : Screen("verification")
    object Monitoring : Screen("monitoring")
}

@Composable
fun SigmaNavigation(
    context: Context,
    prefManager: PreferenceManager,
    navController: NavHostController = rememberNavController()
) {
    val startDestination = if (prefManager.isLoggedIn()) Screen.Dashboard.route else Screen.Splash.route

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Screen.Splash.route) {
            SplashScreen {
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        composable(Screen.Login.route) {
            LoginScreen(
                onLogin = { email, password, role ->
                    val user = UserRepository.login(email, password, role)
                    if (user != null) {
                        prefManager.saveUser(user.fullName, user.email, user.role)
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(Screen.Login.route) { inclusive = true }
                        }
                    } else {
                        Toast.makeText(context, "Invalid credentials or role", Toast.LENGTH_SHORT).show()
                    }
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onRegister = { name, email, password, role ->
                    val user = User(name, email, password, role)
                    val success = UserRepository.addUser(user)
                    if (success) {
                        Toast.makeText(context, "Registration Successful", Toast.LENGTH_SHORT).show()
                        navController.navigate(Screen.Login.route) {
                            popUpTo(Screen.Register.route) { inclusive = true }
                        }
                    } else {
                        Toast.makeText(context, "Email already exists", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(Screen.Dashboard.route) {
            DashboardScreen(
                userName = prefManager.getUserName() ?: "User",
                userRole = prefManager.getUserRole() ?: "Masyarakat",
                onLogout = {
                    prefManager.clear()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Dashboard.route) { inclusive = true }
                    }
                },
                onFeatureClick = { menu ->
                    when (menu.id) {
                        2 -> navController.navigate(Screen.DisasterReport.route)
                        10 -> PdfUtils.openPdfFromAssets(context)
                        6 -> navController.navigate(Screen.Volunteer.route)
                        7 -> navController.navigate(Screen.Verification.route)
                        8 -> navController.navigate(Screen.Monitoring.route)
                        99 -> {
                            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                            context.startActivity(intent)
                        }
                        else -> Toast.makeText(context, "Fitur ${menu.title} segera hadir", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }

        composable(Screen.DisasterReport.route) {
            FeatureTemplate(
                title = "Lapor Bencana",
                actionText = "Kirim Laporan (Email)",
                onAction = {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:bnpb@gmail.com")
                        putExtra(Intent.EXTRA_SUBJECT, "Laporan Bencana SIGMA")
                    }
                    context.startActivity(Intent.createChooser(intent, "Kirim Email"))
                }
            )
        }

        composable(Screen.Guidance.route) {
            FeatureTemplate(
                title = "Panduan Bencana",
                actionText = "Buka Panduan (PDF Offline)",
                onAction = {
                    PdfUtils.openPdfFromAssets(context)
                }
            )
        }

        composable(Screen.Volunteer.route) { FeatureTemplate(title = "Manajemen Relawan") }
        composable(Screen.Verification.route) { FeatureTemplate(title = "Verifikasi") }
        composable(Screen.Monitoring.route) { FeatureTemplate(title = "Monitoring") }
    }
}
