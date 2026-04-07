# Real-Time Vehicle Positions — UX Enhancement Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Surface real-time vehicle positions throughout the app — not just in the buried LineMapView — so a user waiting at a stop can immediately see whether their bus is approaching, and the map itself renders vehicles correctly at all zoom levels with a proper bearing indicator.

**Architecture:** A new `VehicleStore` replaces the per-view GTFS-RT polling so all views share a single live feed. Components read from it via `@Environment`. The `LineMapView` is upgraded with zoom-aware rendering tiers and a redesigned vehicle pin that keeps the route badge upright while only rotating the bearing needle.

**Tech Stack:** SwiftUI, MapKit (iOS 17+ `Map`/`MapCameraPosition`), `@Observable`, GTFS-RT protobuf (custom binary decoder already in `GtfsRtDecoder.swift`).

---

## File Map

| Action | File | Responsibility |
|--------|------|----------------|
| **Create** | `Sources/Stores/VehicleStore.swift` | Global poll loop, indexed vehicle lookup |
| **Create** | `Sources/Components/LiveBadge.swift` | Reusable pulsing green dot + count label |
| **Modify** | `Sources/Models/Departure.swift` | Add resolved `tripId: String?` field |
| **Modify** | `Sources/Stores/ScheduleStore.swift` | Populate `tripId` on `Departure` at build time |
| **Modify** | `Sources/App/TransitKitApp.swift` | Create & inject `VehicleStore` |
| **Modify** | `Sources/App/ContentView.swift` | `.environment(vehicleStore)` |
| **Modify** | `Sources/Components/DepartureRow.swift` | Live dot when trip is tracked |
| **Modify** | `Sources/Views/Orari/LineDetailView.swift` | "N live" chip in header |
| **Modify** | `Sources/Views/Home/HomeTab.swift` | Live dot on nearby-stop departure rows |
| **Modify** | `Sources/Views/Mappa/LineMapView.swift` | Zoom tiers + fixed bearing pin; remove per-view polling |

---

## Task 1 — `Departure.tripId`: resolve trip ID string at build time

Currently `Departure.tripIdIndex: Int?` points into `ScheduleData.tripIds[]` but no view can read the actual string without access to `ScheduleData`. We need the resolved string on the model so `VehicleStore` can do O(1) lookups.

**Files:**
- Modify: `ios/TransitKit/Sources/Models/Departure.swift`
- Modify: `ios/TransitKit/Sources/Stores/ScheduleStore.swift`

- [ ] **Add `tripId` field to `Departure`**

In `Departure.swift`, add one line after `let tripIdIndex: Int?`:

```swift
let tripId: String?       // resolved from tripIds[tripIdIndex]; nil if no real-time data
```

- [ ] **Populate `tripId` in `ScheduleStore.departures(forStopId:)`**

In `ScheduleStore.swift`, inside the `departures(forStopId:)` method, find the block that builds `Departure(...)`. Change:

```swift
// Before
let dep = Departure(
    id: "\(time)_\(lineName)_\(headsign)_\(dock)",
    time: time,
    lineName: lineName,
    routeId: routeId,
    headsign: headsign,
    color: route?.color ?? "#000000",
    textColor: route?.textColor ?? "#FFFFFF",
    transitType: route?.transitType ?? .bus,
    dock: dock,
    patternIndex: patternIdx,
    tripIdIndex: tripIdIdx
)
```

```swift
// After
let resolvedTripId: String? = tripIdIdx.flatMap { idx in
    idx < data.tripIds.count ? data.tripIds[idx] : nil
}
let dep = Departure(
    id: "\(time)_\(lineName)_\(headsign)_\(dock)",
    time: time,
    lineName: lineName,
    routeId: routeId,
    headsign: headsign,
    color: route?.color ?? "#000000",
    textColor: route?.textColor ?? "#FFFFFF",
    transitType: route?.transitType ?? .bus,
    dock: dock,
    patternIndex: patternIdx,
    tripIdIndex: tripIdIdx,
    tripId: resolvedTripId
)
```

- [ ] **Build — verify no compile errors**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

Expected: `Build succeeded`

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Models/Departure.swift \
        ios/TransitKit/Sources/Stores/ScheduleStore.swift
