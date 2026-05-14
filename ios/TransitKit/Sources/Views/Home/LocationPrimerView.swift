import SwiftUI
import CoreLocation

/// Shown on first launch as a primer before the system location prompt.
/// Accept → triggers system prompt, dismisses. Skip → dismisses without prompt
/// (user can still enable from Settings > Privacy later).
struct LocationPrimerView: View {
    @Environment(LocationManager.self) private var locationManager
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        ZStack {
            // Solid background base — same del theme — sotto al ghost map così
            // il testo ha sempre un fondo pulito su cui appoggiarsi.
            AppTheme.background.ignoresSafeArea()

            // Ghost map background — molto leggero per non competere col testo.
            Image("OperatorBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .opacity(0.08)
                .ignoresSafeArea()

            // Fade radiale verso il centro per dare un'isola di leggibilità
            // attorno a icona/titolo senza nascondere la texture ai bordi.
            RadialGradient(
                colors: [
                    AppTheme.background.opacity(0.85),
                    AppTheme.background.opacity(0.0)
                ],
                center: .center,
                startRadius: 60,
                endRadius: 360
            )
            .ignoresSafeArea()
            .allowsHitTesting(false)

            VStack(spacing: 24) {
                Spacer()

                ZStack {
                    Circle()
                        .fill(AppTheme.accent.opacity(0.15))
                        .frame(width: 96, height: 96)
                    LucideIcon.mapPin.sized(40)
                        .foregroundStyle(AppTheme.accent)
                }

                VStack(spacing: 12) {
                    Text(String(localized: "location_primer_title"))
                        .font(.system(size: 26, weight: .bold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .multilineTextAlignment(.center)
                    Text(String(localized: "location_primer_body"))
                        .font(.system(size: 15))
                        .foregroundStyle(AppTheme.textSecondary)
                        .multilineTextAlignment(.center)
                        .padding(.horizontal, 32)
                }

                Spacer()

                VStack(spacing: 12) {
                    Button {
                        UIImpactFeedbackGenerator(style: .medium).impactOccurred()
                        locationManager.requestPermissionAndStart()
                        dismiss()
                    } label: {
                        Text(String(localized: "location_primer_cta_enable"))
                            .font(.system(size: 16, weight: .semibold))
                            .foregroundStyle(.white)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 14)
                            .background(AppTheme.accent, in: RoundedRectangle(cornerRadius: 14))
                    }
                    .accessibilityIdentifier("primer_enable_location")

                    Button {
                        dismiss()
                    } label: {
                        Text(String(localized: "location_primer_cta_skip"))
                            .font(.system(size: 15))
                            .foregroundStyle(AppTheme.textSecondary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                    }
                    .accessibilityIdentifier("primer_skip_location")
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 24)
            }
        }
    }
}
