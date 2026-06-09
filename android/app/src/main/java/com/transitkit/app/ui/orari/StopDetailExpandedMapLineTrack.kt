package com.transitkit.app.ui.orari

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedDeparture
import com.transitkit.app.ui.components.LineBadge
import com.transitkit.app.ui.components.LineBadgeSize

/**
 * Track orizzontale di badge linea sopra la mappa espansa — selettore
 * della "vista linea". Nessuna card attorno: track trasparente
 * edge-to-edge, fade orizzontale ai bordi come affordance di scroll
 * (DstIn su layer offscreen). Selezionato = ring accent esterno;
 * gli altri attenuati a 0.45 quando c'è una selezione attiva.
 */
@Composable
internal fun ExpandedMapLineTrack(
    routes: List<ResolvedDeparture>,
    selectedRouteId: String?,
    onRouteTap: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val uniqueRoutes = remember(routes) { routes.distinctBy { it.routeId } }
    if (uniqueRoutes.isEmpty()) return

    LazyRow(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.00f to Color.Transparent,
                        0.05f to Color.Black,
                        0.90f to Color.Black,
                        1.00f to Color.Transparent,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            },
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(uniqueRoutes, key = { it.routeId }) { route ->
            val isSelected = selectedRouteId == route.routeId
            val anySelection = selectedRouteId != null
            val badgeAlpha by animateFloatAsState(
                targetValue = if (!anySelection || isSelected) 1f else 0.45f,
                animationSpec = tween(200),
                label = "mapLineBadgeAlpha_${route.routeId}",
            )
            val ringAlpha by animateFloatAsState(
                targetValue = if (isSelected) 1f else 0f,
                animationSpec = tween(200),
                label = "mapLineRingAlpha_${route.routeId}",
            )
            Box(
                modifier = Modifier
                    .alpha(badgeAlpha)
                    .border(
                        width = 2.dp,
                        color = colors.accent.copy(alpha = ringAlpha),
                        shape = RoundedCornerShape(13.dp),
                    )
                    .padding(3.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRouteTap(route.routeId)
                    }
                    .testTag("map_line_badge_${route.routeId}"),
            ) {
                LineBadge(
                    name = route.routeName.take(6),
                    colorHex = route.routeColor,
                    textColorHex = route.routeTextColor,
                    size = LineBadgeSize.Medium,
                )
            }
        }
    }
}
