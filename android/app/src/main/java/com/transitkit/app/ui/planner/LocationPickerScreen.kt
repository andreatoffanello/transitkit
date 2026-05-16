package com.transitkit.app.ui.planner

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.components.stopIcon
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Full-screen stop / location picker.
 *
 * role   = "origin" | "destination"
 * source = "home"   | "planner"
 *
 * Empty-state shows quick choices (My location / Pick on map) + Nearby + Recent.
 * Typing filters the full stop list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    role: String,
    source: String,
    plannerViewModel: PlannerViewModel,
    onBack: () -> Unit,
    onNavigateToMapPicker: () -> Unit,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val allStops by plannerViewModel.allStops.collectAsStateWithLifecycle()
    val currentLocation by plannerViewModel.currentLocation.collectAsStateWithLifecycle()
    val recentStops by plannerViewModel.recentStops.collectAsStateWithLifecycle()

    var query by remember { mutableStateOf("") }
    var isFiltering by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }

    val filtered = remember(query, allStops) {
        if (query.isBlank()) emptyList()
        else allStops.asSequence()
            .filter { it.name.contains(query.trim(), ignoreCase = true) }
            .take(60)
            .toList()
    }

    val nearby = remember(currentLocation, allStops) {
        if (currentLocation == null || allStops.isEmpty()) emptyList()
        else plannerViewModel.nearbyStops(limit = 5)
    }

    LaunchedEffect(query) {
        isFiltering = query.isNotBlank()
        if (query.isNotBlank()) {
            delay(120)
            isFiltering = false
        }
    }

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    fun onStopSelected(stop: ResolvedStop) {
        val loc = PlannerLocation.fromStop(stop)
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        when (source) {
            "home" -> if (role == "origin") plannerViewModel.setHomeOrigin(loc)
                      else plannerViewModel.setHomeDestination(loc)
            else   -> if (role == "origin") plannerViewModel.setOrigin(loc)
                      else plannerViewModel.setDestination(loc)
        }
        onBack()
    }

    fun onUseCurrentLocation() {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val loc = currentLocation ?: return
        val planner = PlannerLocation(
            kind = PlannerLocation.Kind.CurrentLocation,
            name = "My location",
            lat = loc.first,
            lon = loc.second,
        )
        when (source) {
            "home" -> if (role == "origin") plannerViewModel.setHomeOrigin(planner)
                      else plannerViewModel.setHomeDestination(planner)
            else   -> if (role == "origin") plannerViewModel.setOrigin(planner)
                      else plannerViewModel.setDestination(planner)
        }
        onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (role == "origin")
                            stringResource(R.string.planner_picker_origin_title)
                        else
                            stringResource(R.string.planner_picker_destination_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ArrowLeft),
                            contentDescription = "Back",
                            modifier = Modifier.size(20.dp),
                        )
                    }
                },
                actions = {
                    TextButton(onClick = onBack) {
                        Text(
                            stringResource(R.string.cancel),
                            color = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.background,
                ),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = {
                    Text(
                        stringResource(R.string.planner_picker_search_placeholder),
                        color = colors.textTertiary,
                    )
                },
                leadingIcon = {
                    Icon(
                        painterResource(LucideIcons.Search),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = colors.textTertiary,
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { query = "" }) {
                            Icon(
                                painterResource(LucideIcons.X),
                                contentDescription = stringResource(R.string.cancel),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .focusRequester(focusRequester)
                    .semantics { contentDescription = "Search stops" },
            )

            if (query.isEmpty()) {
                EmptyStateContent(
                    locationAvailable = currentLocation != null,
                    onUseLocation = ::onUseCurrentLocation,
                    onPickOnMap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToMapPicker()
                    },
                    nearby = nearby,
                    recents = recentStops,
                    onStopSelected = ::onStopSelected,
                )
            } else {
                StopResultsList(
                    isFiltering = isFiltering,
                    results = filtered,
                    onSelect = ::onStopSelected,
                )
            }
        }
    }
}

// ── Empty state (no query) ────────────────────────────────────────────────────

