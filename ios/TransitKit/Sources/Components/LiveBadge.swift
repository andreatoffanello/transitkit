import SwiftUI

// MARK: - LiveBadge

/// Mini-chip "● LIVE" che appare prima del countdown quando il feed RT ha
/// fornito un delay plausibile (dopo clamp di sanità in VehicleStore).
/// L'assenza del chip significa "orario programmato puro" — l'utente sa che
/// l'orario può scivolare senza preavviso.
///
/// Parità Movete `liveBadge` (DepartureRow.swift, commit 960a2c0fbf).
/// Non pulsante: il segnale è il chip stesso, non un'animazione.
struct LiveBadge: View {
    var body: some View {
        HStack(spacing: 3) {
            Circle()
                .fill(AppTheme.realtimeGreen)
                .frame(width: 4, height: 4)
            Text("LIVE")
                .font(.system(size: 9, weight: .semibold, design: .rounded))
                .foregroundStyle(AppTheme.realtimeGreen)
                .tracking(0.3)
                .fixedSize()
        }
        .padding(.horizontal, 5)
        .frame(height: 13)
        .background(
            Capsule(style: .continuous)
                .fill(AppTheme.realtimeGreen.opacity(0.12))
        )
        .overlay(
            Capsule(style: .continuous)
                .stroke(AppTheme.realtimeGreen.opacity(0.25), lineWidth: 0.5)
        )
        .accessibilityLabel(String(localized: "live_badge_a11y"))
        .accessibilityHidden(false)
    }
}
