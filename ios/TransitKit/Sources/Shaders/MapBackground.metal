#include <metal_stdlib>
#include <SwiftUI/SwiftUI_Metal.h>
using namespace metal;

// --- Noise helpers ---

static float hash(float2 p) {
    p = fract(p * float2(127.1, 311.7));
    p += dot(p, p + 74.3);
    return fract(p.x * p.y);
}

static float noise(float2 p) {
    float2 i = floor(p);
    float2 f = fract(p);
    float2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + float2(1.0, 0.0));
    float c = hash(i + float2(0.0, 1.0));
    float d = hash(i + float2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

static float fbm(float2 p) {
    float v = 0.0, a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * noise(p);
        p = p * 2.1 + float2(3.7, 1.9);
        a *= 0.5;
    }
    return v;
}

/// Map reveal effect per layer composition.
///
/// Pensato per essere invocato su N layer della stessa immagine stackati in
/// SwiftUI, ciascuno con un diverso `sharpness` e un diverso `.blur(radius:)`
/// SwiftUI sul View. Il blur "vero" dà la sensazione di nebbia/acquarello;
/// lo shader controlla colore e visibilità.
///
/// Parametri:
///   - sharpness: 0.0 = fog layer (always present), 1.0 = crisp layer
///   - accentR/G/B: colore base operatore. Le varianti vengono derivate
///     in shader (base + versione più luminosa + versione tintata chiara).
[[ stitchable ]] half4 mapGlowEffect(
    float2 position,
    half4 color,
    float2 size,
    float time,
    float sharpness,
    float accentR,
    float accentG,
    float accentB
) {
    // Solo pixel con ink visibile (strade = scuro sull'immagine originale)
    float lum = dot(float3(color.rgb), float3(0.299, 0.587, 0.114));
    float ink = 1.0 - lum;
    if (ink < 0.05) return half4(0.0);

    float2 uv = position / size;

    // --- Colore base + 2 varianti più luminose (niente cafonate multicolor) ---
    half3 accent       = half3(half(accentR), half(accentG), half(accentB));
    half3 accentBright = mix(accent, half3(1.0), 0.18h);  // leggermente più luminoso
    half3 accentLight  = mix(accent, half3(1.0), 0.42h);  // chiaro arioso

    // --- Color waves: mescolano base ↔ brillante ↔ chiaro in pattern fluidi ---
    float wave1 = sin(uv.x * 5.0 + uv.y * 3.0 - time * 1.4) * 0.5 + 0.5;
    float wave2 = sin(-uv.x * 4.0 + uv.y * 6.0 + time * 1.0) * 0.5 + 0.5;
    float dist  = length(uv - float2(0.5, 0.4));
    float wave3 = sin(dist * 10.0 - time * 1.6) * 0.5 + 0.5;
    float wave4 = sin((uv.x + uv.y) * 3.0 - time * 0.5) * 0.5 + 0.5;

    // Color mix: accent ↔ bright su wave1, blend verso light su wave3/wave4
    half3 col1 = mix(accent, accentBright, half(wave1));
    half3 finalColor = mix(col1, accentLight, half(wave3 * 0.4 + wave4 * 0.3));

    // --- Organic breathing (fbm a 2 scale) ---
    float2 nc1 = uv * 2.5 + float2(time * 0.12, time * 0.08);
    float breath1 = fbm(nc1);
    float2 nc2 = uv * 1.2 + float2(-time * 0.07, time * 0.10);
    float breath2 = fbm(nc2 + 50.0);
    float breathing = breath1 * 0.6 + breath2 * 0.4;

    // --- Sharpness-dependent visibility ---
    // Layer fog (sharpness≈0): soglie ampie → quasi sempre visibile
    // Layer crisp (sharpness≈1): soglie strette → visibile solo su peak breathing
    float loThresh = mix(0.15, 0.40, sharpness);
    float hiThresh = mix(0.35, 0.60, sharpness);
    float visibility = smoothstep(loThresh, hiThresh, breathing);

    // Fog mantiene presenza minima anche quando breathing è basso
    float minVis = mix(0.25, 0.0, sharpness);
    visibility = max(visibility, minVis);

    // --- Glow additivo ---
    float glow = wave1 * 0.3 + wave2 * 0.3 + wave3 * 0.2 + wave4 * 0.2;
    glow += pow(wave1 * wave2 * wave3, 0.5) * 0.5;
    glow = pow(glow, 0.6);

    float intensity = ink * glow * visibility;
    half alpha = half(intensity);
    return half4(finalColor * alpha, alpha);
}
