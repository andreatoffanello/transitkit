package com.transitkit.app.ui.mappa

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.LocalTransitColors
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.data.model.ResolvedStop
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize
import com.transitkit.app.ui.components.TimeDisplay
import com.transitkit.app.ui.components.departureTimeState

// ---------------------------------------------------------------------------
// Preview card container — shell condivisa da stop + vehicle preview
// ---------------------------------------------------------------------------

@Composable
internal fun PreviewCardContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val colors = LocalTransitColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(18.dp))
            .clip(RoundedCornerShape(18.dp))
            .background(colors.bgSecondary),
    ) {
        content()
    }
}

// ---------------------------------------------------------------------------
// Stop preview content
// ---------------------------------------------------------------------------

@Composable
internal fun StopPreviewContent(
    stop: ResolvedStop,
    departures: List<ResolvedDeparture>,
    isLoading: Boolean,
    onClose: () -> Unit,
    onNavigateToStop: (stopId: String) -> Unit = {},
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(top = 16.dp, bottom = 20.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = TransitTheme.colors.textPrimary,
                modifier = Modifier.weight(1f),
            )
            IconButton(
                onClick = onClose,
                modifier = Modifier.semantics { testTag = "btn_stop_sheet_close" },
            ) {
                Icon(
                    painter = painterResource(LucideIcons.X),
                    contentDescription = stringResource(R.string.action_chiudi),
                    modifier = Modifier.size(20.dp),
                )
            }
        }

        val distinctTransitTypes = stop.transitTypes.distinct().ifEmpty {
            departures.map { it.transitType }.distinct()
        }
        if (distinctTransitTypes.isNotEmpty()) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp),
            ) {
                distinctTransitTypes.forEach { type ->
                    Icon(
                        painter = painterResource(iconForTransitType(type)),
                        contentDescription = null,
                        tint = TransitTheme.colors.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }

        val staticRoutes = stop.routeNames.mapIndexed { i, name ->
            Triple(name, stop.routeIds.getOrElse(i) { name }, stop.routeColors.getOrElse(i) { "" })
        }
        if (staticRoutes.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 0.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                items(staticRoutes) { (routeName, _, routeColor) ->
                    LineBadge(
                        name = routeName,
                        colorHex = routeColor.takeIf { it.isNotBlank() },
                        size = LineBadgeSize.Medium,
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.height(12.dp))
        }

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp,
                        color = TransitTheme.colors.accent,
                    )
                }
            }
            departures.isEmpty() -> {
                Text(
                    text = stringResource(R.string.mappa_nessuna_partenza),
                    style = MaterialTheme.typography.bodySmall,
                    color = TransitTheme.colors.textSecondary,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            }
            else -> {
                departures.take(5).forEach { dep ->
                    SheetDepartureRow(dep)
                }
            }
        }

        val accentColor = LocalTransitColors.current.accent
        Button(
            onClick = { onNavigateToStop(stop.id) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Icon(painterResource(LucideIcons.Clock), null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.mappa_vedi_orari),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        val context = LocalContext.current
        OutlinedButton(
            onClick = {
                val lat = stop.lat
                val lon = stop.lon
                val uri = android.net.Uri.parse(
                    "geo:$lat,$lon?q=$lat,$lon(${android.net.Uri.encode(stop.name)})"
                )
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                if (intent.resolveActivity(context.packageManager) != null) {
                    context.startActivity(intent)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            border = BorderStroke(1.dp, accentColor.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
        ) {
            Icon(painterResource(LucideIcons.MapPin), null, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(R.string.mappa_apri_maps),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SheetDepartureRow(departure: ResolvedDeparture) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        LineBadge(departure = departure, size = LineBadgeSize.Small)
        Text(
            text = departure.headsign,
            style = MaterialTheme.typography.bodySmall,
            color = LocalTransitColors.current.textPrimary,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TimeDisplay(state = departureTimeState(departure.departureTime))
    }
}
