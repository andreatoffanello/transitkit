import SwiftUI

/// Dev-only playground per iterare sullo shader `mapGlowEffect` senza navigare la app.
///
/// Apri via deep link:
///   xcrun simctl openurl <UDID> transitkit://shader
///
/// Mostra lo shader a pieno schermo + HUD minimale (tap per togglare).
/// Non referenziato da alcuna route di produzione — presentato come fullScreenCover
/// quando `DeepLinkRouter.showShaderPlayground == true`.
struct ShaderPlaygroundView: View {
    @Environment(\.dismiss) private var dismiss
    @Environment(\.colorScheme) private var colorScheme
    @State private var showHUD = true

    var body: some View {
        ZStack {
            // Background layer come in HomeTab — stesso shader, stessi uniform
            operatorMapBackground
                .ignoresSafeArea()

            // Background base (AppTheme.background) dietro, per mostrare com'è renderizzato
            // su contenuto reale. Applicato come "rect" dietro lo shader? No — lo shader
            // già compone su trasparenza, quindi vediamo AppTheme.background che traspare.

            // HUD: titolo, hint, exit button
            if showHUD {
                VStack {
                    HStack {
                        VStack(alignment: .leading, spacing: 2) {
                            Text("Shader Playground")
                                .font(.system(size: 13, weight: .semibold, design: .monospaced))
                                .foregroundStyle(AppTheme.textPrimary)
                            Text("mapGlowEffect — tap to hide HUD")
                                .font(.system(size: 10, design: .monospaced))
                                .foregroundStyle(AppTheme.textSecondary)
                        }
                        Spacer()
                        Button {
                            dismiss()
                        } label: {
                            LucideIcon.x.sized(14)
                                .foregroundStyle(AppTheme.textPrimary)
                                .frame(width: 36, height: 36)
                                .background(.ultraThinMaterial, in: Circle())
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.top, 8)

                    Spacer()

                    // Card mockup per vedere contrasto shader dietro glass
                    VStack(alignment: .leading, spacing: 8) {
                        Text("Mock card glass")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundStyle(AppTheme.textPrimary)
                        Text("Verifica leggibilità del testo sullo sfondo animato.")
                            .font(.system(size: 12))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                    .padding(14)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .background(.ultraThinMaterial, in: RoundedRectangle(cornerRadius: 14))
                    .overlay(
                        RoundedRectangle(cornerRadius: 14, style: .continuous)
                            .strokeBorder(AppTheme.glassBorder, lineWidth: 0.5)
                    )
                    .padding(.horizontal, 16)
                    .padding(.bottom, 32)
                }
            }
        }
        .background(AppTheme.background.ignoresSafeArea())
        .contentShape(Rectangle())
        .onTapGesture {
            withAnimation(.easeInOut(duration: 0.2)) { showHUD.toggle() }
        }
    }

    @ViewBuilder
    private var operatorMapBackground: some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { ctx in
            let t = Float(ctx.date.timeIntervalSinceReferenceDate.truncatingRemainder(dividingBy: 1000.0))
            let isDark: Float = colorScheme == .dark ? 1.0 : 0.0
            let (ar, ag, ab) = Self.rgbComponents(of: AppTheme.accent)
            GeometryReader { geo in
                Image("OperatorBackground")
                    .resizable()
                    .aspectRatio(contentMode: .fill)
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipped()
                    .colorEffect(
                        ShaderLibrary.mapGlowEffect(
                            .float2(geo.size),
                            .float(t),
                            .float(isDark),
                            .float(ar),
                            .float(ag),
                            .float(ab)
                        )
                    )
            }
        }
        .allowsHitTesting(false)
    }

    private static func rgbComponents(of color: Color) -> (Float, Float, Float) {
        let ui = UIColor(color)
        var r: CGFloat = 0, g: CGFloat = 0, b: CGFloat = 0, a: CGFloat = 0
        ui.getRed(&r, green: &g, blue: &b, alpha: &a)
        return (Float(r), Float(g), Float(b))
    }
}
