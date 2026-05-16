package com.transitkit.app.ui.orari

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.transitkit.app.config.LucideIcons
import com.transitkit.app.config.TransitTheme
import com.transitkit.app.data.model.ResolvedStop
import kotlinx.coroutines.delay

private val StopEntranceEasing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

@Composable
internal fun StaggeredStopCard(
    stop: ResolvedStop,
    index: Int,
    animate: Boolean,
    onClick: () -> Unit,
) {
    val colors = TransitTheme.colors
    val shouldAnimate = animate && index <= 15
    var appeared by rememberSaveable { mutableStateOf(!shouldAnimate) }

    LaunchedEffect(stop.id, animate) {
        if (shouldAnimate) {
            appeared = false
            delay(index.toLong() * 30L)
            appeared = true
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = StopEntranceEasing),
        label = "stop_alpha",
    )
    val offsetY by animateFloatAsState(
        targetValue = if (appeared) 0f else 10f,
        animationSpec = tween(durationMillis = 300, easing = StopEntranceEasing),
        label = "stop_offsetY",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                this.alpha = alpha
                translationY = offsetY * density
            }
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.glassFill)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 14.dp)
            .semantics { contentDescription = "stop_row_${stop.id}" },
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stop.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
                maxLines = 1,
            )
            val routes = stop.routeNames.take(6)
            val routeColorHexes = stop.routeColors.take(6)
            if (routes.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    items(routes.size) { i ->
                        com.transitkit.app.ui.components.LineBadge(
                            name = routes[i],
                            colorHex = routeColorHexes.getOrElse(i) { "" },
                            // Stop-list rows use Small — dense list, badge is a
                            // secondary glyph here (not the primary identity).
                            size = com.transitkit.app.ui.components.LineBadgeSize.Small,
                        )
                    }
                    if (stop.routeNames.size > 6) {
                        item {
                            val colors = TransitTheme.colors
                            Text(
                                text = "+${stop.routeNames.size - 6}",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textTertiary,
                                modifier = Modifier
                                    .background(colors.textTertiary.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 5.dp, vertical = 3.dp),
                            )
                        }
                    }
                }
            }
        }

        Icon(
            painter = painterResource(LucideIcons.ChevronRight),
            contentDescription = null,
            tint = colors.textTertiary,
            modifier = Modifier.size(18.dp),
        )
    }
}
