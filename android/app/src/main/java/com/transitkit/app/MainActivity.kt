package com.transitkit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import com.transitkit.app.R
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
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
import com.transitkit.app.data.AppUpdateChecker
import com.transitkit.app.ui.alerts.AlertDetailScreen
import com.transitkit.app.ui.alerts.AlertListScreen
import com.transitkit.app.ui.alerts.AlertToastHost
import com.transitkit.app.ui.alerts.AlertsViewModel
import com.transitkit.app.ui.home.HomeScreen
import com.transitkit.app.ui.update.ForceUpdateScreen
import com.transitkit.app.ui.linee.LineeScreen
import com.transitkit.app.ui.mappa.MappaScreen
import com.transitkit.app.ui.orari.OrariScreen
import com.transitkit.app.ui.orari.StopDetailScreen
import com.transitkit.app.ui.orari.TripDetailScreen
import com.transitkit.app.ui.orari.LineDetailScreen
import com.transitkit.app.ui.info.FareInfoScreen
import com.transitkit.app.ui.info.OperatorInfoScreen
import com.transitkit.app.ui.onboarding.OnboardingScreen
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
        // Status/gesture bar: colori e appearance sono gestiti theme-aware
        // (light + dark) dal SideEffect in TransitKitTheme (AppTheme.kt).
        // NOT edge-to-edge (project rule).
        handleMapDeepLink(intent)
        handlePlannerDeepLink(intent)
        handleSearchDeepLink(intent)
        // Controllo versione al lancio (fire-and-forget, nessun I/O — legge solo config in-memory).
        AppUpdateChecker.check(
            context = applicationContext,
            config = operatorConfig,
            language = java.util.Locale.getDefault().language,
        )
        setContent {
            TransitKitTheme(config = operatorConfig) {
                TransitKitNavigation(operatorConfig = operatorConfig)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        handleMapDeepLink(intent)
        handlePlannerDeepLink(intent)
        handleSearchDeepLink(intent)
    }

    /**
     * `transitkit://planner?from=<name>&to=<name>&when=HH:MM` — pre-popola il
     * form Planner. `when` viene interpretato in operator-local TZ dal VM
     * (vedi PlannerViewModel.applyPendingPrefill), non in TZ device.
     */
    private fun handlePlannerDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "transitkit" || data.host != "planner") return
        val from = data.getQueryParameter("from")?.takeIf { it.isNotBlank() }
        val to = data.getQueryParameter("to")?.takeIf { it.isNotBlank() }
        val whenStr = data.getQueryParameter("when")?.takeIf { it.isNotBlank() }
        if (from == null && to == null && whenStr == null) return
        com.transitkit.app.ui.planner.PendingPlannerPrefillStore.set(
            com.transitkit.app.ui.planner.PendingPlannerPrefill(from = from, to = to, whenStr = whenStr)
        )
    }

    /**
     * `transitkit://search?q=<text>&scope=stops|lines` — pre-popola la search
     * Orari e seleziona la tab indicata.
     */
    private fun handleSearchDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "transitkit" || data.host != "search") return
        val q = data.getQueryParameter("q")?.takeIf { it.isNotBlank() } ?: return
        val scope = when (data.getQueryParameter("scope")?.lowercase()) {
            "stops" -> com.transitkit.app.ui.orari.OrariTab.STOPS
            "lines" -> com.transitkit.app.ui.orari.OrariTab.LINES
            else -> null
        }
        com.transitkit.app.ui.orari.PendingSearchPrefillStore.set(
            com.transitkit.app.ui.orari.PendingSearchPrefill(query = q, scope = scope)
        )
    }

    /**
     * Gestisce URL `transitkit://map?lat=&lng=&zoom=&pitch=`.
     * Settando lo store, [com.transitkit.app.ui.mappa.MappaScreen] lo collega
     * come Flow e applica `viewportState.flyTo`. Compose Navigation gestisce
     * solo path args nativamente — i query params li parsiamo qui a mano
     * (stesso pattern di Movete `handleDeepLinkIntent`).
     */
    private fun handleMapDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        if (data.scheme != "transitkit" || data.host != "map") return
        val lat = data.getQueryParameter("lat")?.toDoubleOrNull() ?: return
        val lng = data.getQueryParameter("lng")?.toDoubleOrNull() ?: return
        val zoom = data.getQueryParameter("zoom")?.toDoubleOrNull() ?: 16.0
        val pitch = data.getQueryParameter("pitch")?.toDoubleOrNull()
        com.transitkit.app.ui.mappa.PendingMapCameraStore.set(
            com.transitkit.app.ui.mappa.PendingMapCamera(lat, lng, zoom, pitch)
        )
    }
}