git commit -m "feat(Departure): resolve tripId string at build time"
```

---

## Task 2 — `VehicleStore`: global polling service

A single `@Observable` class polls the GTFS-RT feed every 15 seconds and exposes indexed lookups. Replaces the ad-hoc polling inside `LineMapView`.

**Files:**
- Create: `ios/TransitKit/Sources/Stores/VehicleStore.swift`

- [ ] **Create `VehicleStore.swift`**

```swift
import Foundation

// MARK: - Vehicle Store

/// Global real-time vehicle feed. Polls every 15s and provides indexed lookups.
/// Shared across all views via @Environment. Gracefully handles missing GTFS-RT URL.
@MainActor
@Observable
final class VehicleStore {
    // MARK: State
    private(set) var vehicles: [GtfsRtVehicle] = []
    private(set) var lastFetchedAt: Date? = nil

    // MARK: Private
    private let vehiclePositionsUrl: String?
    private var pollTask: Task<Void, Never>?

    /// Indexed for O(1) lookup: trip_id → vehicle
    private var vehicleByTripId: [String: GtfsRtVehicle] = [:]
    /// Indexed for O(1) lookup: route_id → vehicles
    private var vehiclesByRouteId: [String: [GtfsRtVehicle]] = [:]

    init(vehiclePositionsUrl: String?) {
        self.vehiclePositionsUrl = vehiclePositionsUrl
    }

    // MARK: - Lifecycle

    func startPolling() {
        guard vehiclePositionsUrl != nil else { return }
        pollTask?.cancel()
        pollTask = Task { [weak self] in
            while !Task.isCancelled {
                await self?.fetch()
                try? await Task.sleep(for: .seconds(15))
            }
        }
    }

    func stopPolling() {
        pollTask?.cancel()
        pollTask = nil
    }

    // MARK: - Lookups

    /// Returns the live vehicle for a specific trip ID, or nil if not tracked.
    func vehicle(forTripId tripId: String) -> GtfsRtVehicle? {
        vehicleByTripId[tripId]
    }

    /// Returns all live vehicles for a route.
    func vehicles(forRouteId routeId: String) -> [GtfsRtVehicle] {
        vehiclesByRouteId[routeId] ?? []
    }

    /// Returns the number of live vehicles for a route.
    func liveCount(forRouteId routeId: String) -> Int {
        vehiclesByRouteId[routeId]?.count ?? 0
    }

    /// True if a trip is currently tracked in the live feed.
    func isLive(tripId: String?) -> Bool {
        guard let tripId else { return false }
        return vehicleByTripId[tripId] != nil
    }

    // MARK: - Fetch

    private func fetch() async {
        guard let urlString = vehiclePositionsUrl,
              let url = URL(string: urlString) else { return }
        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let all = decodeGtfsRtVehicles(from: data)
            await MainActor.run { apply(all) }
        } catch {
            // Silently ignore — feed is optional
        }
    }

    private func apply(_ all: [GtfsRtVehicle]) {
        vehicles = all
        lastFetchedAt = Date()
        vehicleByTripId = Dictionary(uniqueKeysWithValues: all.map { ($0.tripId, $0) })
        vehiclesByRouteId = Dictionary(grouping: all, by: \.routeId)
            .filter { !$0.key.isEmpty }
    }
}
```

- [ ] **Add `VehicleStore.swift` to the Xcode project `pbxproj`**

In `ios/TransitKit.xcodeproj/project.pbxproj`, add three entries following the exact same pattern used for `DeepLinkRouter.swift` (UUIDs are arbitrary, just need to be unique hex strings):

1. In `/* Begin PBXBuildFile section */`, add:
```
		E1F2A3B4C5D6E7F8A9B0C1D2 /* VehicleStore.swift in Sources */ = {isa = PBXBuildFile; fileRef = F1E2D3C4B5A6978869504132 /* VehicleStore.swift */; };
```

2. In `/* Begin PBXFileReference section */`, add:
```
		F1E2D3C4B5A6978869504132 /* VehicleStore.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = VehicleStore.swift; sourceTree = "<group>"; };
```

3. In the `Stores` group `children` array (find it by looking for `ScheduleStore.swift` nearby), add:
```
				F1E2D3C4B5A6978869504132 /* VehicleStore.swift */,
```

4. In `PBXSourcesBuildPhase files`, add:
```
				E1F2A3B4C5D6E7F8A9B0C1D2 /* VehicleStore.swift in Sources */,
