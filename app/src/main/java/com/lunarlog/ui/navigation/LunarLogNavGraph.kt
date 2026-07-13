package com.lunarlog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavBackStackEntry
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
import com.lunarlog.ui.loglist.LogListScreen
import com.lunarlog.ui.logperiod.LogPeriodScreen
import com.lunarlog.ui.onboarding.OnboardingScreen
import com.lunarlog.ui.settings.SettingsScreen
import java.time.LocalDate
import java.net.URI

sealed class Screen(
    val route: String,
    val selectedIcon: ImageVector? = null,
    val unselectedIcon: ImageVector? = null,
    val label: String? = null
) {
    object Home : Screen("home", Icons.Default.Home, Icons.Outlined.Home, "Home")
    object PeriodHistory : Screen("period_history", Icons.Default.WaterDrop, Icons.Outlined.WaterDrop, "Periods")
    object Calendar : Screen("calendar", Icons.Default.DateRange, Icons.Outlined.DateRange, "Calendar")
    object Analysis : Screen("analysis", Icons.Default.Timeline, Icons.Outlined.Timeline, "Insights")
    object Logging : Screen("logging")
    object Details : Screen("details/{date}") {
        fun createRoute(date: Long) = "details/$date"
    }
    object PeriodDetail : Screen("period_detail/{cycleId}") {
        fun createRoute(cycleId: Int) = "period_detail/$cycleId"
    }
    object Settings : Screen("settings")
    object LogHistory : Screen("log_history")
    object Onboarding : Screen("onboarding")
}

private fun getScreenOrder(route: String?): Int {
    return when (route) {
        Screen.Home.route -> 0
        Screen.PeriodHistory.route -> 1
        Screen.Calendar.route -> 2
        Screen.Analysis.route -> 3
        else -> -1
    }
}

private val macFadeSpec = tween<Float>(
    durationMillis = 140,
    easing = FastOutSlowInEasing
)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.macEnterTransition(): EnterTransition? {
    val initial = getScreenOrder(initialState.destination.route)
    val target = getScreenOrder(targetState.destination.route)
    if (initial == -1 || target == -1) return null

    return fadeIn(animationSpec = macFadeSpec)
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.macExitTransition(): ExitTransition? {
    val initial = getScreenOrder(initialState.destination.route)
    val target = getScreenOrder(targetState.destination.route)
    if (initial == -1 || target == -1) return null

    return fadeOut(animationSpec = tween(durationMillis = 90, easing = FastOutSlowInEasing))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LunarLogNavGraph(
    startDestination: String = Screen.Home.route,
    isUpdateAvailable: Boolean = false,
    pendingDeepLink: String? = null,
    onDeepLinkHandled: (Boolean) -> Unit = {},
    onInstallUpdate: () -> Unit = {}
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val bottomNavItems = listOf(Screen.Home, Screen.PeriodHistory, Screen.Calendar, Screen.Analysis)
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    LaunchedEffect(pendingDeepLink, startDestination) {
        val link = pendingDeepLink ?: return@LaunchedEffect
        if (startDestination == Screen.Onboarding.route) return@LaunchedEffect

        val route = lunarLogRouteForDeepLink(link)
        if (route != null) {
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
        onDeepLinkHandled(route != null)
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.98f),
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.primary,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                if (screen == Screen.Home && isUpdateAvailable) {
                                    androidx.compose.material3.BadgedBox(
                                        badge = { androidx.compose.material3.Badge() }
                                    ) {
                                        Icon(
                                            imageVector = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                            contentDescription = screen.label
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!,
                                        contentDescription = screen.label
                                    )
                                }
                            },
                            label = { Text(screen.label!!) },
                            selected = selected,
                            onClick = {
                                if (!selected) {
                                    navController.navigate(screen.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
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
                    enterTransition = { macEnterTransition() },
                    exitTransition = { macExitTransition() }
                ) {
                    HomeScreen(
                        onLogDetailsClicked = {
                            val today = LocalDate.now().toEpochDay()
                            navController.navigate(Screen.Details.createRoute(today))
                        },
                        onSettingsClicked = { navController.navigate(Screen.Settings.route) },
                        isUpdateAvailable = isUpdateAvailable,
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = Screen.Calendar.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://calendar" }),
                    enterTransition = { macEnterTransition() },
                    exitTransition = { macExitTransition() }
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
                    enterTransition = { macEnterTransition() },
                    exitTransition = { macExitTransition() }
                ) {
                    AnalysisScreen(
                        onBack = { navController.popBackStack() },
                        onHistoryClick = { navController.navigate(Screen.LogHistory.route) }
                    )
                }
                composable(Screen.Settings.route) {
                    SettingsScreen(
                        onBack = { navController.popBackStack() },
                        isUpdateAvailable = isUpdateAvailable,
                        onInstallUpdate = onInstallUpdate
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
                    arguments = listOf(navArgument("date") { type = NavType.LongType }),
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://details/{date}" })
                ) { backStackEntry ->
                    val date = backStackEntry.arguments?.getLong("date") ?: LocalDate.now().toEpochDay()
                    LogListScreen(
                        date = date,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(
                    route = Screen.PeriodHistory.route,
                    enterTransition = { macEnterTransition() },
                    exitTransition = { macExitTransition() }
                ) {
                    com.lunarlog.ui.periodhistory.PeriodHistoryScreen(
                        onCycleClick = { cycleId ->
                            navController.navigate(Screen.PeriodDetail.createRoute(cycleId))
                        },
                        onAddPeriodClick = {
                            navController.navigate(Screen.Logging.route)
                        },
                        sharedTransitionScope = this@SharedTransitionLayout,
                        animatedVisibilityScope = this
                    )
                }
                composable(
                    route = Screen.PeriodDetail.route,
                    arguments = listOf(navArgument("cycleId") { type = NavType.IntType })
                ) {
                    com.lunarlog.ui.periodhistory.PeriodDetailScreen(
                        onBack = { navController.popBackStack() },
                        onDayClick = { date ->
                            navController.navigate(Screen.Details.createRoute(date))
                        }
                    )
                }
            }
        }
    }
}

internal fun lunarLogRouteForDeepLink(rawLink: String): String? {
    val uri = try {
        URI(rawLink)
    } catch (_: Exception) {
        return null
    }
    if (!uri.scheme.equals("lunarlog", ignoreCase = true) ||
        uri.userInfo != null ||
        uri.port != -1 ||
        uri.rawQuery != null ||
        uri.rawFragment != null
    ) return null

    val path = uri.path.orEmpty()
    return when (uri.host?.lowercase()) {
        "calendar" -> Screen.Calendar.route.takeIf { path.isEmpty() }
        "analysis" -> Screen.Analysis.route.takeIf { path.isEmpty() }
        "logging" -> Screen.Logging.route.takeIf { path.isEmpty() }
        "details" -> {
            val epochDayText = path.removePrefix("/")
            if (path != "/$epochDayText" || epochDayText.isEmpty() || "/" in epochDayText) {
                return null
            }
            val epochDay = epochDayText.toLongOrNull() ?: return null
            try {
                LocalDate.ofEpochDay(epochDay)
            } catch (_: Exception) {
                return null
            }
            Screen.Details.createRoute(epochDay)
        }
        else -> null
    }
}
