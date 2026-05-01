package com.mahasiswa.sigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mahasiswa.sigma.ui.theme.SIGMATheme
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mahasiswa.sigma.ui.screens.*
import com.mahasiswa.sigma.data.model.UserRole
import com.mahasiswa.sigma.data.auth.AuthManager
import com.mahasiswa.sigma.data.repository.ReportRepository
import com.mahasiswa.sigma.data.model.LocalDisasterReport

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val roleString = intent.getStringExtra("USER_ROLE")
        val userRole = UserRole.fromString(roleString)
        val userEmail = intent.getStringExtra("USER_EMAIL") ?: ""

        setContent {
            SIGMATheme {
                val context = LocalContext.current
                val authManager = remember { AuthManager(context) }
                val userName = authManager.getUserName(userEmail)
                
                DashboardNavigation(userRole = userRole, userName = userName, userEmail = userEmail)
            }
        }
    }

    @Composable
    fun DashboardNavigation(userRole: UserRole, userName: String, userEmail: String) {
        val navController = rememberNavController()

        NavHost(navController = navController, startDestination = "dashboard") {
            composable("dashboard") {
                DashboardScreen(
                    userRole = userRole,
                    userName = userName,
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
                    },
                    onNavigateToProfile = { navController.navigate("profile") }
                )
            }
            composable("profile") {
                ProfileScreen(
                    userRole = userRole,
                    userName = userName,
                    userEmail = userEmail,
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
                    onNavigateToDetail = { report: LocalDisasterReport ->
                        navController.navigate("report_detail/${report.id}")
                    }
                )
            }
            composable("report_detail/{reportId}") { backStackEntry ->
                val reportId = backStackEntry.arguments?.getString("reportId")
                val context = LocalContext.current
                val repository = remember { ReportRepository(context) }
                val report = remember(reportId) { 
                    repository.getAllReports().find { it.id == reportId } 
                }
                
                report?.let {
                    ReportDetailScreen(
                        report = it,
                        onBack = { navController.popBackStack() }
                    )
                }
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