```

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

Expected: `Build succeeded`

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Stores/VehicleStore.swift \
        ios/TransitKit.xcodeproj/project.pbxproj
git commit -m "feat(VehicleStore): global GTFS-RT polling with indexed lookups"
```

---

## Task 3 — Bootstrap: inject `VehicleStore` into the environment

**Files:**
- Modify: `ios/TransitKit/Sources/App/TransitKitApp.swift`
- Modify: `ios/TransitKit/Sources/App/ContentView.swift`

- [ ] **Add `VehicleStore` state to `TransitKitApp`**

In `TransitKitApp.swift`, add after `@State private var router = DeepLinkRouter()`:

```swift
@State private var vehicleStore: VehicleStore = VehicleStore(vehiclePositionsUrl: nil)
```

- [ ] **Initialize with URL after bootstrap and start polling**

In `TransitKitApp.swift`, in `bootstrap()`, after `operatorConfig = config`, add:

```swift
vehicleStore = VehicleStore(vehiclePositionsUrl: config.gtfsRt?.vehiclePositions)
vehicleStore.startPolling()
```

- [ ] **Inject into environment in `TransitKitApp.body`**

In the `.environment(router)` chain in `body`, add:

```swift
.environment(vehicleStore)
```

The full chain should be:
```swift
ContentView(config: operatorConfig)
    .environment(store)
    .environment(favoritesManager)
    .environment(searchHistoryStore)
    .environment(locationManager)
    .environment(router)
    .environment(vehicleStore)
    .tint(AppTheme.accent)
```

- [ ] **Add `@Environment(VehicleStore.self)` to `ContentView`**

In `ContentView.swift`, after `@Environment(DeepLinkRouter.self) private var router`:

```swift
@Environment(VehicleStore.self) private var vehicleStore
```

No other changes to `ContentView` are needed for this task.

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

Expected: `Build succeeded`

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/App/TransitKitApp.swift \
        ios/TransitKit/Sources/App/ContentView.swift
git commit -m "feat: bootstrap VehicleStore and inject into environment"
```

---

## Task 4 — `LiveBadge`: reusable pulsing live indicator

**Files:**
- Create: `ios/TransitKit/Sources/Components/LiveBadge.swift`

- [ ] **Create `LiveBadge.swift`**

```swift
import SwiftUI

// MARK: - Live Badge

/// A small green pulsing dot indicating real-time tracked status.
/// Used in DepartureRow, LineDetailView header, HomeTab stop cards.
///
/// Variants:
///   LiveBadge()                    → dot only
///   LiveBadge(count: 3)            → "● 3 live"
///   LiveBadge(label: "live")       → "● live"
struct LiveBadge: View {
    enum Style {
        case dot                  // just the circle, smallest footprint
        case chip(String)         // "● text" capsule pill
    }

    let style: Style
    @State private var pulsing = false

    init() { self.style = .dot }
    init(count: Int) { self.style = .chip("\(count) live") }
    init(label: String) { self.style = .chip(label) }

    var body: some View {
        switch style {
        case .dot:
            dot
        case .chip(let text):
            HStack(spacing: 4) {
                dot
                Text(text)
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.primary)
            }
            .padding(.horizontal, 8)
            .padding(.vertical, 4)
            .background(.regularMaterial, in: Capsule())
        }
    }

    private var dot: some View {
        ZStack {
            Circle()
                .fill(Color.green.opacity(0.3))
                .frame(width: 10, height: 10)
                .scaleEffect(pulsing ? 1.6 : 1.0)
                .opacity(pulsing ? 0 : 1)

            Circle()
                .fill(Color.green)
                .frame(width: 6, height: 6)
        }
        .onAppear {
            withAnimation(.easeInOut(duration: 1.2).repeatForever(autoreverses: false)) {
                pulsing = true
            }
        }
    }
}
```

- [ ] **Add `LiveBadge.swift` to the Xcode project**

In `project.pbxproj`, add following the same pattern as Task 2. The file lives in the `Components` group (find it by searching for `DepartureRow.swift` nearby).

UUIDs:
```
Build file ref:  A2B3C4D5E6F7A8B9C0D1E2F3 /* LiveBadge.swift in Sources */
File reference:  B3C4D5E6F7A8B9C0D1E2F3A4 /* LiveBadge.swift */
```

pbxproj entries:
```
// PBXBuildFile
		A2B3C4D5E6F7A8B9C0D1E2F3 /* LiveBadge.swift in Sources */ = {isa = PBXBuildFile; fileRef = B3C4D5E6F7A8B9C0D1E2F3A4 /* LiveBadge.swift */; };

