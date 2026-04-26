import SwiftUI

/// Root tab bar container. Five tabs: Home, Orari, Linee, Mappa, Servizi.
/// Settings is accessible as a sheet from Home.
/// Each tab view manages its own NavigationStack internally.
struct ContentView: View {
    let config: OperatorConfig
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(AlertStore.self) private var alertStore
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(DeepLinkRouter.self) private var router
    @State private var selectedTab = 0
    @State private var toastPresenter = AlertToastPresenter()
    @State private var toastDetailAlert: GtfsRtAlert?
    @State private var showShaderPlayground = false

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

            // MARK: Tab 1 — Orari (Stops)
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

            // MARK: Tab 2 — Linee
            LineeTab()
                .tabItem {
                    Label {
                        Text(String(localized: "tab_lines"))
                    } icon: {
                        LucideIcon.route.image
                    }
                }
                .tag(2)
                .accessibilityIdentifier("tab_lines")

            // MARK: Tab 3 — Mappa
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
            .tag(3)
            .accessibilityIdentifier("tab_map")

            // MARK: Tab 4 — Planner
            PlannerTab()
                .tabItem {
                    Label {
                        Text(String(localized: "tab_planner"))
                    } icon: {
                        LucideIcon.navigation.image
                    }
                }
                .tag(4)
                .accessibilityIdentifier("tab_planner")

            // MARK: Tab 5 — Servizi
            ServiziTab(config: config)
                .tabItem {
                    Label {
                        Text(String(localized: "tab_services"))
                    } icon: {
                        LucideIcon.info.image
                    }
                }
                .tag(5)
                .accessibilityIdentifier("tab_services")
        }
        .environment(\.vehiclePositionsUrl, config.gtfsRt?.vehiclePositionsUrl)
        .onChange(of: router.pendingRoute) { _, route in
            if route != nil { selectedTab = 2 }
        }
        .onChange(of: router.pendingStop) { _, stop in
            if stop != nil { selectedTab = 1 }
        }
        .onChange(of: router.pendingTrip) { _, trip in
            if trip != nil { selectedTab = 1 }
        }
        .onChange(of: router.pendingMapPreviewStop) { _, stop in
            if stop != nil { selectedTab = 3 }
        }
        .onChange(of: router.pendingMapPreviewVehicleId) { _, vid in
            if vid != nil { selectedTab = 3 }
        }
        .onChange(of: router.pendingMapOpen) { _, id in
            if id != nil {
                selectedTab = 3
                router.pendingMapOpen = nil
            }
        }
        .onAppear {
            if router.pendingMapPreviewStop != nil { selectedTab = 3 }
            if router.pendingMapPreviewVehicleId != nil { selectedTab = 3 }
        }
        .toolbarBackground(.ultraThinMaterial, for: .tabBar)
        .modifier(TabBarVisibilityModifier())
        .modifier(TabBarAppearanceModifier())
        .tint(.primary)
        .onChange(of: selectedTab) { _, _ in
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
        }
        .onChange(of: alertStore.activeAlerts.map(\.id)) { _, _ in
            evaluateNewAlerts()
        }
        .overlay(alignment: .top) {
            if let alert = toastPresenter.pendingAlert {
                AlertToastView(
                    alert: alert,
                    onTap: {
                        toastDetailAlert = alert
                        toastPresenter.dismiss()
                    },
                    onDismiss: { toastPresenter.dismiss() }
                )
                .padding(.horizontal, 12)
                .padding(.top, 8)
                .transition(.move(edge: .top).combined(with: .opacity))
                .animation(.spring(response: 0.35, dampingFraction: 0.85), value: toastPresenter.pendingAlert?.id)
            }
        }
        .sheet(item: $toastDetailAlert) { alert in
            NavigationStack {
                AlertDetailView(alert: alert)
            }
        }
        .onChange(of: router.showShaderPlayground) { _, requested in
            if requested {
                showShaderPlayground = true
                router.showShaderPlayground = false
            }
        }
        .fullScreenCover(isPresented: $showShaderPlayground) {
            ShaderPlaygroundView()
        }
    }

    /// On every alert feed refresh, enqueue a toast for the first newly-active
    /// alert that touches a favorite stop. Dedup is handled by the presenter.
    private func evaluateNewAlerts() {
        let previouslyActive = alertStore.previouslyActiveIds
        let favorites = Set(favoritesManager.favoriteStopIds)
        guard !favorites.isEmpty else { return }
        let newAlerts = alertStore.activeAlerts.filter { !previouslyActive.contains($0.id) }
        toastPresenter.consider(alerts: newAlerts, favoriteStopIds: favorites)
    }
}

// MARK: - Tab Bar Glass Visibility (iOS 18+)

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

private struct TabBarAppearanceModifier: ViewModifier {
    func body(content: Content) -> some View {
        content.onAppear {
            let appearance = UITabBarAppearance()
            appearance.configureWithDefaultBackground()
            appearance.shadowColor = UIColor.separator

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

            UINavigationBar.appearance().tintColor = .label
        }
    }
}
