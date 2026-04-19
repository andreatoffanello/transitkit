package com.transitkit.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import com.transitkit.app.config.LucideIcons
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.transitkit.app.R
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.config.OperatorConfig
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.config.toColor
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.Departure
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

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
    val greetingHour = remember { java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) }
    val greeting = when {
        greetingHour < 12 -> stringResource(R.string.home_greeting_morning)
        greetingHour < 18 -> stringResource(R.string.home_greeting_afternoon)
        else -> stringResource(R.string.home_greeting_evening)
    }
    val scheduleLoadError by viewModel.scheduleLoadError.collectAsStateWithLifecycle()
    val scheduleIsLoading by viewModel.scheduleIsLoading.collectAsStateWithLifecycle()
    val resolvedFavoriteStopsForLoading by viewModel.resolvedFavoriteStops.collectAsStateWithLifecycle()
    val nearbyStopsForLoading by viewModel.nearbyStops.collectAsStateWithLifecycle()

    val context = LocalContext.current

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
        if (!locationPermissionGranted) {
            locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
    DisposableEffect(locationPermissionGranted) {
        val lm = context.getSystemService(android.location.LocationManager::class.java)
        val listener = android.location.LocationListener { loc ->
            viewModel.updateLocation(loc.latitude, loc.longitude)
        }
        if (locationPermissionGranted && lm != null) {
            try {
                // Initial last-known (GPS preferred, NETWORK fallback)
                (lm.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                    ?: lm.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER))
                    ?.let { viewModel.updateLocation(it.latitude, it.longitude) }
                // Continuous updates: every 30 s OR 50 m movement (both providers)
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

    if (scheduleIsLoading && scheduleLoadError == null &&
        resolvedFavoriteStopsForLoading.isEmpty() && nearbyStopsForLoading.isEmpty()
    ) {
        if (config != null) {
            BrandedLoadingScreen(config = config)
            return
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(TransitTheme.colors.background),
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
                    Icon(painterResource(LucideIcons.WifiOff), contentDescription = null, tint = TransitTheme.colors.realtimeRed, modifier = Modifier.size(18.dp))
                    Text(scheduleLoadError!!, style = MaterialTheme.typography.bodySmall, color = TransitTheme.colors.realtimeRed)
                }
            }
        }
        item {
            HeroSectionWrapper(
                config = config,
                greeting = greeting,
                viewModel = viewModel,
                onNavigateToSettings = onNavigateToSettings,
                onNavigateToAlerts = onNavigateToAlerts,
            )
        }
        item {
            FavoriteStopsSection(
                viewModel = viewModel,
                config = config,
                onNavigateToOrari = onNavigateToOrari,
                onNavigateToStop = onNavigateToStop,
            )
        }
        item {
            NearbyStopsSection(
                viewModel = viewModel,
                locationPermissionGranted = locationPermissionGranted,
                onRequestPermission = { locationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION) },
                onNavigateToStop = onNavigateToStop,
            )
        }
        item {
            Spacer(Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Hero section wrapper — collects liveTripIds independently
// ---------------------------------------------------------------------------

@Composable
private fun HeroSectionWrapper(
    config: OperatorConfig?,
    greeting: String,
    viewModel: HomeViewModel,
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
) {
    val liveTripIds by viewModel.liveTripIds.collectAsStateWithLifecycle()
    val activeAlerts by viewModel.activeAlerts.collectAsStateWithLifecycle()
    Box {
        HeroSection(
            config = config,
            greeting = greeting,
            liveTripIds = liveTripIds,
            activeAlerts = activeAlerts,
            onNavigateToSettings = onNavigateToSettings,
            onNavigateToAlerts = onNavigateToAlerts,
        )
        // Gradient fade da hero a background
        val primaryColor = TransitTheme.colors.primary
        val backgroundColor = TransitTheme.colors.background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .align(Alignment.BottomCenter)
                .background(
                    Brush.verticalGradient(listOf(primaryColor, backgroundColor)),
                ),
        )
    }
}

// ---------------------------------------------------------------------------
// Favorites section — collects its own StateFlows independently
// ---------------------------------------------------------------------------

@Composable
private fun FavoriteStopsSection(
    viewModel: HomeViewModel,
    config: OperatorConfig?,
    onNavigateToOrari: () -> Unit,
    onNavigateToStop: (String, String) -> Unit,
) {
    val favoriteStopIds by viewModel.favoriteStopIds.collectAsStateWithLifecycle()
    val resolvedFavoriteStops by viewModel.resolvedFavoriteStops.collectAsStateWithLifecycle()
    val favoriteDepartures by viewModel.favoriteDepartures.collectAsStateWithLifecycle()
    val liveTripIds by viewModel.liveTripIds.collectAsStateWithLifecycle()

    if (favoriteStopIds.isEmpty()) {
        Column {
            Text(
                stringResource(R.string.section_preferiti).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = TransitTheme.colors.textTertiary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 4.dp),
            )
            EmptyFavoritesState(onNavigateToOrari)
        }
    } else {
        Column {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .padding(top = 16.dp),
            ) {
                Text(
                    stringResource(R.string.section_le_mie_fermate).uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp,
                    color = TransitTheme.colors.textTertiary,
                )
            }
            resolvedFavoriteStops.forEach { stop ->
                FavoriteStopCard(
                    stop = stop,
                    departures = favoriteDepartures[stop.id] ?: emptyList(),
                    liveTripIds = liveTripIds,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { onNavigateToStop(stop.id, stop.name) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nearby stops section — collects its own StateFlows independently
// ---------------------------------------------------------------------------

@Composable
private fun NearbyStopsSection(
    viewModel: HomeViewModel,
    locationPermissionGranted: Boolean,
    onRequestPermission: () -> Unit,
    onNavigateToStop: (String, String) -> Unit,
) {
    val nearbyStops by viewModel.nearbyStops.collectAsStateWithLifecycle()
    val favoriteDepartures by viewModel.favoriteDepartures.collectAsStateWithLifecycle()
    val liveTripIds by viewModel.liveTripIds.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    if (!locationPermissionGranted) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 16.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.bgSecondary)
                .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    val canRequest = androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale(
                        context as androidx.activity.ComponentActivity,
                        android.Manifest.permission.ACCESS_FINE_LOCATION,
                    )
                    if (canRequest) {
                        onRequestPermission()
                    } else {
                        context.startActivity(
                            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", context.packageName, null)
                            }
                        )
                    }
                }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.MapPin),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(20.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.nearby_enable_title),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                Text(
                    text = stringResource(R.string.nearby_enable_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                )
            }
            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier.size(16.dp),
            )
        }
    } else if (nearbyStops.isNotEmpty()) {
        Column {
            Text(
                stringResource(R.string.section_vicino_a_te).uppercase(),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp,
                color = colors.textTertiary,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(top = 16.dp, bottom = 4.dp),
            )
            nearbyStops.forEach { (stop, distanceMeters) ->
                NearbyStopCard(
                    stop = stop,
                    departures = favoriteDepartures[stop.id]?.take(2) ?: emptyList(),
                    liveTripIds = liveTripIds,
                    distanceMeters = distanceMeters,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                    onClick = { onNavigateToStop(stop.id, stop.name) },
                )
            }
        }
    }
}

