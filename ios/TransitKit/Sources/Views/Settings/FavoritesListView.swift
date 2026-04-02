import SwiftUI

// MARK: - FavoritesListView

/// List of favorite stops with swipe-to-delete, tap-to-navigate,
/// and an empty state when no favorites are saved.
struct FavoritesListView: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager

    @State private var selectedStopId: String?

    /// Resolved favorite stops from the schedule store.
    private var favoriteStops: [ResolvedStop] {
        favoritesManager.favoriteStopIds.compactMap { stopId in
            store.stops.first { $0.id == stopId }
        }
    }

    var body: some View {
        Group {
            if favoriteStops.isEmpty {
                emptyState
            } else {
                List {
                    ForEach(favoriteStops) { stop in
                        Button {
                            selectedStopId = stop.id
                        } label: {
                            favoriteRow(stop)
                        }
                        .buttonStyle(.plain)
                        .swipeActions(edge: .trailing, allowsFullSwipe: true) {
                            Button(role: .destructive) {
                                withAnimation {
                                    favoritesManager.remove(stop.id)
                                }
                            } label: {
                                Label {
                                    Text(String(localized: "action_remove"))
                                } icon: {
                                    LucideIcon.starOff.image
                                }
                            }
                            .tint(.red)
                        }
                        .listRowBackground(Color.clear)
                        .listRowSeparator(.hidden)
                        .listRowInsets(EdgeInsets(top: 4, leading: 16, bottom: 4, trailing: 16))
                    }
                }
                .listStyle(.plain)
                .scrollContentBackground(.hidden)
            }
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle(String(localized: "nav_title_favorites"))
        .navigationBarTitleDisplayMode(.large)
    }

    // MARK: - Favorite Row

    private func favoriteRow(_ stop: ResolvedStop) -> some View {
        GlassCard(cornerRadius: 14) {
            HStack(spacing: 12) {
                // Transit type icon
                ZStack {
                    RoundedRectangle(cornerRadius: 10)
                        .fill(AppTheme.accent.opacity(0.12))
                    if let firstType = stop.transitTypes.first {
                        firstType.icon.image
                            .font(.body.weight(.semibold))
                            .foregroundStyle(AppTheme.accent)
                    } else {
                        LucideIcon.bus.image
                            .font(.body.weight(.semibold))
                            .foregroundStyle(AppTheme.accent)
                    }
                }
                .frame(width: 40, height: 40)

                VStack(alignment: .leading, spacing: 3) {
                    Text(stop.name)
                        .font(.subheadline.weight(.semibold))
                        .foregroundStyle(AppTheme.textPrimary)
                        .lineLimit(1)

                    // Line names preview
                    if !stop.lineNames.isEmpty {
                        Text(stop.lineNames.prefix(5).joined(separator: " \u{00B7} "))
                            .font(.caption)
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineLimit(1)
                    }
                }

                Spacer()

                LucideIcon.chevronRight.image
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(AppTheme.textTertiary)
            }
            .padding(14)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(stop.name)
        .accessibilityHint(String(localized: "a11y_hint_show_stop"))
        .accessibilityAddTraits(.isButton)
    }

    // MARK: - Empty State

    private var emptyState: some View {
        VStack(spacing: 16) {
            Spacer()
            LucideIcon.star.sized(48)
                .foregroundStyle(AppTheme.textTertiary)
            Text(String(localized: "favorites_empty_title"))
                .font(.title3.weight(.semibold))
                .foregroundStyle(AppTheme.textPrimary)
            Text(String(localized: "favorites_empty_subtitle"))
                .font(.subheadline)
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
            Spacer()
        }
        .padding(32)
    }
}