// PBXFileReference
		B3C4D5E6F7A8B9C0D1E2F3A4 /* LiveBadge.swift */ = {isa = PBXFileReference; lastKnownFileType = sourcecode.swift; path = LiveBadge.swift; sourceTree = "<group>"; };

// Add to Components group children (near DepartureRow.swift)
				B3C4D5E6F7A8B9C0D1E2F3A4 /* LiveBadge.swift */,

// Add to PBXSourcesBuildPhase
				A2B3C4D5E6F7A8B9C0D1E2F3 /* LiveBadge.swift in Sources */,
```

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Components/LiveBadge.swift \
        ios/TransitKit.xcodeproj/project.pbxproj
git commit -m "feat(LiveBadge): reusable pulsing live indicator component"
```

---

## Task 5 — `DepartureRow`: live dot when trip is tracked

**Files:**
- Modify: `ios/TransitKit/Sources/Components/DepartureRow.swift`

- [ ] **Add `VehicleStore` environment and live dot to `DepartureRow`**

Read the full `DepartureRow.swift` first, then make these two changes:

**Add environment** at the top of the struct (after existing `@Environment` lines):
```swift
@Environment(VehicleStore.self) private var vehicleStore
```

**Add live indicator to the row layout**. Find the HStack that currently shows `[LineBadge] headsign [TimeDisplay]` and add the dot between the headsign and time:

```swift
// After Text(departure.headsign) / truncation modifiers, before Spacer() + TimeDisplay:
if vehicleStore.isLive(tripId: departure.tripId) {
    LiveBadge()
}
```

The full HStack should look like:

```swift
HStack(alignment: .center, spacing: 10) {
    LineBadge(departure: departure, size: isFirst ? .big : .small)

    VStack(alignment: .leading, spacing: 1) {
        Text(departure.headsign)
            .font(isFirst ? .subheadline.weight(.semibold) : .caption.weight(.medium))
            .foregroundStyle(AppTheme.textPrimary)
            .lineLimit(1)
            .truncationMode(.tail)

        if !departure.dock.isEmpty {
            Text(departure.dock)
                .font(.system(size: 10, weight: .medium))
                .foregroundStyle(AppTheme.textTertiary)
        }
    }

    Spacer()

    if vehicleStore.isLive(tripId: departure.tripId) {
        LiveBadge()
    }

    TimeDisplay(departure: departure)
}
```

> **Note:** Read the actual current layout of `DepartureRow.swift` before editing — the exact structure may differ. The principle is: insert `LiveBadge()` to the left of `TimeDisplay`, inside the existing HStack, guarded by `vehicleStore.isLive(tripId: departure.tripId)`.

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

- [ ] **Verify visually**

```bash
xcrun simctl terminate 196EACF2-4B35-4678-89BC-0B09E031A5F0 com.transitkit. 2>/dev/null
xcrun simctl launch 196EACF2-4B35-4678-89BC-0B09E031A5F0 com.transitkit.
sleep 3
xcrun simctl openurl 196EACF2-4B35-4678-89BC-0B09E031A5F0 "transitkit://stop/appalcart_610_state_farm_rd"
sleep 4
# Take screenshot and verify green dot appears on live departures
```

Expected: green pulsing dot next to at least one departure matching a live-tracked trip.

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Components/DepartureRow.swift
git commit -m "feat(DepartureRow): live dot when trip is tracked in GTFS-RT feed"
```

---

## Task 6 — `LineDetailView`: "N live" chip in header

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/LineDetailView.swift`

- [ ] **Add `VehicleStore` to `LineDetailView`**

After `@Environment(DeepLinkRouter.self) private var router`, add:

```swift
@Environment(VehicleStore.self) private var vehicleStore
```

- [ ] **Add live count chip to `lineHeader`**

In `lineHeader`, inside the `VStack(alignment: .leading, spacing: 10)` that contains the route badge and name, add the chip right after the `HStack(spacing: 12)` block (after Spacer):

