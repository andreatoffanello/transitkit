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
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme

/**
 * Operator map background per la Home. Composizione a 4 layer (ispirata a
 * VeniceGlow di civici) — parità con iOS Metal shader `mapGlowEffect`.
 *
 * Ogni layer è la stessa immagine mappa con:
 *  - blur SwiftUI/Compose reale diverso (28 / 12 / 4 / 0 dp)
 *  - sharpness uniform diverso (0.0 fog → 1.0 crisp) per lo shader
 *  - opacity per-layer calibrata light/dark mode
 *
 * Lo shader (AGSL su API 33+) colora in varianti dell'accent operatore:
 * base + versione più luminosa (+18% bianco) + versione chiara aerea (+42% bianco).
 * Breathing fbm + color waves + visibilità sharpness-dipendente + glow additivo.
 *
 * Su API < 33: fallback statico (Image desaturata + radial gradient) senza AGSL.
 */
@Composable
fun OperatorMapBackground(modifier: Modifier = Modifier) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OperatorMapBackgroundLayered(modifier)
    } else {
        OperatorMapBackgroundFallback(modifier)
    }
}

// AGSL shader - parità con ios/Shaders/MapBackground.metal
private const val AGSL_MAP_GLOW = """
uniform shader composable;
uniform float2 size;
uniform float time;
uniform float sharpness;
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
    float2 shift = float2(100.0);
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p = p * 2.0 + shift;
        a *= 0.5;
    }
    return v;
}

half4 main(float2 position) {
    half4 color = composable.eval(position);
    float lum = dot(color.rgb, half3(0.299, 0.587, 0.114));
    float ink = 1.0 - lum;
    if (ink < 0.05) return half4(0.0);

    float2 uv = position / size;

    // Base + 2 varianti più luminose
    half3 accent       = half3(half(accentR), half(accentG), half(accentB));
    half3 accentBright = mix(accent, half3(1.0), half(0.18));
    half3 accentLight  = mix(accent, half3(1.0), half(0.42));

    // Color waves
    float wave1 = sin(uv.x * 5.0 + uv.y * 3.0 - time * 1.4) * 0.5 + 0.5;
    float wave2 = sin(-uv.x * 4.0 + uv.y * 6.0 + time * 1.0) * 0.5 + 0.5;
    float dist  = length(uv - float2(0.5, 0.4));
    float wave3 = sin(dist * 10.0 - time * 1.6) * 0.5 + 0.5;
    float wave4 = sin((uv.x + uv.y) * 3.0 - time * 0.5) * 0.5 + 0.5;

    half3 col1 = mix(accent, accentBright, half(wave1));
    half3 finalColor = mix(col1, accentLight, half(wave3 * 0.4 + wave4 * 0.3));

    // Breathing
    float2 nc1 = uv * 2.5 + float2(time * 0.12, time * 0.08);
    float breath1 = fbm(nc1);
    float2 nc2 = uv * 1.2 + float2(-time * 0.07, time * 0.10);
    float breath2 = fbm(nc2 + 50.0);
    float breathing = breath1 * 0.6 + breath2 * 0.4;

    // Sharpness-dependent visibility
    float loThresh = mix(0.15, 0.40, sharpness);
    float hiThresh = mix(0.35, 0.60, sharpness);
    float visibility = smoothstep(loThresh, hiThresh, breathing);
    float minVis = mix(0.25, 0.0, sharpness);
    visibility = max(visibility, minVis);

    // Glow
    float glow = wave1 * 0.3 + wave2 * 0.3 + wave3 * 0.2 + wave4 * 0.2;
    glow += pow(wave1 * wave2 * wave3, 0.5) * 0.5;
    glow = pow(glow, 0.6);

    float intensity = ink * glow * visibility;
    half alpha = half(intensity);
    return half4(finalColor * alpha, alpha);
}
"""

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun OperatorMapBackgroundLayered(modifier: Modifier = Modifier) {
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

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Deep fog — blur grande, sempre presente
        MapShaderLayer(
            time = time,
            sharpness = 0.0f,
            accent = accent,
            blurRadius = 28.dp,
            layerOpacity = if (isDark) 0.70f else 0.55f
        )
        // Layer 2: Medium fog
        MapShaderLayer(
            time = time,
            sharpness = 0.3f,
            accent = accent,
            blurRadius = 12.dp,
            layerOpacity = if (isDark) 0.50f else 0.42f
        )
        // Layer 3: Forming lines
        MapShaderLayer(
            time = time,
            sharpness = 0.7f,
            accent = accent,
            blurRadius = 4.dp,
            layerOpacity = if (isDark) 0.45f else 0.36f
        )
        // Layer 4: Crisp lines — peak breathing only
        MapShaderLayer(
            time = time,
            sharpness = 1.0f,
            accent = accent,
            blurRadius = 0.dp,
            layerOpacity = if (isDark) 0.50f else 0.38f
        )
    }
}

@androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun MapShaderLayer(
    time: Float,
    sharpness: Float,
    accent: Color,
    blurRadius: androidx.compose.ui.unit.Dp,
    layerOpacity: Float,
) {
    val shader = remember { RuntimeShader(AGSL_MAP_GLOW) }
    Image(
        painter = painterResource(id = R.drawable.operator_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .then(if (blurRadius.value > 0f) Modifier.blur(blurRadius) else Modifier)
            .graphicsLayer {
                this.alpha = layerOpacity
                shader.setFloatUniform("size", this.size.width, this.size.height)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("sharpness", sharpness)
                shader.setFloatUniform("accentR", accent.red)
                shader.setFloatUniform("accentG", accent.green)
                shader.setFloatUniform("accentB", accent.blue)
                renderEffect = RenderEffect
                    .createRuntimeShaderEffect(shader, "composable")
                    .asComposeRenderEffect()
            }
    )
}

/** Fallback API < 33: semplice image desaturata senza shader. */
@Composable
private fun OperatorMapBackgroundFallback(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val inkAlpha = if (isDark) 0.22f else 0.28f
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
    }
}
