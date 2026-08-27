import SwiftUI

/// Linearizes one sRGB component for WCAG relative-luminance math.
private func linearize(_ c: Double) -> Double {
    c <= 0.04045 ? c / 12.92 : pow((c + 0.055) / 1.055, 2.4)
}

/// Parses `#RRGGBB` into 0…1 sRGB components. Returns nil for anything else.
private func rgbComponents(_ hex: String) -> (r: Double, g: Double, b: Double)? {
    let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
    guard clean.count == 6 else { return nil }
    var int: UInt64 = 0
    Scanner(string: clean).scanHexInt64(&int)
    return (Double((int >> 16) & 0xFF) / 255,
            Double((int >> 8) & 0xFF) / 255,
            Double(int & 0xFF) / 255)
}

/// WCAG 2.1 relative luminance of a `#RRGGBB` color.
func relativeLuminance(_ hex: String) -> Double {
    guard let c = rgbComponents(hex) else { return 0 }
    return 0.2126 * linearize(c.r) + 0.7152 * linearize(c.g) + 0.0722 * linearize(c.b)
}

/// WCAG 2.1 contrast ratio between two `#RRGGBB` colors. 1…21.
func contrastRatio(_ a: String, _ b: String) -> Double {
    let la = relativeLuminance(a), lb = relativeLuminance(b)
    return (max(la, lb) + 0.05) / (min(la, lb) + 0.05)
}

/// Computes WCAG 2.1 relative luminance for a hex color and returns
/// the text color (#FFFFFF or #000000) that achieves the highest contrast ratio.
func contrastingTextColor(for backgroundHex: String) -> String {
    guard rgbComponents(backgroundHex) != nil else { return "#FFFFFF" }
    let L = relativeLuminance(backgroundHex)
    // Contrast with white: (1 + 0.05) / (L + 0.05)
    // Contrast with black: (L + 0.05) / (0 + 0.05)
    return 1.05 / (L + 0.05) >= (L + 0.05) / 0.05 ? "#FFFFFF" : "#000000"
}

// MARK: - Accent legibility

/// Lightens `hex` in HSL until it clears `minRatio` against `against`, capped at
/// `maxSteps` bumps of 2% lightness. Returns the original when it already passes.
///
/// Why this exists: `accentColor` is operator config and is only ever validated
/// for legibility against WHITE (`textOnPrimary`). Used raw as a *foreground* on
/// the near-black dark background it fails — appalcart `#06845C` scores 4.15:1,
/// charlottesville `#0066CC` only 3.51:1. Any new operator can be worse still, so
/// the correction has to be computed, not hand-picked per brand.
///
/// `minRatio` è 5.2 e non 4.5 perché `against` è la superficie più scura che
/// esista: quasi nulla è disegnato direttamente su `#080C18`. Le card sono
/// bgSecondary (bianco 5% sopra) e le card sopra la mappa sono più chiare
/// ancora — convergere esatti a 4.5 lì lascia zero margine e il testo finisce
/// sotto AA proprio dove è dipinto davvero. Il surplus copre quel dislivello.
func readableOnDark(_ hex: String, against: String = "#080C18", minRatio: Double = 5.2, maxSteps: Int = 40) -> String {
    guard let c = rgbComponents(hex) else { return hex }
    guard contrastRatio(hex, against) < minRatio else { return hex }

    var (h, s, l) = rgbToHsl(c.r, c.g, c.b)
    // A fully desaturated accent stops reading as brand; keep a floor on S while
    // climbing L so the hue survives the correction. Il floor NON si applica a
    // un accent acromatico: per un grigio `rgbToHsl` restituisce h=0 (rosso) e
    // s=0, e alzare S inventerebbe una tinta che il brand non ha mai avuto —
    // un accent nero uscirebbe rosa. Un brand grigio schiarisce a grigio.
    if s > 0 { s = max(s, 0.25) }
    for _ in 0..<maxSteps {
        l = min(1.0, l + 0.02)
        let candidate = hslToHex(h, s, l)
        if contrastRatio(candidate, against) >= minRatio { return candidate }
        if l >= 1.0 { break }
    }
    // Unreachable for any real hue, but never hand back something illegible.
    return "#F0F0F0"
}