/** Locale-friendly distance formatter matching iOS: "420 m" / "1,2 km". */
@Composable
private fun formatDistance(meters: Double): String {
    if (meters < 1000) {
        return stringResource(R.string.distance_meters, meters.toInt())
    }
    val km = meters / 1000.0
    val locale = java.util.Locale.getDefault()
    val numberFormat = java.text.NumberFormat.getNumberInstance(locale).apply {
        minimumFractionDigits = 1
        maximumFractionDigits = 1
    }
    return stringResource(R.string.distance_kilometers, numberFormat.format(km))
}

// ---------------------------------------------------------------------------
// Quick access section
// ---------------------------------------------------------------------------

@Composable
private fun QuickAccessSection(
    onNavigateToOrari: () -> Unit,
    onNavigateToMappa: () -> Unit,
    onNavigateToInfo: () -> Unit,
) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(top = 16.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = stringResource(R.string.section_accesso_rapido).uppercase(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            color = colors.textTertiary,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            QuickCard(
                icon = LucideIcons.Clock,
                label = stringResource(R.string.quick_label_orari),
                subtitle = stringResource(R.string.quick_orari_subtitle),
                onClick = onNavigateToOrari,
                modifier = Modifier.weight(1f),
            )
            QuickCard(
                icon = LucideIcons.Map,
                label = stringResource(R.string.quick_label_mappa),
                subtitle = stringResource(R.string.quick_mappa_subtitle),
                onClick = onNavigateToMappa,
                modifier = Modifier.weight(1f),
            )
            QuickCard(
                icon = LucideIcons.Info,
                label = stringResource(R.string.quick_label_info),
                subtitle = stringResource(R.string.quick_info_subtitle),
                onClick = onNavigateToInfo,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuickCard(
     icon: Int,
    label: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(colors.bgSecondary)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = label }
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
            maxLines = 1,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textTertiary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ---------------------------------------------------------------------------
// Hero section
// ---------------------------------------------------------------------------

@Composable
private fun HeroSection(
    config: OperatorConfig?,
    greeting: String,
    liveTripIds: Set<String> = emptySet(),
    activeAlerts: List<ServiceAlert> = emptyList(),
    onNavigateToSettings: () -> Unit = {},
    onNavigateToAlerts: () -> Unit = {},
) {
    val context = LocalContext.current
    // Mirrors iOS: UIImage(named: "OperatorLogo") != nil check
    val logoResId = remember {
        context.resources.getIdentifier("operator_logo", "drawable", context.packageName)
    }
    val hasLogo = logoResId != 0

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        TransitTheme.colors.primary,
                        TransitTheme.colors.primary.copy(alpha = 0.85f),
                        TransitTheme.colors.accent.copy(alpha = 0.6f),
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY),
                ),
            )
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = 24.dp, vertical = 28.dp),
    ) {
        // Dot pattern texture
        Canvas(modifier = Modifier.matchParentSize()) {
            val dotColor = Color.White.copy(alpha = 0.06f)
            val spacing = 32.dp.toPx()
            var y = 0f
            while (y < size.height) {
                var x = 0f
                while (x < size.width) {
                    drawCircle(color = dotColor, radius = 2.dp.toPx(), center = Offset(x, y))
                    x += spacing
                }
                y += spacing
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Avatar circle — logo or initials, mirrors iOS heroCard
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (hasLogo) {
                        Image(
                            painter = painterResource(logoResId),
                            contentDescription = config?.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape),
                        )
                    } else {
                        // Initials fallback — mirrors iOS initials(from: config.name)
                        val initials = config?.name
                            ?.split(" ")
                            ?.take(2)
                            ?.mapNotNull { it.firstOrNull()?.uppercaseChar() }
                            ?.joinToString("") ?: ""
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.75f),
                    )
                    Text(
                        text = config?.name ?: "",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                    if (!config?.region.isNullOrBlank()) {
                        Text(
                            text = config!!.region!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                    }
                }
            }

            if (activeAlerts.isNotEmpty()) {
                HeroAlertBanner(
                    count = activeAlerts.size,
                    severity = activeAlerts.maxByOrNull { it.severity.raw }?.severity
                        ?: AlertSeverity.UNKNOWN,
                    onClick = onNavigateToAlerts,
                )
            }
        }

        // Settings button — top-end corner
        val haptic = LocalHapticFeedback.current
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clip(RoundedCornerShape(12.dp))
                .clickable {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onNavigateToSettings()
                }
                .padding(4.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.Settings),
                contentDescription = stringResource(R.string.cd_impostazioni),
                tint = Color.White.copy(alpha = 0.75f),
                modifier = Modifier.size(22.dp),
            )
        }

        // Live chip — bottom-end, only when there are live vehicles
        if (liveTripIds.isNotEmpty()) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    "Live",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                )
            }
        }
    }
}

