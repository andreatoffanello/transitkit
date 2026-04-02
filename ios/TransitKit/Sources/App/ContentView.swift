import SwiftUI

/// Root tab bar container. Five tabs: Home, Orari, Mappa, Info, Settings.
/// Each tab view manages its own NavigationStack internally.
struct ContentView: View {
    let config: OperatorConfig
    @Environment(ScheduleStore.self) private var store
    @State private var selectedTab = 0

    var body: some View {
        TabView(selection: $selectedTab) {
            // MARK: Tab 0 — Home
            HomeTab(selectedTab: $selectedTab)
                .tabItem {
                    Label {
                        Text(String(localized: "tab_home"))
                    } icon: {
                        LucideIcon.home.image
                    }
                }
                .tag(0)
                .accessibilityIdentifier("tab_home")

            // MARK: Tab 1 — Orari (Schedules)
            OrariTab()
                .tabItem {
                    Label {
                        Text(String(localized: "tab_schedules"))
                    } icon: {
                        LucideIcon.clock.image
                    }
                }
                .tag(1)
                .accessibilityIdentifier("tab_schedules")

            // MARK: Tab 2 — Mappa (Map)
            NavigationStack {
                MappaTab(config: config)
            }
            .tabItem {
                Label {
                    Text(String(localized: "tab_map"))
                } icon: {
                    LucideIcon.map.image
                }
            }
            .tag(2)
            .accessibilityIdentifier("tab_map")

            // MARK: Tab 3 — Info
            InfoTab()
                .tabItem {
                    Label {
                        Text(String(localized: "tab_info"))
                    } icon: {
                        LucideIcon.info.image
                    }
                }
                .tag(3)
                .accessibilityIdentifier("tab_info")

            // MARK: Tab 4 — Settings
            SettingsTab()
                .tabItem {
                    Label {
                        Text(String(localized: "tab_settings"))
                    } icon: {
                        LucideIcon.settings.image
                    }
                }
                .tag(4)
                .accessibilityIdentifier("tab_settings")
        }
        .toolbarBackground(.ultraThinMaterial, for: .tabBar)
        .modifier(TabBarVisibilityModifier())
        .modifier(TabBarAppearanceModifier())
        .tint(AppTheme.accent)
        .onChange(of: selectedTab) { _, _ in
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
    }
}

// MARK: - Tab Bar Glass Visibility (iOS 18+)

/// Forces the tab bar background to always be visible.
/// On iOS 18+ uses `toolbarBackgroundVisibility(.visible)`;
/// on older versions the `.toolbarBackground(.ultraThinMaterial)` alone is sufficient.
private struct TabBarVisibilityModifier: ViewModifier {
    func body(content: Content) -> some View {
        if #available(iOS 18.0, *) {
            content.toolbarBackgroundVisibility(.visible, for: .tabBar)
        } else {
            content
        }
    }
}

// MARK: - Tab Bar Appearance

/// Configures the native tab bar appearance with a subtle top separator.
private struct TabBarAppearanceModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithDefaultBackground()
            appearance.shadowColor = UIColor.separator
            UITabBar.appearance().scrollEdgeAppearance = appearance
            UITabBar.appearance().standardAppearance = appearance
        }
    }
}
