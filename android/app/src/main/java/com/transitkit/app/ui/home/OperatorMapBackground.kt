package com.transitkit.app.ui.home

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import android.graphics.Shader
import android.os.Build
import android.os.PowerManager
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.transitkit.app.R
import com.transitkit.app.config.TransitTheme
import kotlinx.coroutines.isActive

/**
 * Operator map background — parità con `VeniceMapBackground` di DoVe (civici).
 *
 * Ottimizzazioni applicate (in commento dove sono):
 *  - **Quick win #1**: tempo avanza a ~15 fps tramite `withFrameNanos` con
 *    accumulator (66 ms budget). Le onde sin operano a 0.5–1.6 rad/s: il cap
 *    a 15 fps non si distingue percettivamente da 30/60 fps ma dimezza
 *    ulteriormente il workload GPU dello shader (benefit termico misurato
 *    su device piccoli su Movete).
 *  - **Quick win #2**: shader stack renderizzato a metà risoluzione (¼ pixel)
 *    e upscalato 2× via `graphicsLayer`. Il fragment shader è dominante e
 *    l'effetto è blurry/ambient — l'upscale è invisibile.
 *  - **Quick win #3**: blur del layer fog ridotto da 28dp a 12dp. Il kernel
 *    blur scala ~lineare col raggio; dimezzare il raggio dimezza il sample
 *    window. Resta percepibilmente foggy.
 *  - **3 layer** invece di 4 (era ridondante: il "medium fog" non aggiungeva
 *    profondità percepibile rispetto al deep fog + forming).
 *  - **FBM a 3 ottave** invece di 4 — risparmio ~25% sample per pixel.
 *  - **Blur via `RenderEffect.createChainEffect`** invece di `Modifier.blur`
 *    separato: una sola pass invece di due.
 *  - **`paused: Boolean`** per pausa esplicita quando il background è coperto
 *    (sheet aperto, navigation modale). `time` accumulato preservato.
 *
 * Fallback API < 33: Image desaturata statica.
 */
@Composable
fun OperatorMapBackground(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        OperatorMapBackgroundLayered(modifier = modifier, paused = paused)
    } else {
        OperatorMapBackgroundFallback(modifier)
    }
}

// ── AGSL shader ──────────────────────────────────────────────────────────────

private const val AGSL_MAP_GLOW = """
uniform shader composable;
uniform float2 size;
uniform float time;
uniform float sharpness;
uniform float accentR;
uniform float accentG;
uniform float accentB;

float hash2(float2 p) {
    p = fract(p * float2(127.1, 311.7));
    p += dot(p, p + 74.3);
    return fract(p.x * p.y);
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

// 3 ottave invece di 4 — stessa qualità percepita, meno sample.
// Scaling 2.1 + offset asimmetrico irrazionale decorrela ottave (no banding).
float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * vnoise(p);
        p = p * 2.1 + float2(3.7, 1.9);
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

    half3 accent       = half3(half(accentR), half(accentG), half(accentB));
    half3 accentBright = mix(accent, half3(1.0), half(0.18));
    half3 accentLight  = mix(accent, half3(1.0), half(0.42));

    float wave1 = sin(uv.x * 5.0 + uv.y * 3.0 - time * 1.4) * 0.5 + 0.5;
    float wave2 = sin(-uv.x * 4.0 + uv.y * 6.0 + time * 1.0) * 0.5 + 0.5;
    float dist  = length(uv - float2(0.5, 0.4));
    float wave3 = sin(dist * 10.0 - time * 1.6) * 0.5 + 0.5;
    float wave4 = sin((uv.x + uv.y) * 3.0 - time * 0.5) * 0.5 + 0.5;

    half3 col1 = mix(accent, accentBright, half(wave1));
    half3 finalColor = mix(col1, accentLight, half(wave3 * 0.4 + wave4 * 0.3));

    float2 nc1 = uv * 2.5 + float2(time * 0.12, time * 0.08);
    float breath1 = fbm(nc1);
    float2 nc2 = uv * 1.2 + float2(-time * 0.07, time * 0.10);
    float breath2 = fbm(nc2 + 50.0);
    float breathing = breath1 * 0.6 + breath2 * 0.4;

    float loThresh = mix(0.15, 0.40, sharpness);
    float hiThresh = mix(0.35, 0.60, sharpness);
    float visibility = smoothstep(loThresh, hiThresh, breathing);
    float minVis = mix(0.25, 0.0, sharpness);
    visibility = max(visibility, minVis);

    float glow = wave1 * 0.3 + wave2 * 0.3 + wave3 * 0.2 + wave4 * 0.2;
    glow += pow(wave1 * wave2 * wave3, 0.5) * 0.5;
    glow = pow(glow, 0.6);

    float intensity = ink * glow * visibility;
    half alpha = half(intensity);
    return half4(finalColor * alpha, alpha);
}
"""