/**
 * Tappable pill inside the hero that opens the alert list.
 * Slightly brighter fill on SEVERE to signal urgency without losing the
 * hero's gradient legibility.
 */
@Composable
private fun HeroAlertBanner(
    count: Int,
    severity: AlertSeverity,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val fillAlpha = if (severity == AlertSeverity.SEVERE) 0.22f else 0.16f
    val label = if (count == 1) stringResource(R.string.alerts_banner_one)
                else stringResource(R.string.alerts_banner_many, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = fillAlpha))
            .border(0.5.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(LucideIcons.AlertTriangle),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.size(14.dp),
        )
    }
}

// ---------------------------------------------------------------------------
// Search bar
// ---------------------------------------------------------------------------

@Composable
private fun SearchBar(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 8.dp),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.08f)),
        border = BorderStroke(1.5.dp, TransitTheme.colors.accent.copy(alpha = 0.4f)),
    ) {
        val cdSearch = stringResource(R.string.search_placeholder_home)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .semantics { contentDescription = cdSearch }
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                painter = painterResource(LucideIcons.Search),
                contentDescription = null,
                tint = TransitTheme.colors.accent,
                modifier = Modifier.size(22.dp),
            )
            Text(
                text = stringResource(R.string.search_placeholder_home_dots),
                style = MaterialTheme.typography.bodyMedium,
                color = TransitTheme.colors.textSecondary,
            )
            Spacer(Modifier.weight(1f))
            Icon(
                painter = painterResource(LucideIcons.Mic),
                contentDescription = null,
                tint = TransitTheme.colors.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyFavoritesState(onNavigateToOrari: () -> Unit) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(20.dp)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(colors.accent.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(LucideIcons.MapPin),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(30.dp),
                )
            }
            Text(
                text = stringResource(R.string.empty_favorites_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
            )
            Text(
                text = stringResource(R.string.empty_favorites_body),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onNavigateToOrari,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = Color.White,
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp,
                ),
            ) {
                Icon(painterResource(LucideIcons.Search), contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(stringResource(R.string.empty_favorites_cta), fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Favorite stop card
// ---------------------------------------------------------------------------

@Composable
fun FavoriteStopCard(
    stop: ResolvedStop,
    departures: List<Departure>,
    liveTripIds: Set<String> = emptySet(),
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val cardDesc = context.getString(R.string.cd_fermata_preferita, stop.name, stop.routeNames.joinToString(", "))
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TransitTheme.colors.glassBorder, RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = cardDesc
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TransitTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    painter = painterResource(com.transitkit.app.ui.components.stopIcon(stop.transitTypes)),
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (departures.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_departures),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textTertiary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    departures.forEach { departure ->
                        DepartureRow(departure, isLive = liveTripIds.contains(departure.tripId))
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Nearby stop card
// ---------------------------------------------------------------------------

@Composable
fun NearbyStopCard(
    stop: ResolvedStop,
    departures: List<Departure>,
    liveTripIds: Set<String> = emptySet(),
    distanceMeters: Double? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
) {
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current
    val cardDesc = context.getString(R.string.cd_fermata_vicina, stop.name, stop.routeNames.joinToString(", "))
    Card(
        onClick = {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            onClick()
        },
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, TransitTheme.colors.glassBorder, RoundedCornerShape(16.dp))
            .semantics {
                contentDescription = cardDesc
            },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TransitTheme.colors.bgSecondary),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TransitTheme.colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (distanceMeters != null) {
                    Text(
                        text = formatDistance(distanceMeters),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = TransitTheme.colors.textTertiary,
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Icon(
                    painter = painterResource(com.transitkit.app.ui.components.stopIcon(stop.transitTypes)),
                    contentDescription = null,
                    tint = TransitTheme.colors.accent,
                    modifier = Modifier.size(16.dp),
                )
            }
            Spacer(Modifier.height(8.dp))
            if (departures.isEmpty()) {
                Text(
                    stringResource(R.string.no_departures),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textTertiary,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    departures.forEach { DepartureRow(it, isLive = liveTripIds.contains(it.tripId)) }
                }
            }
        }
    }
}

@Composable
private fun DepartureRow(departure: Departure, isLive: Boolean = false) {
    val colors = TransitTheme.colors
    val routeColor = departure.routeColor
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { "#$it".toColor() }.getOrNull() }
        ?: colors.accent
    val routeTextColor = departure.routeTextColor
        ?.takeIf { it.isNotBlank() }
        ?.let { runCatching { "#${departure.routeTextColor}".toColor() }.getOrNull() }
        ?: Color.White

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        // Live vehicle dot — veicolo rilevato in real-time via GTFS-RT vehicle positions
        if (isLive) {
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.realtimeGreen),
            )
            Spacer(Modifier.width(4.dp))
        }
        com.transitkit.app.ui.components.LineBadge(
            name = departure.routeShortName.take(5),
            colorHex = departure.routeColor,
            textColorHex = departure.routeTextColor,
            transitType = departure.transitType,
            size = com.transitkit.app.ui.components.LineBadgeSize.Small,
            showTransitIcon = true,
        )
        Spacer(Modifier.width(8.dp))
        // Headsign
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        // Orario
        Text(
            text = departure.realtimeDepartureTime?.take(5) ?: departure.departureTime.take(5),
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontFeatureSettings = "tnum",
            ),
            color = colors.textPrimary,
        )
        // Dot realtime
        if (departure.isRealtime) {
            Spacer(Modifier.width(5.dp))
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(colors.realtimeGreen),
            )
        }
    }
}


private fun transitTypeIcon(type: Int): Int = when (type) {
    0 -> LucideIcons.Train
    1 -> LucideIcons.Train
    2 -> LucideIcons.Train
    4 -> LucideIcons.Ship
    else -> LucideIcons.BusFront
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
            // Operator avatar with initials
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
