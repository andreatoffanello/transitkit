import SwiftUI

// MARK: - App Entry Point

@main
struct TransitKitApp: App {
    @State private var store: ScheduleStore?
    @State private var favoritesManager: FavoritesManager?
    @State private var operatorConfig: OperatorConfig?
    @State private var configError: String?

    var body: some Scene {
        WindowGroup {
            Group {
                if let store, let favoritesManager, let operatorConfig {
                    ContentView(config: operatorConfig)
                        .environment(store)
                        .environment(favoritesManager)
                        .tint(AppTheme.accent)
                } else if let configError {
                    errorView(message: configError)
                } else {
                    loadingView
                }
            }
            .task { await bootstrap() }
        }
    }

    // MARK: - Bootstrap

    private func bootstrap() async {
        do {
            let config = try ConfigLoader.load()
            AppTheme.configure(from: config)
            operatorConfig = config
            let scheduleStore = ScheduleStore(operatorId: config.id)
            store = scheduleStore
            favoritesManager = FavoritesManager(operatorId: config.id)
            await scheduleStore.load()
        } catch {
            configError = error.localizedDescription
        }
    }

    // MARK: - Loading View

    private var loadingView: some View {
        VStack(spacing: 16) {
            ProgressView()
                .controlSize(.large)
                .tint(AppTheme.accent)
            Text(String(localized: "powered_by_transitkit"))
                .font(.system(.caption, weight: .medium))
                .foregroundStyle(AppTheme.textTertiary)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.background.ignoresSafeArea())
    }

    // MARK: - Error View

    private func errorView(message: String) -> some View {
        VStack(spacing: 20) {
            LucideIcon.alertTriangle.image
                .font(.system(size: 40))
                .foregroundStyle(AppTheme.realtimeRed)

            Text(String(localized: "error_loading"))
                .font(.system(.headline))
                .foregroundStyle(AppTheme.textPrimary)

            Text(message)
                .font(.system(.subheadline))
                .foregroundStyle(AppTheme.textSecondary)
                .multilineTextAlignment(.center)
                .padding(.horizontal, 32)

            Button {
                configError = nil
                Task { await bootstrap() }
            } label: {
                Text(String(localized: "error_retry"))
                    .font(.system(.body, weight: .semibold))
                    .padding(.horizontal, 24)
                    .padding(.vertical, 12)
            }
            .buttonStyle(.borderedProminent)
            .tint(AppTheme.accent)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.background.ignoresSafeArea())
    }
}
