package com.transitkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.core.view.WindowCompat
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.transitkit.app.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitKitTheme
import com.transitkit.app.config.TransitTheme
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.transitkit.app.ui.alerts.AlertDetailScreen
import com.transitkit.app.ui.alerts.AlertListScreen
import com.transitkit.app.ui.alerts.AlertToastHost
import com.transitkit.app.ui.home.HomeScreen
import com.transitkit.app.ui.linee.LineeScreen
import com.transitkit.app.ui.mappa.MappaScreen
import com.transitkit.app.ui.orari.OrariScreen
import com.transitkit.app.ui.orari.StopDetailScreen
import com.transitkit.app.ui.orari.TripDetailScreen
import com.transitkit.app.ui.orari.LineDetailScreen
import com.transitkit.app.ui.info.FareInfoScreen
import com.transitkit.app.ui.info.OperatorInfoScreen
import com.transitkit.app.ui.servizi.AccessibilityInfoScreen
import com.transitkit.app.ui.servizi.ContactInfoScreen
import com.transitkit.app.ui.servizi.ServiceDetailScreen
import com.transitkit.app.ui.servizi.ServiziScreen
import com.transitkit.app.ui.settings.AboutScreen
import com.transitkit.app.ui.settings.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import javax.inject.Inject

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Orari : Screen("orari")
    object Linee : Screen("linee_graph")
    object Mappa : Screen("mappa")
    object Servizi : Screen("servizi")
}

private const val ROUTE_LINEE_ROOT = "linee"
private const val ROUTE_SETTINGS_FROM_HOME = "settings_from_home"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var operatorConfig: OperatorConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar: paint the surface colour the content uses and draw dark icons so
        // the old flat grey band disappears. NOT edge-to-edge (project rule).
        val lightBg = 0xFFF5F7FA.toInt()
        window.statusBarColor = lightBg
        WindowCompat.getInsetsController(window, window.decorView)
            .isAppearanceLightStatusBars = true
        setContent {
            TransitKitTheme(config = operatorConfig) {
                TransitKitNavigation(operatorConfig = operatorConfig)
            }
        }
    }
}

