#include <metal_stdlib>
using namespace metal;

// --- Noise helpers ---

static inline float hash(float2 p) {
    return fract(sin(dot(p, float2(127.1, 311.7))) * 43758.5453);
}

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

static inline float fbm4(float2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 4; i++) {
        v += a * vnoise(p);
        p *= 2.03;
        a *= 0.5;
    }
    return v;
}

/// Ghost map reveal effect.
///
/// Design (rewrite richiesto dal designer):
///   - Base tenue sempre visibile (come prima versione — mappa leggera percepibile)
///   - SOPRA la base, tante macchie independenti che appaiono/scompaiono:
///     noise ad alta frequenza con smoothstep morbido → dissoluzione graduale
///     tra invisibile e pieno, tipo vaporoso/acquarello
///   - Le macchie driftano con il tempo su due scale diverse (crea composizione
///     che cambia continuamente, mai una singola onda voluminosa)
///   - Glare halo attorno alle zone rivelate per effetto "bleed" acquarello
///   - Accent tint nelle macchie rivelate
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

    // --- Base tenue: densità bassa ma presente ovunque ---
    float2 centered = uv - 0.5;
    float vignette = 1.0 - smoothstep(0.32, 0.82, length(centered));
    vignette = mix(0.55, 1.0, vignette);
    float baseAlpha = 0.14 * vignette;

    // --- Macchie: 3 layer di noise a frequenze basse (macchie larghe) ---
    // Drift molto lento per movimento "calmo" e contemplativo
    float noiseA = fbm4(uv * 4.5 + float2(time * 0.10, time * 0.07));
    float noiseB = fbm4(uv * 8.0 + float2(-time * 0.12, time * 0.09));
    float noiseC = fbm4(uv * 13.0 + float2(time * 0.07, -time * 0.11));

    // Smoothstep con banda larga → dissoluzione molto sfumata sui bordi
    float blobA = smoothstep(0.44, 0.82, noiseA);
    float blobB = smoothstep(0.48, 0.84, noiseB);
    float blobC = smoothstep(0.52, 0.86, noiseC);

    float blobs = max(blobA, max(blobB * 0.85, blobC * 0.70));

    // --- Glare: alone sfocato ampio che bleeds out oltre le macchie ---
    // Frequenze molto basse → macchie di glare grandi che si espandono
    float haloInner = fbm4(uv * 2.2 + float2(time * 0.08, -time * 0.06));
    float haloOuter = fbm4(uv * 0.9 + float2(-time * 0.05, time * 0.04));
    // Soglie basse e bande larghe → halo dolce ma presente in molte zone
    float halo = smoothstep(0.38, 0.72, haloInner) * 0.80
               + smoothstep(0.42, 0.78, haloOuter) * 0.55;
    halo = clamp(halo, 0.0, 1.0);

    // --- Breathing molto lento (~20s) per pulsazione globale ---
    float breath = 0.85 + 0.15 * sin(time * 0.32);

    // --- Film grain: hash-based animato, visibile ma non dominante ---
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float grain = (hash(grainSeed) - 0.5) * 0.18;

    // Reveal strength combinato: halo contribuisce pesantemente per sentirlo.
    float reveal = max(blobs, halo * 0.95);

    // --- Dissolvenza-blur aggressiva via ink thresholding adattivo ---
    // threshold da 0.88 (zone "morte": quasi nessun ink sopravvive) a 0.0
    // (zone pienamente rivelate: tutto visibile).
    // Banda transizione larga (0.38) → fade-out graduale lungo, effetto
    // "sciogliersi nell'aria".
    float threshold = mix(0.88, 0.0, reveal);
    float softInk = smoothstep(threshold, threshold + 0.38, ink);

    // Boost dato dalle macchie + halo (molto più presente).
    float blobBoost = (blobs * 0.55 + halo * 0.45) * breath;

    // Alpha finale: tutto modulato da softInk → linee sottili dissolvono
    // nelle zone morenti, lingering solo le più dense.
    float alpha = baseAlpha * softInk + softInk * (blobBoost + grain * 0.25);
    alpha = clamp(alpha, 0.0, 0.55);

    // --- Accent tint: ink vira verso colore operatore nelle macchie rivelate ---
    half3 accentCol = half3(half(accentR), half(accentG), half(accentB));
    float tintAmount = blobs * 0.50 + halo * 0.20;

    half3 rgb;
    if (isDark > 0.5) {
        rgb = mix(half3(1.0), accentCol, half(tintAmount));
    } else {
        rgb = mix(color.rgb, accentCol, half(tintAmount));
    }

    return half4(rgb, half(alpha));
}
