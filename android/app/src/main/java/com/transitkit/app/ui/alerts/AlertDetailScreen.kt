package com.transitkit.app.ui.alerts

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.platform.LocalContext
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
import com.transitkit.app.config.toColor
import com.transitkit.app.data.model.AlertCause
import com.transitkit.app.data.model.AlertEffect
import com.transitkit.app.data.model.AlertSeverity
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.data.model.ScheduleRoute
import com.transitkit.app.data.model.ServiceAlert
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun AlertDetailScreen(
    alertId: String,
    onBack: () -> Unit,
    viewModel: AlertsViewModel = hiltViewModel(),
) {
    val alerts by viewModel.alerts.collectAsStateWithLifecycle()
    val routesById by viewModel.routesById.collectAsStateWithLifecycle()
    val stopsById by viewModel.stopsById.collectAsStateWithLifecycle()
    val alert = alerts.firstOrNull { it.id == alertId }
    val colors = TransitTheme.colors
    val context = LocalContext.current

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
                            tint = colors.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
            )
        },
        containerColor = colors.background,
    ) { padding ->
        if (alert == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    stringResource(R.string.alerts_title),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                )
            }
            return@Scaffold
        }

        val affectedRoutes = alert.affectedRouteIds.mapNotNull { routesById[it] }
            .sortedBy { it.name.ifEmpty { it.longName.ifEmpty { it.id } } }
        val affectedStops = alert.affectedStopIds.mapNotNull { stopsById[it] }
            .sortedBy { it.name }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Hero
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SeverityPill(severity = alert.severity)
                    Text(
                        text = localizedHeader(alert),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                    )
                }
            }

            // Description card
            item {
                val body = localizedDescription(alert)
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
                    border = BorderStroke(1.dp, colors.glassBorder),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Text(
                        text = body.ifEmpty { stringResource(R.string.alert_no_description) },
                        style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }

            // Metadata card — effective period, cause, effect, "Affects all".
            // Filled-in only when at least one row has content; avoids an empty
            // card on alerts with description-only payload.
            metadataCardItem(alert = alert, affectedRoutes = affectedRoutes, affectedStops = affectedStops)

            // Affected routes
            if (affectedRoutes.isNotEmpty()) {
                item {
                    LabelledAlertCard(
                        icon = LucideIcons.Route,
                        label = stringResource(R.string.alerts_affected_routes),
                    ) {
                        FlowRowRoutes(routes = affectedRoutes)
                    }
                }
            }

            // Affected stops
            if (affectedStops.isNotEmpty()) {
                item {
                    LabelledAlertCard(
                        icon = LucideIcons.MapPin,
                        label = stringResource(R.string.alerts_affected_stops),
                    ) {
                        StopList(stops = affectedStops)
                    }
                }
            }

            // Read more
            val url = alert.url
            if (!url.isNullOrBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.bgSecondary)
                            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
                            .clickable {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Icon(
                            painter = painterResource(LucideIcons.ExternalLink),
                            contentDescription = null,
                            tint = colors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            stringResource(R.string.alerts_read_more),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f),
                        )
                        Icon(
                            painter = painterResource(LucideIcons.ChevronRight),
                            contentDescription = null,
                            tint = colors.textTertiary,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

/**
 * Adds the metadata card to a LazyColumn when there's something to show.
 * Items are: effective period, cause, effect, plus an "Affects all service"
 * footer when no specific routes/stops are listed (alert is system-wide).
 */
private fun androidx.compose.foundation.lazy.LazyListScope.metadataCardItem(
    alert: ServiceAlert,
    affectedRoutes: List<ScheduleRoute>,
    affectedStops: List<ResolvedStop>,
) {
    val periodStart = alert.activePeriods.mapNotNull { it.start }.minOrNull()
    val periodEnd = alert.activePeriods.mapNotNull { it.end }.maxOrNull()
    val hasPeriod = periodStart != null || periodEnd != null
    val hasCause = alert.cause != AlertCause.UNKNOWN_CAUSE
    val hasEffect = alert.effect != AlertEffect.UNKNOWN_EFFECT && alert.effect != AlertEffect.NO_EFFECT
    val affectsAll = affectedRoutes.isEmpty() && affectedStops.isEmpty()
    if (!hasPeriod && !hasCause && !hasEffect && !affectsAll) return

    item {
        val colors = TransitTheme.colors
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
            border = BorderStroke(1.dp, colors.glassBorder),
            elevation = CardDefaults.cardElevation(0.dp),
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (hasPeriod) {
                    MetadataRow(
                        icon = LucideIcons.Clock,
                        label = stringResource(R.string.alert_meta_when),
                        value = formatPeriod(periodStart, periodEnd),
                    )
                }
                if (hasCause) {
                    MetadataRow(
                        icon = LucideIcons.Info,
                        label = stringResource(R.string.alert_meta_cause),
                        value = causeLabel(alert.cause),
                    )
                }
                if (hasEffect) {
                    MetadataRow(
                        icon = LucideIcons.AlertTriangle,
                        label = stringResource(R.string.alert_meta_effect),
                        value = effectLabel(alert.effect),
                    )
                }
                if (affectsAll) {
                    MetadataRow(
                        icon = LucideIcons.BusFront,
                        label = stringResource(R.string.alert_affects_all),
                        value = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(icon: Int, label: String, value: String?) {
    val colors = TransitTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.accent,
            modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = colors.textTertiary,
                letterSpacing = 0.5.sp,
            )
            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                )
            }
        }
    }
}

private val periodFormatter: SimpleDateFormat by lazy {
    SimpleDateFormat("d MMM, HH:mm", Locale.getDefault())
}

private fun formatPeriod(start: Long?, end: Long?): String {
    val fmt = periodFormatter
    val s = start?.let { fmt.format(Date(it * 1000L)) }
    val e = end?.let { fmt.format(Date(it * 1000L)) }
    return when {
        s != null && e != null -> "$s — $e"
        s != null -> "From $s"
        e != null -> "Until $e"
        else -> ""
    }
}

@Composable
private fun causeLabel(cause: AlertCause): String = when (cause) {
    AlertCause.TECHNICAL_PROBLEM -> stringResource(R.string.alert_cause_technical)
    AlertCause.STRIKE -> stringResource(R.string.alert_cause_strike)
    AlertCause.DEMONSTRATION -> stringResource(R.string.alert_cause_demonstration)
    AlertCause.ACCIDENT -> stringResource(R.string.alert_cause_accident)
    AlertCause.HOLIDAY -> stringResource(R.string.alert_cause_holiday)
    AlertCause.WEATHER -> stringResource(R.string.alert_cause_weather)
    AlertCause.MAINTENANCE -> stringResource(R.string.alert_cause_maintenance)
    AlertCause.CONSTRUCTION -> stringResource(R.string.alert_cause_construction)
    AlertCause.POLICE_ACTIVITY -> stringResource(R.string.alert_cause_police)
    AlertCause.MEDICAL_EMERGENCY -> stringResource(R.string.alert_cause_medical)
    AlertCause.OTHER_CAUSE -> stringResource(R.string.alert_cause_other)
    AlertCause.UNKNOWN_CAUSE -> stringResource(R.string.alert_cause_other)
}

@Composable
private fun effectLabel(effect: AlertEffect): String = when (effect) {
    AlertEffect.NO_SERVICE -> stringResource(R.string.alert_effect_no_service)
    AlertEffect.REDUCED_SERVICE -> stringResource(R.string.alert_effect_reduced_service)
    AlertEffect.SIGNIFICANT_DELAYS -> stringResource(R.string.alert_effect_delays)
    AlertEffect.DETOUR -> stringResource(R.string.alert_effect_detour)
    AlertEffect.ADDITIONAL_SERVICE -> stringResource(R.string.alert_effect_additional)
    AlertEffect.MODIFIED_SERVICE -> stringResource(R.string.alert_effect_modified)
    AlertEffect.STOP_MOVED -> stringResource(R.string.alert_effect_stop_moved)
    AlertEffect.ACCESSIBILITY_ISSUE -> stringResource(R.string.alert_effect_accessibility)
    AlertEffect.OTHER_EFFECT -> stringResource(R.string.alert_effect_other)
    AlertEffect.UNKNOWN_EFFECT -> stringResource(R.string.alert_effect_other)
    AlertEffect.NO_EFFECT -> stringResource(R.string.alert_effect_other)
}

@Composable
private fun SeverityPill(severity: AlertSeverity) {
    val color = severityColor(severity)
    val label = when (severity) {
        AlertSeverity.SEVERE  -> stringResource(R.string.alert_severity_severe)
        AlertSeverity.WARNING -> stringResource(R.string.alert_severity_warning)
        AlertSeverity.INFO    -> stringResource(R.string.alert_severity_info)
        AlertSeverity.UNKNOWN -> stringResource(R.string.alert_severity_advisory)
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color),
        )
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = color,
            letterSpacing = 0.6.sp,
        )
    }
}