// ── Layer config — 3 layer (fog / forming / crisp) ───────────────────────────

private data class GlowLayer(
    val sharpness: Float,
    val blurDp: Float,
    val opacityLight: Float,
    val opacityDark: Float,
)

private val glowLayers = listOf(
    GlowLayer(sharpness = 0.0f, blurDp = 12f, opacityLight = 0.55f, opacityDark = 0.70f),
    GlowLayer(sharpness = 0.6f, blurDp = 6f,  opacityLight = 0.40f, opacityDark = 0.50f),
    GlowLayer(sharpness = 1.0f, blurDp = 0f,  opacityLight = 0.36f, opacityDark = 0.48f),
)

// Quick win #1: ~15 fps cap. Le onde sin a 0.5–1.6 rad/s sono percettivamente
// indistinguibili da 30+ fps, e dimezzare ancora il framerate dimezza il
// workload GPU dello shader. Misurato come benefit termico sensibile su
// device piccoli — pattern ereditato da Movete (CityMapBackground).
private const val FRAME_BUDGET_NS = 66_666_666L

// ── Public composable ────────────────────────────────────────────────────────

@Composable
private fun OperatorMapBackgroundLayered(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
) {
    val isDark = isSystemInDarkTheme()
    val accent = TransitTheme.colors.accent

    // Auto-pause: powerSave OR thermal severe+ OR lifecycle non resumed OR paused esterno.
    val isPowerSave = rememberIsPowerSaveMode()
    val isThermallyConstrained = rememberIsThermallyConstrained()
    val isResumed = rememberIsResumed()
    val effectivelyPaused = paused || isPowerSave || isThermallyConstrained || !isResumed

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(effectivelyPaused) {
        if (effectivelyPaused) return@LaunchedEffect
        var prev: Long = -1L
        var accumulated = 0L
        while (isActive) {
            withFrameNanos { now ->
                if (prev < 0L) prev = now
                val delta = now - prev
                prev = now
                accumulated += delta
                if (accumulated >= FRAME_BUDGET_NS) {
                    time += accumulated / 1_000_000_000f
                    accumulated = 0L
                }
            }
        }
    }

    // Quick win #2: render a metà risoluzione + upscale 2×.
    // Offscreen compositing: HWUI può fare tile rendering e creare seam
    // animati sullo shader → forziamo un singolo buffer offscreen.
    Box(
        modifier = modifier
            .fillMaxSize()
            .clipToBounds()
            .clearAndSetSemantics { },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = 2f
                    scaleY = 2f
                    transformOrigin = TransformOrigin(0f, 0f)
                    compositingStrategy = CompositingStrategy.Offscreen
                }
                .layout { measurable, constraints ->
                    val halfW = constraints.maxWidth / 2
                    val halfH = constraints.maxHeight / 2
                    val placeable = measurable.measure(Constraints.fixed(halfW, halfH))
                    layout(constraints.maxWidth, constraints.maxHeight) {
                        placeable.place(0, 0)
                    }
                },
        ) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                glowLayers.forEach { layer ->
                    ShaderMapLayer(
                        time = time,
                        sharpness = layer.sharpness,
                        accent = accent,
                        blurDp = layer.blurDp.dp,
                        layerOpacity = if (isDark) layer.opacityDark else layer.opacityLight,
                    )
                }
            }
        }
    }
}

