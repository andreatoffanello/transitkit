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
    // Due campioni fbm a seed diversi generano un vettore di displacement.
    // L'offset temporale lento fa "respirare" le forme come acquarello bagnato.
    float warpTime = time * 0.08;
    float2 warp = float2(
        fbm(uv * 2.8 + float2(warpTime, 0.0)),
        fbm(uv * 2.8 + float2(5.2, 1.3) + float2(0.0, warpTime))
    ) * 0.18;
    float2 warpedUV = uv + warp - 0.09;  // centra il warp

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

    // --- Cloud multiplier: low-freq fbm drifts slowly, wash effect ---
    float cloud = fbm(uv * 1.8 + float2(time * 0.06, -time * 0.04));
    float cloudMod = mix(0.55, 1.20, cloud);  // modula base ±

    // --- Breathing modulation (~16s) ---
    float breath = 0.80 + 0.20 * sin(time * 0.40);

    // --- Film grain: hash-based, animated ---
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float noise = hash(grainSeed);
    float grain = (noise - 0.5) * 0.25;

    // --- Vignette ---
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.30, 0.80, length(centered));
    vignette = mix(0.60, 1.0, vignette);

    // Combined alpha:
    //   - base ink * cloud * vignette (densità varia fluidamente)
    //   - bright spotlights + glare boost (alone)
    //   - anti-spot dimmer
    //   - grain on top
    float baseAlpha = 0.20 * cloudMod * vignette;
    float spotBoost = (spot1 * 0.30 + glare1 * 0.18 + spot2 * 0.20 + glare2 * 0.12) * breath;
    float darkPenalty = darkZone * 0.12;
    float alpha = ink * (baseAlpha + spotBoost - darkPenalty + grain * 0.40);
    alpha = clamp(alpha, 0.0, 0.60);

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
