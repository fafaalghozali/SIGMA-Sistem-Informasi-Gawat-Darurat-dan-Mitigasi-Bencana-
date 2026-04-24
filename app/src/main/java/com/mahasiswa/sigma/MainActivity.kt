package com.mahasiswa.sigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.mahasiswa.sigma.ui.theme.SIGMATheme
import com.mahasiswa.sigma.ui.navigation.SigmaNavigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SIGMATheme {
                SigmaNavigation()
            }
        }
    }
}
