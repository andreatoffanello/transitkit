package com.transitkit.app.ui.update

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NorthEast
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.transitkit.app.R
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Overlay full-screen bloccante per force-update obbligatorio.
 * Nessun bottone di chiusura: l'unica azione è andare allo store.
 *
 * Mirrora [app.dove.venezia.ui.screens.ForceUpdateScreen] con colori
 * adattati al brand accent di TransitKit (rosso d'urgenza fisso, indipendente
 * dal tema operatore — segnale univoco "devi aggiornare").
 */
@Composable
fun ForceUpdateScreen(
    message: String,
    onUpdate: () -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current

    val accentBright = Color(0xFFE55A3D)
    val accentDeep   = Color(0xFFB83C2A)
    val bgTop        = if (isDark) Color(0xFF1A0E0A) else Color(0xFFFFF5EB)
    val bgBottom     = if (isDark) Color(0xFF0A0A0A) else Color(0xFFFFE4D1)

    // Animazioni di entrata orchestrate
    val iconScale    = remember { Animatable(0.55f) }
    val iconAlpha    = remember { Animatable(0f) }
    val glowScale    = remember { Animatable(0.6f) }
    val glowAlpha    = remember { Animatable(0f) }
    val titleScale   = remember { Animatable(0.9f) }
    val titleAlpha   = remember { Animatable(0f) }
    val titleOffsetY = remember { Animatable(16f) }
    val bodyAlpha    = remember { Animatable(0f) }
    val bodyOffsetY  = remember { Animatable(14f) }
    val ctaAlpha     = remember { Animatable(0f) }
    val ctaOffsetY   = remember { Animatable(22f) }

    LaunchedEffect(Unit) {
        launchAnim(glowAlpha, 1f, tween(500))
        launchAnim(glowScale, 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
        launchAnim(iconScale, 1f, spring(dampingRatio = 0.62f, stiffness = Spring.StiffnessLow))
        launchAnim(iconAlpha, 1f, tween(600))
        delay(180)
        launchAnim(titleScale, 1f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
        launchAnim(titleAlpha, 1f, tween(450))
        launchAnim(titleOffsetY, 0f, spring(dampingRatio = 0.72f, stiffness = Spring.StiffnessLow))
        delay(160)
        launchAnim(bodyAlpha, 1f, tween(550))
        launchAnim(bodyOffsetY, 0f, tween(550))
        delay(140)
        launchAnim(ctaAlpha, 1f, tween(500))
        launchAnim(ctaOffsetY, 0f, spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow))
    }

    val infinite = rememberInfiniteTransition(label = "fu")
    val iconRotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "rot",
    )
    val glowPulse by infinite.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.10f,
        animationSpec = infiniteRepeatable(tween(2600), RepeatMode.Reverse),
        label = "pulse",
    )
    val ctaPulse by infinite.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.025f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "ctaPulse",
    )
    val wavePhase by infinite.animateFloat(
        initialValue = 0f,
        targetValue = (2 * PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing), RepeatMode.Restart),
        label = "wave",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .semantics { testTag = "force_update_screen" }
            .background(Brush.verticalGradient(colors = listOf(bgTop, bgBottom))),
    ) {
        // Onde animate al fondo (3 strati con phase shift)
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .align(Alignment.BottomCenter),
        ) {
            drawWaveLayer(wavePhase, 1.0f, 22f, accentBright.copy(alpha = if (isDark) 0.18f else 0.22f), 1.0f)
            drawWaveLayer(wavePhase + 1.2f, 1.6f, 16f, accentDeep.copy(alpha = if (isDark) 0.22f else 0.16f), 0.78f)
            drawWaveLayer(wavePhase + 2.6f, 2.2f, 11f, Color.White.copy(alpha = if (isDark) 0.05f else 0.30f), 0.60f)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Icona con glow radiale dietro
            Box(contentAlignment = Alignment.Center) {
                Canvas(
                    modifier = Modifier
                        .size(320.dp)
                        .scale(glowScale.value * glowPulse)
                        .alpha(glowAlpha.value),
                ) {
                    val center = Offset(size.width / 2, size.height / 2)
                    val radius = size.width / 2
                    val brush = ShaderBrush(
                        RadialGradientShader(
                            center = center,
                            radius = radius,
                            colors = listOf(
                                accentBright.copy(alpha = if (isDark) 0.45f else 0.30f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    drawCircle(brush = brush, radius = radius, center = center)
                }

                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .scale(iconScale.value)
                        .alpha(iconAlpha.value)
                        .blur(if (iconAlpha.value > 0.95f) 0.dp else 4.dp)
                        .shadow(
                            elevation = 18.dp,
                            shape = CircleShape,
                            ambientColor = accentDeep,
                            spotColor = accentDeep,
                        )
                        .clip(CircleShape)
                        .background(Brush.linearGradient(colors = listOf(accentBright, accentDeep))),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(44.dp)
                            .rotate(iconRotation),
                    )
                }
            }

            Spacer(Modifier.height(36.dp))

            val titleColor = if (isDark) Color(0xFFFFF5EB) else Color(0xFF1A0E0A)
            val bodyColor  = if (isDark) Color(0xFFE0CDB6) else Color(0xFF6B4F3F)

            Text(
                text = stringResource(R.string.update_force_title),
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                color = titleColor,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .scale(titleScale.value)
                    .alpha(titleAlpha.value)
                    .offset(y = with(density) { titleOffsetY.value.dp }),
            )

            Spacer(Modifier.height(14.dp))

            Text(
                text = message,
                fontSize = 16.sp,
                color = bodyColor,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                modifier = Modifier
                    .alpha(bodyAlpha.value)
                    .offset(y = with(density) { bodyOffsetY.value.dp })
                    .padding(horizontal = 12.dp),
            )

            Spacer(Modifier.height(48.dp))

            // CTA gradient con pulse continuo + tap feedback
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            val pressedScale = if (pressed) 0.97f else ctaPulse

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .alpha(ctaAlpha.value)
                    .offset(y = with(density) { ctaOffsetY.value.dp })
                    .scale(pressedScale)
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = accentDeep.copy(alpha = 0.4f),
                        spotColor = accentDeep.copy(alpha = 0.4f),
                    )
                    .clip(RoundedCornerShape(18.dp))
                    .background(Brush.linearGradient(colors = listOf(accentBright, accentDeep)))
                    .pointerInput(Unit) { detectTapGestures(onTap = { onUpdate() }) }
                    .semantics { testTag = "btn_force_update" },
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = stringResource(R.string.update_force_cta),
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Rounded.NorthEast,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }
    }
}

// ── Helpers privati ────────────────────────────────────────────────────────

private suspend fun launchAnim(
    a: Animatable<Float, *>,
    target: Float,
    spec: androidx.compose.animation.core.AnimationSpec<Float>,
) { a.animateTo(target, animationSpec = spec) }

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawWaveLayer(
    phase: Float,
    frequency: Float,
    amplitude: Float,
    color: Color,
    heightFraction: Float,
) {
    val width = size.width
    val height = size.height * heightFraction
    val midY = size.height - height + height * 0.5f
    val path = Path()
    path.moveTo(0f, midY)
    var x = 0f
    val step = 4f
    while (x <= width) {
        val relative = x / width
        val y = midY + sin(relative * 2f * PI.toFloat() * frequency + phase) * amplitude
        path.lineTo(x, y)
        x += step
    }
    path.lineTo(width, size.height)
    path.lineTo(0f, size.height)
    path.close()
    drawPath(path = path, color = color)
}