@OptIn(ExperimentalComposeUiApi::class)
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

    val mapEnabled = operatorConfig.features.enableMap
    val items = remember(mapEnabled, labelHome, labelOrari, labelLinee, labelMappa, labelAvvisi) {
        buildList {
            add(Triple(Screen.Home,    LucideIcons.LayoutDashboard, labelHome))
            add(Triple(Screen.Orari,   LucideIcons.Clock,           labelOrari))
            add(Triple(Screen.Linee,   LucideIcons.Route,           labelLinee))
            if (mapEnabled) {
                add(Triple(Screen.Mappa, LucideIcons.Map, labelMappa))
            }
            add(Triple(Screen.Avvisi,  LucideIcons.Bell,            labelAvvisi))
        }
    }

    // Live counter of active alerts for the Avvisi tab badge.
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    val activeAlerts by alertsViewModel.alerts.collectAsStateWithLifecycle()
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

    val operatorTz = remember(plannerViewModel.operatorTimezone) {
        java.util.TimeZone.getTimeZone(plannerViewModel.operatorTimezone)
    }

    // Force-update overlay: bloccante, nessuna interazione possibile con il resto dell'app.
    val forceUpdateState by AppUpdateChecker.state.collectAsStateWithLifecycle()
    val forcedUpdate = forceUpdateState as? AppUpdateChecker.Requirement.Forced
    if (forcedUpdate != null) {
        val context = androidx.compose.ui.platform.LocalContext.current
        ForceUpdateScreen(
            message = forcedUpdate.message,
            onUpdate = {
                AppUpdateChecker.openStore(context, forcedUpdate.storeUrl)
            },
        )
        return
    }

    CompositionLocalProvider(
        LocalHideBottomBarRequests provides hideBottomBarRequests,
        com.transitkit.app.ui.planner.LocalOperatorTimeZone provides operatorTz,
    ) {
    Scaffold(
        // testTagsAsResourceId: espone i Modifier.testTag come resource-id
        // nell'albero accessibility — senza, il matching `id:` di Maestro
        // sui tag Compose è inaffidabile (vede solo content-desc/testo).
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        bottomBar = {
            if (showBottomBar) {
                // Stacco dal contenuto (iOS parity: la UITabBar ha material +
                // hairline di sistema): ombra soffusa sul wrapper + hairline
                // top. Senza, la bar appoggiava flat sulla pagina.
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.shadow(elevation = 8.dp),
                ) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f),
                    )
                // windowInsets = WindowInsets(0) forces the bar to not reserve
                // extra bottom inset (we're not edge-to-edge). Without this the
                // last ~30px of the item's visual area sits below the bar's
                // own gesture region and Material3 silently drops taps there
                // — the symptom QA hit on label "Routes"/"Schedules".
                NavigationBar(
                    containerColor = colors.tabBarBg,
                    windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0),
                ) {
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
                                        Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(20.dp))
                                    }
                                } else {
                                    Icon(painterResource(icon), contentDescription = label, modifier = Modifier.size(20.dp))
                                }
                            },
                            label = { Text(label, maxLines = 1) },
                            alwaysShowLabel = true,
                            selected = isSelected,
                            onClick = {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            composable(
                Screen.Home.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://home" },
                    navDeepLink { uriPattern = "transitkit://favorites" },
                ),
            ) {
                HomeScreen(
                    onNavigateToOrari = {
                        navController.navigate(Screen.Orari.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToLinee = {
                        navController.navigate(Screen.Linee.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToMappa = {
                        navController.navigate(Screen.Mappa.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToSettings = {
                        navController.navigate(ROUTE_SETTINGS_FROM_HOME)
                    },
                    onNavigateToAlerts = {
                        navController.navigate(Screen.Avvisi.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                    onNavigateToLocationPicker = { role ->
                        navController.navigate("location_picker/$role/home")
                    },
                    onNavigateToAssign = { key ->
                        navController.navigate("location_picker_assign/$key")
                    },
                    plannerViewModel = plannerViewModel,
                )
            }

            // ── Settings (pushed from Home) ─────────────────────────────────────
            composable(
                ROUTE_SETTINGS_FROM_HOME,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://settings" },
                    navDeepLink { uriPattern = "transitkit://settings/notifications" },
                ),
            ) {
                SettingsScreen(
                    onBack = { navController.popBackStack() },
                    onNavigateToOrari = {
                        navController.popBackStack()
                        navController.navigate(Screen.Orari.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true; restoreState = true
                        }
                    },
                )
            }

            // ── Orari (fermate only) ─────────────────────────────────────────────
            composable(
                Screen.Orari.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://orari" },
                    navDeepLink { uriPattern = "transitkit://schedules" },
                    navDeepLink { uriPattern = "transitkit://search" },
                ),
            ) {
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
            composable(
                Screen.Linee.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://lines" },
                    navDeepLink { uriPattern = "transitkit://linee" },
                ),
            ) {
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
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState = false
                        }
                    },
                    onShowVehicleOnMap = { vehicleId ->
                        // Line vehicle card → open the map focused on the vehicle
                        // (same deep-link route the trip screen uses). Uri overload,
                        // NOT navigate(route:String) — see TripDetail wiring below.
                        val encoded = URLEncoder.encode(vehicleId, StandardCharsets.UTF_8.name())
                        navController.navigate(
                            android.net.Uri.parse("transitkit://map/vehicle/$encoded"),
                            androidx.navigation.navOptions { launchSingleTop = true },
                        )
                    },
                )
            }
            composable(
                route = "mappa_line/{routeId}",
                arguments = listOf(navArgument("routeId") { type = NavType.StringType }),
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://line/{routeId}/map" },
                    navDeepLink { uriPattern = "transitkit://line/{routeId}/map/{direction}" },
                    navDeepLink { uriPattern = "transitkit://map/route/{routeId}" },
                    navDeepLink { uriPattern = "transitkit://map/route/{routeId}/{direction}" },
                ),
            ) { backStackEntry ->
                val routeId = URLDecoder.decode(
                    backStackEntry.arguments?.getString("routeId") ?: "",
                    StandardCharsets.UTF_8.name(),
                )
                MappaScreen(
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                    initialRouteId = routeId,
                    onBack = { navController.popBackStack() },
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
                    onShowVehicleOnMap = { vehicleId ->
                        // Open the Mappa destination focused on this vehicle via its
                        // navDeepLink (transitkit://map/vehicle/{vehicleId} → initialVehicleId).
                        // MUST use the Uri overload: navigate(route:String) matches route
                        // PATTERNS, not navDeepLink uriPatterns, so passing the URI string
                        // there threw IllegalArgumentException and crashed the app.
                        val encoded = URLEncoder.encode(vehicleId, StandardCharsets.UTF_8.name())
                        navController.navigate(
                            android.net.Uri.parse("transitkit://map/vehicle/$encoded"),
                            androidx.navigation.navOptions { launchSingleTop = true },
                        )
                    },
                )
            }

            // ── Planner ──────────────────────────────────────────────────────────
            composable(
                Screen.Planner.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://planner" },
                ),
            ) {
                PlannerScreen(
                    onBack = {
                        // Pop back to Home; if Home isn't in the stack, navigate there.
                        if (!navController.popBackStack(Screen.Home.route, inclusive = false)) {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
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
                    onNavigateToAssign = { key ->
                        navController.navigate("location_picker_assign/$key")
                    },
                )
            }

            // ── Assign saved place (Casa / Lavoro) ───────────────────────────────
            composable(
                route = "location_picker_assign/{key}",
                arguments = listOf(
                    navArgument("key") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable
                LocationPickerScreen(
                    role = "origin",    // non rilevante in assign mode
                    source = "assign",
                    assignKey = key,
                    plannerViewModel = plannerViewModel,
                    onBack = { navController.popBackStack() },
                    onNavigateToMapPicker = {
                        navController.navigate("location_picker_map_assign/$key")
                    },
                    onNavigateToAssign = null, // nested assign non supportato
                )
            }

            // ── Map-based assign picker (Casa / Lavoro) ──────────────────────────
            composable(
                route = "location_picker_map_assign/{key}",
                arguments = listOf(
                    navArgument("key") { type = NavType.StringType },
                ),
            ) { backStackEntry ->
                val key = backStackEntry.arguments?.getString("key") ?: return@composable
                LocationPickerMapScreen(
                    role = "origin",
                    source = "assign",
                    assignKey = key,
                    plannerViewModel = plannerViewModel,
                    onConfirm = {
                        // Pop map + assign picker → ritorna alla schermata precedente (Home/Planner/Picker)
                        navController.popBackStack("location_picker_assign/$key", inclusive = true)
                    },
                    onBack = { navController.popBackStack() },
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
                val journey by plannerViewModel.selectedJourney.collectAsStateWithLifecycle()
                val origin by plannerViewModel.origin.collectAsStateWithLifecycle()
                val destination by plannerViewModel.destination.collectAsStateWithLifecycle()
                val userLocation by plannerViewModel.currentLocation.collectAsStateWithLifecycle()
                val resolvedJourney = journey
                    ?: run { navController.popBackStack(); return@composable }
                JourneyDetailScreen(
                    journey = resolvedJourney,
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
                    onNavigateToStop = { stopId, stopName ->
                        val encodedId = URLEncoder.encode(stopId, StandardCharsets.UTF_8.name())
                        val encodedName = URLEncoder.encode(stopName, StandardCharsets.UTF_8.name())
                        navController.navigate("stop/$encodedId?name=$encodedName")
                    },
                    onOpenTripDetail = { tripId, fromStopId, routeName, routeColor ->
                        navController.navigate("trip/$tripId?fromStopId=$fromStopId&routeName=$routeName&routeColor=$routeColor")
                    },
                    onOpenLineDetail = { routeId ->
                        val encodedId = URLEncoder.encode(routeId, StandardCharsets.UTF_8.name())
                        navController.navigate("line/$encodedId")
                    },
                    initialVehicleId = vehicleId,
                    initialPreviewStopId = previewStopId,
                )
            }

            // ── Servizi (drilled-in from Home — no longer a tab) ────────────────
            composable(
                Screen.Servizi.route,
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://servizi" }),
            ) {
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
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://servizi/{serviceId}" }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("serviceId") ?: return@composable
                ServiceDetailScreen(
                    serviceId = URLDecoder.decode(id, StandardCharsets.UTF_8.name()),
                    onBack = { navController.popBackStack() },
                    onNavigateToMappa = {
                        navController.navigate(Screen.Mappa.route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
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
            composable(
                Screen.Avvisi.route,
                deepLinks = listOf(
                    navDeepLink { uriPattern = "transitkit://alerts" },
                    navDeepLink { uriPattern = "transitkit://avvisi" },
                ),
            ) {
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
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://alert/{alertId}" }),
            ) { backStackEntry ->
                val id = backStackEntry.arguments?.getString("alertId") ?: return@composable
                AlertDetailScreen(
                    alertId = URLDecoder.decode(id, StandardCharsets.UTF_8.name()),
                    onBack = { navController.popBackStack() },
                )
            }

            // ── Onboarding (dev/test deep link — first-run flow lives in
            //     prefs but this route lets us showcase it on demand) ────────
            composable(
                route = "onboarding",
                deepLinks = listOf(navDeepLink { uriPattern = "transitkit://onboarding" }),
            ) {
                val ctx = androidx.compose.ui.platform.LocalContext.current
                val prefs = remember {
                    ctx.getSharedPreferences("transitkit_prefs", android.content.Context.MODE_PRIVATE)
                }
                OnboardingScreen(
                    prefs = prefs,
                    onComplete = { navController.popBackStack() },
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
