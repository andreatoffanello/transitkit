import SwiftUI

// MARK: - SettingsTab

/// Main settings tab with favorites, notifications, language, and about sections.
/// Content visibility is driven by `OperatorConfig.features`.
struct SettingsTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager

    @AppStorage("notificationsEnabled") private var notificationsEnabled = false

    private var config: OperatorConfig? {
        try? ConfigLoader.load()
    }

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        NavigationStack {
            List {
                if let config {
                    // MARK: Operator Brand Card
                    Section {
                        operatorCard(config: config)
                    }
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)

                    // MARK: Favorites
                    if config.features.enableFavorites {
                        Section {
                            NavigationLink {
                                FavoritesListView()
                            } label: {
                                settingsRow(
                                    icon: .star,
                                    iconColor: .yellow,
                                    title: String(localized: "settings_favorites"),
                                    detail: favoritesManager.favoriteStopIds.isEmpty
                                        ? String(localized: "settings_favorites_none")
                                        : String(format: NSLocalizedString("settings_favorites_count", comment: ""), favoritesManager.favoriteStopIds.count)
                                )
                            }
                        } header: {
                            Text(String(localized: "settings_section_favorites"))
                        }
                    }

                    // MARK: Notifications
                    if config.features.enableNotifications {
                        Section {
                            Toggle(isOn: $notificationsEnabled) {
                                settingsRow(
                                    icon: .bell,
                                    iconColor: AppTheme.accent,
                                    title: String(localized: "settings_notifications"),
                                    detail: nil
                                )
                            }
                            .tint(AppTheme.accent)
                        } header: {
                            Text(String(localized: "settings_section_notifications"))
                        } footer: {
                            Text(String(localized: "settings_notifications_footer"))
                        }
                    }

                    // MARK: Language
                    Section {
                        settingsRow(
                            icon: .globe,
                            iconColor: AppTheme.accent,
                            title: String(localized: "settings_language"),
                            detail: currentLanguageLabel
                        )
                    } header: {
                        Text(String(localized: "settings_section_language"))
                    } footer: {
                        Text(String(localized: "settings_language_footer"))
                    }

                    // MARK: About
                    Section {
                        NavigationLink {
                            AboutView(config: config)
                        } label: {
                            settingsRow(
                                icon: .info,
                                iconColor: AppTheme.textSecondary,
                                title: String(localized: "settings_about"),
                                detail: nil
                            )
                        }

                        HStack {
                            settingsRow(
                                icon: .shield,
                                iconColor: AppTheme.textTertiary,
                                title: String(localized: "settings_version"),
                                detail: nil
                            )
                            Spacer()
                            Text(appVersion)
                                .font(.subheadline)
                                .foregroundStyle(AppTheme.textTertiary)
                        }
                    } header: {
                        Text(String(localized: "settings_section_about"))
                    }
                }
            }
            .listStyle(.insetGrouped)
            .scrollContentBackground(.hidden)
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_settings"))
            .navigationBarTitleDisplayMode(.large)
        }
    }

    // MARK: - Operator Brand Card

    private func operatorCard(config: OperatorConfig) -> some View {
        HStack(spacing: 12) {
            ZStack {
                LinearGradient(
                    colors: [
                        Color(hex: "06845C"),
                        Color(hex: "165F9C")
                    ],
                    startPoint: .topLeading,
                    endPoint: .bottomTrailing
                )
                Text(initials(for: config.name))
                    .font(.system(size: 16, weight: .bold))
                    .foregroundColor(.white)
            }
            .frame(width: 48, height: 48)
            .clipShape(RoundedRectangle(cornerRadius: 12))

            VStack(alignment: .leading, spacing: 2) {
                Text(config.name)
                    .font(.headline)
                    .fontWeight(.bold)
                    .foregroundStyle(AppTheme.textPrimary)
                Text(config.region)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }

            Spacer()
        }
        .padding(16)
        .background(Color(.secondarySystemGroupedBackground))
        .clipShape(RoundedRectangle(cornerRadius: 16))
        .padding(.horizontal, 16)
        .padding(.vertical, 8)
    }

    private func initials(for name: String) -> String {
        let words = name.split(separator: " ").filter { !$0.isEmpty }
        switch words.count {
        case 0: return "?"
        case 1: return String(words[0].prefix(2)).uppercased()
        default: return "\(words[0].prefix(1))\(words[1].prefix(1))".uppercased()
        }
    }

    // MARK: - Settings Row

    private func settingsRow(
        icon: LucideIcon,
        iconColor: Color,
        title: String,
        detail: String?
    ) -> some View {
        HStack(spacing: 12) {
            RoundedRectangle(cornerRadius: 8)
                .fill(iconColor.opacity(0.12))
                .frame(width: 32, height: 32)
                .overlay(
                    icon.sized(14)
                        .foregroundStyle(iconColor)
                )
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                if let detail {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                }
            }
        }
    }

    // MARK: - Language Label

    private var currentLanguageLabel: String {
        let preferred = Locale.preferredLanguages.first ?? "en"
        let locale = Locale(identifier: preferred)
        return locale.localizedString(forLanguageCode: preferred)?.capitalized ?? preferred
    }
}