```swift
// Live vehicle chip — only shown when feed has active vehicles for this route
let liveCount = vehicleStore.liveCount(forRouteId: route.id)
if liveCount > 0 {
    Button {
        showLineMap = true
    } label: {
        LiveBadge(count: liveCount)
    }
    .accessibilityIdentifier("btn_live_chip")
}
```

Place this inside the `VStack(alignment: .leading, spacing: 10)` in `lineHeader`, after the HStack with the big badge and route name, before `.padding(.horizontal, 20)`. The chip taps directly to the map.

- [ ] **Build + verify**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"

xcrun simctl openurl 196EACF2-4B35-4678-89BC-0B09E031A5F0 "transitkit://line/3"
sleep 4
# Screenshot: verify "● 1 live" chip appears in Blue line header
```

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Views/Orari/LineDetailView.swift
git commit -m "feat(LineDetailView): live count chip in header, taps to map"
```

---

## Task 7 — `HomeTab`: live dot on nearby-stop departure rows

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Add `VehicleStore` environment**

After `@Environment(LocationManager.self) private var locationManager`, add:

```swift
@Environment(VehicleStore.self) private var vehicleStore
```

- [ ] **Add live dot to `mainStopCard` departure rows**

In `mainStopCard(_ stop:)`, find the `ForEach(departures)` HStack and add `LiveBadge()` before `TimeDisplay`:

```swift
ForEach(departures) { dep in
    HStack(spacing: 6) {
        LineBadge(departure: dep, size: .big)
        Text(dep.headsign)
            .font(.caption)
            .foregroundStyle(AppTheme.textSecondary)
            .lineLimit(1)
            .truncationMode(.tail)
        Spacer()
        if vehicleStore.isLive(tripId: dep.tripId) {
            LiveBadge()
        }
        TimelineView(.periodic(from: .now, by: 30)) { _ in
            TimeDisplay(departure: dep)
        }
    }
}
```

- [ ] **Build + verify**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Views/Home/HomeTab.swift
git commit -m "feat(HomeTab): live dot on nearby stop departure rows"
```

---

## Task 8 — `LineMapView`: migrate to `VehicleStore` + remove per-view polling

The existing `LineMapView` has its own `fetchVehicles()` / `startPolling()` / `pollTask`. Now that `VehicleStore` handles this globally, remove the duplication.

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Mappa/LineMapView.swift`

- [ ] **Replace local polling with VehicleStore**

