package com.transitkit.app.ui.planner

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.api.GeocodeResult
import com.transitkit.app.data.api.MapboxGeocodingService
import com.transitkit.app.data.model.PlannerLocation
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.store.SavedPlace
import com.transitkit.app.data.store.SavedPlaceKeys
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.stopIcon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Full-screen stop / location picker. Doppio uso via [assignKey]:
 * - null: seleziona una posizione come origine/destinazione del viaggio.
 * - non-null: modalità assign — la stessa UI ma salva solo la scorciatoia
 *   casa/lavoro e torna indietro senza toccare il viaggio. Parità iOS
 *   LocationPickerSearch + LocationPickerAssignSheet.
 *
 * La pagina assign è raggiunta via push navigation (route
 * `location_picker_assign/{key}`) — MAI tramite Dialog/BottomSheet.
 *
 * role   = "origin" | "destination"
 * assignKey = null | "home" | "work"
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationPickerScreen(
    role: String,
    source: String,
    assignKey: String? = null,
    plannerViewModel: PlannerViewModel,
    onBack: () -> Unit,
    onNavigateToMapPicker: () -> Unit,
    onNavigateToAssign: ((key: String) -> Unit)? = null,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val isAssigning = assignKey != null

    val allStops by plannerViewModel.allStops.collectAsStateWithLifecycle()
    val currentLocation by plannerViewModel.currentLocation.collectAsStateWithLifecycle()
    val recentStops by plannerViewModel.recentStops.collectAsStateWithLifecycle()
    val savedPlaces by plannerViewModel.savedPlacesStore.savedPlaces.collectAsStateWithLifecycle()

    // In assign mode, pre-fill the search field with the existing saved address (if any).
    val initialQuery = remember(assignKey) {
        if (assignKey != null) plannerViewModel.savedPlacesStore.savedPlace(assignKey)?.name ?: "" else ""
    }
    var query by remember { mutableStateOf(initialQuery) }
    var isSearching by remember { mutableStateOf(false) }
    var geocodeResults by remember { mutableStateOf<List<GeocodeResult>>(emptyList()) }
    val focusRequester = remember { FocusRequester() }

    val mapCenter = plannerViewModel.mapFallbackCenter
    val language = java.util.Locale.getDefault().language.takeIf { it.isNotBlank() } ?: "en"
    val operatorCountry = plannerViewModel.operatorCountry

    // Filtered GTFS stops
    val filteredStops = remember(query, allStops) {
        if (query.isBlank()) emptyList()
        else allStops.asSequence()
            .filter { it.name.contains(query.trim(), ignoreCase = true) }
            .take(40)
            .toList()
    }

    // Debounced forward geocode (only when query is long enough)
    LaunchedEffect(query) {
        val trimmed = query.trim()
        if (trimmed.length < 2) {
            geocodeResults = emptyList()
            isSearching = query.isNotBlank()
            return@LaunchedEffect
        }
        isSearching = true
        delay(300)
        val bias = currentLocation ?: Pair(mapCenter.first, mapCenter.second)
        geocodeResults = MapboxGeocodingService.forwardGeocode(
            query = trimmed,
            biasNear = bias,
            language = language,
            country = operatorCountry,
        )
        isSearching = false
    }

    LaunchedEffect(Unit) {
        delay(200)
        focusRequester.requestFocus()
    }

    // Commit in assign mode → persist scorciatoia e torna indietro.
    // Commit in select mode → aggiorna origine/destinazione del planner.
    fun commit(name: String, lat: Double, lon: Double, gtfsId: String?, kind: PlannerLocation.Kind) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (assignKey != null) {
            if (kind == PlannerLocation.Kind.CurrentLocation) {
                // Resolve readable name before saving
                scope.launch {
                    val resolved = MapboxGeocodingService.reverseGeocode(lat, lon, language)
                    plannerViewModel.savedPlacesStore.setPlace(assignKey, resolved?.takeIf { it.isNotBlank() } ?: name, lat, lon)
                    onBack()
                }
            } else {
                plannerViewModel.savedPlacesStore.setPlace(assignKey, name, lat, lon)
                onBack()
            }
        } else {
            val loc = PlannerLocation(kind = kind, name = name, lat = lat, lon = lon, stopId = gtfsId)
            when (source) {
                "home" -> if (role == "origin") plannerViewModel.setHomeOrigin(loc)
                          else plannerViewModel.setHomeDestination(loc)
                else   -> if (role == "origin") plannerViewModel.setOrigin(loc)
                          else plannerViewModel.setDestination(loc)
            }
            onBack()
        }
    }

    val myLocationLabel = stringResource(R.string.planner_picker_my_location)

    val title = when (assignKey) {
        SavedPlaceKeys.HOME -> stringResource(R.string.planner_picker_assign_home)
        SavedPlaceKeys.WORK -> stringResource(R.string.planner_picker_assign_work)
        else -> if (role == "origin") stringResource(R.string.planner_picker_origin_title)
                else stringResource(R.string.planner_picker_destination_title)
    }

    val nearby = remember(currentLocation, allStops) {
        if (currentLocation == null || allStops.isEmpty()) emptyList()
        else plannerViewModel.nearbyStops(limit = 5)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
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
                        Text(stringResource(R.string.cancel), color = colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
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
                    .semantics { contentDescription = "Search stops and places" },
            )

            if (query.isEmpty()) {
                EmptyStateContent(
                    isAssigning = isAssigning,
                    locationAvailable = currentLocation != null,
                    savedHome = savedPlaces[SavedPlaceKeys.HOME],
                    savedWork = savedPlaces[SavedPlaceKeys.WORK],
                    onUseSaved = { place ->
                        commit(place.name, place.lat, place.lon, null, PlannerLocation.Kind.Place)
                    },
                    onAssign = { key -> onNavigateToAssign?.invoke(key) },
                    onRemove = { key -> plannerViewModel.savedPlacesStore.removePlace(key) },
                    onUseLocation = {
                        val loc = currentLocation
                        if (loc != null) {
                            commit(myLocationLabel, loc.first, loc.second, null, PlannerLocation.Kind.CurrentLocation)
                        }
                    },
                    onPickOnMap = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onNavigateToMapPicker()
                    },
                    nearby = nearby,
                    recents = recentStops,
                    onStopSelected = { stop ->
                        commit(stop.name, stop.lat, stop.lon, stop.id, PlannerLocation.Kind.Stop)
                    },
                )
            } else {
                MixedResultsList(
                    isSearching = isSearching,
                    stops = filteredStops,
                    places = geocodeResults,
                    onSelectStop = { stop ->
                        commit(stop.name, stop.lat, stop.lon, stop.id, PlannerLocation.Kind.Stop)
                    },
                    onSelectPlace = { result ->
                        commit(result.name, result.lat, result.lon, null, PlannerLocation.Kind.Place)
                    },
                )
            }
        }
    }
}

