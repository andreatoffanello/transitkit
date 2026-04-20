#include <metal_stdlib>
using namespace metal;

/// Ghost map background for StopDetailView.
/// Dark ink lines from the operator background image are shown at very low opacity (~4-10%)
/// with a slow-drifting Gaussian spotlight on a Lissajous path that makes the map feel alive.
/// Handles both light mode (dark lines on transparent) and dark mode (light lines).
[[ stitchable ]] half4 mapGlowEffect(
    float2 position,
    half4 color,
    float2 size,
    float time,
    float isDark
) {
    float2 uv = position / size;

    // Luminance of the image pixel
    float lum = dot(float3(color.rgb), float3(0.299, 0.587, 0.114));

    // "Ink density": high where dark lines are, near-zero on white background.
    // In dark mode invert: treat bright areas as ink so lines read light-on-dark.
    float ink = isDark > 0.5 ? lum : (1.0 - lum);

    // Soft Gaussian spotlight drifting on a slow Lissajous path (~62s per cycle)
    float t = time * 0.10;
    float2 lightUV = float2(
        0.5 + 0.40 * sin(t),
        0.5 + 0.30 * sin(t * 1.618 + 0.9)
    );
    float d = length(uv - lightUV);
    float spotlight = exp(-d * d * 4.5);

    // Base visibility: 15% for ink lines; up to +10% near the spotlight centre.
    // Bumped from 4%/+7% — card glass materials + footer gradient provide local readability.
    float alpha = ink * (0.15 + spotlight * 0.10);

    // Output: original ink colour in light mode, white in dark mode
    half3 rgb = isDark > 0.5 ? half3(1.0) : color.rgb;
    return half4(rgb, half(alpha));
}