Replace the entire content of `LineMapView` struct with this (keep `VehiclePositionsUrlKey` and extension at the bottom — they're still needed by `ContentView`):

```swift
struct LineMapView: View {
    let route: Route
    let directionId: Int

    @Environment(\.dismiss) private var dismiss
    @Environment(VehicleStore.self) private var vehicleStore

    private var lineColor: Color { Color(hex: route.color) }
    private var textColor: Color { Color(hex: contrastingTextColor(for: route.color)) }

    private var vehicles: [GtfsRtVehicle] {
        vehicleStore.vehicles(forRouteId: route.id)
    }

    // MARK: - Zoom tracking
    @State private var cameraPosition: MapCameraPosition = .automatic
    @State private var latitudeDelta: Double = 0.1   // degrees; drives rendering tier

    var body: some View {
        ZStack(alignment: .topLeading) {
            Map(position: $cameraPosition) {
                RouteOverlay(route: route, directionId: directionId)

                ForEach(vehicles) { vehicle in
                    Annotation("", coordinate: CLLocationCoordinate2D(
                        latitude: Double(vehicle.latitude),
                        longitude: Double(vehicle.longitude)
                    )) {
                        VehiclePinView(
                            routeName: route.name,
                            color: lineColor,
                            textColor: textColor,
                            bearing: vehicle.bearing,
                            zoomTier: zoomTier
                        )
                    }
                }
            }
            .mapStyle(.standard(pointsOfInterest: .excludingAll))
            .ignoresSafeArea()
            .onMapCameraChange { context in
                latitudeDelta = context.region.span.latitudeDelta
            }

            // Close button
            Button { dismiss() } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 14, weight: .semibold))
                    .foregroundStyle(.primary)
                    .frame(width: 36, height: 36)
                    .background(.regularMaterial, in: Circle())
                    .shadow(color: .black.opacity(0.12), radius: 4, y: 2)
            }
            .padding(.top, 56)
            .padding(.leading, 16)
            .accessibilityLabel("Chiudi mappa")
            .accessibilityIdentifier("btn_linemap_close")

            // Live vehicle count badge (top right)
            if !vehicles.isEmpty {
                VStack {
                    HStack {
                        Spacer()
                        LiveBadge(count: vehicles.count)
                            .shadow(color: .black.opacity(0.1), radius: 3, y: 1)
                            .padding(.top, 58)
                            .padding(.trailing, 16)
                    }
                    Spacer()
                }
            }
        }
    }

    // MARK: - Zoom tier

    enum ZoomTier {
        case far      // latitudeDelta > 0.08 — tiny dot
        case medium   // 0.03…0.08 — small badge, no needle
        case close    // < 0.03 — full badge + bearing needle
    }

    private var zoomTier: ZoomTier {
        if latitudeDelta > 0.08 { return .far }
        if latitudeDelta > 0.03 { return .medium }
        return .close
    }
}
```

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Views/Mappa/LineMapView.swift
git commit -m "refactor(LineMapView): migrate to VehicleStore, add zoom-tier state"
```

---

## Task 9 — `VehiclePinView`: upright badge + rotating bearing needle

This is the most visual task. The badge (circle + route name) must stay upright regardless of bearing; only the directional needle rotates.

**Key insight:** Put the badge and the needle in the same `ZStack` with a fixed `46×46` frame. The needle is a `Canvas` that draws centered in the frame; `rotationEffect` on it rotates around the frame center = the badge center. The badge has no `rotationEffect`.

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Mappa/LineMapView.swift` (the private structs at bottom)

- [ ] **Replace `VehiclePinView` and `Triangle` with the new implementation**

Delete the existing `VehiclePinView` struct and `Triangle` struct entirely. Replace with:

```swift
// MARK: - Vehicle Pin View

private struct VehiclePinView: View {
    let routeName: String
    let color: Color
    let textColor: Color
    let bearing: Float
    let zoomTier: LineMapView.ZoomTier

    var body: some View {
        switch zoomTier {
        case .far:
            // Tiny dot — no label, no needle
            Circle()
                .fill(color)
                .frame(width: 8, height: 8)
                .overlay(Circle().stroke(.white, lineWidth: 1))

        case .medium:
            // Compact badge, no needle — readable but not cluttered
            ZStack {
                Circle()
                    .fill(color)
                    .frame(width: 22, height: 22)
                    .overlay(Circle().stroke(.white, lineWidth: 1.2))
                Text(routeName)
                    .font(.system(size: 7, weight: .black, design: .rounded))
                    .foregroundStyle(textColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 2)
            }
            .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
            .drawingGroup()

        case .close:
            // Full badge + bearing needle that rotates independently
            FullVehiclePin(
                routeName: routeName,
                color: color,
                textColor: textColor,
                bearing: bearing
            )
        }
    }
}

// MARK: - Full Vehicle Pin (close zoom)

/// Badge stays upright; only the directional needle rotates.
/// Both elements share a 46×46 ZStack so `rotationEffect` on the needle
/// anchors at the badge center.
private struct FullVehiclePin: View {
    let routeName: String
    let color: Color
    let textColor: Color
    let bearing: Float

    private static let size: CGFloat = 46

    var body: some View {
        ZStack {
            // ── Bearing needle ──────────────────────────────────────────
            // Canvas fills the full 46×46 frame.
            // Triangle tip drawn at (cx, cy-20), base at (cx±5, cy-12).
            // rotationEffect rotates the canvas around its center = badge center.
            // Only shown when bearing is non-zero (bearing == 0 means "unknown").
            if bearing != 0 {
                Canvas { ctx, size in
                    let cx = size.width / 2
                    let cy = size.height / 2
                    var path = Path()
                    path.move(to: CGPoint(x: cx,   y: cy - 20))   // tip
                    path.addLine(to: CGPoint(x: cx - 5, y: cy - 11)) // left base
                    path.addLine(to: CGPoint(x: cx + 5, y: cy - 11)) // right base
                    path.closeSubpath()
                    // Filled with line color
                    ctx.fill(path, with: .color(color))
                    // White outline for contrast on similar-colored backgrounds
                    ctx.stroke(path, with: .color(.white), lineWidth: 1.5)
                }
                .frame(width: Self.size, height: Self.size)
                .rotationEffect(.degrees(Double(bearing)))
            }

            // ── Badge ────────────────────────────────────────────────────
            // Centered in the ZStack. Never rotated.
            ZStack {
                Circle()
                    .fill(color)
                    .frame(width: 30, height: 30)
                    .overlay(Circle().stroke(.white, lineWidth: 1.5))
                Text(routeName)
                    .font(.system(size: 9, weight: .black, design: .rounded))
                    .foregroundStyle(textColor)
                    .lineLimit(1)
                    .minimumScaleFactor(0.5)
                    .padding(.horizontal, 2)
            }
            .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
        }
        .frame(width: Self.size, height: Self.size)
        .drawingGroup()
        .accessibilityLabel("\(routeName) in transito")
        .accessibilityIdentifier("vehicle_\(routeName)")
    }
}
```

- [ ] **Build**

```bash
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "id=196EACF2-4B35-4678-89BC-0B09E031A5F0" \
  build 2>&1 | grep -E "error:|Build succeeded"
```

Expected: `Build succeeded`

- [ ] **Install + open map and verify visually**

```bash
# Get app path
APP=$(xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -showBuildSettings 2>/dev/null | grep "BUILT_PRODUCTS_DIR" | head -1 | awk '{print $3}')
xcrun simctl install 196EACF2-4B35-4678-89BC-0B09E031A5F0 "$APP/TransitKit.app"
xcrun simctl launch 196EACF2-4B35-4678-89BC-0B09E031A5F0 com.transitkit.
sleep 3
xcrun simctl openurl 196EACF2-4B35-4678-89BC-0B09E031A5F0 "transitkit://line/3/map"
sleep 4
# Take screenshot — verify:
# 1. Badge shows "B" upright (not rotated)
# 2. Small triangle arrow points in bearing direction
# 3. Zooming out → switches to dot
```

- [ ] **Verify all three zoom tiers by pinch-zooming in simulator**

At close zoom: full badge + needle visible, text readable.
At medium zoom: smaller badge, no needle, text still readable.
At far zoom: tiny 8px dot, no text.

- [ ] **Commit**

```bash
git add ios/TransitKit/Sources/Views/Mappa/LineMapView.swift
git commit -m "feat(LineMapView): zoom-aware pin tiers + upright badge with rotating bearing needle"
```

---

## Self-Review

### Spec coverage

| Requirement | Task |
|---|---|
| Live dot on departure rows | Task 5 |
| "N live" chip in LineDetailView header | Task 6 |
| Live dot on HomeTab nearby stops | Task 7 |
| Zoom-aware rendering (3 tiers) | Task 8+9 |
| Badge stays upright, only needle rotates | Task 9 |
| Global polling (not per-view) | Tasks 2+3+8 |
| `tripId` available on `Departure` for lookup | Task 1 |
| `LiveBadge` reusable component | Task 4 |

### Type consistency check

- `VehicleStore.isLive(tripId: String?) -> Bool` — used in Tasks 5, 7 ✅
- `VehicleStore.liveCount(forRouteId: String) -> Int` — used in Task 6 ✅
- `VehicleStore.vehicles(forRouteId: String) -> [GtfsRtVehicle]` — used in Task 8 ✅
- `Departure.tripId: String?` — added in Task 1, used in Tasks 5, 7 ✅
- `LineMapView.ZoomTier` enum — defined in Task 8, used in Task 9 ✅
- `VehiclePinView(zoomTier: LineMapView.ZoomTier)` — updated signature in Task 9; called in Task 8 ✅
- `LiveBadge(count: Int)` / `LiveBadge()` / `LiveBadge(label:)` — defined in Task 4, used in Tasks 5, 6, 7, 8 ✅

### Placeholder scan

No TBDs, TODOs, or "similar to Task N" references. All code blocks are complete. ✅

### Dependency order

Tasks must run in order 1 → 2 → 3 → 4 → 5 → 6 → 7 → 8 → 9. Task 5 requires Task 1 (tripId) + Task 2 (VehicleStore) + Task 3 (injected) + Task 4 (LiveBadge). Tasks 6 and 7 have the same prereqs. Tasks 8 and 9 are independent from 5–7 but require 2+3. ✅
