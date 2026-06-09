import SwiftUI

/// Root tab bar container. Five tabs: Home, Orari, Linee, Mappa, Avvisi.
/// Avvisi carries a live `.badge(activeAlerts.count)` of active service alerts.
/// Servizi has moved off the tab bar — reachable from the operator card in Home.
/// Settings accessible as sheet from Home. Planner accessible from Home quick-block.
/// Each tab view manages its own NavigationStack internally.
struct ContentView: View {
    let config: OperatorConfig
    @Environment(ScheduleStore.self) private var store
    @Environment(VehicleStore.self) private var vehicleStore
    @Environment(AlertStore.self) private var alertStore
    @Environment(FavoritesManager.self) private var favoritesManager
    @Environment(DeepLinkRouter.self) private var router
    @Environment(\.scenePhase) private var scenePhase
    @State private var selectedTab = 0
    @State private var toastPresenter = AlertToastPresenter()
    @State private var toastDetailAlert: GtfsRtAlert?
    #if DEBUG
    @State private var showShaderPlayground = false
    #endif
    @State private var deeplinkAlert: GtfsRtAlert?

    /// Dimensione icone tab bar — gli asset Lucide sono 24pt intrinseci,
    /// leggermente ridotti per proporzione con le label.
    private let tabIconPt: CGFloat = 22

    private func tabIcon(_ icon: LucideIcon) -> Image {
        Image(uiImage: icon.uiImage(pt: tabIconPt))
    }

    var body: some View {
        TabView(selection: $selectedTab) {
            // MARK: Tab 0 — Home
            HomeTab(selectedTab: $selectedTab)
                .tabItem {
                    Label {
                        Text(String(localized: "tab_home"))
                    } icon: {
                        tabIcon(.home)
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
                        tabIcon(.clock)
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
                        tabIcon(.route)
                    }
                }
                .tag(2)
                .accessibilityIdentifier("tab_lines")

            // MARK: Tab 3 — Mappa
            // NavigationStack is REQUIRED here so MappaTab's
            // `.navigationDestination(item: $navigationDestinationStop)` can
            // actually push StopDetailView. Without the wrap the "Open stop"
            // CTA on the preview card flipped the binding silently and SwiftUI
            // had no stack to push onto — the preview just dismissed.
            NavigationStack {
                MappaTab(config: config)
            }
                .tabItem {
                    Label {
                        Text(String(localized: "tab_map"))
                    } icon: {
                        tabIcon(.map)
                    }
                }
                .tag(3)
                .accessibilityIdentifier("tab_map")

            // MARK: Tab 4 — Avvisi
            NavigationStack {
                AlertListView()
            }
                .tabItem {
                    Label {
                        Text(String(localized: "tab_alerts"))
                    } icon: {
                        tabIcon(.bell)
                    }
                }
                .badge(alertStore.activeAlerts.count)
                .tag(4)
                .accessibilityIdentifier("tab_alerts")
        }
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
        .onChange(of: router.pendingMapPreviewRouteId) { _, rid in
            if rid != nil { selectedTab = 3 }
        }
        .onChange(of: router.pendingMapOpen) { _, id in
            // Only switch tabs here — MappaTab consumes `pendingMapOpen` and
            // wipes its own selection state. If we wiped to nil here too, the
            // `.onChange(of:)` inside MappaTab could miss the trigger because
            // SwiftUI may collapse the UUID→nil transition into a single tick.
            if id != nil { selectedTab = 3 }
        }
        .onChange(of: router.pendingTabSwitch) { _, switchReq in
            guard let switchReq else { return }
            selectedTab = switchReq.index
            router.pendingTabSwitch = nil
        }
        .onChange(of: router.pendingAlertId) { _, alertId in
            guard let alertId,
                  let alert = alertStore.allAlerts.first(where: { $0.id == alertId })
            else { return }
            router.pendingAlertId = nil
            deeplinkAlert = alert
        }
        .onAppear {
            if router.pendingMapPreviewStop != nil { selectedTab = 3 }
            if router.pendingMapPreviewVehicleId != nil { selectedTab = 3 }
        }
        .onChange(of: scenePhase) { _, phase in
            // Pause GTFS-RT polling when the app is not active to avoid
            // burning battery on background HTTPS requests every 15/60s.
            // Restart immediately on .active so the user sees fresh data.
            switch phase {
            case .active:
                vehicleStore.startPolling()
                alertStore.startPolling()
            case .inactive, .background:
                vehicleStore.stopPolling()
                alertStore.stopPolling()
            @unknown default:
                break
            }
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
        .fullScreenCover(item: $toastDetailAlert) { alert in
            NavigationStack {
                AlertDetailView(alert: alert)
            }
        }
        .fullScreenCover(item: $deeplinkAlert) { alert in
            NavigationStack {
                AlertDetailView(alert: alert)
            }
        }
        #if DEBUG
        .onChange(of: router.showShaderPlayground) { _, requested in
            if requested {
                showShaderPlayground = true
                router.showShaderPlayground = false
            }
        }
        .fullScreenCover(isPresented: $showShaderPlayground) {
            ShaderPlaygroundView()
        }
        #endif
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
