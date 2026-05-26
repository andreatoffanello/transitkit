import SwiftUI

// MARK: - SettingsTab

struct SettingsTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(LocationManager.self) private var locationManager
    @Environment(PushNotificationManager.self) private var pushManager
    @Environment(\.dismiss) private var dismiss
    @Environment(\.operatorConfig) private var config

    @AppStorage("notificationsEnabled") private var notificationsEnabled = false
    @State private var notificationsBusy = false

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    /// Display name dell'app (AppalRider), localizzato via InfoPlist.xcstrings.
    /// Diverso da `config.name` (AppalCART) che è il nome dell'operatore di
    /// cui mostriamo i dati.
    private var appDisplayName: String {
        (Bundle.main.localizedInfoDictionary?["CFBundleDisplayName"] as? String)
            ?? (Bundle.main.infoDictionary?["CFBundleDisplayName"] as? String)
            ?? "AppalRider"
    }

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 24) {
                    if let config {
                        operatorCard(config: config)
                            .padding(.top, 4)

                        // MARK: Favorites
                        if config.features.enableFavorites {
                            section(title: String(localized: "settings_section_favorites")) {
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
                        }

                        // MARK: Notifications
                        if config.features.enableNotifications {
                            section(title: String(localized: "settings_section_notifications")) {
                                GlassCard(cornerRadius: 16) {
                                    notificationsRow
                                }
                            }
                        }

                        // MARK: Privacy (Location)
                        section(title: String(localized: "settings_location_section")) {
                            GlassCard(cornerRadius: 16) {
                                locationRow
                            }
                        }

                        // MARK: About
                        section(title: String(localized: "settings_section_about")) {
                            GlassCard(cornerRadius: 16) {
                                VStack(spacing: 0) {
                                    if let url = URL(string: config.url) {
                                        Link(destination: url) {
                                            settingsRow(
                                                icon: .globe,
                                                iconColor: AppTheme.accent,
                                                title: String(localized: "about_operator_website"),
                                                detail: config.name,
                                                tappable: false,
                                                trailing: {
                                                    AnyView(
                                                        LucideIcon.externalLink.sized(14)
                                                            .foregroundStyle(AppTheme.textTertiary)
                                                    )
                                                }
                                            )
                                        }
                                        .buttonStyle(.plain)

                                        rowSeparator
                                    }

                                    if let privacyUrl = config.privacyUrl,
                                       let url = URL(string: privacyUrl) {
                                        Link(destination: url) {
                                            settingsRow(
                                                icon: .shield,
                                                iconColor: AppTheme.accent,
                                                title: String(localized: "about_privacy_policy"),
                                                tappable: false,
                                                trailing: {
                                                    AnyView(
                                                        LucideIcon.externalLink.sized(14)
                                                            .foregroundStyle(AppTheme.textTertiary)
                                                    )
                                                }
                                            )
                                        }
                                        .buttonStyle(.plain)

                                        rowSeparator
                                    }

                                    settingsRow(
                                        icon: .info,
                                        iconColor: AppTheme.textTertiary,
                                        title: String(localized: "settings_version"),
                                        tappable: false,
                                        trailing: {
                                            AnyView(
                                                Text(appVersion)
                                                    .font(.subheadline)
                                                    .foregroundStyle(AppTheme.textTertiary)
                                                    .monospacedDigit()
                                            )
                                        }
                                    )
                                }
                            }
                        }

                        // MARK: Disclaimer
                        section(title: String(localized: "settings_info_section")) {
                            // Args: %1$@ = app name (AppalRider), %2$@ = operator name (AppalCART).
                            // Reso: "AppalRider is not developed or managed by AppalCART.
                            // Timetable data is officially provided by AppalCART."
                            Text(String(format: String(localized: "settings_disclaimer_body"), appDisplayName, config.name))
                                .font(.system(size: 13))
                                .foregroundStyle(AppTheme.textSecondary)
                                .lineSpacing(2)
                                .fixedSize(horizontal: false, vertical: true)
                                .padding(.horizontal, 4)
                        }

                        Spacer(minLength: 40)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
            }
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(String(localized: "tab_settings"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        UIImpactFeedbackGenerator(style: .light).impactOccurred()
                        dismiss()
                    } label: {
                        LucideIcon.x.sized(18)
                            .foregroundStyle(AppTheme.textSecondary)
                            .frame(width: 32, height: 32)
                            .background(
                                Circle().fill(AppTheme.bgSecondary)
                            )
                    }
                    .accessibilityIdentifier("btn_settings_close")
                    .accessibilityLabel(String(localized: "action_close"))
                }
            }
        }
        .onAppear {
            Task { await pushManager.refreshAuthorizationStatus() }
        }
    }

    // MARK: - Notifications binding

    private var notificationsBinding: Binding<Bool> {
        Binding(
            get: {
                notificationsEnabled && pushManager.authorizationStatus == .authorized
            },
            set: { newValue in
                notificationsBusy = true
                Task {
                    defer { Task { @MainActor in notificationsBusy = false } }
                    if newValue {
                        let granted = await pushManager.requestAuthorization()
                        notificationsEnabled = granted
                    } else {
                        await pushManager.disableAndUnsubscribe()
                        notificationsEnabled = false
                    }
                }
            }
        )
    }

    private var notificationsDetailText: String {
        switch pushManager.authorizationStatus {
        case .denied:
            String(localized: "settings_notifications_denied")
        case .authorized where notificationsEnabled:
            String(localized: "settings_notifications_active")
        default:
            String(localized: "settings_notifications_footer")
        }
    }

    // MARK: - Operator Brand Card

    /// Header card della pagina impostazioni: mostra il brand AppalCART (logo
    /// operatore = logo app, è una build white-label). Tappabile per richiamo
    /// di info aggiuntive sull'operatore.
    private func operatorCard(config: OperatorConfig) -> some View {
        GlassCard(cornerRadius: 16) {
            HStack(spacing: 14) {
                Group {
                    if UIImage(named: "OperatorLogo") != nil {
                        Image("OperatorLogo")
                            .resizable()
                            .scaledToFit()
                    } else {
                        ZStack {
                            LinearGradient(
                                colors: [AppTheme.accent, Color(hex: config.theme.primaryColor)],
                                startPoint: .topLeading,
                                endPoint: .bottomTrailing
                            )
                            LucideIcon.bus.sized(22)
                                .foregroundStyle(.white)
                        }
                    }
                }
                .frame(width: 48, height: 48)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                VStack(alignment: .leading, spacing: 2) {
                    // Brand dell'APP (AppalRider), non dell'operatore — fonte:
                    // CFBundleDisplayName localizzato in InfoPlist.xcstrings.
                    Text(appDisplayName)
                        .font(.system(size: 17, weight: .bold))
                        .foregroundStyle(AppTheme.textPrimary)
                    if !config.region.isEmpty {
                        Text(config.region)
                            .font(.system(size: 13))
                            .foregroundStyle(AppTheme.textSecondary)
                    }
                }

                Spacer(minLength: 0)
            }
            .padding(16)
        }
    }

    // MARK: - Row separator

    /// Divider tra righe dentro una stessa card. Allineato alla colonna del
    /// testo (icona 40 + spacing 14 + padding card 16 = 70pt leading).
    private var rowSeparator: some View {
        Rectangle()
            .fill(AppTheme.separatorLine)
            .frame(height: 0.5)
            .padding(.leading, 70)
    }

    // MARK: - Notifications Row

    /// Riga dedicata per il toggle notifiche: stesso pattern di `settingsRow`
    /// (icona 40×40 + titolo/sottotitolo) con il Toggle in coda, ma senza il
    /// doppio padding che il wrapping in Toggle introduceva nella vecchia
    /// implementazione.
    private var notificationsRow: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(AppTheme.accent.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(LucideIcon.bell.sized(16).foregroundStyle(AppTheme.accent))

            VStack(alignment: .leading, spacing: 2) {
                Text(String(localized: "settings_notifications"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(notificationsDetailText)
                    .font(.caption)
                    .foregroundStyle(AppTheme.textSecondary)
                    .lineLimit(2)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 12)

            Toggle("", isOn: notificationsBinding)
                .labelsHidden()
                .tint(AppTheme.accent)
                .disabled(notificationsBusy || !pushManager.firebaseConfigured)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
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

    // MARK: - Section wrapper

    /// Gruppo titolo+contenuto coerente con la Home: sentence case 15sp
    /// SemiBold textPrimary, leading 2 per restare flush con il padding
    /// orizzontale delle card.
    @ViewBuilder
    private func section<Content: View>(
        title: String,
        @ViewBuilder content: () -> Content
    ) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(.system(size: 15, weight: .semibold))
                .foregroundStyle(AppTheme.textPrimary)
                .padding(.leading, 2)
            content()
        }
    }

}
