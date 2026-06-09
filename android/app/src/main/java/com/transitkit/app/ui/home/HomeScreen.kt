package com.transitkit.app.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.ui.planner.PlannerViewModel

@Composable
fun HomeScreen(
    onNavigateToOrari: () -> Unit,
    onNavigateToLinee: () -> Unit = {},
    onNavigateToMappa: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToServizi: () -> Unit = {},
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
    onNavigateToPlanner: () -> Unit = {},
    onNavigateToLocationPicker: (role: String) -> Unit = {},
    plannerViewModel: PlannerViewModel,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val config = uiState.config
    val context = LocalContext.current

    val scheduleLoadError by viewModel.scheduleLoadError.collectAsStateWithLifecycle()
    val scheduleIsLoading by viewModel.scheduleIsLoading.collectAsStateWithLifecycle()
    val resolvedFavoriteStops by viewModel.resolvedFavoriteStops.collectAsStateWithLifecycle()
    val favoriteDepartures by viewModel.favoriteDepartures.collectAsStateWithLifecycle()
    val nearbyStops by viewModel.nearbyStops.collectAsStateWithLifecycle()
    val nearbyDepartures by viewModel.nearbyDepartures.collectAsStateWithLifecycle()
    val liveTripIds by viewModel.liveTripIds.collectAsStateWithLifecycle()
    val liveVehicleCount by viewModel.liveVehicleCount.collectAsStateWithLifecycle()
    val routesCount by viewModel.routesCount.collectAsStateWithLifecycle()
    val routesByName by viewModel.routesByName.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val shouldShowOnboarding by viewModel.shouldShowOnboarding.collectAsStateWithLifecycle()

    val prefs = remember { context.getSharedPreferences("transitkit_prefs", android.content.Context.MODE_PRIVATE) }

    var locationPermissionGranted by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> locationPermissionGranted = granted }

    LaunchedEffect(Unit) {
        viewModel.checkOnboarding(prefs)
    }

    DisposableEffect(locationPermissionGranted) {
        val lm = context.getSystemService(android.location.LocationManager::class.java)
        val listener = android.location.LocationListener { loc ->
            viewModel.updateLocation(loc.latitude, loc.longitude)
            plannerViewModel.updateGpsLocation(loc.latitude, loc.longitude)
        }
        if (locationPermissionGranted && lm != null) {
            try {
                (lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER))
                    ?.let {
                        viewModel.updateLocation(it.latitude, it.longitude)
                        plannerViewModel.updateGpsLocation(it.latitude, it.longitude)
                    }
                listOf(
                    android.location.LocationManager.GPS_PROVIDER,
                    android.location.LocationManager.NETWORK_PROVIDER,
                ).filter { lm.isProviderEnabled(it) }.forEach { provider ->
                    // minTime 5s + minDistance 0 m → first fix arrives quickly
                    // (incl. on emulators where `getLastKnownLocation` is null
                    // and `emu geo fix` doesn't simulate movement); subsequent
                    // updates are still rate-limited by the 5s window, so power
                    // cost stays low.
                    lm.requestLocationUpdates(provider, 5_000L, 0f, listener)
                }
            } catch (_: SecurityException) {}
        }
        onDispose {
            try { lm?.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    // Full-screen onboarding stories al primo avvio (pre-permission).
    if (shouldShowOnboarding) {
        com.transitkit.app.ui.onboarding.OnboardingScreen(
            prefs = prefs,
            onComplete = { viewModel.markOnboardingSeen(prefs) },
            onLocationGranted = { locationPermissionGranted = true },
            onNotificationsGranted = {
                viewModel.onNotificationsPermissionGranted()
            },
        )
        return
    }

    if (scheduleIsLoading && scheduleLoadError == null &&
        resolvedFavoriteStops.isEmpty() && nearbyStops.isEmpty()
    ) {
        if (config != null) {
            BrandedLoadingScreen(config = config)
            return
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        OperatorMapBackground()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars),
            contentPadding = PaddingValues(bottom = 100.dp),
        ) {
            if (scheduleLoadError != null) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TransitTheme.colors.realtimeRed.copy(alpha = 0.10f))
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painterResource(LucideIcons.WifiOff),
                            contentDescription = null,
                            tint = TransitTheme.colors.realtimeRed,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            scheduleLoadError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = TransitTheme.colors.realtimeRed,
                        )
                    }
                }
            }

            if (activeAlerts.isNotEmpty()) {
                item {
                    HomeAlertChip(
                        alerts = activeAlerts,
                        onClick = onNavigateToAlerts,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                    )
                }
            }

            item {
                HomeMinimalHeader(
                    config = config,
                    onSettingsClick = onNavigateToSettings,
                )
            }

            item {
                PlannerHomeBox(
                    plannerViewModel = plannerViewModel,
                    onNavigateToLocationPicker = onNavigateToLocationPicker,
                    onNavigateToPlanner = onNavigateToPlanner,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item {
                FavoritesSection(
                    stops = resolvedFavoriteStops,
                    departures = favoriteDepartures,
                    liveTripIds = liveTripIds,
                    operatorTimezone = viewModel.operatorConfig.timezone,
                    onStopClick = { stop -> onNavigateToStop(stop.id, stop.name) },
                    onBrowseStops = onNavigateToOrari,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item {
                NearbySection(
                    nearbyStops = nearbyStops,
                    routesByName = routesByName,
                    permissionGranted = locationPermissionGranted,
                    onEnableLocation = {
                        val canRequest = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                            context as androidx.activity.ComponentActivity,
                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                        )
                        if (canRequest || !hasRequestedPermission(prefs)) {
                            markPermissionRequested(prefs)
                            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                            )
                        }
                    },
                    onStopClick = { stop -> onNavigateToStop(stop.id, stop.name) },
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            if (config != null) {
                item {
                    OperatorReferenceSection(
                        config = config,
                        liveVehicleCount = liveVehicleCount,
                        routesCount = routesCount,
                        onClick = onNavigateToServizi,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }

            item {
                HomeServiziLink(
                    onClick = onNavigateToServizi,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                )
            }

            item {
                HomeFooterDisclaimer(
                    operatorName = config?.name ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                )
            }
        }

        // Footer gradient fade for readability above the tab bar.
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(120.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            TransitTheme.colors.background.copy(alpha = 0.9f),
                        )
                    )
                )
        )
    }
}

private fun hasRequestedPermission(prefs: android.content.SharedPreferences): Boolean =
    prefs.getBoolean("has_requested_location_permission", false)

private fun markPermissionRequested(prefs: android.content.SharedPreferences) {
    prefs.edit().putBoolean("has_requested_location_permission", true).apply()
}
