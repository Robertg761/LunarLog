package com.lunarlog.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.consumeWindowInsets
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.lunarlog.ui.loghistory.LogHistoryScreen
import com.lunarlog.ui.loglist.LogListScreen
import com.lunarlog.ui.logperiod.LogPeriodScreen
import com.lunarlog.ui.onboarding.OnboardingScreen
import com.lunarlog.ui.periodhistory.PeriodDetailScreen
import com.lunarlog.ui.periodhistory.PeriodHistoryScreen
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
    object Analysis : Screen("analysis", Icons.Default.Timeline, Icons.Outlined.Timeline, "Analysis")
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

private val bottomNavItems = listOf(Screen.Home, Screen.PeriodHistory, Screen.Calendar, Screen.Analysis)

private val tabRoutes: Set<String> = bottomNavItems.map { it.route }.toSet()

private fun isTabRoute(route: String?): Boolean = route != null && route in tabRoutes

// Motion has exactly two shapes in this app.
//
// 1. Push / pop (drill in and back out) — a directional slide plus a short fade. Declared once as
//    the NavHost defaults so every destination that does not opt out gets it, instead of silently
//    inheriting NavHost's 700ms cross-fade.
// 2. Lateral tab switches — an M3 fade-through (outgoing fades out, then the incoming one fades and
//    scales up). Deliberately *not* a slide: the four tabs are siblings, not a hierarchy, so a
//    drill-in slide would misrepresent where you went.
private const val PUSH_DURATION_MS = 300
private const val PUSH_FADE_MS = 150
private const val TAB_FADE_OUT_MS = 90
private const val TAB_FADE_IN_MS = 210

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushEnterTransition(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(PUSH_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(PUSH_FADE_MS, easing = LinearEasing))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.pushExitTransition(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(PUSH_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(PUSH_FADE_MS, easing = LinearEasing))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popEnterTransition(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(PUSH_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeIn(animationSpec = tween(PUSH_FADE_MS, easing = LinearEasing))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.popExitTransition(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(PUSH_DURATION_MS, easing = FastOutSlowInEasing)
    ) + fadeOut(animationSpec = tween(PUSH_FADE_MS, easing = LinearEasing))

/**
 * Returns null — i.e. "fall through to the NavHost default push/pop" — whenever either endpoint is
 * not a bottom-nav tab, so a Home -> Settings push animates as a push and only tab -> tab gets the
 * fade-through.
 */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabEnterTransition(): EnterTransition? {
    if (!isTabRoute(initialState.destination.route) || !isTabRoute(targetState.destination.route)) {
        return null
    }
    val spec = tween<Float>(
        durationMillis = TAB_FADE_IN_MS,
        delayMillis = TAB_FADE_OUT_MS,
        easing = LinearOutSlowInEasing
    )
    return fadeIn(animationSpec = spec) +
        scaleIn(
            initialScale = 0.92f,
            animationSpec = tween(
                durationMillis = TAB_FADE_IN_MS,
                delayMillis = TAB_FADE_OUT_MS,
                easing = LinearOutSlowInEasing
            )
        )
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.tabExitTransition(): ExitTransition? {
    if (!isTabRoute(initialState.destination.route) || !isTabRoute(targetState.destination.route)) {
        return null
    }
    // Exits fully before the enter starts (the enter is delayed by exactly this duration), so the
    // Scaffold background never shows through mid-transition.
    return fadeOut(animationSpec = tween(TAB_FADE_OUT_MS, easing = LinearEasing))
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun LunarLogNavGraph(
    startDestination: String = Screen.Home.route,
    isUpdateAvailable: Boolean = false,
    pendingDeepLink: String? = null,
    onDeepLinkHandled: (Boolean) -> Unit = {},
    onInstallUpdate: () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = isTabRoute(currentDestination?.route)

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
                // Transparent, like the app bars: the bar sits directly on the screen background so
                // there is no tonal seam where the content ends. Content is never underneath it —
                // the Scaffold's innerPadding is applied (and consumed) below.
                NavigationBar(
                    containerColor = Color.Transparent,
                    tonalElevation = 0.dp
                ) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val icon = if (selected) screen.selectedIcon!! else screen.unselectedIcon!!
                        NavigationBarItem(
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            ),
                            icon = {
                                if (screen == Screen.Home && isUpdateAvailable) {
                                    androidx.compose.material3.BadgedBox(
                                        badge = {
                                            androidx.compose.material3.Badge(
                                                modifier = Modifier.semantics {
                                                    contentDescription = "Update available"
                                                }
                                            )
                                        }
                                    ) {
                                        // The label below is the accessible name; describing the
                                        // icon too makes TalkBack say it twice.
                                        Icon(imageVector = icon, contentDescription = null)
                                    }
                                } else {
                                    Icon(imageVector = icon, contentDescription = null)
                                }
                            },
                            label = { Text(screen.label!!) },
                            alwaysShowLabel = true,
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
        },
        // The Scaffold owns snackbar placement, so it clears the bottom bar (and the system nav
        // bar inset) automatically instead of an overlay guessing at a fixed offset.
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        // padding() offsets but does not consume, so every inner Scaffold and TopAppBar was
        // re-applying the full system-bar inset on top of this one.
        SharedTransitionLayout(
            modifier = Modifier
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
        ) {
            NavHost(
                navController = navController,
                startDestination = startDestination,
                enterTransition = { pushEnterTransition() },
                exitTransition = { pushExitTransition() },
                popEnterTransition = { popEnterTransition() },
                popExitTransition = { popExitTransition() }
            ) {
                composable(
                    route = Screen.Home.route,
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabEnterTransition() },
                    popExitTransition = { tabExitTransition() }
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
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabEnterTransition() },
                    popExitTransition = { tabExitTransition() }
                ) {
                    // CalendarScreen never registers a shared element, so the scopes are not passed.
                    CalendarScreen(
                        onDayClicked = { date ->
                            navController.navigate(Screen.Details.createRoute(date))
                        }
                    )
                }
                composable(
                    route = Screen.Analysis.route,
                    deepLinks = listOf(navDeepLink { uriPattern = "lunarlog://analysis" }),
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabEnterTransition() },
                    popExitTransition = { tabExitTransition() }
                ) {
                    AnalysisScreen(
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
                    LogHistoryScreen(
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
                    enterTransition = { tabEnterTransition() },
                    exitTransition = { tabExitTransition() },
                    popEnterTransition = { tabEnterTransition() },
                    popExitTransition = { tabExitTransition() }
                ) {
                    // PeriodHistoryScreen never registers a shared element, so the scopes are not
                    // passed.
                    PeriodHistoryScreen(
                        onCycleClick = { cycleId ->
                            navController.navigate(Screen.PeriodDetail.createRoute(cycleId))
                        },
                        onAddPeriodClick = {
                            navController.navigate(Screen.Logging.route)
                        }
                    )
                }
                composable(
                    route = Screen.PeriodDetail.route,
                    arguments = listOf(navArgument("cycleId") { type = NavType.IntType })
                ) {
                    PeriodDetailScreen(
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
