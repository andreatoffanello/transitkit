package com.transitkit.app.ui.home

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.os.Build
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
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.transitkit.app.R
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ghost map background per la Home. Su API 33+ usa AGSL (RuntimeShader) con
 * dual spotlight / breathing / grain / vignette — parità con iOS Metal shader.
 * Su API < 33 fallback a dual radial gradient animato su Image desaturata.
 */
@Composable
fun OperatorMapBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OperatorMapBackgroundAgsl(modifier)
    } else {
        OperatorMapBackgroundFallback(modifier)
    }
}

// AGSL shader. Logica identica a MapBackground.metal.
private const val AGSL_MAP_GLOW = """
uniform shader composable;
uniform float2 size;
uniform float time;
uniform float isDark;

half4 main(float2 position) {
    float2 uv = position / size;

    half4 color = composable.eval(position);
    float lum = dot(color.rgb, half3(0.299, 0.587, 0.114));
    float ink = isDark > 0.5 ? lum : (1.0 - lum);

    // Primary spotlight (slow wide)
    float t1 = time * 0.10;
    float2 light1 = float2(
        0.5 + 0.42 * sin(t1),
        0.5 + 0.32 * sin(t1 * 1.618 + 0.9)
    );
    float d1 = length(uv - light1);
    float spot1 = exp(-d1 * d1 * 4.5);

    // Secondary spotlight (faster tighter)
    float t2 = time * 0.15 + 3.14;
    float2 light2 = float2(
        0.5 + 0.35 * sin(t2 * 1.3),
        0.5 + 0.28 * cos(t2 * 0.8)
    );
    float d2 = length(uv - light2);
    float spot2 = exp(-d2 * d2 * 11.0);

    // Breathing
    float breath = 0.88 + 0.12 * sin(time * 0.08);

    // Film grain
    float2 grainSeed = position + float2(time * 11.3, time * 7.7);
    float noise = fract(sin(dot(grainSeed, float2(12.9898, 78.233))) * 43758.5453);
    float grain = (noise - 0.5) * 0.12;

    // Vignette
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.35, 0.78, length(centered));
    vignette = mix(0.78, 1.0, vignette);

    float baseAlpha = 0.13 * vignette;
    float spotBoost = (spot1 * 0.10 + spot2 * 0.06) * breath;
    float alpha = ink * (baseAlpha + spotBoost + grain * 0.25);
    alpha = clamp(alpha, 0.0, 0.32);

    half3 rgb = isDark > 0.5 ? half3(1.0) : color.rgb;
    return half4(rgb, half(alpha));
}
"""

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun OperatorMapBackgroundAgsl(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val infinite = rememberInfiniteTransition(label = "shaderTime")
    val time by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1_000_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    val shader = remember { RuntimeShader(AGSL_MAP_GLOW) }

    Image(
        painter = painterResource(id = R.drawable.operator_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                shader.setFloatUniform("size", this.size.width, this.size.height)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("isDark", if (isDark) 1f else 0f)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            }
    )
}

// Fallback per API < 33: due radial gradient su image desaturata.
@Composable
private fun OperatorMapBackgroundFallback(modifier: Modifier = Modifier) {
    val infinite = rememberInfiniteTransition(label = "fallbackTime")

    val phaseX by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseX"
    )
    val phaseY by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI * 1.618f).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 62_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phaseY"
    )
    val phase2 by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI * 1.3f).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 41_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "phase2"
    )

    val isDark = isSystemInDarkTheme()
    val inkAlpha = if (isDark) 0.14f else 0.18f
    val primarySpotAlpha = if (isDark) 0.08f else 0.06f
    val secondarySpotAlpha = if (isDark) 0.05f else 0.04f

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
            val lx1 = (0.5f + 0.42f * sin(phaseX)) * size.width
            val ly1 = (0.5f + 0.32f * sin(phaseY + 0.9f)) * size.height
            val r1 = size.minDimension * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = primarySpotAlpha), Color.Transparent),
                    center = Offset(lx1, ly1),
                    radius = r1
                ),
                radius = r1,
                center = Offset(lx1, ly1)
            )

            val lx2 = (0.5f + 0.35f * sin(phase2)) * size.width
            val ly2 = (0.5f + 0.28f * cos(phase2 * 0.8f)) * size.height
            val r2 = size.minDimension * 0.35f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color.White.copy(alpha = secondarySpotAlpha), Color.Transparent),
                    center = Offset(lx2, ly2),
                    radius = r2
                ),
                radius = r2,
                center = Offset(lx2, ly2)
            )
        }
    }
}
