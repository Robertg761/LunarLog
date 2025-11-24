package com.lunarlog.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lunarlog.ui.home.HomeScreen
import com.lunarlog.ui.logdetails.LogDetailsScreen
import com.lunarlog.ui.logperiod.LogPeriodScreen
import java.time.LocalDate

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Logging : Screen("logging")
    object Details : Screen("details/{date}") {
        fun createRoute(date: Long) = "details/$date"
    }
}

@Composable
fun LunarLogNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Screen.Home.route) {
        composable(Screen.Home.route) {
            HomeScreen(
                onLogPeriodClicked = {
                    navController.navigate(Screen.Logging.route)
                },
                onLogDetailsClicked = {
                    val today = LocalDate.now().toEpochDay()
                    navController.navigate(Screen.Details.createRoute(today))
                }
            )
        }
        composable(Screen.Logging.route) {
            LogPeriodScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = Screen.Details.route,
            arguments = listOf(navArgument("date") { type = NavType.LongType })
        ) {
            LogDetailsScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
