package com.transitkit.app.ui.orari

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.ui.components.TransitSearchBar

@Composable
fun OrariScreen(
    onNavigateToStop: (stopId: String, stopName: String) -> Unit = { _, _ -> },
    onNavigateToLine: (routeId: String) -> Unit = { },
    viewModel: OrariViewModel = hiltViewModel(),
) {
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val stops by viewModel.stops.collectAsStateWithLifecycle()
    val recentStopIds by viewModel.recentStopIds.collectAsStateWithLifecycle()
    val selectedTransitType by viewModel.selectedTransitType.collectAsStateWithLifecycle()
    val availableTransitTypes by viewModel.availableTransitTypes.collectAsStateWithLifecycle()
    val groupedStops by viewModel.groupedStops.collectAsStateWithLifecycle()
    val scheduleLoadError by viewModel.scheduleLoadError.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Text(
            text = stringResource(R.string.tab_orari),
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold,
                fontSize = 17.sp,
            ),
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )
        if (scheduleLoadError != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.bgSecondary)
                    .border(1.dp, colors.realtimeRed.copy(alpha = 0.20f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 11.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(painterResource(LucideIcons.WifiOff), contentDescription = null, tint = colors.realtimeRed, modifier = Modifier.size(16.dp))
                Text(scheduleLoadError!!, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
            }
        }
        // Search bar
        TransitSearchBar(
            query = searchQuery,
            placeholder = stringResource(R.string.search_placeholder_stops),
            onQueryChange = viewModel::onSearchQueryChanged,
            a11yTag = "search_schedules",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        )

        // Loading indicator — shown below search bar during initial data load
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = colors.accent,
                trackColor = colors.accent.copy(alpha = 0.12f),
            )
        }

        HorizontalDivider(color = colors.separator, thickness = 0.5.dp)

        StopsTab(
            stops = stops,
            groupedStops = groupedStops,
            searchQuery = searchQuery,
            recentStopIds = recentStopIds,
            availableTransitTypes = availableTransitTypes,
            selectedTransitType = selectedTransitType,
            onTransitTypeSelected = viewModel::selectTransitType,
            onStopClick = { stop ->
                viewModel.recordStopVisit(stop.id)
                onNavigateToStop(stop.id, stop.name)
            },
        )
    }
}
