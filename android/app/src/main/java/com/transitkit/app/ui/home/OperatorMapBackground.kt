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
import com.transitkit.app.config.TransitTheme
import kotlin.math.cos
import kotlin.math.sin

/**
 * Ghost map background per la Home.
 *
 * Su API 33+ usa AGSL (RuntimeShader) per parità con Metal iOS:
 *   - Dual bright spotlight (primary slow+wide, secondary faster+tight, counter-phase)
 *   - Anti-spotlight (zona dimmer che crea bande di contrasto visibili)
 *   - Accent tint (ink vira verso colore operatore nelle zone illuminate)
 *   - Film grain animato, breathing intensity, vignette ai bordi
 *
 * Su API < 33: fallback a dual radial gradient su Image desaturata + accent tint statico.
 */
@Composable
fun OperatorMapBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OperatorMapBackgroundAgsl(modifier)
    } else {
        OperatorMapBackgroundFallback(modifier)
    }
}

// AGSL shader - watercolor/gas reveal, parita' con MapBackground.metal iOS.
private const val AGSL_MAP_GLOW = """
uniform shader composable;
uniform float2 size;
uniform float time;
uniform float isDark;
uniform float accentR;
uniform float accentG;
uniform float accentB;

float hash2(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash2(i);
    float b = hash2(i + float2(1.0, 0.0));
    float c = hash2(i + float2(0.0, 1.0));
    float d = hash2(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * vnoise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 position) {
    float2 uv = position / size;

    half4 color = composable.eval(position);
    float lum = dot(color.rgb, half3(0.299, 0.587, 0.114));
    float ink = isDark > 0.5 ? lum : (1.0 - lum);

    // Domain warping fbm → organic spotlight shapes (watercolor)
    float warpTime = time * 0.08;
    float2 warp = float2(
        fbm(uv * 2.8 + float2(warpTime, 0.0)),
        fbm(uv * 2.8 + float2(5.2, 1.3) + float2(0.0, warpTime))
    ) * 0.18;
    float2 warpedUV = uv + warp - 0.09;

    // Primary spotlight + glare
    float t1 = time * 0.40;
    float2 light1 = float2(
        0.5 + 0.42 * sin(t1),
        0.5 + 0.35 * sin(t1 * 1.618 + 0.9)
    );
    float d1 = length(warpedUV - light1);
    float spot1 = exp(-d1 * d1 * 2.5);
    float glare1 = exp(-d1 * d1 * 0.7) * 0.35;

    // Secondary spotlight + glare
    float t2 = time * 0.62 + 3.14;
    float2 light2 = float2(
        0.5 + 0.38 * sin(t2 * 1.3),
        0.5 + 0.32 * cos(t2 * 0.8)
    );
    float d2 = length(warpedUV - light2);
    float spot2 = exp(-d2 * d2 * 5.5);
    float glare2 = exp(-d2 * d2 * 1.5) * 0.25;

    // Anti-spotlight
    float t3 = time * 0.48 + 1.57;
    float2 darkCenter = float2(
        0.5 - 0.42 * sin(t3 * 0.9),
        0.5 - 0.35 * cos(t3 * 1.1)
    );
    float d3 = length(warpedUV - darkCenter);
    float darkZone = exp(-d3 * d3 * 3.5);

    // Cloud multiplier (watercolor wash drifting)
    float cloud = fbm(uv * 1.8 + float2(time * 0.06, -time * 0.04));
    float cloudMod = mix(0.55, 1.20, cloud);

    // Breathing (~16s)
    float breath = 0.80 + 0.20 * sin(time * 0.40);

    // Film grain
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float grain = (hash2(grainSeed) - 0.5) * 0.25;

    // Vignette
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.30, 0.80, length(centered));
    vignette = mix(0.60, 1.0, vignette);

    float baseAlpha = 0.20 * cloudMod * vignette;
    float spotBoost = (spot1 * 0.30 + glare1 * 0.18 + spot2 * 0.20 + glare2 * 0.12) * breath;
    float darkPenalty = darkZone * 0.12;
    float alpha = ink * (baseAlpha + spotBoost - darkPenalty + grain * 0.40);
    alpha = clamp(alpha, 0.0, 0.60);

    half3 accentCol = half3(half(accentR), half(accentG), half(accentB));
    float tintAmount = spot1 * 0.45 + glare1 * 0.30;
    half3 rgb;
    if (isDark > 0.5) {
        rgb = mix(half3(1.0), accentCol, half(tintAmount));
    } else {
        rgb = mix(color.rgb, accentCol, half(tintAmount));
    }

    return half4(rgb, half(alpha));
}
"""

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun OperatorMapBackgroundAgsl(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val accent = TransitTheme.colors.accent

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
                shader.setFloatUniform("accentR", accent.red)
                shader.setFloatUniform("accentG", accent.green)
                shader.setFloatUniform("accentB", accent.blue)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            }
    )
}

// Fallback per API < 33: due radial gradient su Image desaturata + tint accent statico.
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
    val accent = TransitTheme.colors.accent
    val inkAlpha = if (isDark) 0.22f else 0.28f  // più visibile anche qui
    val primarySpotAlpha = if (isDark) 0.14f else 0.12f
    val secondarySpotAlpha = if (isDark) 0.09f else 0.08f

    val desaturate = remember { ColorMatrix().apply { setToSaturation(0f) } }

    Box(modifier = modifier.fillMaxSize()) {
        // Base desaturated map
        Image(
            painter = painterResource(id = R.drawable.operator_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = inkAlpha,
            colorFilter = ColorFilter.colorMatrix(desaturate),
            modifier = Modifier.fillMaxSize()
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            // Primary spotlight (accent-tinted)
            val lx1 = (0.5f + 0.42f * sin(phaseX)) * size.width
            val ly1 = (0.5f + 0.32f * sin(phaseY + 0.9f)) * size.height
            val r1 = size.minDimension * 0.70f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        accent.copy(alpha = primarySpotAlpha),
                        Color.Transparent
                    ),
                    center = Offset(lx1, ly1),
                    radius = r1
                ),
                radius = r1,
                center = Offset(lx1, ly1)
            )

            // Secondary spotlight (white)
            val lx2 = (0.5f + 0.35f * sin(phase2)) * size.width
            val ly2 = (0.5f + 0.28f * cos(phase2 * 0.8f)) * size.height
            val r2 = size.minDimension * 0.40f
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
