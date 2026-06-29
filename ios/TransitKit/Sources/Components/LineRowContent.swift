import SwiftUI

/// Glass card row for a single transit line — the canonical line row used by
/// both the Lines tab (`LinesListView`) and the map line/stop picker
/// (`LinePickerSheet`). Single source of truth so the two search surfaces stay
/// visually identical.
///
/// Shows the colored `LineBadge`, long name, pre-cached stop-sequence marquee,
/// optional transit-type + direction subtitle, and the live-vehicle pill.
/// The whole card is the tap target — no trailing chevron (redundant on a
/// full-width tappable card).
struct LineRowContent: View {
    let route: APIRoute
    let hasMultipleTypes: Bool
    let store: ScheduleStore
    let liveCount: Int

    private var resolvedTransitType: TransitType { route.resolvedTransitType }

    var body: some View {
        HStack(spacing: 12) {
            // Line badge
            LineBadge(
                name: route.name,
                color: route.color ?? "#000000",
                textColor: route.textColor,
                transitType: resolvedTransitType,
                size: .large
            )

            VStack(alignment: .leading, spacing: 2) {
                // APIRoute long name
                Text(route.longName ?? route.name)
                    .font(.subheadline.weight(.semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)

                // Stop sequence marquee — reads from pre-cached store dictionary (O(1))
                if let sequence = store.routeStopSequences[route.id], !sequence.isEmpty {
                    Text(sequence)
                        .font(.system(size: 11))
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }

                // Subtitle: transit type (only when multiple types) + direction count (only when >1)
                let showTransitType = hasMultipleTypes
                let showDirections = route.directions.count > 1
                if showTransitType || showDirections {
                    HStack(spacing: 6) {
                        if showTransitType {
                            resolvedTransitType.icon.sized(10)
                                .foregroundStyle(AppTheme.textTertiary)
                            Text(resolvedTransitType.displayName)
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }

                        if showDirections {
                            if showTransitType {
                                Text("·")
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.textTertiary)
                            }
                            Text("↔ \(route.directions.count)")
                                .font(.caption2.weight(.medium))
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    }
                }
            }

            Spacer(minLength: 8)

            // Live vehicle count badge — chip statico senza animazione/material:
            // dentro LazyVStack l'animazione pulsante di LiveBadge combinata col
            // ri-render del vehicleStore ogni 15s creava layout instability.
            if liveCount > 0 {
                HStack(spacing: 4) {
                    Circle()
                        .fill(AppTheme.realtimeGreen)
                        .frame(width: 6, height: 6)
                    Text("\(liveCount) live")
                        .font(.system(size: 11, weight: .semibold))
                        .foregroundStyle(AppTheme.realtimeGreen)
                }
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(AppTheme.realtimeGreen.opacity(0.12), in: Capsule())
                .accessibilityIdentifier("line_live_badge_\(route.id)")
            }
        }
        .padding(.vertical, 12)
        .padding(.horizontal, 12)
        .background(AppTheme.glassFill, in: RoundedRectangle(cornerRadius: 12))
        .overlay(
            RoundedRectangle(cornerRadius: 12)
                .stroke(AppTheme.glassBorder, lineWidth: 1)
        )
        .contentShape(Rectangle())
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(String(format: NSLocalizedString("line_badge_a11y", comment: ""), route.name))
        .accessibilityHint(String(localized: "a11y_hint_show_line_stops"))
        .accessibilityAddTraits(.isButton)
    }
}
