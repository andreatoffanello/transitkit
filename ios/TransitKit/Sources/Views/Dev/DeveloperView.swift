import SwiftUI
import UIKit

// MARK: - Developer mode

/// Flag e utility della modalità sviluppatore.
///
/// Vive fuori dalla view perché `TransitKitApp` (launch argument
/// `--reset-schedule-cache`) e la riga "Clear schedule cache" devono cancellare
/// **la stessa** directory: duplicare il path significa che al primo rename uno
/// dei due smette di cancellare davvero e nessuno se ne accorge.
enum DeveloperMode {
    /// Stessa chiave dell'`@AppStorage` in `SettingsTab`/`DeveloperView`.
    /// Serve anche fuori da una view (routing dei deep link in `TransitKitApp`).
    static let storageKey = "developerModeEnabled"

    static var isEnabled: Bool { UserDefaults.standard.bool(forKey: storageKey) }

    /// Directory di cache su disco dello `ScheduleLoader`.
    static var scheduleCacheDirectory: URL {
        FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
            .appendingPathComponent("TransitKit", isDirectory: true)
    }

    static func clearScheduleCache() {
        try? FileManager.default.removeItem(at: scheduleCacheDirectory)
    }
}

// MARK: - DeveloperView

/// Strumenti interni, sbloccati da 7 tap sulla riga versione in Impostazioni.
///
/// **Le stringhe di questa schermata sono hardcoded in inglese di proposito** e
/// non passano da `L()`, unica eccezione in tutta l'app: la vede solo chi
/// sviluppa o fa QA, e tradurla vorrebbe dire mantenere chiavi in tre lingue per
/// testo che nessun utente legge. Per lo stesso motivo non ha
/// `.id(localization.language)`: non c'è niente da ridisegnare al cambio lingua.
struct DeveloperView: View {
    @Environment(\.operatorConfig) private var config
    @Environment(ScheduleStore.self) private var store
    @Environment(PushNotificationManager.self) private var pushManager
    @Environment(\.dismiss) private var dismiss

    @AppStorage(DeveloperMode.storageKey) private var developerModeEnabled = false

    @State private var showShaderPlayground = false
    @State private var showOnboarding = false
    @State private var clearingCache = false
    @State private var tokenCopied = false

    private var appVersion: String {
        let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "1.0"
        let build = Bundle.main.infoDictionary?["CFBundleVersion"] as? String ?? "1"
        return "\(version) (\(build))"
    }

