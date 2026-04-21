#include <metal_stdlib>
using namespace metal;

/// Ghost map background for the Home screen.
/// Renders the operator territory map as ink lines over transparent, with:
///   1) Dual Gaussian spotlights on contra-rotating Lissajous paths (depth illusion)
///   2) Breathing intensity modulation (very slow sin on spotlight gain)
///   3) Film-grain noise on the ink to add living texture
///   4) Subtle vignette so edges recede and cards feel anchored
/// Light mode → dark ink on transparent. Dark mode → light ink on transparent.
[[ stitchable ]] half4 mapGlowEffect(
    float2 position,
    half4 color,
    float2 size,
    float time,
    float isDark
) {
    float2 uv = position / size;

    // Luminance → ink density (high where dark lines are)
    float lum = dot(float3(color.rgb), float3(0.299, 0.587, 0.114));
    float ink = isDark > 0.5 ? lum : (1.0 - lum);

    // --- Primary spotlight: slow, wide (~62s per cycle) ---
    float t1 = time * 0.10;
    float2 light1 = float2(
        0.5 + 0.42 * sin(t1),
        0.5 + 0.32 * sin(t1 * 1.618 + 0.9)
    );
    float d1 = length(uv - light1);
    float spot1 = exp(-d1 * d1 * 4.5);

    // --- Secondary spotlight: faster, tighter, counter-phase (~41s) ---
    float t2 = time * 0.15 + 3.14;
    float2 light2 = float2(
        0.5 + 0.35 * sin(t2 * 1.3),
        0.5 + 0.28 * cos(t2 * 0.8)
    );
    float d2 = length(uv - light2);
    float spot2 = exp(-d2 * d2 * 11.0);

    // --- Breathing: intensity pulse (~79s) ---
    float breath = 0.88 + 0.12 * sin(time * 0.08);

    // --- Film grain: hash-based, animated by time ---
    float2 grainSeed = position + float2(time * 11.3, time * 7.7);
    float noise = fract(sin(dot(grainSeed, float2(12.9898, 78.233))) * 43758.5453);
    float grain = (noise - 0.5) * 0.12;

    // --- Vignette: edges fade ~20% ---
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.35, 0.78, length(centered));
    vignette = mix(0.78, 1.0, vignette);

    // Combined alpha:
    //   - base ink at 13% * vignette
    //   - spotlight 1 boost up to +10%
    //   - spotlight 2 boost up to +6% (tighter, more localized)
    //   - all spotlight gain modulated by breath
    //   - grain adds ±3% on top of ink
    float baseAlpha = 0.13 * vignette;
    float spotBoost = (spot1 * 0.10 + spot2 * 0.06) * breath;
    float alpha = ink * (baseAlpha + spotBoost + grain * 0.25);
    alpha = clamp(alpha, 0.0, 0.32);

    half3 rgb = isDark > 0.5 ? half3(1.0) : color.rgb;
    return half4(rgb, half(alpha));
}
