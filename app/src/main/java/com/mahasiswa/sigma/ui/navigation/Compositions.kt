package com.mahasiswa.sigma.ui.navigation

import androidx.compose.runtime.compositionLocalOf

val LocalBackStack = compositionLocalOf<MutableList<Route>> {
    error("No BackStack provided")
}
