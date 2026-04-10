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

class DashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SIGMATheme {
                var currentScreen by remember { mutableStateOf("dashboard") }

                if (currentScreen == "dashboard") {
                    DashboardScreen(
                        onLogout = {
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            finish()
                        },
                        onFeatureClick = { id ->
                            when (id) {
                                2 -> currentScreen = "disaster_report"
                                10 -> PdfUtils.openPdfFromAssets(this)
                                99 -> {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:112"))
                                    startActivity(intent)
                                }
                            }
                        }
                    )
                } else if (currentScreen == "disaster_report") {
                    FeatureTemplate(
                        title = "Lapor Bencana",
                        actionText = "Kirim Laporan (Email)",
                        onAction = {
                            val intent = Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:bnpb@gmail.com")
                                putExtra(Intent.EXTRA_SUBJECT, "Laporan Bencana SIGMA")
                            }
                            startActivity(Intent.createChooser(intent, "Kirim Email"))
                        }
                    )
                }
            }
        }
    }

    override fun onBackPressed() {
        // Logika sederhana untuk kembali ke dashboard jika sedang di halaman fitur
        // Di aplikasi produksi, sebaiknya gunakan NavHost internal
        super.onBackPressed()
    }
}