    var body: some View {
        ScrollView {
            LazyVStack(alignment: .leading, spacing: 24) {
                buildSection
                flagsSection
                endpointsSection
                pushSection
                toolsSection
                disableSection

                Spacer(minLength: 40)
            }
            .padding(.horizontal, 16)
            .padding(.top, 4)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .navigationTitle("Developer")
        .navigationBarTitleDisplayMode(.large)
        // Playground e onboarding sono presentati da qui e non via
        // `transitkit://`: Impostazioni è un fullScreenCover sopra il TabView,
        // quindi una presentazione instradata dal router finirebbe *sotto*
        // questa schermata e sembrerebbe non aver fatto nulla.
        .fullScreenCover(isPresented: $showShaderPlayground) {
            ShaderPlaygroundView()
        }
        .fullScreenCover(isPresented: $showOnboarding) {
            OnboardingStoriesView()
        }
    }

    // MARK: - Sections

    private var buildSection: some View {
        section(title: "Build") {
            GlassCard(cornerRadius: 16) {
                VStack(spacing: 0) {
                    infoRow("Version", appVersion)
                    rowSeparator
                    infoRow("Operator", config?.id)
                    rowSeparator
                    infoRow("Brand", config?.brandName ?? config?.name)
                    rowSeparator
                    infoRow("Timezone", config?.timezone)
                }
            }
        }
    }

    private var flagsSection: some View {
        section(title: "Feature flags") {
            GlassCard(cornerRadius: 16) {
                VStack(spacing: 0) {
                    if let features = config?.features {
                        flagRow("enableMap", features.enableMap)
                        rowSeparator
                        flagRow("enableGeolocation", features.enableGeolocation)
                        rowSeparator
                        flagRow("enableFavorites", features.enableFavorites)
                        rowSeparator
                        flagRow("enableNotifications", features.enableNotifications)
                    } else {
                        infoRow("config", "not loaded")
                    }
                }
            }
        }
    }

    private var endpointsSection: some View {
        section(title: "Endpoints") {
            GlassCard(cornerRadius: 16) {
                VStack(spacing: 0) {
                    infoRow("CDN", config?.cdnUrl)
                    rowSeparator
                    infoRow("Routing", config?.routingEndpoint)
                    rowSeparator
                    infoRow("Console", config?.consoleApiUrl)
                    rowSeparator
                    infoRow("RT vehicles", config?.gtfsRt?.vehiclePositionsUrl)
                    rowSeparator
                    infoRow("RT trips", config?.gtfsRt?.tripUpdatesUrl)
                    rowSeparator
                    infoRow("RT alerts", config?.gtfsRt?.serviceAlertsUrl)
                }
            }
        }
    }

    private var pushSection: some View {
        section(title: "Push") {
            GlassCard(cornerRadius: 16) {
                Button {
                    guard let token = pushManager.fcmToken else { return }
                    Haptics.commit()
                    UIPasteboard.general.string = token
                    tokenCopied = true
                } label: {
                    actionRow(
                        icon: .bell,
                        title: "FCM token",
                        detail: fcmTokenDetail,
                        tappable: pushManager.fcmToken != nil
                    )
                }
                .buttonStyle(.plain)
                .disabled(pushManager.fcmToken == nil)
                .accessibilityIdentifier("dev_fcm_token_row")
            }
        }
    }

    private var toolsSection: some View {
        section(title: "Tools") {
            GlassCard(cornerRadius: 16) {
                VStack(spacing: 0) {
                    Button {
                        Haptics.commit()
                        clearScheduleCache()
                    } label: {
                        actionRow(
                            icon: .refreshCw,
                            title: "Clear schedule cache",
                            detail: clearingCache ? "Reloading from CDN…" : "Wipes disk cache and reloads",
                            tappable: true
                        )
                    }
                    .buttonStyle(.plain)
                    .disabled(clearingCache)
                    .accessibilityIdentifier("dev_clear_cache_row")

                    rowSeparator

                    Button {
                        Haptics.navigate()
                        showShaderPlayground = true
                    } label: {
                        actionRow(
                            icon: .eye,
                            title: "Shader playground",
                            detail: "mapGlowEffect, full screen",
                            tappable: true
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("dev_shader_row")

                    rowSeparator

                    Button {
                        Haptics.navigate()
                        showOnboarding = true
                    } label: {
                        actionRow(
                            icon: .repeat2,
                            title: "Replay onboarding",
                            detail: "Same flow as transitkit://onboarding",
                            tappable: true
                        )
                    }
                    .buttonStyle(.plain)
                    .accessibilityIdentifier("dev_onboarding_row")
                }
            }
        }
    }

    private var disableSection: some View {
        section(title: "Developer mode") {
            GlassCard(cornerRadius: 16) {
                Button {
                    Haptics.toggleOff()
                    developerModeEnabled = false
                    dismiss()
                } label: {
                    actionRow(
                        icon: .circleX,
                        title: "Disable developer mode",
                        detail: "Tap the version row 7 times to get back in",
                        tint: AppTheme.realtimeRed,
                        tappable: true
                    )
                }
                .buttonStyle(.plain)
                .accessibilityIdentifier("dev_disable_row")
            }
        }
    }

    // MARK: - Actions

    private var fcmTokenDetail: String {
        guard let token = pushManager.fcmToken else {
            return pushManager.firebaseConfigured
                ? "No token yet — notifications not authorized"
                : "Firebase not configured in this build"
        }
        return tokenCopied ? "Copied to clipboard" : "\(token.prefix(12))… tap to copy"
    }

    private func clearScheduleCache() {
        clearingCache = true
        DeveloperMode.clearScheduleCache()
        Task { @MainActor in
            await store.refresh()
            clearingCache = false
        }
    }

    // MARK: - Rows

    /// Chiave/valore. Il valore è monospaced perché quasi sempre è un URL o un
    /// id da confrontare a occhio con quello del config.
    private func infoRow(_ key: String, _ value: String?) -> some View {
        HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(key)
                .font(.system(size: 13, weight: .medium))
                .foregroundStyle(AppTheme.textSecondary)

            Spacer(minLength: 12)

            Text(value ?? "—")
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(value == nil ? AppTheme.textTertiary : AppTheme.textPrimary)
                .multilineTextAlignment(.trailing)
                .textSelection(.enabled)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    private func flagRow(_ key: String, _ value: Bool) -> some View {
        HStack(spacing: 12) {
            Text(key)
                .font(.system(size: 12, design: .monospaced))
                .foregroundStyle(AppTheme.textSecondary)

            Spacer(minLength: 12)

            Text(value ? "true" : "false")
                .font(.system(size: 12, weight: .semibold, design: .monospaced))
                .foregroundStyle(value ? AppTheme.accentReadable : AppTheme.textTertiary)
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 12)
    }

    /// Stessa anatomia di `settingsRow` in Impostazioni (tile 40×40 + titolo +
    /// dettaglio): la modalità sviluppatore vive dentro Impostazioni e non deve
    /// sembrare un'altra app.
    private func actionRow(
        icon: LucideIcon,
        title: String,
        detail: String?,
        tint: Color = AppTheme.textSecondary,
        tappable: Bool
    ) -> some View {
        HStack(spacing: 14) {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(tint.opacity(0.12))
                .frame(width: 40, height: 40)
                .overlay(icon.sized(16).foregroundStyle(tint))

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(AppTheme.textPrimary)
                if let detail {
                    Text(detail)
                        .font(.caption)
                        .foregroundStyle(AppTheme.textSecondary)
                        .lineLimit(2)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }

            Spacer(minLength: 0)

            if tappable {
                LucideIcon.chevronRight.sized(16)
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .padding(.horizontal, 16)
        .padding(.vertical, 14)
        .contentShape(Rectangle())
    }

    private var rowSeparator: some View {
        Rectangle()
            .fill(AppTheme.separatorLine)
            .frame(height: 0.5)
            .padding(.leading, 16)
    }

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
