package com.mahasiswa.sigma

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.mahasiswa.sigma.ui.theme.SIGMATheme
import com.mahasiswa.sigma.ui.navigation.SigmaNavigation
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        
        enableEdgeToEdge()

        setContent {
            SIGMATheme {
                SigmaNavigation()
            }
        }
    }
}
