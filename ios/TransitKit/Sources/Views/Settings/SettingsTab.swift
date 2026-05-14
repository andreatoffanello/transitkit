import SwiftUI

// MARK: - SettingsTab

struct SettingsTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(LocationManager.self) private var locationManager

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
            ScrollView {
                LazyVStack(spacing: 20) {
                    if let config {
                        operatorCard(config: config)

                        // MARK: Favorites
                        if config.features.enableFavorites {
                            sectionHeader(String(localized: "settings_section_favorites"))
                            GlassCard(cornerRadius: 16) {
                                NavigationLink {
                                    FavoritesListView()
                                } label: {
                                    settingsRow(
                                        icon: .star,
                                        iconColor: .yellow,
                                        title: String(localized: "settings_favorites"),
                                        detail: favoritesManager.favoriteStopIds.isEmpty
                                            ? String(localized: "settings_favorites_none")
                                            : String(format: NSLocalizedString("settings_favorites_count", comment: ""), favoritesManager.favoriteStopIds.count),
                                        tappable: true
                                    )
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        // MARK: Notifications
                        if config.features.enableNotifications {
                            sectionHeader(String(localized: "settings_section_notifications"))
                            GlassCard(cornerRadius: 16) {
                                Toggle(isOn: $notificationsEnabled) {
                                    settingsRow(
                                        icon: .bell,
                                        iconColor: AppTheme.accent,
                                        title: String(localized: "settings_notifications"),
                                        detail: String(localized: "settings_notifications_footer"),
                                        tappable: false
                                    )
                                }
                                .tint(AppTheme.accent)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 4)
                            }
                        }

                        // MARK: Language
                        sectionHeader(String(localized: "settings_section_language"))
                        GlassCard(cornerRadius: 16) {
                            settingsRow(
                                icon: .globe,
                                iconColor: AppTheme.accent,
                                title: String(localized: "settings_language"),
                                tappable: false,
                                trailing: {
                                    AnyView(
                                        Text(currentLanguageLabel)
                                            .font(.subheadline)
                                            .foregroundStyle(AppTheme.textTertiary)
                                    )
                                }
                            )
                        }

                        // MARK: Privacy (Location)
                        sectionHeader(String(localized: "settings_location_section"))
                        GlassCard(cornerRadius: 16) {
                            locationRow
                        }

                        // MARK: About
                        sectionHeader(String(localized: "settings_section_about"))
                        GlassCard(cornerRadius: 16) {
                            VStack(spacing: 0) {
                                NavigationLink {
                                    AboutView(config: config)
                                } label: {
                                    settingsRow(
                                        icon: .info,
                                        iconColor: AppTheme.textSecondary,
                                        title: String(localized: "settings_about"),
                                        tappable: true
                                    )
                                }
                                .buttonStyle(.plain)

                                Rectangle()
                                    .fill(AppTheme.separatorLine)
                                    .frame(height: 0.5)
                                    .padding(.leading, 56)

                                settingsRow(
                                    icon: .shield,
                                    iconColor: AppTheme.textTertiary,
                                    title: String(localized: "settings_version"),
                                    tappable: false,
                                    trailing: {
                                        AnyView(
                                            Text(appVersion)
                                                .font(.subheadline)
                                                .foregroundStyle(AppTheme.textTertiary)
                                        )
                                    }
                                )
                            }
                        }

                        // MARK: Disclaimer
                        sectionHeader(String(localized: "settings_info_section"))
                        GlassCard(cornerRadius: 16) {
                            Text(String(format: String(localized: "settings_disclaimer_body"), config.name, config.fullName))
                                .font(.system(size: 13))
                                .foregroundStyle(AppTheme.textSecondary)
                                .padding(.horizontal, 16)
                                .padding(.vertical, 14)
                        }

                        Spacer(minLength: 40)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 8)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_settings"))
            .navigationBarTitleDisplayMode(.large)
        }
    }

    // MARK: - Operator Brand Card

    private func operatorCard(config: OperatorConfig) -> some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                ZStack {
                    LinearGradient(
                        colors: [Color(hex: "06845C"), Color(hex: "165F9C")],
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
        }
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

    @ViewBuilder
    private func settingsRow(
        icon: LucideIcon,
        iconColor: Color,
        title: String,
        detail: String? = nil,
        tappable: Bool,
        trailing: (() -> AnyView)? = nil
    ) -> some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(iconColor.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(icon.sized(16).foregroundStyle(iconColor))

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

            Spacer(minLength: 0)

            if let trailing {
                trailing()
            } else if tappable {
                LucideIcon.chevronRight.sized(16)
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }

    // MARK: - Location Row

    private var locationRow: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(AppTheme.accent.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(LucideIcon.mapPin.sized(16).foregroundStyle(AppTheme.accent))

            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "settings_location_title"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(locationStatusDescription)
                    .font(.caption)
                    .foregroundStyle(AppTheme.textSecondary)
            }

            Spacer(minLength: 0)

            locationActionButton
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
    }

    private var locationStatusDescription: String {
        switch locationManager.authorizationStatus {
        case .authorizedWhenInUse, .authorizedAlways:
            return String(localized: "nearby_enable_subtitle")
        case .denied, .restricted:
            return String(localized: "nearby_denied_subtitle")
        case .notDetermined:
            return String(localized: "nearby_enable_subtitle")
        @unknown default:
            return ""
        }
    }

    @ViewBuilder
    private var locationActionButton: some View {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            Button(String(localized: "settings_location_enable")) {
                locationManager.requestPermissionAndStart()
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.accent)
            .accessibilityIdentifier("settings_location_enable_button")
        case .denied, .restricted:
            Button(String(localized: "settings_location_open")) {
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            }
            .buttonStyle(.bordered)
            .accessibilityIdentifier("settings_location_open_button")
        default:
            LucideIcon.check.sized(18)
                .foregroundStyle(AppTheme.accent)
                .accessibilityIdentifier("settings_location_granted")
        }
    }

    // MARK: - Section Header

    private func sectionHeader(_ title: String) -> some View {
        HStack {
            Text(title.uppercased())
                .font(.caption.weight(.semibold))
                .kerning(0.6)
                .foregroundStyle(AppTheme.textTertiary)
            Spacer()
        }
        .padding(.horizontal, 4)
        .padding(.top, 4)
    }

    // MARK: - Language Label

    private var currentLanguageLabel: String {
        let preferred = Locale.preferredLanguages.first ?? "en"
        let locale = Locale(identifier: preferred)
        return locale.localizedString(forLanguageCode: preferred)?.capitalized ?? preferred
    }
}
