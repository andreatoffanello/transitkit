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

    // --- Macchie: 2 layer di noise ad alta frequenza in movimento ---
    // Layer A: macchie medie (~10-12 per schermo), drift lento
    float noiseA = fbm4(uv * 7.5 + float2(time * 0.10, time * 0.07));
    // Layer B: macchie più piccole (~20-30), drift in direzione opposta
    float noiseB = fbm4(uv * 14.0 + float2(-time * 0.12, time * 0.09));

    // Smoothstep morbido → dissoluzione graduale tra "nulla" e "pieno"
    // Bordi ampi dello smoothstep (0.35→0.85) per simulare blur/sfumatura
    // lunga, tipo vaporoso.
    float blobA = smoothstep(0.38, 0.78, noiseA);
    float blobB = smoothstep(0.42, 0.82, noiseB);

    // Combinazione pesata: layer A domina, B aggiunge varietà fine
    float blobs = blobA * 0.65 + blobB * 0.45;
    blobs = clamp(blobs, 0.0, 1.0);

    // --- Glare diffuso: alone attorno alle zone rivelate (bleed acquarello) ---
    // Prendiamo un noise più basso come "halo" che bleeds out dalle blobs
    float haloNoise = fbm4(uv * 3.8 + float2(time * 0.06, -time * 0.05));
    float halo = smoothstep(0.45, 0.75, haloNoise) * 0.35;

    // --- Breathing molto lento (~20s) per pulsazione globale ---
    float breath = 0.85 + 0.15 * sin(time * 0.32);

    // --- Film grain: hash-based animato, visibile ma non dominante ---
    float2 grainSeed = position + float2(time * 13.7, time * 9.3);
    float grain = (hash(grainSeed) - 0.5) * 0.18;

    // Boost dato dalle macchie sopra la base tenue.
    // blobBoost va da 0 (zone non rivelate = solo base tenue) a ~0.35 (zone pienamente
    // rivelate = ink nitido). Halo aggiunge un layer intermedio sfumato.
    float blobBoost = (blobs * 0.35 + halo * 0.15) * breath;

    // Alpha finale:
    //   - baseAlpha sempre presente (mappa tenue percepibile)
    //   - blobBoost aggiunge rivelazioni localizzate sopra
    //   - grain su tutto (indipendente da blobs per dare texture continua)
    float alpha = ink * (baseAlpha + blobBoost + grain * 0.25);
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
