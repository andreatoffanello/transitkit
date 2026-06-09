package com.transitkit.app.ui.alerts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.ServiceAlert

private enum class AlertFilter { Mine, All }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    onBack: (() -> Unit)? = null,
    onNavigateToAlert: (alertId: String) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val routesById by viewModel.routesById.collectAsStateWithLifecycle()
    val myLineIds by viewModel.favoriteRouteIds.collectAsStateWithLifecycle()
    val colors = TransitTheme.colors

    var filter by rememberSaveable { mutableStateOf(AlertFilter.Mine) }
    LaunchedEffect(myLineIds) {
        if (myLineIds.isEmpty() && filter == AlertFilter.Mine) {
            filter = AlertFilter.All
        }
    }

    val myAlerts = remember(alerts, myLineIds) {
        alerts.filter { it.isRelevant(myLineIds) }
    }
    val orderedAll = remember(alerts, myLineIds) {
        alerts.sortedWith(
            compareByDescending<ServiceAlert> { it.isRelevant(myLineIds) }
                .thenByDescending { it.firstActiveStart ?: 0L }
        )
    }
    val visible = if (filter == AlertFilter.Mine) myAlerts else orderedAll

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.alerts_title),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(
                                painterResource(LucideIcons.ChevronLeft),
                                contentDescription = stringResource(R.string.cd_indietro),
                                tint = colors.textPrimary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            FilterRow(
                filter = filter,
                mineCount = myAlerts.size,
                allCount = alerts.size,
                showMineChip = myLineIds.isNotEmpty(),
                onSelect = { filter = it },
            )

            when {
                visible.isEmpty() -> AlertEmptyState(
                    modifier = Modifier.fillMaxSize(),
                )
                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(visible, key = { it.id }) { alert ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            if (filter == AlertFilter.All
                                && myLineIds.isNotEmpty()
                                && alert.isRelevant(myLineIds)
                            ) {
                                Text(
                                    text = stringResource(R.string.alerts_your_line_badge),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        letterSpacing = 0.5.sp,
                                    ),
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 4.dp),
                                )
                            }
                            AlertCard(
                                alert = alert,
                                routesById = routesById,
                                onClick = { onNavigateToAlert(alert.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    filter: AlertFilter,
    mineCount: Int,
    allCount: Int,
    showMineChip: Boolean,
    onSelect: (AlertFilter) -> Unit,
) {
    val colors = TransitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showMineChip) {
            AlertFilterPill(
                title = stringResource(R.string.alerts_filter_mine),
                count = mineCount,
                isSelected = filter == AlertFilter.Mine,
                onClick = { onSelect(AlertFilter.Mine) },
                testTagId = "filter_mine",
            )
        }
        AlertFilterPill(
            title = stringResource(R.string.alerts_filter_all),
            count = allCount,
            isSelected = filter == AlertFilter.All,
            onClick = { onSelect(AlertFilter.All) },
            testTagId = "filter_all",
        )
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun AlertFilterPill(
    title: String,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    testTagId: String,
) {
    val colors = TransitTheme.colors
    val pillBg = if (isSelected) colors.accent.copy(alpha = 0.18f) else colors.bgSecondary.copy(alpha = 0.5f)
    val pillBorder = if (isSelected) colors.accent.copy(alpha = 0.5f) else colors.glassBorder
    Surface(
        modifier = Modifier
            .clickable(onClick = onClick)
            .semantics { testTag = testTagId },
        shape = CircleShape,
        color = pillBg,
        border = BorderStroke(1.dp, pillBorder),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (isSelected) colors.textPrimary else colors.textSecondary,
            )
            Surface(
                shape = CircleShape,
                color = if (isSelected) colors.accent.copy(alpha = 0.28f) else colors.bgSecondary,
            ) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isSelected) colors.textPrimary else colors.textTertiary,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
    }
}

@Composable
private fun AlertEmptyState(modifier: Modifier = Modifier) {
    val colors = TransitTheme.colors
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(colors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(LucideIcons.Bell),
                contentDescription = null,
                tint = colors.accent,
                modifier = Modifier.size(32.dp),
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.alerts_empty_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colors.textPrimary,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.alerts_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
        )
    }
}

// -- Shared helpers retained for AlertDetailScreen + StopDetailAlerts -----------

/** Picks the localized header from the per-language map, using resolved() semantics. */
internal fun localizedHeader(alert: ServiceAlert): String =
    alert.headerText.resolvedAlertText()

internal fun localizedDescription(alert: ServiceAlert): String =
    alert.descriptionText.resolvedAlertText()

/** Matches LocalizedText.resolved() — primary language → "en" → first available → key. */
internal fun Map<String, String>.resolvedAlertText(): String {
    val lang = java.util.Locale.getDefault().language.lowercase()
    return this[lang] ?: this["en"] ?: this[""] ?: values.firstOrNull() ?: ""
}

@Composable
internal fun severityColor(severity: AlertSeverity): Color {
    val colors = TransitTheme.colors
    return when (severity) {
        AlertSeverity.SEVERE  -> Color(0xFFDC2626)
        AlertSeverity.WARNING -> Color(0xFFD97706)
        AlertSeverity.INFO    -> colors.accent
        AlertSeverity.UNKNOWN -> colors.textTertiary
    }
}
