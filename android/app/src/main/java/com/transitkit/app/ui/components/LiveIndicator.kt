package com.transitkit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.config.TransitTheme

@Composable
fun LiveIndicator(
    size: Dp = 8.dp,
    animated: Boolean = true,
    modifier: Modifier = Modifier,
) {
    val color = TransitTheme.colors.realtimeGreen
    if (animated) {
        val infiniteTransition = rememberInfiniteTransition(label = "live")
        val scale by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "scale",
        )
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.7f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(900, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "alpha",
        )
        Box(
            modifier = modifier
                .size(size)
                .scale(scale)
                .alpha(alpha)
                .background(color, CircleShape),
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(color, CircleShape),
        )
    }
}

/**
 * Mini-chip "● LIVE" che appare prima del countdown quando il feed RT ha
 * fornito un delay plausibile (VehicleStore.reliableDelayMinutes != null,
 * dopo clamp di sanità −5…+30 min). L'assenza del chip significa
 * "orario programmato puro" (clamp scattato o RT assente per il trip).
 *
 * Parità Movete `LiveBadge` (DepartureRow.kt, commit 5707de3acf):
 * - dot 4dp + "LIVE" 9sp SemiBold rounded
 * - lineHeight=9.sp + includeFontPadding=false + LineHeightStyle(trim=Both)
 *   per eliminare il font padding legacy Android che gonfia verticalmente.
 * - bg verde 12% alpha + stroke verde 25% alpha 0.5dp
 * - padding horizontal=6dp, vertical=1dp
 */
@Composable
fun LiveBadge(
    modifier: Modifier = Modifier,
) {
    val color = TransitTheme.colors.realtimeGreen
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50),
            )
            .border(
                BorderStroke(0.5.dp, color.copy(alpha = 0.25f)),
                shape = RoundedCornerShape(50),
            )
            .padding(horizontal = 6.dp, vertical = 1.dp)
            .semantics { contentDescription = "Live" },
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .background(color, shape = CircleShape),
        )
        Text(
            text = "LIVE",
            color = color,
            fontSize = 9.sp,
            lineHeight = 9.sp,
            fontWeight = FontWeight.SemiBold,
            style = TextStyle(
                platformStyle = PlatformTextStyle(
                    includeFontPadding = false,
                ),
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            ),
        )
    }
}
