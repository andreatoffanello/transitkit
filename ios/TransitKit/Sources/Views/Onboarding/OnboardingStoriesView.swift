import SwiftUI
import CoreLocation

/// First-launch onboarding in the Instagram-stories style: full-screen pages,
/// segmented progress bar in alto, swipe orizzontale per cambiare pagina.
///
/// Apple compliance: i prompt di sistema (posizione, notifiche) vengono
/// invocati SOLO al tap della CTA, dopo una pagina di pre-primer che spiega
/// chiaramente il beneficio per l'utente. Ogni pagina permesso ha sempre un
/// "Forse più tardi" che fa avanzare senza chiamare il prompt — niente dark
/// pattern, niente penalty per il rifiuto.
struct OnboardingStoriesView: View {
    @Environment(LocationManager.self) private var locationManager
    @Environment(PushNotificationManager.self) private var pushManager
    @Environment(\.dismiss) private var dismiss

    @State private var page: Int = 0
    @State private var permissionInFlight: Bool = false

    private let pageCount = 4

    var body: some View {
        ZStack {
            // Shader brandizzato (stesso della Home) — crea continuità visiva
            // tra onboarding e app. Intensity più alta perché l'onboarding non
            // ha le card della Home che diluiscono visivamente la texture.
            AppTheme.background.ignoresSafeArea()
            OperatorShaderBackground(intensity: 1.35).ignoresSafeArea()

            // Fade radiale: dà un'isola di leggibilità al centro dove vive il
            // testo, lasciando la texture più viva ai bordi.
            RadialGradient(
                colors: [
                    AppTheme.background.opacity(0.65),
                    AppTheme.background.opacity(0.0)
                ],
                center: .center,
                startRadius: 80,
                endRadius: 420
            )
            .ignoresSafeArea()
            .allowsHitTesting(false)

            // Soft accent vignette in fondo per separare la CTA dal background.
            VStack {
                Spacer()
                LinearGradient(
                    colors: [AppTheme.background.opacity(0), AppTheme.background.opacity(0.85)],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .frame(height: 220)
            }
            .ignoresSafeArea()
            .allowsHitTesting(false)

            VStack(spacing: 0) {
                progressBar
                    .padding(.horizontal, 16)
                    .padding(.top, 12)

                TabView(selection: $page) {
                    welcomePage.tag(0)
                    locationPage.tag(1)
                    notificationsPage.tag(2)
                    donePage.tag(3)
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.spring(response: 0.45, dampingFraction: 0.85), value: page)
            }
        }
    }

    // MARK: - Progress bar + close

    private var progressBar: some View {
        HStack(spacing: 6) {
            ForEach(0..<pageCount, id: \.self) { i in
                Capsule()
                    .fill(i <= page ? AppTheme.textPrimary.opacity(0.85) : AppTheme.textPrimary.opacity(0.18))
                    .frame(height: 3)
                    .animation(.easeInOut(duration: 0.25), value: page)
            }
            Button {
                UIImpactFeedbackGenerator(style: .light).impactOccurred()
                dismiss()
            } label: {
                LucideIcon.x.sized(16)
                    .foregroundStyle(AppTheme.textSecondary)
                    .frame(width: 28, height: 28)
                    .background(Circle().fill(AppTheme.bgSecondary))
            }
            .padding(.leading, 8)
            .accessibilityIdentifier("onboarding_close")
            .accessibilityLabel(String(localized: "action_close"))
        }
    }

    // MARK: - Page templates

    /// Layout condiviso: hero icon (96pt) + titolo + body + spacer + bottoni.
    @ViewBuilder
    private func storyPage(
        accent: Color,
        icon: LucideIcon,
        title: String,
        body: String,
        primaryCta: String,
        primaryAction: @escaping () -> Void,
        secondaryCta: String? = nil,
        secondaryAction: (() -> Void)? = nil
    ) -> some View {
        VStack(spacing: 0) {
            Spacer(minLength: 24)

            ZStack {
                Circle()
                    .fill(accent.opacity(0.14))
                    .frame(width: 120, height: 120)
                icon.sized(48)
                    .foregroundStyle(accent)
            }

            VStack(spacing: 14) {
                Text(title)
                    .font(.system(size: 28, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Text(body)
                    .font(.system(size: 16))
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, 32)
            }
            .padding(.top, 28)

            Spacer()

            VStack(spacing: 10) {
                Button {
                    UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                    primaryAction()
                } label: {
                    Text(primaryCta)
                        .font(.system(size: 16, weight: .semibold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 15)
                        .background(accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .disabled(permissionInFlight)

                if let secondaryCta, let secondaryAction {
                    Button {
                        secondaryAction()
                    } label: {
                        Text(secondaryCta)
                            .font(.system(size: 15, weight: .medium))
                            .foregroundStyle(AppTheme.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .disabled(permissionInFlight)
                }
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
        }
    }

    // MARK: - Pages

    private var welcomePage: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 32)

            Group {
                if UIImage(named: "OperatorLogo") != nil {
                    Image("OperatorLogo")
                        .resizable()
                        .scaledToFit()
                } else {
                    LucideIcon.busFront.sized(64)
                        .foregroundStyle(AppTheme.accent)
                }
            }
            .frame(width: 120, height: 120)

            VStack(spacing: 14) {
                Text(String(localized: "onb_welcome_title"))
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)
                Text(String(localized: "onb_welcome_body"))
                    .font(.system(size: 16))
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
                    .lineSpacing(3)
                    .padding(.horizontal, 28)
            }
            .padding(.top, 28)

            Spacer()

            Button {
                UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                advance()
            } label: {
                Text(String(localized: "onb_welcome_cta"))
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 15)
                    .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            }
            .padding(.horizontal, 24)
            .padding(.bottom, 32)
            .accessibilityIdentifier("onb_welcome_cta")
        }
    }

    private var locationPage: some View {
        storyPage(
            accent: AppTheme.accent,
            icon: .mapPin,
            title: String(localized: "onb_location_title"),
            body: String(localized: "onb_location_body"),
            primaryCta: String(localized: "onb_location_cta"),
            primaryAction: {
                permissionInFlight = true
                locationManager.requestPermissionAndStart()
                // Avanza immediatamente: il prompt OS appare sopra, indipendente
                // dal flusso UI. Lo stato vero (granted/denied) si legge dal
                // LocationManager nelle altre schermate.
                Task { @MainActor in
                    try? await Task.sleep(nanoseconds: 400_000_000)
                    permissionInFlight = false
                    advance()
                }
            }
        )
        .accessibilityIdentifier("onb_location_page")
    }

    private var notificationsPage: some View {
        storyPage(
            accent: AppTheme.accent,
            icon: .bell,
            title: String(localized: "onb_notif_title"),
            body: String(localized: "onb_notif_body"),
            primaryCta: String(localized: "onb_notif_cta"),
            primaryAction: {
                permissionInFlight = true
                Task {
                    _ = await pushManager.requestAuthorization()
                    await MainActor.run {
                        permissionInFlight = false
                        advance()
                    }
                }
            },
            secondaryCta: String(localized: "onb_notif_skip"),
            secondaryAction: { advance() }
        )
        .accessibilityIdentifier("onb_notif_page")
    }

    private var donePage: some View {
        storyPage(
            accent: AppTheme.accent,
            icon: .busFront,
            title: String(localized: "onb_done_title"),
            body: String(localized: "onb_done_body"),
            primaryCta: String(localized: "onb_done_cta"),
            primaryAction: { dismiss() }
        )
        .accessibilityIdentifier("onb_done_page")
    }

    // MARK: - Navigation

    private func advance() {
        if page < pageCount - 1 {
            withAnimation(.spring(response: 0.45, dampingFraction: 0.85)) {
                page += 1
            }
        } else {
            dismiss()
        }
    }
}
