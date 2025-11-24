package com.lunarlog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timeline
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
import androidx.navigation.navDeepLink
import com.lunarlog.ui.analysis.AnalysisScreen
import com.lunarlog.ui.calendar.CalendarScreen
import com.lunarlog.ui.home.HomeScreen
import com.lunarlog.ui.logdetails.LogDetailsScreen
import com.lunarlog.ui.logperiod.LogPeriodScreen
import com.lunarlog.ui.onboarding.OnboardingScreen
import com.lunarlog.ui.settings.SettingsScreen
import java.time.LocalDate

sealed class Screen(
    val route: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val label: String? = null
) {
    object Home : Screen("home", Icons.Default.Home, Icons.Outlined.Home, "Home")
    object Calendar : Screen("calendar", Icons.Default.DateRange, Icons.Outlined.DateRange, "Calendar")
    object Analysis : Screen("analysis", Icons.Default.Timeline, Icons.Outlined.Timeline, "Insights")
    object Logging : Screen("logging")
    object Details : Screen("details/{date}") {
        fun createRoute(date: Long) = "details/$date"
    }
    object Settings : Screen("settings")
    object LogHistory : Screen("log_history")
    object Onboarding : Screen("onboarding")
}

private fun getScreenOrder(route: String?): Int {
    return when (route) {
        Screen.Home.route -> 0
        Screen.Calendar.route -> 1
        Screen.Analysis.route -> 2
        else -> -1
    }
}

@OptIn(ExperimentalSharedTransitionApi::class)
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
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = {
                                Icon(
                                    imageVector = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                    contentDescription = screen.label
                                )
                            },
                            label = { Text(screen.label!!) },
                            selected = selected,
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
        SharedTransitionLayout(modifier = Modifier.padding(innerPadding)) {
            NavHost(
                navController = navController,
                startDestination = startDestination
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    },
                    exitTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    }
                ) {
                    HomeScreen(
                        onLogPeriodClicked = { navController.navigate(Screen.Logging.route) },
                        onLogDetailsClicked = {
                            val today = LocalDate.now().toEpochDay()
                            navController.navigate(Screen.Details.createRoute(today))
                        },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = Screen.Calendar.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://calendar" }),
                    enterTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    },
                    exitTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    }
                ) {
                    CalendarScreen(
                        onDayClicked = { date ->
                            navController.navigate(Screen.Details.createRoute(date))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = Screen.Analysis.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://analysis" }),
                    enterTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    },
                    exitTransition = {
                        val initial = getScreenOrder(initialState.destination.route)
                        val target = getScreenOrder(targetState.destination.route)
                        if (initial != -1 && target != -1) {
                            if (initial < target) slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, tween(300))
                            else slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, tween(300))
                        } else null
                    }
                ) {
                    AnalysisScreen(
                        onBack = { navController.popBackStack() },
                        onHistoryClick = { navController.navigate(Screen.LogHistory.route) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Screen.LogHistory.route) {
                    com.lunarlog.ui.loghistory.LogHistoryScreen(
                        onBackClick = { navController.popBackStack() },
                        onLogClick = { date ->
                            navController.navigate(Screen.Details.createRoute(date))
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
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
                composable(
                    route = Screen.Logging.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://logging" })
                ) {
                    LogPeriodScreen(
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.Details.route,
                    arguments = listOf(navArgument("date") { type = NavType.LongType })
                ) {
                    LogDetailsScreen(
                        onBack = { navController.popBackStack() },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
            }
        }
    }
}