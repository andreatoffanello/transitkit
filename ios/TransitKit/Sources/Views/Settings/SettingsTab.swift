import SwiftUI

// MARK: - SettingsTab

struct SettingsTab: View {
    @Environment(ScheduleStore.self) private var store
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(LocationManager.self) private var locationManager
    @Environment(PushNotificationManager.self) private var pushManager
    @Environment(DeepLinkRouter.self) private var router
    @Environment(\.dismiss) private var dismiss
    @Environment(\.operatorConfig) private var config

    @AppStorage("notificationsEnabled") private var notificationsEnabled = false
    @State private var notificationsBusy = false
    /// Statistiche d'uso anonime (solo operatori in `UsageStats.allowlist`).
    @State private var usageStatsOn = UsageStats.isEnabled

    @AppStorage(DeveloperMode.storageKey) private var developerModeEnabled = false
    /// Tap consecutivi sulla riga versione. Finestra mobile di 2s (vedi
    /// `registerVersionTap`) perché un tap distratto oggi e uno domani non
    /// devono sommarsi fino a sbloccare la modalità da soli.
    @State private var versionTapCount = 0
    @State private var lastVersionTap: Date?
    @State private var developerHint: String?

    /// Letto nel body (riga "Lingua"): la dipendenza da `language` fa
    /// ri-renderizzare Impostazioni non appena l'utente cambia lingua nella
    /// picker, senza uscire dalla schermata.
    private let localization = LocalizationManager.shared

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
                ScrollViewReader { proxy in
                LazyVStack(alignment: .leading, spacing: 24) {
                    if let config {
                        operatorCard(config: config)
                            .padding(.top, 4)

                        // MARK: Favorites
                        if config.features.enableFavorites {
                            section(title: L("settings_section_favorites")) {
                                GlassCard(cornerRadius: 16) {
                                    NavigationLink {
                                        FavoritesListView(onSelectStop: { stop in openStop(stop) })
                                    } label: {
                                        settingsRow(
                                            icon: .star,
                                            iconColor: .yellow,
                                            title: L("settings_favorites"),
                                            detail: favoritesManager.favoriteStopIds.isEmpty
                                                ? L("settings_favorites_none")
                                                : String(format: L("settings_favorites_count", comment: ""), favoritesManager.favoriteStopIds.count),
                                            tappable: true
                                        )
                                    }
                                    .buttonStyle(.plain)
                                    .hapticNavigationLink()
                                }
                            }
                        }

                        // MARK: Notifications
                        if config.features.enableNotifications {
                            section(title: L("settings_section_notifications")) {
                                GlassCard(cornerRadius: 16) {
                                    notificationsRow
                                }
                            }
                            .id("notifications")
                        }

                        // MARK: Language
                        section(title: L("settings_section_language")) {
                            GlassCard(cornerRadius: 16) {
                                NavigationLink {
                                    LanguagePickerView()
                                } label: {
                                    settingsRow(
                                        icon: .globe,
                                        iconColor: AppTheme.accentReadable,
                                        title: L("settings_section_language"),
                                        detail: localization.language.endonym,
                                        tappable: true
                                    )
                                }
                                .buttonStyle(.plain)
                                .hapticNavigationLink()
                                .accessibilityIdentifier("settings_language_row")
                            }
                        }

                        // MARK: Privacy (Location)
                        section(title: L("settings_location_section")) {
                            GlassCard(cornerRadius: 16) {
                                locationRow
                            }
                        }

                        // MARK: Usage statistics (solo operatori in allowlist)
                        if UsageStats.isAvailable {
                            section(title: L("settings_stats_section")) {
                                GlassCard(cornerRadius: 16) {
                                    usageStatsRow
                                }
                            }
                        }

                        // MARK: About
                        section(title: L("settings_section_about")) {
                            GlassCard(cornerRadius: 16) {
                                VStack(spacing: 0) {
                                    if let url = URL(string: config.url) {
                                        Link(destination: url) {
                                            settingsRow(
                                                icon: .globe,
                                                iconColor: AppTheme.accentReadable,
                                                title: L("about_operator_website"),
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
                                                iconColor: AppTheme.accentReadable,
                                                title: L("about_privacy_policy"),
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
                                        title: L("settings_version"),
                                        detail: developerHint,
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
                                    // Il contatore sta sul call site e non nel
                                    // builder: `settingsRow` resta una riga di
                                    // Impostazioni, non un gesto nascosto.
                                    .onTapGesture { registerVersionTap() }
                                    .accessibilityIdentifier("settings_version_row")
                                }
                            }
                        }

                        // MARK: Developer
                        // Sezione interna: le stringhe sono in inglese
                        // hardcoded di proposito, unica parte dell'app che non
                        // passa da `L()` — vedi `DeveloperView`.
                        if developerModeEnabled {
                            section(title: "Developer") {
                                GlassCard(cornerRadius: 16) {
                                    NavigationLink {
                                        DeveloperView()
                                    } label: {
                                        settingsRow(
                                            icon: .settings,
                                            iconColor: AppTheme.textSecondary,
                                            title: "Developer",
                                            detail: "Build info, feature flags, cache",
                                            tappable: true
                                        )
                                    }
                                    .buttonStyle(.plain)
                                    .hapticNavigationLink()
                                    .accessibilityIdentifier("settings_developer_row")
                                }
                            }
                        }

                        // MARK: Disclaimer
                        // Nessun titolo di sezione: il disclaimer è una nota a
                        // piè di pagina, non una sezione. Averne uno produceva due
                        // intestazioni consecutive identiche ("About"/"About" in
                        // inglese, "Info"/"Informazioni" in italiano), perché la
                        // card qui sopra è già la sezione informativa. Android lo
                        // rende come testo semplice — questa è la parità.
                        //
                        // Args: %1$@ = app name (AppalRider), %2$@ = operator name (AppalCART).
                        // Reso: "AppalRider is not developed or managed by AppalCART.
                        // Timetable data is officially provided by AppalCART."
                        Text(String(format: L("settings_disclaimer_body"), appDisplayName, config.name))
                            .font(.system(size: 13))
                            .foregroundStyle(AppTheme.textSecondary)
                            .lineSpacing(2)
                            .fixedSize(horizontal: false, vertical: true)
                            .padding(.horizontal, 4)

                        Spacer(minLength: 40)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 4)
                .onAppear {
                    // Consume the deeplink anchor set by `transitkit://settings/<anchor>`.
                    // The anchor key matches the `.id(...)` on the target section
                    // (currently "notifications" — extend as new anchors are added).
                    guard let anchor = router.pendingSettingsAnchor else { return }
                    router.pendingSettingsAnchor = nil
                    Task { @MainActor in
                        try? await Task.sleep(for: .milliseconds(80))
                        withAnimation(.snappy(duration: 0.35)) {
                            proxy.scrollTo(anchor, anchor: .top)
                        }
                    }
                }
                }
            }
            .background(AppTheme.background.ignoresSafeArea())
            // Stessa ragione del TabView in ContentView: `L()` produce String,
            // SwiftUI non sa che il testo è cambiato e ridisegna solo i rami
            // che leggono `localization.language` (la riga Lingua sì, i titoli
            // di sezione no). L'id sta sulla ScrollView e non sul
            // NavigationStack: il contenuto si ricostruisce, la picker pushata
            // resta dov'è.
            .id(localization.language)
            .navigationTitle(L("tab_settings"))
            .navigationBarTitleDisplayMode(.large)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    SheetCloseButton {
                        dismiss()
                    }
                    .accessibilityIdentifier("btn_settings_close")
                }
            }
        }
        .onAppear {
            Task { await pushManager.refreshAuthorizationStatus() }
        }
    }

    // MARK: - Open a stop from Favorites

    /// Il dettaglio fermata esiste solo nello stack di Orari, e Impostazioni è
    /// un `fullScreenCover` sopra il TabView: prima si chiude il cover, poi si
    /// consegna la fermata al router. L'hop di runloop serve a non far
    /// coincidere la dismissal del cover con il cambio di tab + push che
    /// `ContentView`/`OrariTab` fanno appena `pendingStop` cambia — altrimenti
    /// la push avviene dietro al cover e l'utente si ritrova sul dettaglio
    /// senza transizione.
    private func openStop(_ stop: ResolvedStop) {
        dismiss()
        Task { @MainActor in
            try? await Task.sleep(for: .milliseconds(320))
            router.pendingStop = stop
        }
    }

    // MARK: - Notifications binding

    private var notificationsBinding: Binding<Bool> {
        Binding(
            get: {
                notificationsEnabled && pushManager.authorizationStatus == .authorized
            },
            set: { newValue in
                MainActor.assumeIsolated {
                    newValue ? Haptics.toggleOn() : Haptics.toggleOff()
                }
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
            L("settings_notifications_denied")
        case .authorized where notificationsEnabled:
            L("settings_notifications_active")
        default:
            L("settings_notifications_footer")
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
                .fill(AppTheme.accentReadable.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(LucideIcon.bell.sized(16).foregroundStyle(AppTheme.accentReadable))

            VStack(alignment: .leading, spacing: 2) {
                Text(L("settings_notifications"))
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

    // MARK: - Usage Stats Row

    private var usageStatsRow: some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(AppTheme.accentReadable.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(LucideIcon.shield.sized(16).foregroundStyle(AppTheme.accentReadable))

            VStack(alignment: .leading, spacing: 2) {
                Text(L("settings_stats_title"))
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                Text(L("settings_stats_desc"))
                    .font(.caption)
                    .foregroundStyle(AppTheme.textSecondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 12)

            Toggle("", isOn: $usageStatsOn)
                .labelsHidden()
                .tint(AppTheme.accent)
                .accessibilityIdentifier("settings_usage_stats_toggle")
                .onChange(of: usageStatsOn) { _, on in
                    Haptics.selection()
                    UsageStats.setEnabled(on)
                }
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
                .fill(AppTheme.accentReadable.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(LucideIcon.mapPin.sized(16).foregroundStyle(AppTheme.accentReadable))

            VStack(alignment: .leading, spacing: 2) {
                Text(L("settings_location_title"))
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
            return L("settings_location_active_subtitle")
        case .denied, .restricted:
            return L("nearby_denied_subtitle")
        case .notDetermined:
            return L("nearby_enable_subtitle")
        @unknown default:
            return ""
        }
    }

    @ViewBuilder
    private var locationActionButton: some View {
        switch locationManager.authorizationStatus {
        case .notDetermined:
            Button(L("settings_location_enable")) {
                Haptics.commit()
                locationManager.requestPermissionAndStart()
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.accent)
            .accessibilityIdentifier("settings_location_enable_button")
        case .denied, .restricted:
            // `.bordered` prende il tint di sistema: blu Apple fuori brand e
            // sotto contrasto in dark. Il trattamento secondario condiviso
            // porta la label a bianco.
            Button {
                Haptics.commit()
                if let url = URL(string: UIApplication.openSettingsURLString) {
                    UIApplication.shared.open(url)
                }
            } label: {
                Text(L("settings_location_open"))
                    .font(.subheadline.weight(.semibold))
                    .secondaryButtonSurface(cornerRadius: 10, horizontalPadding: 12, verticalPadding: 8)
            }
            .buttonStyle(.plain)
            .accessibilityIdentifier("settings_location_open_button")
        default:
            LucideIcon.check.sized(18)
                .foregroundStyle(AppTheme.accentReadable)
                .accessibilityIdentifier("settings_location_granted")
        }
    }

    // MARK: - Developer mode unlock

    /// 7 tap sulla riga versione sbloccano la modalità sviluppatore. Dal 4° in
    /// poi la riga dice quanti ne mancano: senza feedback il gesto è
    /// indistinguibile da "non succede niente" e nessuno arriva a sette.
    /// Ri-tappare a modalità già attiva non la spegne — quel comando vive
    /// dentro la sezione Developer, dove è visibile.
    private func registerVersionTap() {
        guard !developerModeEnabled else { return }

        let now = Date()
        if let lastVersionTap, now.timeIntervalSince(lastVersionTap) > 2 {
            versionTapCount = 0
        }
        lastVersionTap = now
        versionTapCount += 1

        let remaining = 7 - versionTapCount
        if remaining <= 0 {
            versionTapCount = 0
            developerModeEnabled = true
            Haptics.commit()
            withAnimation(.snappy(duration: 0.25)) {
                developerHint = "Developer mode enabled"
            }
            Task { @MainActor in
                try? await Task.sleep(for: .seconds(2))
                withAnimation(.snappy(duration: 0.25)) { developerHint = nil }
            }
        } else if remaining <= 3 {
            Haptics.tick()
            withAnimation(.snappy(duration: 0.2)) {
                // Singolare esplicito: all'ultimo tap "1 taps away" si legge rotto.
                developerHint = remaining == 1 ? "1 more tap…" : "\(remaining) more taps…"
            }
        } else {
            developerHint = nil
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
