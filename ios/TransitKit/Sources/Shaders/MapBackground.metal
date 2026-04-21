#include <metal_stdlib>
using namespace metal;

// --- Noise helpers ---

static inline float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

/// Value noise con interpolazione smoothstep.
static inline float vnoise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

/// Fractal Brownian Motion — 3 octaves. Produce texture "cloudy/watercolor".
static inline float fbm(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 3; i++) {
        v += a * vnoise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

/// Ghost map background con reveal tipo gas/acquarello.
/// - Spotlight con domain warping fbm → forme organiche, non cerchi geometrici
/// - Glare halo: bagliore sfocato che bleeds out dalle bright zones
/// - Cloud multiplier: densità ink modulata da fbm lento (watercolor wash)
/// - Anti-spotlight, breathing, grain, vignette, accent tint
[[ stitchable ]] half4 mapGlowEffect(
    float2 position,
    half4 color,
    float2 size,
    float time,
    float isDark,
    float accentR,
    float accentG,
    float accentB
) {
    float2 uv = position / size;

    // Luminance → ink density
    float lum = dot(float3(color.rgb), float3(0.299, 0.587, 0.114));
    float ink = isDark > 0.5 ? lum : (1.0 - lum);

    // --- Domain warping per forme organiche ---
    // Frequenza alta (uv * 6.5) genera warp dettagliato con varietà ravvicinate
    // invece di una singola onda voluminosa. Ampiezza ridotta a 0.09 per preservare
    // struttura dello spotlight pur aggiungendo mottling fine.
    float warpTime = time * 0.10;
    float2 warp = float2(
        fbm(uv * 6.5 + float2(warpTime, 0.0)),
        fbm(uv * 6.5 + float2(5.2, 1.3) + float2(0.0, warpTime))
    ) * 0.09;
    float2 warpedUV = uv + warp - 0.045;

    // --- Primary spotlight (~16s, warped per organic shape) ---
    float t1 = time * 0.40;
    float2 light1 = float2(
        0.5 + 0.42 * sin(t1),
        0.5 + 0.35 * sin(t1 * 1.618 + 0.9)
    );
    float d1 = length(warpedUV - light1);
    float spot1 = exp(-d1 * d1 * 2.5);
    // Glare halo: bagliore largo sfocato sopra lo spotlight (bleed out)
    float glare1 = exp(-d1 * d1 * 0.7) * 0.35;

    // --- Secondary spotlight (~10s, warped) ---
    float t2 = time * 0.62 + 3.14;
    float2 light2 = float2(
        0.5 + 0.38 * sin(t2 * 1.3),
        0.5 + 0.32 * cos(t2 * 0.8)
    );
    float d2 = length(warpedUV - light2);
    float spot2 = exp(-d2 * d2 * 5.5);
    float glare2 = exp(-d2 * d2 * 1.5) * 0.25;

    // --- Anti-spotlight (~13s, warped) ---
    float t3 = time * 0.48 + 1.57;
    float2 darkCenter = float2(
        0.5 - 0.42 * sin(t3 * 0.9),
        0.5 - 0.35 * cos(t3 * 1.1)
    );
    float d3 = length(warpedUV - darkCenter);
    float darkZone = exp(-d3 * d3 * 3.5);

    // --- Reveal mask: combina spotlight + cloud layers.
    // Dove il mask è basso, l'ink sparisce completamente (blur-to-invisible).
    // Dove è alto, l'ink si rivela nitido.
    float cloudMid = fbm(uv * 4.0 + float2(time * 0.08, -time * 0.06));
    float cloudFine = fbm(uv * 11.0 + float2(-time * 0.05, time * 0.07));
    float cloudReveal = cloudMid * 0.60 + cloudFine * 0.40;

    // --- Breathing modulation (~16s) ---
    float breath = 0.80 + 0.20 * sin(time * 0.40);

    // --- Film grain: hash-based, animated ---
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float grain = (hash(grainSeed) - 0.5) * 0.20;

    // --- Vignette ---
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.28, 0.82, length(centered));
    vignette = mix(0.45, 1.0, vignette);

    // Reveal signal: spotlight + glare + cloud reveal
    // Lo spotlight primario/secondario + glare contribuiscono direttamente;
    // le nuvole aggiungono chiazze random di "rivelazione" parziale.
    float spotReveal = (spot1 * 1.0 + glare1 * 0.6 + spot2 * 0.8 + glare2 * 0.4) * breath;
    float reveal = spotReveal + cloudReveal * 0.55;

    // Anti-spot riduce reveal in quella zona
    reveal -= darkZone * 0.35;

    // Smoothstep per transizione "wipe": sotto il lower threshold sparisce,
    // sopra il higher si vede nitido.
    float mask = smoothstep(0.25, 0.80, reveal);

    // Alpha finale: ink visibile SOLO dove il mask è alto. Grain aggiunto sopra.
    float alpha = ink * mask * vignette + ink * grain * 0.15 * mask;
    alpha = clamp(alpha, 0.0, 0.65);

    // Accent tint nello spotlight
    half3 rgb;
    half3 accentCol = half3(half(accentR), half(accentG), half(accentB));
    float tintAmount = (spot1 * 0.45 + glare1 * 0.30);
    if (isDark > 0.5) {
        rgb = mix(half3(1.0), accentCol, half(tintAmount));
    } else {
        rgb = mix(color.rgb, accentCol, half(tintAmount));
    }

    return half4(rgb, half(alpha));
}
