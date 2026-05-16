package com.transitkit.app.ui.orari

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedDeparture

/**
 * Riga linee → **filtri** (iOS parity): UNA sola riga horizontal-scrollable
 * con pill "Tutti" + un `LineBadge` per ciascuna rotta che serve la
 * fermata. Ogni badge È il filtro: tap → filtra le partenze a quella linea.
 * Selezionato = opacity piena; deselezionato = `alpha 0.35`.
 *
 * Scroll orizzontale invece di FlowRow wrap: iOS pattern, niente wrap su
 * più righe né "+N" expander (rumore visivo).
 */
@Composable
internal fun LineBadgeRow(
    routes: List<ResolvedDeparture>,
    selectedRouteId: String?,
    onRouteSelected: (String?) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) {
    val colors = TransitTheme.colors
    val haptic = LocalHapticFeedback.current
    val uniqueRoutes = remember(routes) { routes.distinctBy { it.routeId } }
    if (uniqueRoutes.isEmpty()) return

    LazyRow(
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        item(key = "filter_all") {
            val tuttiSelected = selectedRouteId == null
            val tuttiAlpha by animateFloatAsState(
                targetValue = if (tuttiSelected) 1f else 0.35f,
                animationSpec = tween(200),
                label = "tuttiAlpha",
            )
            Box(
                modifier = Modifier
                    .alpha(tuttiAlpha)
                    .clip(RoundedCornerShape(10.dp))
                    .background(
                        if (tuttiSelected) colors.accent else colors.accent.copy(alpha = 0.18f),
                        RoundedCornerShape(10.dp),
                    )
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRouteSelected(null)
                    }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.filter_all),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (tuttiSelected) androidx.compose.ui.graphics.Color.White else colors.accent,
                )
            }
        }
        items(uniqueRoutes, key = { it.routeId }) { route ->
            val isSelected = selectedRouteId == route.routeId
            val anyFilterActive = selectedRouteId != null
            val targetAlpha = when {
                !anyFilterActive -> 1f
                isSelected -> 1f
                else -> 0.35f
            }
            val badgeAlpha by animateFloatAsState(
                targetValue = targetAlpha,
                animationSpec = tween(200),
                label = "badgeAlpha_${route.routeId}",
            )
            com.transitkit.app.ui.components.LineBadge(
                name = route.routeName.take(6),
                colorHex = route.routeColor,
                textColorHex = route.routeTextColor,
                size = com.transitkit.app.ui.components.LineBadgeSize.Medium,
                modifier = Modifier
                    .alpha(badgeAlpha)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onRouteSelected(if (isSelected) null else route.routeId)
                    },
            )
        }
    }
}
