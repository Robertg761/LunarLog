package com.lunarlog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.lunarlog.ui.home.HomeScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Logging : Screen("logging")
}

@Composable
fun LunarLogNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onLogPeriodClicked = { navController.navigate(Screen.Logging.route) }
            )
        }
        composable(Screen.Logging.route) {
            // TODO: Implement LoggingScreen
            // For now, we just show a placeholder or empty composable to avoid crash if navigated
             androidx.compose.material3.Text("Logging Screen Placeholder")
        }
    }
}
