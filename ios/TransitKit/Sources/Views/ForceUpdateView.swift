import SwiftUI

/// Blocking full-screen overlay shown when the installed version is below the
/// operator's required minimum (`appUpdate.ios.minVersion` + `force:true`).
/// No dismiss path — the only action is opening the App Store.
///
/// Mirrors `ForceUpdateView` from DoVe with identical animation approach and
/// wave background, adapted to use TransitKit's `AppTheme` accent palette.
struct ForceUpdateView: View {
    let message: String
    let storeUrl: URL

    @Environment(\.colorScheme) private var colorScheme
    @State private var visible = false
    @State private var iconRotation: Double = 0
    @State private var glowPulse: CGFloat = 0.85
    @State private var ctaPulse: CGFloat = 1.0
    @State private var pressing = false

    private var isDark: Bool { colorScheme == .dark }

    private var accentDeep: Color { AppTheme.primary }
    private var accentBright: Color { AppTheme.accent }
    private var bgTop: Color {
        isDark ? Color(hex: "#0D1220") : Color(hex: "#EEF2FF")
    }
    private var bgBottom: Color {
        isDark ? Color(hex: "#080C18") : Color(hex: "#D8E2FF")
    }

    var body: some View {
        GeometryReader { geo in
            ZStack {
                LinearGradient(
                    colors: [bgTop, bgBottom],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea()

                animatedWaves(size: geo.size)
                    .ignoresSafeArea()
                    .allowsHitTesting(false)

                VStack(spacing: 0) {
                    Spacer()

                    iconWithGlow
                        .padding(.bottom, 32)

                    Text(String(localized: "update_force_title"))
                        .font(.system(size: 30, weight: .bold, design: .rounded))
                        .foregroundStyle(isDark ? Color(hex: "#F0F4FF") : Color(hex: "#0D1220"))
                        .multilineTextAlignment(.center)
                        .scaleEffect(visible ? 1 : 0.9)
                        .opacity(visible ? 1 : 0)
                        .offset(y: visible ? 0 : 16)
                        .animation(
                            .spring(response: 0.65, dampingFraction: 0.72).delay(0.18),
                            value: visible
                        )

                    Text(message)
                        .font(.system(size: 16, weight: .regular))
                        .foregroundStyle(isDark ? Color(hex: "#B0BEDD") : Color(hex: "#3D4E78"))
                        .multilineTextAlignment(.center)
                        .lineSpacing(4)
                        .padding(.horizontal, 36)
                        .padding(.top, 14)
                        .opacity(visible ? 1 : 0)
                        .offset(y: visible ? 0 : 14)
                        .animation(.easeOut(duration: 0.55).delay(0.34), value: visible)

                    Spacer()

                    updateButton
                        .padding(.horizontal, 24)
                        .padding(.bottom, 36)
                        .opacity(visible ? 1 : 0)
                        .offset(y: visible ? 0 : 22)
                        .animation(
                            .spring(response: 0.7, dampingFraction: 0.78).delay(0.48),
                            value: visible
                        )
                }
            }
        }
        .onAppear {
            visible = true
            withAnimation(.linear(duration: 6).repeatForever(autoreverses: false)) {
                iconRotation = 360
            }
            withAnimation(.easeInOut(duration: 2.6).repeatForever(autoreverses: true)) {
                glowPulse = 1.10
            }
            withAnimation(
                .easeInOut(duration: 1.8).repeatForever(autoreverses: true).delay(1.0)
            ) {
                ctaPulse = 1.025
            }
        }
    }

    @ViewBuilder
    private var iconWithGlow: some View {
        ZStack {
            Circle()
                .fill(
                    RadialGradient(
                        colors: [
                            accentBright.opacity(isDark ? 0.45 : 0.30),
                            accentBright.opacity(0)
                        ],
                        center: .center,
                        startRadius: 4,
                        endRadius: 160
                    )
                )
                .frame(width: 320, height: 320)
                .scaleEffect(visible ? glowPulse : 0.6)
                .opacity(visible ? 1 : 0)
                .animation(.easeOut(duration: 0.6), value: visible)

            Circle()
                .fill(
                    LinearGradient(
                        colors: [accentBright, accentDeep],
                        startPoint: .topLeading,
                        endPoint: .bottomTrailing
                    )
                )
                .frame(width: 96, height: 96)
                .shadow(color: accentDeep.opacity(0.35), radius: 18, x: 0, y: 10)
                .overlay(
                    Circle()
                        .strokeBorder(.white.opacity(0.15), lineWidth: 1)
                )
                .scaleEffect(visible ? 1 : 0.55)
                .opacity(visible ? 1 : 0)
                .blur(radius: visible ? 0 : 4)
                .animation(.spring(response: 0.7, dampingFraction: 0.62), value: visible)

            LucideIcon.refreshCw.sized(44)
                .foregroundStyle(.white)
                .rotationEffect(.degrees(iconRotation))
                .scaleEffect(visible ? 1 : 0.6)
                .opacity(visible ? 1 : 0)
                .animation(.spring(response: 0.7, dampingFraction: 0.62).delay(0.05), value: visible)
        }
    }

    @ViewBuilder
    private var updateButton: some View {
        Button {
            UIImpactFeedbackGenerator(style: .medium).impactOccurred()
            AppUpdateChecker.shared.openStore(storeUrl)
        } label: {
            HStack(spacing: 10) {
                Text(String(localized: "update_force_cta"))
                    .font(.system(size: 17, weight: .semibold))
                LucideIcon.externalLink.sized(18)
                    .frame(width: 18, height: 18)
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(
                LinearGradient(
                    colors: [accentBright, accentDeep],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
            )
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(
                RoundedRectangle(cornerRadius: 18, style: .continuous)
                    .strokeBorder(.white.opacity(0.18), lineWidth: 1)
            )
            .shadow(color: accentDeep.opacity(0.40), radius: 20, x: 0, y: 10)
        }
        .accessibilityIdentifier("btn_force_update")
        .scaleEffect(pressing ? 0.97 : ctaPulse)
        .animation(.spring(response: 0.3, dampingFraction: 0.7), value: pressing)
        .simultaneousGesture(
            DragGesture(minimumDistance: 0)
                .onChanged { _ in pressing = true }
                .onEnded { _ in pressing = false }
        )
    }

    @ViewBuilder
    private func animatedWaves(size: CGSize) -> some View {
        TimelineView(.animation(minimumInterval: 1.0 / 30.0)) { ctx in
            let t = ctx.date.timeIntervalSinceReferenceDate
            ZStack(alignment: .bottom) {
                WaveShape(amplitude: 22, frequency: 1.0, phase: t * 0.45)
                    .fill(accentBright.opacity(isDark ? 0.18 : 0.22))
                    .frame(height: 220)
                WaveShape(amplitude: 16, frequency: 1.6, phase: t * 0.75 + 1.2)
                    .fill(accentDeep.opacity(isDark ? 0.22 : 0.16))
                    .frame(height: 170)
                WaveShape(amplitude: 11, frequency: 2.2, phase: t * 1.10 + 2.6)
                    .fill(.white.opacity(isDark ? 0.05 : 0.30))
                    .frame(height: 130)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .bottom)
        }
    }
}

// MARK: - Wave shape

private struct WaveShape: Shape {
    var amplitude: CGFloat
    var frequency: CGFloat
    var phase: Double

    func path(in rect: CGRect) -> Path {
        var path = Path()
        let midY = rect.height * 0.5
        let width = rect.width
        let step: CGFloat = 4

        path.move(to: CGPoint(x: 0, y: midY))
        var x: CGFloat = 0
        while x <= width {
            let relative = x / width
            let y = midY + sin(relative * .pi * 2 * frequency + CGFloat(phase)) * amplitude
            path.addLine(to: CGPoint(x: x, y: y))
            x += step
        }
        path.addLine(to: CGPoint(x: width, y: rect.height))
        path.addLine(to: CGPoint(x: 0, y: rect.height))
        path.closeSubpath()
        return path
    }
}