@Composable
private fun EmptyStateContent(
    locationAvailable: Boolean,
    onUseLocation: () -> Unit,
    onPickOnMap: () -> Unit,
    nearby: List<Pair<ResolvedStop, Double>>,
    recents: List<ResolvedStop>,
    onStopSelected: (ResolvedStop) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item("quick_choices") {
            QuickChoicesSection(
                locationAvailable = locationAvailable,
                onUseLocation = onUseLocation,
                onPickOnMap = onPickOnMap,
            )
        }

        if (nearby.isNotEmpty()) {
            item("nearby_header") {
                SectionHeader(text = stringResource(R.string.planner_picker_section_nearby))
            }
            items(nearby, key = { "nearby_${it.first.id}" }) { (stop, meters) ->
                StopResultRow(
                    stop = stop,
                    distanceMeters = meters,
                    onClick = { onStopSelected(stop) },
                )
                IndentedDivider()
            }
        }

        if (recents.isNotEmpty()) {
            item("recent_header") {
                SectionHeader(text = stringResource(R.string.planner_picker_section_recent))
            }
            items(recents.take(6), key = { "recent_${it.id}" }) { stop ->
                StopResultRow(
                    stop = stop,
                    distanceMeters = null,
                    leadingIconRes = LucideIcons.Clock,
                    onClick = { onStopSelected(stop) },
                )
                IndentedDivider()
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    val colors = TransitTheme.colors
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = colors.textTertiary,
        modifier = Modifier.padding(start = 20.dp, top = 20.dp, bottom = 6.dp),
    )
}

@Composable
private fun IndentedDivider() {
    val colors = TransitTheme.colors
    HorizontalDivider(
        thickness = 0.5.dp,
        color = colors.separator,
        modifier = Modifier.padding(start = 56.dp),
    )
}

// ── Quick choices ─────────────────────────────────────────────────────────────

@Composable
private fun QuickChoicesSection(
    locationAvailable: Boolean,
    onUseLocation: () -> Unit,
    onPickOnMap: () -> Unit,
) {
    val colors = TransitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Primary action: My location (filled accent background when available)
        QuickChoiceRow(
            iconRes = LucideIcons.Crosshair,
            iconTint = if (locationAvailable) colors.accent else colors.textTertiary,
            iconBackground = if (locationAvailable) colors.accent.copy(alpha = 0.12f) else colors.glassFill,
            title = stringResource(R.string.planner_picker_my_location),
            subtitle = if (locationAvailable)
                stringResource(R.string.planner_picker_location_active)
            else
                stringResource(R.string.planner_picker_location_inactive),
            isPrimary = true,
            enabled = locationAvailable,
            onClick = onUseLocation,
        )
        // Secondary action: Pick on map (lighter visual weight)
        QuickChoiceRow(
            iconRes = LucideIcons.MapPin,
            iconTint = colors.textSecondary,
            iconBackground = colors.glassFill,
            title = stringResource(R.string.planner_picker_pick_on_map),
            subtitle = stringResource(R.string.planner_picker_pick_on_map_sub),
            isPrimary = false,
            enabled = true,
            onClick = onPickOnMap,
        )
    }
}

@Composable
private fun QuickChoiceRow(
    iconRes: Int,
    iconTint: androidx.compose.ui.graphics.Color,
    iconBackground: androidx.compose.ui.graphics.Color,
    title: String,
    subtitle: String,
    isPrimary: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val containerColor = if (isPrimary) colors.bgSecondary else colors.glassFill.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .background(containerColor, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(iconBackground, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isPrimary) FontWeight.SemiBold else FontWeight.Medium,
                color = if (enabled) colors.textPrimary else colors.textTertiary,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
            )
        }
        Icon(
            painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(16.dp),
        )
    }
}

// ── Search results ────────────────────────────────────────────────────────────

@Composable
private fun StopResultsList(
    isFiltering: Boolean,
    results: List<ResolvedStop>,
    onSelect: (ResolvedStop) -> Unit,
) {
    val colors = TransitTheme.colors
    when {
        isFiltering && results.isEmpty() -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(22.dp),
                    color = colors.accent,
                    strokeWidth = 2.dp,
                )
            }
        }
        results.isEmpty() -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    painterResource(LucideIcons.Search),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(28.dp),
                )
                Text(
                    stringResource(R.string.planner_picker_no_results),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textTertiary,
                )
            }
        }
        else -> {
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                items(results, key = { "result_${it.id}" }) { stop ->
                    StopResultRow(
                        stop = stop,
                        distanceMeters = null,
                        onClick = { onSelect(stop) },
                    )
                    IndentedDivider()
                }
            }
        }
    }
}

@Composable
private fun StopResultRow(
    stop: ResolvedStop,
    distanceMeters: Double?,
    leadingIconRes: Int? = null,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = stop.name },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(colors.glassFill, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(id = leadingIconRes ?: stopIcon(stop.transitTypes)),
                contentDescription = null,
                tint = if (leadingIconRes != null) colors.textSecondary else colors.accent,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = when {
                distanceMeters != null && stop.routeNames.isNotEmpty() ->
                    stringResource(R.string.planner_picker_walking_distance, distanceMeters.roundToInt()) +
                        " · " + stop.routeNames.take(4).joinToString(" · ")
                distanceMeters != null ->
                    stringResource(R.string.planner_picker_walking_distance, distanceMeters.roundToInt())
                stop.routeNames.isNotEmpty() ->
                    stop.routeNames.take(5).joinToString(" · ")
                else -> ""
            }
            if (sub.isNotEmpty()) {
                Text(
                    text = sub,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
