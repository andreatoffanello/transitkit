import SwiftUI

// MARK: - EmptyStateView

/// Premium empty state with decorative accent-tinted circle, Lucide icon, title, and subtitle.
/// Reusable across all views that need to display an empty or no-results state.
///
/// Usage:
/// ```swift
/// EmptyStateView(
///     icon: .train,
///     title: "No departures",
///     subtitle: "There are no upcoming departures from this stop."
/// )
/// ```
struct EmptyStateView: View {
    let icon: LucideIcon
    let title: String
    let subtitle: String

    var body: some View {
        VStack(spacing: 16) {
            ZStack {
                // Subtle radial glow behind the circle
                Circle()
                    .fill(
                        RadialGradient(
                            colors: [AppTheme.accent.opacity(0.10), .clear],
                            center: .center,
                            startRadius: 0,
                            endRadius: 60
                        )
                    )
                    .frame(width: 120, height: 120)

                // Main accent-tinted circle
                Circle()
                    .fill(AppTheme.accent.opacity(0.08))
                    .frame(width: 80, height: 80)

                // Icon
                icon.image
                    .font(.system(.title, weight: .light))
                    .foregroundStyle(AppTheme.accent)
            }

            Text(title)
                .font(.headline)
                .foregroundStyle(AppTheme.textPrimary)

            Text(subtitle)
                .font(.subheadline)
                .foregroundStyle(AppTheme.textTertiary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 40)
        }
        .frame(maxWidth: .infinity)
        .padding(.vertical, 40)
        .accessibilityElement(children: .combine)
    }
}

// MARK: - Convenience Initializers

extension EmptyStateView {
    /// Empty state for no departures found.
    static func noDepartures(
        title: String = String(localized: "empty_no_departures_title"),
        subtitle: String = String(localized: "empty_no_departures_subtitle")
    ) -> EmptyStateView {
        EmptyStateView(icon: .clock, title: title, subtitle: subtitle)
    }

    /// Empty state for no stops found (search or nearby).
    static func noStops(
        title: String = String(localized: "empty_no_stops_title"),
        subtitle: String = String(localized: "empty_no_stops_subtitle")
    ) -> EmptyStateView {
        EmptyStateView(icon: .mapPinOff, title: title, subtitle: subtitle)
    }

    /// Empty state for no lines found.
    static func noLines(
        title: String = String(localized: "empty_no_lines_title"),
        subtitle: String = String(localized: "empty_no_lines_subtitle")
    ) -> EmptyStateView {
        EmptyStateView(icon: .list, title: title, subtitle: subtitle)
    }

    /// Empty state for search with no results.
    static func noResults(
        title: String = String(localized: "empty_no_results_title"),
        subtitle: String = String(localized: "empty_no_results_subtitle")
    ) -> EmptyStateView {
        EmptyStateView(icon: .search, title: title, subtitle: subtitle)
    }
}
