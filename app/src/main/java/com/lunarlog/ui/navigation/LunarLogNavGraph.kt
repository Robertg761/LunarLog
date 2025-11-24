package com.lunarlog.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.lunarlog.ui.analysis.AnalysisScreen
import com.lunarlog.ui.calendar.CalendarScreen
import com.lunarlog.ui.home.HomeScreen
import com.lunarlog.ui.logdetails.LogDetailsScreen
import com.lunarlog.ui.logperiod.LogPeriodScreen
import com.lunarlog.ui.onboarding.OnboardingScreen
import com.lunarlog.ui.settings.SettingsScreen
import java.time.LocalDate

sealed class Screen(val route: String, val icon: ImageVector? = null, val label: String? = null) {
    object Home : Screen("home", Icons.Default.Home, "Home")
    object Calendar : Screen("calendar", Icons.Default.DateRange, "Calendar")
    object Analysis : Screen("analysis", Icons.Default.Timeline, "Insights")
    object Logging : Screen("logging")
    object Details : Screen("details/{date}") {
        fun createRoute(date: Long) = "details/$date"
    }
    object Settings : Screen("settings")
    object Onboarding : Screen("onboarding")
}

@Composable
fun LunarLogNavGraph(
    startDestination: String = Screen.Home.route
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(Screen.Home, Screen.Calendar, Screen.Analysis)
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label!!) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onLogPeriodClicked = { navController.navigate(Screen.Logging.route) },
                    onLogDetailsClicked = {
                        val today = LocalDate.now().toEpochDay()
                        navController.navigate(Screen.Details.createRoute(today))
                    },
                    // These are now handled by Bottom Nav
                    onSettingsClicked = { navController.navigate(Screen.Settings.route) }
                )
            }
            composable(Screen.Analysis.route) {
                AnalysisScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(
                    onOnboardingComplete = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(
                    onDayClicked = { date ->
                        navController.navigate(Screen.Details.createRoute(date))
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
}
