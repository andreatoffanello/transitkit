import SwiftUI

/// Animated brand background: stack di 4 layer dell'asset `OperatorBackground`
/// passati attraverso lo shader Metal `mapGlowEffect` con sharpness crescente.
/// Tintato sull'accent dell'operatore, dà l'identità visiva "atmosfera mappa".
///
/// Estratto da `HomeTab` per essere riusabile in Onboarding e altre schermate
/// che vogliono ereditare lo stesso mood brandizzato.
///
/// PERF: animare SOLO in Home (o onboarding). Passare `animated: false` negli
/// hub con mappe a schermo pieno — lì il bg è un frame statico, non è visibile
/// e tenerlo a 30fps friggeva i device piccoli. Invariante:
/// mappa idle = zero loop 60fps + shader inattivo (parità DoVe VeniceMapBackground).
struct OperatorShaderBackground: View {
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.scenePhase) private var scenePhase

    @State private var thermalState: ProcessInfo.ThermalState = ProcessInfo.processInfo.thermalState
    @State private var isLowPowerMode: Bool = ProcessInfo.processInfo.isLowPowerModeEnabled
    @State private var startDate = Date.now

    /// Boost globale all'opacità di tutti i layer. 1.0 = come la Home; 1.4
    /// circa = onboarding (texture più presente perché manca il foreground
    /// denso di card che la diluisce).
    var intensity: CGFloat = 1.0

    /// Anima lo shader Metal + i blur. Spento (statico) fuori dalla Home, dove
    /// la mappa copre tutto il display: tenere shader + 4 blur a 30fps sopra
    /// una mappa già pesante friggeva i device piccoli. Vedi HomeTab.
    /// Parità con DoVe `VeniceMapBackground(animated:)`.
    var animated: Bool = true

    private var shouldAnimate: Bool {
        animated
            && scenePhase == .active
            && !isLowPowerMode
            && thermalState != .serious
            && thermalState != .critical
    }

    /// Frame "a regime" per lo stato statico — drift già avviato, non il t=0 piatto.
    /// Valore fisso → nessun ricalcolo; il bg statico non è visibile sotto la mappa.
    /// Parità con DoVe `frozenElapsed`.
    private var frozenTime: Float { 8.0 }

    var body: some View {
        let (ar, ag, ab) = Self.rgbComponents(of: AppTheme.accent)
        let isDark = colorScheme == .dark

        GeometryReader { geo in
            Group {
                if shouldAnimate {
                    // Cap a ~30fps: il drift è lento (sin a ≤1.6 rad/s), 60fps bruciano
                    // cicli su delta invisibili. `.periodic` schedula esattamente 30
                    // frame/s indipendentemente dal refresh del display → dimezza il
                    // costo shader+blur. Parità con DoVe (.periodic(by: 1/30)).
                    TimelineView(.periodic(from: startDate, by: 1.0 / 30.0)) { ctx in
                        let t = Float(ctx.date.timeIntervalSince(startDate).truncatingRemainder(dividingBy: 1000.0))
                        layersStack(size: geo.size, time: t, accent: (ar, ag, ab), isDark: isDark)
                    }
                } else {
                    // Statico: un solo frame, niente TimelineView. Shader + 4 blur
                    // girano UNA sola volta → niente GPU continuo.
                    layersStack(size: geo.size, time: frozenTime, accent: (ar, ag, ab), isDark: isDark)
                }
            }
        }
        .allowsHitTesting(false)
        .onReceive(NotificationCenter.default.publisher(for: ProcessInfo.thermalStateDidChangeNotification)) { _ in
            thermalState = ProcessInfo.processInfo.thermalState
        }
        .onReceive(NotificationCenter.default.publisher(for: Notification.Name.NSProcessInfoPowerStateDidChange)) { _ in
            isLowPowerMode = ProcessInfo.processInfo.isLowPowerModeEnabled
        }
    }

    @ViewBuilder
    private func layersStack(
        size: CGSize,
        time: Float,
        accent: (Float, Float, Float),
        isDark: Bool
    ) -> some View {
        ZStack {
            layer(size: size, time: time, sharpness: 0.0, accent: accent)
                .blur(radius: 28)
                .opacity(clampedOpacity(isDark ? 0.70 : 0.55))

            layer(size: size, time: time, sharpness: 0.3, accent: accent)
                .blur(radius: 12)
                .opacity(clampedOpacity(isDark ? 0.50 : 0.42))

            layer(size: size, time: time, sharpness: 0.7, accent: accent)
                .blur(radius: 4)
                .opacity(clampedOpacity(isDark ? 0.45 : 0.36))

            layer(size: size, time: time, sharpness: 1.0, accent: accent)
                .opacity(clampedOpacity(isDark ? 0.50 : 0.38))
        }
    }

    private func clampedOpacity(_ base: Double) -> Double {
        min(1.0, base * Double(intensity))
    }

    @ViewBuilder
    private func layer(
        size: CGSize,
        time: Float,
        sharpness: Float,
        accent: (Float, Float, Float)
    ) -> some View {
        Image("OperatorBackground")
            .resizable()
            .aspectRatio(contentMode: .fill)
            .frame(width: size.width, height: size.height)
            .clipped()
            .colorEffect(
                ShaderLibrary.mapGlowEffect(
                    .float2(size),
                    .float(time),
                    .float(sharpness),
                    .float(accent.0),
                    .float(accent.1),
                    .float(accent.2)
                )
            )
    }

    private static func rgbComponents(of color: Color) -> (Float, Float, Float) {
        let ui = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Float(r), Float(g), Float(b))
    }
}
