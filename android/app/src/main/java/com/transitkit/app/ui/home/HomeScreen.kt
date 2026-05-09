package com.transitkit.app.ui.home

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ServiceAlert
import com.transitkit.app.ui.components.DepartureTimeState
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.LiveIndicator
import com.transitkit.app.ui.components.TimeDisplay
import com.transitkit.app.ui.components.departureTimeState
import com.transitkit.app.ui.components.stopIcon

@Composable
fun HomeScreen(
    onNavigateToOrari: () -> Unit,
    onNavigateToLinee: () -> Unit = {},
    onNavigateToMappa: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
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
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    val shouldShowPrimer by viewModel.shouldShowLocationPrimer.collectAsStateWithLifecycle()

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
        viewModel.checkLocationPrimer(prefs, locationPermissionGranted)
    }

    DisposableEffect(locationPermissionGranted) {
        val lm = context.getSystemService(android.location.LocationManager::class.java)
        val listener = android.location.LocationListener { loc ->
            viewModel.updateLocation(loc.latitude, loc.longitude)
        }
        if (locationPermissionGranted && lm != null) {
            try {
                (lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER))
                    ?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                listOf(
                    android.location.LocationManager.GPS_PROVIDER,
                    android.location.LocationManager.NETWORK_PROVIDER,
                ).filter { lm.isProviderEnabled(it) }.forEach { provider ->
                    lm.requestLocationUpdates(provider, 30_000L, 50f, listener)
                }
            } catch (_: SecurityException) {}
        }
        onDispose {
            try { lm?.removeUpdates(listener) } catch (_: Exception) {}
        }
    }

    // Full-screen primer takes over the Home on first launch (pre-permission).
    if (shouldShowPrimer) {
        LocationPrimerScreen(
            onEnableLocation = {
                viewModel.markLocationPrimerSeen(prefs)
                locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
            },
            onSkip = { viewModel.markLocationPrimerSeen(prefs) }
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
                    nearbyDepartures = nearbyDepartures,
                    liveTripIds = liveTripIds,
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
                    operatorTimezone = viewModel.operatorConfig.timezone,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            item {
                HomeFooterDisclaimer(
                    operatorName = config?.name ?: "",
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 16.dp),
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

// ---------------------------------------------------------------------------
// HomeMinimalHeader — logo + operator name, settings cog on the trailing edge.
// ---------------------------------------------------------------------------

@Composable
private fun HomeMinimalHeader(
    config: OperatorConfig?,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val colors = TransitTheme.colors
    val logoId = remember {
        runCatching {
            context.resources.getIdentifier("operator_logo", "drawable", context.packageName)
        }.getOrDefault(0)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (logoId != 0) {
            Image(
                painter = painterResource(id = logoId),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape),
            )
        }
        config?.let {
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    text = it.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                val region = it.region?.takeIf { r -> r.isNotBlank() }
                if (region != null) {
                    Text(
                        text = region,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        val cdSettings = stringResource(R.string.tab_settings)
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onSettingsClick()
                }
                .semantics { contentDescription = cdSettings },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.Settings),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// HomeAlertChip — compact chip above the header when alerts are active.
// ---------------------------------------------------------------------------

@Composable
private fun HomeAlertChip(
    alerts: List<ServiceAlert>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val highestSeverity = alerts.maxByOrNull { it.severity.raw }?.severity ?: AlertSeverity.UNKNOWN
    val chipColor = when (highestSeverity) {
        AlertSeverity.SEVERE -> colors.realtimeRed
        AlertSeverity.WARNING -> colors.realtimeOrange
        else -> colors.accent
    }
    val label = if (alerts.size == 1)
        stringResource(R.string.alerts_banner_one)
    else
        stringResource(R.string.alerts_banner_many, alerts.size)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.glassFill)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = label }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.AlertTriangle),
            contentDescription = null,
            tint = chipColor,
            modifier = Modifier.size(13.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// FavoritesSection + NearbySection
// ---------------------------------------------------------------------------

@Composable
private fun FavoritesSection(
    stops: List<ResolvedStop>,
    departures: Map<String, List<Departure>>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    onStopClick: (ResolvedStop) -> Unit,
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(text = stringResource(R.string.section_preferiti))
        if (stops.isEmpty()) {
            EmptyFavoritesCard(onBrowseStops = onBrowseStops)
        } else {
            stops.take(5).forEach { stop ->
                StopCard(
                    stop = stop,
                    departures = departures[stop.id] ?: emptyList(),
                    liveTripIds = liveTripIds,
                    operatorTimezone = operatorTimezone,
                    onClick = { onStopClick(stop) },
                )
            }
        }
    }
}

@Composable
private fun NearbySection(
    nearbyStops: List<Pair<ResolvedStop, Double>>,
    nearbyDepartures: Map<String, List<Departure>>,
    liveTripIds: Set<String>,
    permissionGranted: Boolean,
    onEnableLocation: () -> Unit,
    onStopClick: (ResolvedStop) -> Unit,
    operatorTimezone: String,
    modifier: Modifier = Modifier,
) {
    when {
        !permissionGranted -> EnableLocationChip(onClick = onEnableLocation, modifier = modifier)
        nearbyStops.isNotEmpty() -> {
            Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeader(text = stringResource(R.string.section_vicino_a_te))
                nearbyStops.forEach { (stop, distance) ->
                    StopCard(
                        stop = stop,
                        departures = nearbyDepartures[stop.id] ?: emptyList(),
                        liveTripIds = liveTripIds,
                        operatorTimezone = operatorTimezone,
                        distanceMeters = distance,
                        onClick = { onStopClick(stop) },
                    )
                }
            }
        }
        else -> Unit
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.5.sp,
        color = TransitTheme.colors.textTertiary,
        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
    )
}

@Composable
private fun EnableLocationChip(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val cd = stringResource(R.string.home_enable_location_chip)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(colors.glassFill)
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = cd }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.MapPin),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(14.dp),
        )
        Text(
            text = stringResource(R.string.home_enable_location_chip),
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(12.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// StopCard — unified card for favorites + nearby.
// ---------------------------------------------------------------------------

@Composable
private fun StopCard(
    stop: ResolvedStop,
    departures: List<Departure>,
    liveTripIds: Set<String>,
    operatorTimezone: String,
    distanceMeters: Double? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val isImminent = departures.firstOrNull()?.let {
        isWithinFiveMinutes(it, operatorTimezone)
    } ?: false

    Surface(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = colors.glassFill,
        tonalElevation = 1.dp,
        border = if (isImminent) BorderStroke(1.5.dp, colors.accent.copy(alpha = 0.6f)) else null,
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    painter = painterResource(id = stopIcon(stop.transitTypes)),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                distanceMeters?.let {
                    Text(
                        text = walkingTimeLabel(it),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textTertiary,
                    )
                }
            }

            if (departures.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_departures),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                )
            } else {
                val shown = departures.take(3)
                Column {
                    shown.forEachIndexed { index, dep ->
                        StopCardDepartureRow(
                            departure = dep,
                            isLive = dep.tripId in liveTripIds,
                            operatorTimezone = operatorTimezone,
                        )
                        if (index < shown.size - 1) {
                            HorizontalDivider(
                                color = colors.separator,
                                thickness = 0.5.dp,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StopCardDepartureRow(
    departure: Departure,
    isLive: Boolean,
    operatorTimezone: String,
) {
    val colors = TransitTheme.colors
    val rawTime = departure.realtimeDepartureTime ?: departure.departureTime
    val timeState = departureTimeState(rawTime, operatorTimezone)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        LineBadge(
            name = departure.routeShortName.take(5),
            colorHex = departure.routeColor,
            textColorHex = departure.routeTextColor,
            transitType = departure.transitType,
            size = LineBadgeSize.Medium,
        )
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (isLive) {
            LiveIndicator(animated = true)
        }
        TimeDisplay(state = timeState)
    }
}

@Composable
private fun walkingTimeLabel(meters: Double): String {
    val minutes = kotlin.math.ceil(meters / 80.0).toInt()
    return when {
        minutes <= 1 -> stringResource(R.string.walking_1_min)
        minutes > 10 -> stringResource(R.string.walking_10_plus_min)
        else -> stringResource(R.string.walking_n_min, minutes)
    }
}

private fun isWithinFiveMinutes(dep: Departure, tz: String): Boolean {
    val raw = dep.realtimeDepartureTime ?: dep.departureTime
    return when (val state = departureTimeState(raw, tz)) {
        is DepartureTimeState.Departing -> true
        is DepartureTimeState.Minutes -> state.minutes in 0..5
        else -> false
    }
}

// ---------------------------------------------------------------------------
// EmptyFavoritesCard + HomeFooterDisclaimer
// ---------------------------------------------------------------------------

@Composable
private fun EmptyFavoritesCard(
    onBrowseStops: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = colors.glassFill,
        tonalElevation = 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(colors.accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(LucideIcons.Star),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp),
                )
            }
            Text(
                text = stringResource(R.string.home_empty_favorites_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Text(
                text = stringResource(R.string.home_empty_favorites_body),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Button(
                onClick = onBrowseStops,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ),
            ) {
                Text(stringResource(R.string.home_empty_favorites_cta))
            }
        }
    }
}

@Composable
private fun HomeFooterDisclaimer(
    operatorName: String,
    modifier: Modifier = Modifier,
) {
    if (operatorName.isEmpty()) return
    Text(
        text = stringResource(R.string.home_footer_disclaimer, operatorName),
        style = MaterialTheme.typography.labelSmall,
        color = TransitTheme.colors.textTertiary,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth(),
    )
}

// ---------------------------------------------------------------------------
// Branded loading screen — shown on cold start while schedule loads
// ---------------------------------------------------------------------------

@Composable
private fun BrandedLoadingScreen(config: OperatorConfig) {
    val colors = TransitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(colors.accent.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = config.name.take(2).uppercase(),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.accent,
                )
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = config.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                )
                LinearProgressIndicator(
                    modifier = Modifier.width(120.dp),
                    color = colors.accent,
                    trackColor = colors.accent.copy(alpha = 0.12f),
                )
            }
            Text(
                text = stringResource(R.string.info_powered_by),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textTertiary,
            )
        }
    }
}
