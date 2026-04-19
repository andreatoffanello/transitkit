package com.transitkit.app.ui.alerts

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.transitkit.app.R
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.AlertEffect
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.ServiceAlert

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertListScreen(
    onBack: () -> Unit,
    onNavigateToAlert: (alertId: String) -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val sorted = alerts.sortedWith(
        compareByDescending<ServiceAlert> { it.severity.raw }
            .thenBy { localizedHeader(it) }
    )
    val colors = TransitTheme.colors

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
                    IconButton(onClick = onBack) {
                        Icon(
                            painterResource(LucideIcons.ChevronLeft),
                            contentDescription = stringResource(R.string.cd_indietro),
                            tint = colors.accent,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        if (sorted.isEmpty()) {
            AlertEmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 16.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(sorted, key = { it.id }) { alert ->
                    AlertRow(
                        alert = alert,
                        onClick = { onNavigateToAlert(alert.id) },
                    )
                }
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
                painter = painterResource(LucideIcons.ChevronRight),
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

@Composable
internal fun AlertRow(
    alert: ServiceAlert,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                BorderStroke(1.dp, colors.glassBorder),
                RoundedCornerShape(14.dp),
            ),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(severityColor(alert.severity)),
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedHeader(alert),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                )
                val subtitle = alertSubtitle(alert)
                if (!subtitle.isNullOrEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textTertiary,
                        maxLines = 1,
                    )
                }
            }

            Icon(
                painter = painterResource(LucideIcons.ChevronRight),
                contentDescription = null,
                tint = colors.textTertiary,
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(14.dp),
            )
        }
    }
}

// -- Shared helpers (also used by AlertDetailScreen) ---------------------------

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
        AlertSeverity.SEVERE  -> Color(0xFFDC2626) // red-600
        AlertSeverity.WARNING -> Color(0xFFD97706) // amber-600
        AlertSeverity.INFO    -> colors.accent
        AlertSeverity.UNKNOWN -> colors.textTertiary
    }
}

@Composable
internal fun alertSubtitle(alert: ServiceAlert): String? {
    val parts = mutableListOf<String>()
    if (alert.affectedRouteIds.isNotEmpty()) {
        parts += stringResource(R.string.alerts_affected_routes_count, alert.affectedRouteIds.size)
    }
    if (alert.affectedStopIds.isNotEmpty()) {
        parts += stringResource(R.string.alerts_affected_stops_count, alert.affectedStopIds.size)
    }
    if (parts.isNotEmpty()) return parts.joinToString(" · ")
    return effectLabel(alert.effect)
}

@Composable
internal fun effectLabel(effect: AlertEffect): String? = when (effect) {
    AlertEffect.NO_SERVICE          -> stringResource(R.string.alert_effect_no_service)
    AlertEffect.REDUCED_SERVICE     -> stringResource(R.string.alert_effect_reduced_service)
    AlertEffect.SIGNIFICANT_DELAYS  -> stringResource(R.string.alert_effect_delays)
    AlertEffect.DETOUR              -> stringResource(R.string.alert_effect_detour)
    AlertEffect.STOP_MOVED          -> stringResource(R.string.alert_effect_stop_moved)
    AlertEffect.ADDITIONAL_SERVICE  -> stringResource(R.string.alert_effect_additional)
    AlertEffect.MODIFIED_SERVICE    -> stringResource(R.string.alert_effect_modified)
    AlertEffect.ACCESSIBILITY_ISSUE -> stringResource(R.string.alert_effect_accessibility)
    AlertEffect.NO_EFFECT, AlertEffect.UNKNOWN_EFFECT, AlertEffect.OTHER_EFFECT -> null
}
