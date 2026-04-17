package com.mahasiswa.sigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mahasiswa.sigma.ui.theme.SIGMATheme
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.mahasiswa.sigma.ui.screens.*

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val userRole = intent.getStringExtra("USER_ROLE") ?: "Masyarakat"

        setContent {
            SIGMATheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                when (currentScreen) {
                    "dashboard" -> {
                        DashboardScreen(
                            userRole = userRole,
                            onNavigateToProfile = { currentScreen = "profile" },
                            onFeatureClick = { id ->
                                when (id) {
                                    1 -> currentScreen = "map"
                                    2 -> currentScreen = "disaster_report"
                                    3 -> currentScreen = "shelter_info"
                                    7 -> currentScreen = "search_disaster"
                                    10 -> PdfUtils.openPdfFromAssets(this)
                                    5 -> currentScreen = "volunteer_registration"
                                    6 -> currentScreen = "admin_verification"
                                    99 -> {
                                        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                        startActivity(intent)
                                    }
                                }
                            }
                        )
                    }
                    "profile" -> ProfileScreen(
                        userRole = userRole,
                        onBack = { currentScreen = "dashboard" },
                        onLogout = {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        }
                    )
                    "map" -> MapScreen(onBack = { currentScreen = "dashboard" })
                    "disaster_report" -> DisasterReportScreen(
                        onBack = { currentScreen = "dashboard" },
                        onSubmit = { _, _, _ -> currentScreen = "dashboard" }
                    )
                    "search_disaster" -> SearchDisasterScreen(onBack = { currentScreen = "dashboard" })
                    "shelter_info" -> ShelterInfoScreen(onBack = { currentScreen = "dashboard" })
                    "volunteer_registration" -> VolunteerRegistrationScreen(onBack = { currentScreen = "dashboard" })
                    "admin_verification" -> AdminVerificationScreen(onBack = { currentScreen = "dashboard" })
                }
            }
        }
    }
}