/// Specchio di `readableOnDark` per il tema chiaro: scurisce `hex` in HSL finché
/// non supera `minRatio` contro `against`.
///
/// Serve la stessa protezione del ramo dark, e per la stessa ragione: l'accent
/// arriva dal config dell'operatore. Un verde chiaro (rfta `#4CAF50`) come label
/// di un bottone secondario sul proprio fill tonale sta a 2.5:1 — di giorno il
/// bottone sparisce. Scendere di L conserva l'hue del brand dove alzarla lo
/// laverebbe. Parità con Android `Color.readableOnLight()`.
func readableOnLight(_ hex: String, against: String = "#FFFFFF", minRatio: Double = 4.5, maxSteps: Int = 50) -> String {
    guard let c = rgbComponents(hex) else { return hex }
    guard contrastRatio(hex, against) < minRatio else { return hex }

    var (h, s, l) = rgbToHsl(c.r, c.g, c.b)
    if s > 0 { s = max(s, 0.25) }
    for _ in 0..<maxSteps {
        l = max(0, l - 0.02)
        let candidate = hslToHex(h, s, l)
        if contrastRatio(candidate, against) >= minRatio { return candidate }
        if l <= 0 { break }
    }
    return "#111827"
}

/// Composita `hex` con opacità `alpha` sopra `over`. Serve a misurare il
/// contrasto contro il fondo REALE su cui un token viene dipinto, non contro
/// il colore di sfondo della schermata.
func composite(_ hex: String, alpha: Double, over: String) -> String {
    guard let f = rgbComponents(hex), let b = rgbComponents(over) else { return hex }
    let r = f.r * alpha + b.r * (1 - alpha)
    let g = f.g * alpha + b.g * (1 - alpha)
    let bl = f.b * alpha + b.b * (1 - alpha)
    return String(format: "#%02X%02X%02X",
                  Int((r * 255).rounded()), Int((g * 255).rounded()), Int((bl * 255).rounded()))
}

private func rgbToHsl(_ r: Double, _ g: Double, _ b: Double) -> (Double, Double, Double) {
    let maxV = max(r, g, b), minV = min(r, g, b)
    let l = (maxV + minV) / 2
    guard maxV != minV else { return (0, 0, l) }
    let d = maxV - minV
    let s = l > 0.5 ? d / (2 - maxV - minV) : d / (maxV + minV)
    var h: Double
    switch maxV {
    case r: h = (g - b) / d + (g < b ? 6 : 0)
    case g: h = (b - r) / d + 2
    default: h = (r - g) / d + 4
    }
    return (h / 6, s, l)
}

private func hslToHex(_ h: Double, _ s: Double, _ l: Double) -> String {
    func hue2rgb(_ p: Double, _ q: Double, _ t0: Double) -> Double {
        var t = t0
        if t < 0 { t += 1 }
        if t > 1 { t -= 1 }
        if t < 1.0 / 6 { return p + (q - p) * 6 * t }
        if t < 1.0 / 2 { return q }
        if t < 2.0 / 3 { return p + (q - p) * (2.0 / 3 - t) * 6 }
        return p
    }
    let r, g, b: Double
    if s == 0 {
        (r, g, b) = (l, l, l)
    } else {
        let q = l < 0.5 ? l * (1 + s) : l + s - l * s
        let p = 2 * l - q
        r = hue2rgb(p, q, h + 1.0 / 3)
        g = hue2rgb(p, q, h)
        b = hue2rgb(p, q, h - 1.0 / 3)
    }
    return String(format: "#%02X%02X%02X",
                  Int((r * 255).rounded()), Int((g * 255).rounded()), Int((b * 255).rounded()))
}
