import SwiftUI

// MARK: - AlertToastOverlay

/// In-app toast that surfaces a freshly-arrived service alert when the affected
/// entity is one of the user's favorites. Shown overlay-style above the tab bar,
/// auto-dismisses after 4 seconds, tappable to drill into the full alert detail.
///
/// Keeps an in-memory "shown" set to dedup across poll cycles within a single
/// app session — we don't want to re-announce the same alert if the user
/// dismisses and it's still in the feed 60s later.
@MainActor
@Observable
final class AlertToastPresenter {
    private(set) var pendingAlert: GtfsRtAlert?
    private var shownIds: Set<String> = []
    private var dismissTask: Task<Void, Never>?

    /// Consider a batch of newly-active alerts; show the first one that matches
    /// a favorite stop, if any, and hasn't been shown yet this session.
    func consider(alerts: [GtfsRtAlert], favoriteStopIds: Set<String>) {
        for alert in alerts where !shownIds.contains(alert.id) {
            if !alert.affectedStopIds.isDisjoint(with: favoriteStopIds) {
                shownIds.insert(alert.id)
                present(alert)
                return // one at a time; the next one shows on the next poll cycle
            }
        }
    }

    private func present(_ alert: GtfsRtAlert) {
        pendingAlert = alert
        UINotificationFeedbackGenerator().notificationOccurred(.warning)
        dismissTask?.cancel()
        dismissTask = Task { [weak self] in
            try? await Task.sleep(for: .seconds(4))
            guard !Task.isCancelled else { return }
            withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
                self?.pendingAlert = nil
            }
        }
    }

    func dismiss() {
        dismissTask?.cancel()
        withAnimation(.spring(response: 0.35, dampingFraction: 0.85)) {
            pendingAlert = nil
        }
    }
}

// MARK: - Toast view

struct AlertToastView: View {
    let alert: GtfsRtAlert
    let onTap: () -> Void
    let onDismiss: () -> Void

    var body: some View {
        Button(action: onTap) {
            HStack(alignment: .top, spacing: 12) {
                ZStack {
                    Circle()
                        .fill(severityColor.opacity(0.18))
                        .frame(width: 32, height: 32)
                    LucideIcon.alertTriangle.sized(16)
                        .foregroundStyle(severityColor)
                }

                VStack(alignment: .leading, spacing: 2) {
                    Text(String(localized: "alerts_toast_kicker"))
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(AppTheme.textTertiary)
                        .kerning(0.4)
                    Text(alert.headerText.resolved())
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(2)
                        .multilineTextAlignment(.leading)
                }

                Spacer(minLength: 0)

                Button {
                    onDismiss()
                } label: {
                    LucideIcon.x.sized(14)
                        .foregroundStyle(AppTheme.textTertiary)
                        .padding(6)
                }
                .buttonStyle(.plain)
                .accessibilityLabel(String(localized: "action_close"))
            }
            .padding(14)
            .background(
                // Material, non bgSecondary: il toast galleggia sopra
                // QUALUNQUE tab (mappa, shader home) — il token glass in
                // dark (White 5%) sarebbe trasparente sul contenuto.
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .fill(.regularMaterial)
                    .shadow(color: .black.opacity(0.18), radius: 18, y: 6)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .strokeBorder(AppTheme.glassBorder, lineWidth: 1)
            )
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("alert_toast")
    }

    private var severityColor: Color {
        switch alert.severity {
        case .severe:  return .red
        case .warning: return .orange
        case .info:    return AppTheme.accent
        case .unknown: return AppTheme.textSecondary
        }
    }
}
