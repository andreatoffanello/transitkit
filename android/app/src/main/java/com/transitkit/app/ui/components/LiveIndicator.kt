package com.transitkit.app.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
