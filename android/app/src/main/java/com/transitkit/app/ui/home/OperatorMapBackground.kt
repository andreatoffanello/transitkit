package com.transitkit.app.ui.home

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.transitkit.app.R
import kotlin.math.sin

/**
 * Ghost map background per la Home: mostra l'immagine dell'operatore come
 * texture desaturata a ~15-18% con uno spotlight radiale animato su percorso
 * lissajous lento (~62s per ciclo).
 */
@Composable
fun OperatorMapBackground(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "mapSpotlight")

    val phaseX by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseX"
    )
    val phaseY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI * 1.618f).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseY"
    )

    val isDark = isSystemInDarkTheme()
    val inkAlpha = if (isDark) 0.14f else 0.18f
    val spotAlpha = if (isDark) 0.07f else 0.06f

    val desaturate = remember { ColorMatrix().apply { setToSaturation(0f) } }

    Box(modifier = modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.operator_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = inkAlpha,
            colorFilter = ColorFilter.colorMatrix(desaturate),
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            val lx = (0.5f + 0.40f * sin(phaseX)) * size.width
            val ly = (0.5f + 0.30f * sin(phaseY + 0.9f)) * size.height
            val radius = size.minDimension * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = spotAlpha),
                        Color.Transparent
                    ),
                    center = Offset(lx, ly),
                    radius = radius
                ),
                radius = radius,
                center = Offset(lx, ly)
            )
        }
    }
}