// ── State observers per auto-pause ───────────────────────────────────────────

@Composable
private fun rememberIsPowerSaveMode(): Boolean {
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    var isPowerSave by remember { mutableStateOf(powerManager.isPowerSaveMode) }
    DisposableEffect(context, powerManager) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context?, intent: Intent?) {
                isPowerSave = powerManager.isPowerSaveMode
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(PowerManager.ACTION_POWER_SAVE_MODE_CHANGED),
        )
        onDispose { context.unregisterReceiver(receiver) }
    }
    return isPowerSave
}

@Composable
private fun rememberIsThermallyConstrained(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val context = LocalContext.current
    val powerManager = remember(context) {
        context.getSystemService(Context.POWER_SERVICE) as PowerManager
    }
    var constrained by remember {
        mutableStateOf(powerManager.currentThermalStatus >= PowerManager.THERMAL_STATUS_SEVERE)
    }
    DisposableEffect(powerManager) {
        val listener = PowerManager.OnThermalStatusChangedListener { status ->
            constrained = status >= PowerManager.THERMAL_STATUS_SEVERE
        }
        powerManager.addThermalStatusListener(listener)
        onDispose { powerManager.removeThermalStatusListener(listener) }
    }
    return constrained
}

@Composable
private fun rememberIsResumed(): Boolean {
    val lifecycleOwner = LocalLifecycleOwner.current
    var resumed by remember {
        mutableStateOf(lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED))
    }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> resumed = true
                Lifecycle.Event.ON_PAUSE -> resumed = false
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return resumed
}

@RequiresApi(Build.VERSION_CODES.TIRAMISU)
@Composable
private fun ShaderMapLayer(
    time: Float,
    sharpness: Float,
    accent: Color,
    blurDp: Dp,
    layerOpacity: Float,
) {
    val shader = remember { RuntimeShader(AGSL_MAP_GLOW) }
    val density = LocalDensity.current.density
    val blurPx = blurDp.value * density

    Image(
        painter = painterResource(id = R.drawable.operator_background),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxSize()
            .alpha(layerOpacity)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                shader.setFloatUniform("size", size.width, size.height)
                shader.setFloatUniform("time", time)
                shader.setFloatUniform("sharpness", sharpness)
                shader.setFloatUniform("accentR", accent.red)
                shader.setFloatUniform("accentG", accent.green)
                shader.setFloatUniform("accentB", accent.blue)

                val shaderFx = RenderEffect.createRuntimeShaderEffect(shader, "composable")
                renderEffect = if (blurPx > 0f) {
                    // Quick win #6: blur in chain con lo shader → single pass.
                    // CLAMP (non DECAL): evita bordi scuri quando il kernel
                    // blur samples fuori dal source.
                    RenderEffect.createChainEffect(
                        RenderEffect.createBlurEffect(blurPx, blurPx, Shader.TileMode.CLAMP),
                        shaderFx,
                    )
                } else {
                    shaderFx
                }.asComposeRenderEffect()
            },
    )
}

/** Fallback API < 33: Image desaturata statica. */
@Composable
private fun OperatorMapBackgroundFallback(modifier: Modifier = Modifier) {
    val isDark = isSystemInDarkTheme()
    val inkAlpha = if (isDark) 0.22f else 0.28f
    val desaturate = remember { ColorMatrix().apply { setToSaturation(0f) } }
    Box(modifier = modifier.fillMaxSize().clipToBounds().clearAndSetSemantics { }) {
        Image(
            painter = painterResource(id = R.drawable.operator_background),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = inkAlpha,
            colorFilter = ColorFilter.colorMatrix(desaturate),
            modifier = Modifier.fillMaxSize(),
        )
    }
}
