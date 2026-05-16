package com.transitkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.core.view.WindowCompat
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import com.transitkit.app.ui.components.LocalHideBottomBarRequests
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.transitkit.app.ui.alerts.AlertDetailScreen
import com.transitkit.app.ui.alerts.AlertListScreen
import com.transitkit.app.ui.alerts.AlertToastHost
import com.transitkit.app.ui.alerts.AlertsViewModel
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.transitkit.app.ui.planner.JourneyDetailScreen
import com.transitkit.app.ui.planner.LocationPickerMapScreen
import com.transitkit.app.ui.planner.LocationPickerScreen
import com.transitkit.app.ui.planner.PlannerScreen
import com.transitkit.app.ui.planner.PlannerViewModel
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
    /** Service info — no longer in the tab bar, reachable from Home. */
    object Servizi : Screen("servizi")
    /** Service alerts — global list with Mine/All filter. Tab bar slot
     *  carries a live counter of currently-active alerts. */
    object Avvisi : Screen("alerts")
    object Planner : Screen("planner")
}

private const val ROUTE_LINEE_ROOT = "linee"
private const val ROUTE_SETTINGS_FROM_HOME = "settings_from_home"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var operatorConfig: OperatorConfig

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status + gesture nav bars share the surface colour so the chrome reads as a
        // single light frame instead of a dark handle clashing with a light top band.
        // NOT edge-to-edge (project rule).
        val lightBg = 0xFFF5F7FA.toInt()
        window.statusBarColor = lightBg
        window.navigationBarColor = lightBg
        WindowCompat.getInsetsController(window, window.decorView).apply {
            isAppearanceLightStatusBars = true
            isAppearanceLightNavigationBars = true
        }
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
    // Scoped to the activity — shared between Planner tab and JourneyDetail push.
    val plannerViewModel: PlannerViewModel = hiltViewModel()
    val labelHome    = stringResource(R.string.tab_home)
    val labelOrari   = stringResource(R.string.tab_orari)
    val labelLinee   = stringResource(R.string.tab_linee)
    val labelMappa   = stringResource(R.string.tab_mappa)
    val labelAvvisi  = stringResource(R.string.tab_alerts)

    val items = buildList {
        add(Triple(Screen.Home,    LucideIcons.LayoutDashboard, labelHome))
        add(Triple(Screen.Orari,   LucideIcons.Clock,           labelOrari))
        add(Triple(Screen.Linee,   LucideIcons.Route,           labelLinee))
        if (operatorConfig.features.enableMap) {
            add(Triple(Screen.Mappa, LucideIcons.Map, labelMappa))
        }
        add(Triple(Screen.Avvisi,  LucideIcons.Bell,            labelAvvisi))
    }

    // Live counter of active alerts for the Avvisi tab badge.
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    val activeAlerts by alertsViewModel.alerts.collectAsState()
    val avvisiCount = activeAlerts.size

    // Drilled-in destinations (detail screens, settings) must NOT show the root bottom nav.
    val tabRoutes = remember(items) { items.map { it.first.route }.toSet() }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route
    // Full-screen overlays inside a tab destination can request the tab bar
    // be hidden (see [HideBottomBarWhileVisible]). Counter, not boolean,
    // so multiple overlays compose without races on dismiss.
    val hideBottomBarRequests = remember { mutableStateOf(0) }
    val showBottomBar = currentRoute in tabRoutes && hideBottomBarRequests.value == 0

    CompositionLocalProvider(LocalHideBottomBarRequests provides hideBottomBarRequests) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = colors.tabBarBg) {
                    items.forEach { (screen, icon, label) ->
                        val isSelected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                        val showBadge = screen == Screen.Avvisi && avvisiCount > 0
                        NavigationBarItem(
                            icon = {
                                if (showBadge) {
                                    BadgedBox(
                                        badge = {
                                            Badge { Text(avvisiCount.toString(), maxLines = 1) }
                                        },
                                    ) {
                                        Icon(painterResource(icon), contentDescription = label)
                                    }
                                } else {
                                    Icon(painterResource(icon), contentDescription = label)
                                }
                            },
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
                                indicatorColor = colors.accent.copy(alpha = 0.22f),
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
                    onNavigateToAbout = {
                        navController.navigate("about_from_home")
                    },
                    onNavigateToAlerts = {
                        navController.navigate(Screen.Avvisi.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToServizi = {
                        navController.navigate(Screen.Servizi.route)
                    },
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                    onNavigateToPlanner = {
                        navController.navigate(Screen.Planner.route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToLocationPicker = { role ->
                        navController.navigate("location_picker/$role/home")
                    },
                    plannerViewModel = plannerViewModel,
                )
            }

            // ── Settings (pushed from Home) ─────────────────────────────────────
            composable(ROUTE_SETTINGS_FROM_HOME) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
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
                    onNavigateToTrip = { tripId, fromStopId, routeColor, headsign, routeName ->
                        val encodedTripId = URLEncoder.encode(tripId, StandardCharsets.UTF_8.name())
                        val encodedFromStopId = URLEncoder.encode(fromStopId, StandardCharsets.UTF_8.name())
                        val encodedColor = URLEncoder.encode(routeColor, StandardCharsets.UTF_8.name())
                        val encodedHeadsign = URLEncoder.encode(headsign, StandardCharsets.UTF_8.name())
                        val encodedRouteName = URLEncoder.encode(routeName, StandardCharsets.UTF_8.name())
                        navController.navigate("trip/$encodedTripId?fromStopId=$encodedFromStopId&routeColor=$encodedColor&headsign=$encodedHeadsign&routeName=$encodedRouteName")
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

            // ── Planner ──────────────────────────────────────────────────────────
            composable(Screen.Planner.route) {
                PlannerScreen(
                    onBack = {
                        // Pop back to Home; if Home isn't in the stack, navigate there.
                        if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    inclusive = false
                                }
                                launchSingleTop = true
                            }
                        }
                    },
                    onNavigateToJourneyDetail = { journey ->
                        plannerViewModel.selectJourney(journey)
                        navController.navigate("journey_detail")
                    },
                    onNavigateToLocationPicker = { role ->
                        navController.navigate("location_picker/$role/planner")
                    },
                    viewModel = plannerViewModel,
                )
            }

            // ── Location picker (origin / destination) ───────────────────────────
            composable(
                route = "location_picker/{role}/{source}",
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: return@composable
                val source = backStackEntry.arguments?.getString("source") ?: return@composable
                LocationPickerScreen(
                    role = role,
                    source = source,
                    plannerViewModel = plannerViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToMapPicker = {
                        navController.navigate("location_picker_map/$role/$source")
                    },
                )
            }

            // ── Map-based location picker ────────────────────────────────────────
            composable(
                route = "location_picker_map/{role}/{source}",
                arguments = listOf(
                    navArgument("role") { type = NavType.StringType },
                    navArgument("source") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val role = backStackEntry.arguments?.getString("role") ?: return@composable
                val source = backStackEntry.arguments?.getString("source") ?: return@composable
                LocationPickerMapScreen(
                    role = role,
                    source = source,
                    plannerViewModel = plannerViewModel,
                    onConfirm = {
                        val targetRoute = if (source == "home") Screen.Home.route else Screen.Planner.route
                        navController.popBackStack(targetRoute, inclusive = false)
                    },
                    onBack = { navController.popBackStack() },
                )
            }

            composable(route = "journey_detail") {
                val journey = plannerViewModel.selectedJourney.collectAsState().value
                    ?: run { navController.popBackStack(); return@composable }
                val origin = plannerViewModel.origin.collectAsState().value
                val destination = plannerViewModel.destination.collectAsState().value
                val userLocation = plannerViewModel.currentLocation.collectAsState().value
                JourneyDetailScreen(
                    journey = journey,
                    onBack = { navController.popBackStack() },
                    originName = origin?.name,
                    destinationName = destination?.name,
                    userLocation = userLocation,
                )
            }

            // ── Mappa ────────────────────────────────────────────────────────────
            composable(
                Screen.Mappa.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://map" },
                    navDeepLink { uriPattern = "transitkit://map/vehicle/{vehicleId}" },
                    navDeepLink { uriPattern = "transitkit://map/stop/{previewStopId}" },
                ),
                arguments = listOf(
                    navArgument("vehicleId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("previewStopId") { type = NavType.StringType; nullable = true; defaultValue = null },
                ),
            ) { backStackEntry ->
                val vehicleId = backStackEntry.arguments?.getString("vehicleId")
                val previewStopId = backStackEntry.arguments?.getString("previewStopId")
                MappaScreen(
                    onNavigateToStop = { stopId ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId")
                    },
                    onOpenTripDetail = { tripId, fromStopId, routeName, routeColor ->
                        navController.navigate("trip/$tripId?fromStopId=$fromStopId&routeName=$routeName&routeColor=$routeColor")
                    },
                    initialVehicleId = vehicleId,
                    initialPreviewStopId = previewStopId,
                )
            }

            // ── Servizi (drilled-in from Home — no longer a tab) ────────────────
            composable(Screen.Servizi.route) {
                ServiziScreen(
                    onBack = { navController.popBackStack() },
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

            // ── Service alerts (root tab) ────────────────────────────────────────
            composable(Screen.Avvisi.route) {
                AlertListScreen(
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
}
