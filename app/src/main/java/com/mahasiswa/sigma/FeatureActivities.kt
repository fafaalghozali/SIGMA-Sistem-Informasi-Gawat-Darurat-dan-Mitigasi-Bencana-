package com.mahasiswa.sigma

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mahasiswa.sigma.ui.theme.SIGMATheme

@Composable
fun FeatureTemplate(title: String, actionText: String? = null, onAction: (() -> Unit)? = null) {
    SIGMATheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(16.dp))
                Text(text = "Halaman Fitur SIGMA")
                
                if (actionText != null && onAction != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(onClick = onAction, modifier = Modifier.fillMaxWidth()) {
                        Text(actionText)
                    }
                }
            }
        }
    }
}

class DisasterReportActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
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

class MapActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatureTemplate(
                title = "Peta Bencana",
                actionText = "Buka Google Maps",
                onAction = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:-6.2088,106.8456?q=Posko"))
                    intent.setPackage("com.google.android.apps.maps")
                    startActivity(intent)
                }
            )
        }
    }
}

class GuidanceActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FeatureTemplate(
                title = "Panduan Bencana",
                actionText = "Buka Website BNPB",
                onAction = {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://bnpb.go.id")))
                }
            )
        }
    }
}

class NotificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FeatureTemplate(title = "Notifikasi") }
    }
}

class ShelterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FeatureTemplate(title = "Posko & Bantuan") }
    }
}

class VolunteerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FeatureTemplate(title = "Manajemen Relawan") }
    }
}

class VerificationActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FeatureTemplate(title = "Verifikasi") }
    }
}

class MonitoringActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { FeatureTemplate(title = "Monitoring") }
    }
}
