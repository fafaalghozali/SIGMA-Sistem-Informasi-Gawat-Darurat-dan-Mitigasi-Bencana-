package com.mahasiswa.sigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mahasiswa.sigma.ui.theme.SIGMATheme
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mahasiswa.sigma.ui.screens.*

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userRole = intent.getStringExtra("USER_ROLE") ?: "Masyarakat"

        setContent {
            SIGMATheme {
                DashboardNavigation(userRole = userRole)
            }
        }
    }

    @Composable
    fun DashboardNavigation(userRole: String) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    userRole = userRole,
                    onNavigateToProfile = { navController.navigate("profile") },
                    onFeatureClick = { id ->
                        when (id) {
                            1 -> navController.navigate("map")
                            2 -> navController.navigate("disaster_report")
                            3 -> navController.navigate("shelter_info")
                            7 -> navController.navigate("search_disaster")
                            10 -> PdfUtils.openPdfFromAssets(this@DashboardActivity)
                            5 -> navController.navigate("volunteer_registration")
                            6 -> navController.navigate("admin_verification")
                            99 -> {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                startActivity(intent)
                            }
                        }
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    userRole = userRole,
                    navController = navController,
                    onBack = { navController.popBackStack() },
                    onLogout = {
                        val intent = Intent(this@DashboardActivity, MainActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                )
            }
            composable("image_picker") {
                ImagePickerScreen(
                    navController = navController,
                    onBack = { navController.popBackStack() }
                )
            }
            composable("map") {
                MapScreen(onBack = { navController.popBackStack() })
            }
            composable("disaster_report") {
                DisasterReportScreen(
                    onBack = { navController.popBackStack() },
                    onSubmit = { _, _, _ -> navController.popBackStack() }
                )
            }
            composable("search_disaster") {
                SearchDisasterScreen(onBack = { navController.popBackStack() })
            }
            composable("shelter_info") {
                ShelterInfoScreen(onBack = { navController.popBackStack() })
            }
            composable("volunteer_registration") {
                VolunteerRegistrationScreen(onBack = { navController.popBackStack() })
            }
            composable("admin_verification") {
                AdminVerificationScreen(onBack = { navController.popBackStack() })
            }
        }
    }
}