@Composable
fun TransitKitNavigation(operatorConfig: OperatorConfig) {
    val navController = rememberNavController()
    val haptic = LocalHapticFeedback.current
    val colors = TransitTheme.colors
    val labelHome    = stringResource(R.string.tab_home)
    val labelOrari   = stringResource(R.string.tab_orari)
    val labelLinee   = stringResource(R.string.tab_linee)
    val labelMappa   = stringResource(R.string.tab_mappa)
    val labelServizi = stringResource(R.string.tab_servizi)

    val items = buildList {
        add(Triple(Screen.Home,    LucideIcons.LayoutDashboard, labelHome))
        add(Triple(Screen.Orari,   LucideIcons.Clock,           labelOrari))
        add(Triple(Screen.Linee,   LucideIcons.Route,           labelLinee))
        if (operatorConfig.features.enableMap) {
            add(Triple(Screen.Mappa, LucideIcons.Map, labelMappa))
        }
        add(Triple(Screen.Servizi, LucideIcons.Info,            labelServizi))
    }

    // Drilled-in destinations (detail screens, settings) must NOT show the root bottom nav.
    val tabRoutes = remember(items) { items.map { it.first.route }.toSet() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    val showBottomBar = currentRoute in tabRoutes

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = colors.tabBarBg) {
                    items.forEach { (screen, icon, label) ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            icon = { Icon(painterResource(icon), contentDescription = label) },
                            label = { Text(label, maxLines = 1) },
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = colors.accent,
                                selectedTextColor = colors.accent,
                                indicatorColor = colors.accent.copy(alpha = 0.15f),
                                unselectedIconColor = colors.tabInactive,
                                unselectedTextColor = colors.tabInactive,
                            ),
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier,
            enterTransition = {
                slideInHorizontally(
                    initialOffsetX = { it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                ) + fadeIn(tween(300, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(tween(200, easing = FastOutSlowInEasing))
            },
            popEnterTransition = {
                fadeIn(tween(200, easing = FastOutSlowInEasing))
            },
            popExitTransition = {
                slideOutHorizontally(
                    targetOffsetX = { it / 4 },
                    animationSpec = tween(300, easing = FastOutSlowInEasing),
                ) + fadeOut(tween(300, easing = FastOutSlowInEasing))
            },
        ) {
            // ── Home ────────────────────────────────────────────────────────────
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToOrari = {
                        navController.navigate(Screen.Orari.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToLinee = {
                        navController.navigate(Screen.Linee.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToMappa = {
                        navController.navigate(Screen.Mappa.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(ROUTE_SETTINGS_FROM_HOME)
                    },
                    onNavigateToAlerts = {
                        navController.navigate("alerts")
                    },
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                )
            }

            // ── Settings (pushed from Home) ─────────────────────────────────────
            composable(ROUTE_SETTINGS_FROM_HOME) {
                SettingsScreen(
                    onNavigateToAbout = { navController.navigate("about_from_home") },
                    onNavigateToOrari = {
                        navController.popBackStack()
                        navController.navigate(Screen.Orari.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                )
            }
            composable("about_from_home") {
                AboutScreen(
                    config = operatorConfig,
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Orari (fermate only) ─────────────────────────────────────────────
            composable(Screen.Orari.route) {
                OrariScreen(
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                    onNavigateToLine = { routeId ->
                        val encodedId = URLEncoder.encode(routeId, StandardCharsets.UTF_8.name())
                        navController.navigate("line/$encodedId")
                    },
                )
            }

            // ── Linee ────────────────────────────────────────────────────────────
            composable(Screen.Linee.route) {
                LineeScreen(
                    onNavigateToLine = { routeId ->
                        val encodedId = URLEncoder.encode(routeId, StandardCharsets.UTF_8.name())
                        navController.navigate("line/$encodedId")
                    },
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                )
            }

            // ── Shared detail screens (stops, lines, trips) ──────────────────────
            composable(
                route = "line/{routeId}",
                arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://line/{routeId}" }),
            ) { backStackEntry ->
                val routeId = backStackEntry.arguments?.getString("routeId") ?: return@composable
                LineDetailScreen(
                    routeId = URLDecoder.decode(routeId, StandardCharsets.UTF_8.name()),
                    onBack = { navController.popBackStack() },
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                    onNavigateToAlert = { alertId ->
                        val encoded = URLEncoder.encode(alertId, StandardCharsets.UTF_8.name())
                        navController.navigate("alert_detail/$encoded")
                    },
                    onNavigateToMap = { rId ->
                        val encoded = URLEncoder.encode(rId, StandardCharsets.UTF_8.name())
                        navController.navigate("mappa_line/$encoded") {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                )
            }
            composable(
                route = "mappa_line/{routeId}",
                arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val routeId = URLDecoder.decode(
                    backStackEntry.arguments?.getString("routeId") ?: "",
                    StandardCharsets.UTF_8.name(),
                )
                MappaScreen(
                    onNavigateToStop = { stopId ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId")
                    },
                    initialRouteId = routeId,
                )
            }
            composable(
                route = "stop/{stopId}?name={stopName}",
                arguments = listOf(
                    navArgument("stopId") { type = NavType.StringType },
                    navArgument("stopName") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://stop/{stopId}" },
                    navDeepLink { uriPattern = "transitkit://stop/{stopId}/schedule" },
                ),
            ) { backStackEntry ->
                val stopId = backStackEntry.arguments?.getString("stopId") ?: return@composable
                val stopName = backStackEntry.arguments?.getString("stopName")
                StopDetailScreen(
                    stopId = URLDecoder.decode(stopId, StandardCharsets.UTF_8.name()),
                    stopName = stopName?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.name()) } ?: stopId,
                    onBack = { navController.popBackStack() },
                    onNavigateToTrip = { tripId, fromStopId, routeColor, headsign, routeName ->
                        val encodedTripId = URLEncoder.encode(tripId, StandardCharsets.UTF_8.name())
                        val encodedFromStopId = URLEncoder.encode(fromStopId, StandardCharsets.UTF_8.name())
                        val encodedColor = URLEncoder.encode(routeColor, StandardCharsets.UTF_8.name())
                        val encodedHeadsign = URLEncoder.encode(headsign, StandardCharsets.UTF_8.name())
                        val encodedRouteName = URLEncoder.encode(routeName, StandardCharsets.UTF_8.name())
                        navController.navigate("trip/$encodedTripId?fromStopId=$encodedFromStopId&routeColor=$encodedColor&headsign=$encodedHeadsign&routeName=$encodedRouteName")
                    },
                    onNavigateToAlert = { alertId ->
                        val encoded = URLEncoder.encode(alertId, StandardCharsets.UTF_8.name())
                        navController.navigate("alert_detail/$encoded")
                    },
                )
            }
            composable(
                route = "trip/{tripId}?fromStopId={fromStopId}&routeColor={routeColor}&headsign={headsign}&routeName={routeName}",
                arguments = listOf(
                    navArgument("tripId") { type = NavType.StringType },
                    navArgument("fromStopId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("routeColor") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("headsign") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("routeName") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://trip/{tripId}/{fromStopId}/{routeName}" }),
            ) { backStackEntry ->
                val tripId = backStackEntry.arguments?.getString("tripId") ?: return@composable
                TripDetailScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToStop = { stopId ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId")
                    },
                )
            }

            // ── Mappa ────────────────────────────────────────────────────────────
            composable(
                Screen.Mappa.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://map" },
                    navDeepLink { uriPattern = "transitkit://map/vehicle/{vehicleId}" },
                ),
                arguments = listOf(
                    navArgument("vehicleId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                MappaScreen(
                    onNavigateToStop = { stopId ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId")
                    },
                    onOpenTripDetail = { tripId, fromStopId, routeName, routeColor ->
                        navController.navigate("trip/$tripId?fromStopId=$fromStopId&routeName=$routeName&routeColor=$routeColor")
                    },
                    initialVehicleId = vehicleId,
                )
            }

            // ── Servizi ──────────────────────────────────────────────────────────
            composable(Screen.Servizi.route) {
                ServiziScreen(
                    onNavigateToService = { serviceId ->
                        val encoded = URLEncoder.encode(serviceId, StandardCharsets.UTF_8.name())
                        navController.navigate("service_detail/$encoded")
                    },
                    onNavigateToFares = { _, _ ->
                        navController.navigate("fare_info")
                    },
                    onNavigateToAccessibility = {
                        navController.navigate("accessibility_info")
                    },
                    onNavigateToContact = {
                        navController.navigate("contact_info")
                    },
                    onNavigateToOperator = { _ ->
                        navController.navigate("operator_info")
                    },
                )
            }

            composable(
                route = "service_detail/{serviceId}",
                arguments = listOf(navArgument("serviceId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("serviceId") ?: return@composable
                ServiceDetailScreen(
                    serviceId = URLDecoder.decode(id, StandardCharsets.UTF_8.name()),
                    onBack = { navController.popBackStack() },
                    onNavigateToMappa = {
                        navController.navigate(Screen.Mappa.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }

            composable("contact_info") {
                ContactInfoScreen(onBack = { navController.popBackStack() })
            }

            composable("accessibility_info") {
                AccessibilityInfoScreen(onBack = { navController.popBackStack() })
            }

            composable("fare_info") {
                val fares = operatorConfig.fares
                if (fares != null) {
                    FareInfoScreen(
                        fares = fares,
                        operatorUrl = operatorConfig.url.takeIf { it.isNotBlank() },
                        onBack = { navController.popBackStack() },
                    )
                }
            }

            composable("operator_info") {
                OperatorInfoScreen(
                    config = operatorConfig,
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Service alerts ───────────────────────────────────────────────────
            composable("alerts") {
                AlertListScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToAlert = { alertId ->
                        val encoded = URLEncoder.encode(alertId, StandardCharsets.UTF_8.name())
                        navController.navigate("alert_detail/$encoded")
                    },
                )
            }

            composable(
                route = "alert_detail/{alertId}",
                arguments = listOf(navArgument("alertId") { type = NavType.StringType }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("alertId") ?: return@composable
                AlertDetailScreen(
                    alertId = URLDecoder.decode(id, StandardCharsets.UTF_8.name()),
                    onBack = { navController.popBackStack() },
                )
            }
        }

        // Top-of-stack toast overlay — visible across all tabs.
        AlertToastHost(
            onNavigateToAlert = { alertId ->
                val encoded = URLEncoder.encode(alertId, StandardCharsets.UTF_8.name())
                navController.navigate("alert_detail/$encoded")
            },
        )
        }
    }
}