@Composable
private fun LabelledAlertCard(
    icon: Int,
    label: String,
    content: @Composable () -> Unit,
) {
    val colors = TransitTheme.colors
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colors.bgSecondary),
        border = BorderStroke(1.dp, colors.glassBorder),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp),
            ) {
                Icon(
                    painter = painterResource(icon),
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textTertiary,
                    letterSpacing = 0.6.sp,
                )
            }
            content()
        }
    }
}

/**
 * Compact wrapping row of route pills. Uses plain Row with wrap via the new
 * `FlowRow` in Compose M3 if available — fallback: single Row that clips.
 */
@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun FlowRowRoutes(routes: List<ScheduleRoute>) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        routes.forEach { route ->
            RoutePill(route)
        }
    }
}

@Composable
private fun RoutePill(route: ScheduleRoute) {
    val label = route.name.ifEmpty { route.longName.ifEmpty { route.id } }
    com.transitkit.app.ui.components.LineBadge(
        name = label,
        colorHex = route.color.takeIf { it.isNotBlank() },
        textColorHex = route.textColor.takeIf { it.isNotBlank() },
        size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
    )
}

@Composable
private fun StopList(stops: List<ResolvedStop>) {
    val colors = TransitTheme.colors
    Column {
        stops.forEachIndexed { index, stop ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(vertical = 10.dp),
            ) {
                Icon(
                    painter = painterResource(LucideIcons.MapPin),
                    contentDescription = null,
                    tint = colors.textTertiary,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = stop.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                )
            }
            if (index < stops.size - 1) {
                HorizontalDivider(color = colors.separator)
            }
        }
    }
}
