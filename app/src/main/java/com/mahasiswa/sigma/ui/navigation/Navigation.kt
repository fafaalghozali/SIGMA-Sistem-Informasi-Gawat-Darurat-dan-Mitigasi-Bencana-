package com.mahasiswa.sigma.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import android.content.Context
import android.content.Intent
import android.app.Activity
import com.mahasiswa.sigma.DashboardActivity
import com.mahasiswa.sigma.ui.screens.SplashScreen
import com.mahasiswa.sigma.ui.screens.LoginScreen
import com.mahasiswa.sigma.ui.screens.RegisterScreen
import com.mahasiswa.sigma.data.model.UserRole

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("login")
    object Register : Screen("register")
}

@Composable
fun SigmaNavigation(
    context: Context,
    navController: NavHostController = rememberNavController()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route
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
                onNavigateToDashboard = { role, email ->
                    val intent = Intent(context, DashboardActivity::class.java).apply {
                        putExtra("USER_ROLE", role.name)
                        putExtra("USER_EMAIL", email)
                    }
                    context.startActivity(intent)
                    (context as? Activity)?.finish()
                },
                onNavigateToRegister = {
                    navController.navigate(Screen.Register.route)
                }
            )
        }

        composable(Screen.Register.route) {
            RegisterScreen(
                onNavigateToDashboard = { _ ->
                    // Redirection is handled inside RegisterScreen to onNavigateToLogin
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}
