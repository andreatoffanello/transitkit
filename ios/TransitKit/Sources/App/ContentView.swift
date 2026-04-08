import SwiftUI

/// Root tab bar container. Five tabs: Home, Orari, Mappa, Info, Settings.
/// Each tab view manages its own NavigationStack internally.
struct ContentView: View {
    let config: OperatorConfig
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(DeepLinkRouter.self) private var router
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
        .environment(\.vehiclePositionsUrl, config.gtfsRt?.vehiclePositionsUrl)
        .onChange(of: router.pendingRoute) { _, route in
            if route != nil { selectedTab = 1 }
        }
        .onChange(of: router.pendingStop) { _, stop in
            if stop != nil { selectedTab = 1 }
        }
        .onChange(of: router.pendingTrip) { _, trip in
            if trip != nil { selectedTab = 1 }
        }
        .toolbarBackground(.ultraThinMaterial, for: .tabBar)
        .modifier(TabBarVisibilityModifier())
        .modifier(TabBarAppearanceModifier())
        .tint(.primary)
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
/// Selected item uses neutral .label color — accent is reserved for buttons/links only.
private struct TabBarAppearanceModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithDefaultBackground()
            appearance.shadowColor = UIColor.separator

            // Neutral chrome: selected item uses .label, inactive uses .secondaryLabel
            let item = UITabBarItemAppearance()
            item.selected.iconColor = .label
            item.selected.titleTextAttributes = [.foregroundColor: UIColor.label]
            item.normal.iconColor = .secondaryLabel
            item.normal.titleTextAttributes = [.foregroundColor: UIColor.secondaryLabel]
            appearance.stackedLayoutAppearance = item
            appearance.inlineLayoutAppearance = item
            appearance.compactInlineLayoutAppearance = item

            UITabBar.appearance().scrollEdgeAppearance = appearance
            UITabBar.appearance().standardAppearance = appearance
            UITabBar.appearance().tintColor = .label

            // Nav bar back button and toolbar items use neutral color
            UINavigationBar.appearance().tintColor = .label
        }
    }
}
