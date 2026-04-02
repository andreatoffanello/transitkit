import SwiftUI

// MARK: - App Entry Point

@main
struct TransitKitApp: App {
    @State private var store: ScheduleStore?
    @State private var favoritesManager: FavoritesManager?
    @State private var searchHistoryStore: SearchHistoryStore?
    @State private var locationManager = LocationManager()
    @State private var operatorConfig: OperatorConfig?
    @State private var loadingConfig: OperatorConfig?
    @State private var configError: String?

    var body: some Scene {
        WindowGroup {
            Group {
                if let store, let favoritesManager, let searchHistoryStore, let operatorConfig {
                    ContentView(config: operatorConfig)
                        .environment(store)
                        .environment(favoritesManager)
                        .environment(searchHistoryStore)
                        .environment(locationManager)
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
            loadingConfig = config
            let scheduleStore = ScheduleStore(operatorId: config.id)
            scheduleStore.configure(with: config)
            favoritesManager = FavoritesManager(operatorId: config.id)
            searchHistoryStore = SearchHistoryStore(operatorId: config.id)
            await scheduleStore.load()
            store = scheduleStore
            operatorConfig = config
        } catch {
            configError = error.localizedDescription
        }
    }

    // MARK: - Loading View

    private var operatorInitials: String {
        guard let name = loadingConfig?.name else { return "" }
        let words = name.split(separator: " ").prefix(2)
        return words.compactMap { $0.first }.map(String.init).joined()
    }

    private var loadingView: some View {
        VStack(spacing: 20) {
            if let config = loadingConfig {
                // Avatar circle with operator initials
                ZStack {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 64, height: 64)
                    Text(operatorInitials)
                        .font(.system(size: 22, weight: .bold))
                        .foregroundStyle(.white)
                }
                // Operator name
                Text(config.name)
                    .font(.title2.bold())
                    .foregroundStyle(AppTheme.textPrimary)
            }
            // Subtle loading indicator
            ProgressView()
                .tint(AppTheme.accent)
            if loadingConfig == nil {
                Text(String(localized: "powered_by_transitkit"))
                    .font(.system(.caption, weight: .medium))
                    .foregroundStyle(AppTheme.textTertiary)
            }
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(AppTheme.background.ignoresSafeArea())
    }

    // MARK: - Error View

    private func errorView(message: String) -> some View {
        VStack(spacing: 20) {
            LucideIcon.alertTriangle.sized(40)
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
