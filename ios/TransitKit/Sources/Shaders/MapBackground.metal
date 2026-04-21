#include <metal_stdlib>
using namespace metal;

/// Ghost map background for the Home screen.
///
/// The map is rendered as ink lines with multiple moving lighting effects that create
/// clearly visible variation across the screen:
///   1) Primary bright spotlight — slow wide Lissajous (~62s), strong boost
///   2) Secondary bright spotlight — faster tighter counter-phase (~41s)
///   3) Anti-spotlight — counter-phase dimmer zone that reduces ink locally, creating
///      visible contrast bands that drift across the map
///   4) Accent tint — ink shifts toward operator accent color in the bright spotlight zone
///   5) Film grain — hash-based noise animated per frame for "living" texture
///   6) Vignette — edges fade for content anchoring
///
/// Uniforms:
///   accentR/G/B — operator accent color components (0..1), used for tint shift.
///   isDark — 1.0 for dark mode (renders light ink on transparent).
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

    // --- Primary bright spotlight (~16s per cycle, fast enough to be noticeable) ---
    float t1 = time * 0.40;
    float2 light1 = float2(
        0.5 + 0.45 * sin(t1),
        0.5 + 0.35 * sin(t1 * 1.618 + 0.9)
    );
    float d1 = length(uv - light1);
    float spot1 = exp(-d1 * d1 * 3.0);

    // --- Secondary bright spotlight (~10s per cycle, faster tighter) ---
    float t2 = time * 0.62 + 3.14;
    float2 light2 = float2(
        0.5 + 0.38 * sin(t2 * 1.3),
        0.5 + 0.32 * cos(t2 * 0.8)
    );
    float d2 = length(uv - light2);
    float spot2 = exp(-d2 * d2 * 7.0);

    // --- Anti-spotlight: dimmer zone drifting contra direction (~13s) ---
    float t3 = time * 0.48 + 1.57;
    float2 darkCenter = float2(
        0.5 - 0.42 * sin(t3 * 0.9),
        0.5 - 0.35 * cos(t3 * 1.1)
    );
    float d3 = length(uv - darkCenter);
    float darkZone = exp(-d3 * d3 * 4.0);

    // --- Breathing modulation on bright spots (~16s) ---
    float breath = 0.80 + 0.20 * sin(time * 0.40);

    // --- Film grain: hash-based, animated ---
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float noise = fract(sin(dot(grainSeed, float2(12.9898, 78.233))) * 43758.5453);
    float grain = (noise - 0.5) * 0.18;

    // --- Vignette: edges fade ~30% ---
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.30, 0.78, length(centered));
    vignette = mix(0.65, 1.0, vignette);

    // Combined alpha:
    //   base ~18% * vignette
    //   bright spot1 boost up to +28% (breath modulated)
    //   bright spot2 boost up to +18% (breath modulated)
    //   anti-spot reduces up to -10% (creates visible contrast bands)
    //   grain ±5% on the ink
    float baseAlpha = 0.18 * vignette;
    float spotBoost = (spot1 * 0.28 + spot2 * 0.18) * breath;
    float darkPenalty = darkZone * 0.10;
    float alpha = ink * (baseAlpha + spotBoost - darkPenalty + grain * 0.35);
    alpha = clamp(alpha, 0.0, 0.55);

    // --- Ink color with accent tint in the bright spotlight zone ---
    half3 rgb;
    if (isDark > 0.5) {
        // Dark mode: white ink, tinted toward accent in spotlight
        half3 accentCol = half3(half(accentR), half(accentG), half(accentB));
        rgb = mix(half3(1.0), accentCol, half(spot1 * 0.45));
    } else {
        // Light mode: keep ink dark, but blend toward accent in bright zone
        half3 inkCol = color.rgb;
        half3 accentCol = half3(half(accentR), half(accentG), half(accentB));
        rgb = mix(inkCol, accentCol, half(spot1 * 0.35));
    }

    return half4(rgb, half(alpha));
}
