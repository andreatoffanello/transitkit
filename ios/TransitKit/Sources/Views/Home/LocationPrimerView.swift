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
            // Same ghost map background as Home for visual continuity
            Image("OperatorBackground")
                .resizable()
                .aspectRatio(contentMode: .fill)
                .ignoresSafeArea()
                .opacity(0.18)
                .overlay(Color.black.opacity(0.15).ignoresSafeArea())

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