// ── Empty state (no query) ────────────────────────────────────────────────────

@Composable
private fun EmptyStateContent(
    isAssigning: Boolean,
    locationAvailable: Boolean,
    savedHome: SavedPlace?,
    savedWork: SavedPlace?,
    onUseSaved: (SavedPlace) -> Unit,
    onAssign: (String) -> Unit,
    onRemove: (String) -> Unit,
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
                isAssigning = isAssigning,
                locationAvailable = locationAvailable,
                savedHome = savedHome,
                savedWork = savedWork,
                onUseSaved = onUseSaved,
                onAssign = onAssign,
                onRemove = onRemove,
                onUseLocation = onUseLocation,
                onPickOnMap = onPickOnMap,
            )
        }

        if (!isAssigning && nearby.isNotEmpty()) {
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

        if (!isAssigning && recents.isNotEmpty()) {
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

// ── Quick choices + saved places ─────────────────────────────────────────────

@Composable
private fun QuickChoicesSection(
    isAssigning: Boolean,
    locationAvailable: Boolean,
    savedHome: SavedPlace?,
    savedWork: SavedPlace?,
    onUseSaved: (SavedPlace) -> Unit,
    onAssign: (String) -> Unit,
    onRemove: (String) -> Unit,
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
        // Casa/Lavoro — non mostrare in assign mode (stai già impostando una)
        if (!isAssigning) {
            SavedPlaceRow(
                iconRes = LucideIcons.Home,
                title = stringResource(R.string.planner_picker_home),
                saved = savedHome,
                onUse = { savedHome?.let(onUseSaved) },
                onAssign = { onAssign(SavedPlaceKeys.HOME) },
                onRemove = { onRemove(SavedPlaceKeys.HOME) },
            )
            SavedPlaceRow(
                iconRes = LucideIcons.Briefcase,
                title = stringResource(R.string.planner_picker_work),
                saved = savedWork,
                onUse = { savedWork?.let(onUseSaved) },
                onAssign = { onAssign(SavedPlaceKeys.WORK) },
                onRemove = { onRemove(SavedPlaceKeys.WORK) },
            )
        }

        // My location
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

        // Pick on map
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

/** Scorciatoia casa/lavoro. Impostata → tap = usa, menu ⋯ = Modifica/Rimuovi.
 *  Non impostata → tap = apre la pagina assign (via onAssign), chevron a destra. */
@Composable
private fun SavedPlaceRow(
    iconRes: Int,
    title: String,
    saved: SavedPlace?,
    onUse: () -> Unit,
    onAssign: () -> Unit,
    onRemove: () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (saved != null) onUse() else onAssign() }
            .background(colors.bgSecondary, RoundedCornerShape(14.dp))
            .padding(start = 14.dp, top = 12.dp, bottom = 12.dp, end = 4.dp)
            .semantics { testTag = "saved_place_row_$title" },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(colors.accent.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painterResource(iconRes),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(18.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
            )
            Text(
                text = saved?.name ?: stringResource(R.string.planner_picker_set_address),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (saved != null) {
            var menuExpanded by remember { mutableStateOf(false) }
            Box {
                IconButton(
                    onClick = { menuExpanded = true },
                    modifier = Modifier
                        .size(36.dp)
                        .semantics { testTag = "saved_place_menu_$title" },
                ) {
                    Icon(
                        painterResource(LucideIcons.MoreHorizontal),
                        contentDescription = title,
                        tint = colors.textTertiary,
                        modifier = Modifier.size(18.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.planner_picker_edit)) },
                        leadingIcon = {
                            Icon(
                                painterResource(LucideIcons.Pencil),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = { menuExpanded = false; onAssign() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.planner_picker_remove),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painterResource(LucideIcons.Trash2),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                        onClick = { menuExpanded = false; onRemove() },
                    )
                }
            }
        } else {
            Icon(
                painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .size(16.dp)
                    .padding(end = 8.dp),
            )
        }
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
    // Use bgSecondary for both variants: glassFill.copy(alpha=0.5f) replaces (not
    // multiplies) the alpha and renders as 50%-white in dark mode — a light-gray
    // card that makes icon and text illegible. bgSecondary is dark-aware.
    val containerColor = colors.bgSecondary
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

// ── Mixed results (stops + places) ───────────────────────────────────────────

@Composable
private fun MixedResultsList(
    isSearching: Boolean,
    stops: List<ResolvedStop>,
    places: List<GeocodeResult>,
    onSelectStop: (ResolvedStop) -> Unit,
    onSelectPlace: (GeocodeResult) -> Unit,
) {
    val colors = TransitTheme.colors
    when {
        isSearching && stops.isEmpty() && places.isEmpty() -> {
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
        stops.isEmpty() && places.isEmpty() -> {
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
            // Partiziona: fermate GTFS (più rilevanti) + luoghi/indirizzi, con header sezione.
            // Parità iOS resultsList: "FERMATE" / "LUOGHI".
            LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
                if (stops.isNotEmpty()) {
                    item("header_stops") {
                        SectionHeader(stringResource(R.string.planner_picker_section_stops))
                    }
                    items(stops, key = { "stop_${it.id}" }) { stop ->
                        StopResultRow(stop = stop, distanceMeters = null, onClick = { onSelectStop(stop) })
                        IndentedDivider()
                    }
                }
                if (places.isNotEmpty()) {
                    item("header_places") {
                        SectionHeader(stringResource(R.string.planner_picker_section_places))
                    }
                    items(places, key = { "place_${it.id}" }) { result ->
                        PlaceResultRow(result = result, onClick = { onSelectPlace(result) })
                        IndentedDivider()
                    }
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
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            // Distance label (if applicable)
            if (distanceMeters != null) {
                Text(
                    text = stringResource(R.string.planner_picker_walking_distance, distanceMeters.roundToInt()),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textTertiary,
                    maxLines = 1,
                )
            }
            // Line badges: use colored pills when color data is available,
            // fallback to plain text when routeColors is empty (legacy data).
            if (stop.routeNames.isNotEmpty()) {
                if (stop.routeColors.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        stop.routeNames.take(5).forEachIndexed { i, routeName ->
                            LineBadge(
                                name = routeName.take(5),
                                colorHex = stop.routeColors.getOrNull(i),
                                size = LineBadgeSize.Small,
                            )
                        }
                    }
                } else {
                    Text(
                        text = stop.routeNames.take(5).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceResultRow(
    result: GeocodeResult,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics { contentDescription = result.name },
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
                painterResource(LucideIcons.MapPin),
                contentDescription = null,
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = result.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            val subtitle = result.locality?.takeIf { it.isNotBlank() }
                ?: stringResource(R.string.planner_picker_result_place)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textTertiary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
