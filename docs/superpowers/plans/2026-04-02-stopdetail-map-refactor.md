# StopDetailView Map Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the map+sheet pattern in StopDetailView with an inline 190pt 3D map header + matchedGeometryEffect fullscreen expand, eliminating scroll lock and all sheet workarounds.

**Architecture:** `StopDetailView` becomes a standard NavigationStack push with a single root `ScrollView`. The `Map` lives as a clipped 190pt header. A `@Namespace` + `@State var mapExpanded` pair drives a `matchedGeometryEffect` hero animation to fullscreen overlay. All drag/card state removed.

**Tech Stack:** SwiftUI iOS 17+, MapKit (SwiftUI), `matchedGeometryEffect`, `@Observable`, `@Namespace`

---

## Files

| File | Change |
|---|---|
| `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift` | Major refactor — remove sheet pattern, add inline map header + fullscreen overlay |
| `ios/TransitKit/Sources/Views/Home/HomeTab.swift` | Line 92: `Image(systemName: "bus")` → `LucideIcon.bus.image` |

---

## Task 1: Remove map+sheet state and card machinery

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift`

Remove the following properties and methods that belong to the old map+sheet pattern. After this task the file will not compile — that is expected.

- [ ] **Step 1: Remove card state variables**

Delete these lines from the property declarations (lines 21–27):
```swift
// DELETE these lines:
private static let peekHeight: CGFloat = 160
private static let mediumFraction: CGFloat = 0.50
@State private var cardExpanded = false
@State private var cardHeightOverride: CGFloat? = nil
@GestureState private var dragState: CGFloat = 0
```

- [ ] **Step 2: Remove showMap state variable**

Delete:
```swift
@State private var showMap = false
```

- [ ] **Step 3: Remove bottomCard function**

Delete the entire `bottomCard(height:screenHeight:)` function (lines 177–219).

- [ ] **Step 4: Remove mapContent ViewBuilder**

Delete the entire `mapContent` `@ViewBuilder` property (lines 222–251).

- [ ] **Step 5: Remove delayed map reveal in onAppear**

In the `.onAppear` modifier, remove the `Task { sleep / withAnimation showMap }` block. Keep only `centerOnStop()`:
```swift
.onAppear {
    centerOnStop()
}
```

- [ ] **Step 6: Remove showMap conditional in body**

The current body has `if showMap { mapContent ... }`. Delete that block entirely; body cleanup in Task 2.

---

## Task 2: Rewrite body as standard ScrollView

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift`

Add the new namespace and expanded state, then rewrite the body.

- [ ] **Step 1: Add namespace and mapExpanded state**

After the existing `@State private var nearbyStopsData: [ResolvedStop] = []` line, add:
```swift
@Namespace private var mapNamespace
@State private var mapExpanded: Bool = false
```

- [ ] **Step 2: Replace the body computed property**

Replace the entire `var body: some View { ... }` (currently GeometryReader-based) with:

```swift
var body: some View {
    ZStack {
        ScrollView(showsIndicators: false) {
            VStack(spacing: 0) {
                mapHeader
                stopInlineContent
            }
        }
        .background(AppTheme.background.ignoresSafeArea())

        if mapExpanded {
            expandedMapOverlay
                .transition(.identity)
                .zIndex(10)
        }
    }
    .navigationTitle("")
    .navigationBarTitleDisplayMode(.inline)
    .toolbar(.visible, for: .tabBar)
    .toolbar {
        ToolbarItem(placement: .principal) {
            VStack(spacing: 1) {
                Text(stop.name)
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(AppTheme.textPrimary)
                    .lineLimit(1)
                    .truncationMode(.middle)
            }
        }
        ToolbarItem(placement: .topBarTrailing) {
            HStack(spacing: 4) {
                Button {
                    showNotificationsSheet = true
                } label: {
                    Image(systemName: "bell")
                        .foregroundStyle(AppTheme.accent)
                }
                .accessibilityLabel(String(localized: "stop_notifications"))
                .accessibilityIdentifier("btn_notifications")

                Button {
                    favoritesManager.toggle(stop.id)
                } label: {
                    Image(systemName: favoritesManager.isFavorite(stop.id) ? "star.fill" : "star")
                        .foregroundStyle(AppTheme.accent)
                }
                .accessibilityLabel(favoritesManager.isFavorite(stop.id)
                    ? String(localized: "remove_from_favorites")
                    : String(localized: "add_to_favorites"))
                .accessibilityIdentifier("btn_favorite")
            }
        }
    }
    .sheet(isPresented: $showNotificationsSheet) {
        NavigationStack {
            VStack(spacing: 16) {
                Text(String(localized: "stop_notifications"))
                    .font(.headline)
                Text(String(localized: "stop_notifications_placeholder"))
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle(stop.name)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showNotificationsSheet = false } label: {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundStyle(.secondary)
                    }
                }
            }
        }
        .presentationDetents([.medium])
    }
    .fullScreenCover(isPresented: $showFullSchedule) {
        FullScheduleSheet(stop: stop)
    }
    .onAppear {
        centerOnStop()
    }
    .task {
        nearbyStopsData = store.nearbyStops(to: stop)
    }
    .onDisappear {
        selectedNearbyStop = nil
    }
    .navigationDestination(item: $selectedNearbyStop) { nearbyStop in
        StopDetailView(stop: nearbyStop)
    }
}
```

---

## Task 3: Add mapHeader with 3D camera and expand button

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift`

Add after the `// MARK: - Body` section (after the closing brace of `body`).

- [ ] **Step 1: Add mapHeader ViewBuilder**

```swift
// MARK: - Map Header

@ViewBuilder
private var mapHeader: some View {
    Map(position: $mapPosition) {
        if stop.docks.isEmpty {
            Annotation(stop.name, coordinate: stopCoordinate) {
                ZStack {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 28, height: 28)
                    (stop.transitTypes.first ?? .bus).icon.image
                        .font(.system(size: 13, weight: .bold))
                        .foregroundStyle(.white)
                }
                .shadow(color: .black.opacity(0.2), radius: 3, y: 1)
            }
        } else {
            ForEach(stop.docks, id: \.letter) { dock in
                let coord = CLLocationCoordinate2D(latitude: dock.lat, longitude: dock.lng)
                Annotation(String(localized: "dock_label \(dock.letter)"), coordinate: coord) {
                    DockPin(letter: dock.letter)
                }
            }
        }
    }
    .matchedGeometryEffect(id: "stopMap", in: mapNamespace)
    .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
    .frame(height: 190)
    .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
    .disabled(true)
    .overlay(alignment: .bottomTrailing) {
        Button {
            withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                mapExpanded = true
            }
        } label: {
            LucideIcon.maximize2.image
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.primary)
                .frame(width: 32, height: 32)
                .background(.regularMaterial, in: Capsule())
        }
        .buttonStyle(.plain)
        .padding(10)
        .accessibilityLabel("Espandi mappa")
        .accessibilityIdentifier("btn_expand_map")
    }
    .padding(.horizontal, 16)
    .padding(.top, 12)
    .padding(.bottom, 8)
}
```

- [ ] **Step 2: Update centerOnStop() for 3D pitch**

Replace the existing `centerOnStop()` function with:

```swift
private func centerOnStop() {
    if stop.docks.count > 1 {
        let lats = stop.docks.map(\.lat)
        let lngs = stop.docks.map(\.lng)
        let center = CLLocationCoordinate2D(
            latitude: (lats.min()! + lats.max()!) / 2,
            longitude: (lngs.min()! + lngs.max()!) / 2
        )
        let spanDeg = max((lats.max()! - lats.min()!) * 5.0, 0.005)
        let distance = spanDeg * 111_000 * 1.3
        mapPosition = .camera(MapCamera(
            centerCoordinate: center,
            distance: max(distance, 500),
            heading: 0,
            pitch: 50
        ))
    } else {
        mapPosition = .camera(MapCamera(
            centerCoordinate: stopCoordinate,
            distance: 400,
            heading: 0,
            pitch: 50
        ))
    }
}
```

---

## Task 4: Add expandedMapOverlay

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift`

Add after the `mapHeader` section.

- [ ] **Step 1: Add expandedMapOverlay ViewBuilder**

```swift
// MARK: - Expanded Map Overlay

@ViewBuilder
private var expandedMapOverlay: some View {
    let expandedCamera = MapCameraPosition.camera(MapCamera(
        centerCoordinate: stopCoordinate,
        distance: 350,
        heading: 0,
        pitch: 65
    ))

    Map(initialPosition: expandedCamera) {
        if stop.docks.isEmpty {
            Annotation(stop.name, coordinate: stopCoordinate) {
                ZStack {
                    Circle()
                        .fill(AppTheme.accent)
                        .frame(width: 32, height: 32)
                    (stop.transitTypes.first ?? .bus).icon.image
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(.white)
                }
                .shadow(color: .black.opacity(0.3), radius: 4, y: 2)
            }
        } else {
            ForEach(stop.docks, id: \.letter) { dock in
                let coord = CLLocationCoordinate2D(latitude: dock.lat, longitude: dock.lng)
                Annotation(String(localized: "dock_label \(dock.letter)"), coordinate: coord) {
                    DockPin(letter: dock.letter)
                }
            }
        }
    }
    .matchedGeometryEffect(id: "stopMap", in: mapNamespace)
    .mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
    .ignoresSafeArea(.all)
    .overlay(alignment: .topTrailing) {
        Button {
            withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                mapExpanded = false
            }
        } label: {
            LucideIcon.x.image
                .font(.system(size: 14, weight: .semibold))
                .foregroundStyle(.primary)
                .frame(width: 36, height: 36)
                .background(.regularMaterial, in: Capsule())
        }
        .buttonStyle(.plain)
        .padding(.top, 60)
        .padding(.trailing, 16)
        .accessibilityLabel("Chiudi mappa")
        .accessibilityIdentifier("btn_close_map")
    }
    .gesture(
        DragGesture(minimumDistance: 20)
            .onEnded { value in
                if value.translation.height > 80 {
                    withAnimation(.spring(response: 0.42, dampingFraction: 0.82)) {
                        mapExpanded = false
                    }
                }
            }
    )
}
```

---

## Task 5: Add stopInlineContent (replaces stopSheetContent)

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Orari/StopDetailView.swift`

The old `stopSheetContent` was a `ScrollView` wrapping all sections — now it's just a `VStack` since it lives inside the root `ScrollView`.

- [ ] **Step 1: Add stopInlineContent**

Add after `expandedMapOverlay`:

```swift
// MARK: - Inline Content (below map header)

private var stopInlineContent: some View {
    let allNext = upcomingDepartures
    let visible = showMoreDepartures ? allNext : Array(allNext.prefix(initialVisibleCount))
    let extraCount = allNext.count - initialVisibleCount

    return VStack(spacing: 0) {
        // Stop name header
        inlineStopHeader

        // Lines section
        linesSection

        // Nearby stops
        nearbyStopsSection

        // Next departures
        if !visible.isEmpty {
            nextDeparturesSection(visible)

            if !showMoreDepartures && extraCount > 0 {
                Button {
                    withAnimation(.spring(duration: 0.3)) {
                        showMoreDepartures = true
                    }
                } label: {
                    HStack(spacing: 4) {
                        LucideIcon.chevronDown.image
                            .font(.system(size: 11, weight: .semibold))
                        Text(String(localized: "show_more \(extraCount)"))
                            .font(.system(size: 13, weight: .medium))
                    }
                    .foregroundStyle(AppTheme.accent)
                    .frame(maxWidth: .infinity)
                    .padding(.vertical, 10)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .padding(.horizontal, 16)
                .padding(.bottom, 4)
            }
        } else if filterLine != nil && upcomingDepartures.isEmpty {
            VStack(spacing: 12) {
                Text(String(localized: "no_departures_for_line \(filterLine ?? "")"))
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.textSecondary)
                    .multilineTextAlignment(.center)
                Button(String(localized: "show_all_departures")) {
                    withAnimation { filterLine = nil }
                }
                .font(.footnote.weight(.semibold))
                .foregroundStyle(AppTheme.accent)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 24)
            .padding(.horizontal, 20)
        } else if !store.isLoading {
            VStack(spacing: 8) {
                LucideIcon.clock.image
                    .font(.system(size: 28))
                    .foregroundStyle(AppTheme.textTertiary)
                Text(String(localized: "no_departures"))
                    .font(.system(size: 14))
                    .foregroundStyle(AppTheme.textSecondary)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 32)
        }

        // Full schedule button
        if !allDayGroups.isEmpty {
            Button {
                showFullSchedule = true
            } label: {
                HStack(spacing: 6) {
                    LucideIcon.clock.image
                        .font(.system(size: 14, weight: .semibold))
                    Text(String(localized: "full_schedule"))
                        .font(.system(size: 14, weight: .semibold))
                }
                .foregroundStyle(AppTheme.accent)
                .frame(maxWidth: .infinity)
                .padding(.vertical, 12)
                .background(AppTheme.accent.opacity(0.08))
                .clipShape(RoundedRectangle(cornerRadius: 12))
            }
            .accessibilityIdentifier("btn_full_schedule")
            .padding(.horizontal, 20)
            .padding(.top, 4)
            .padding(.bottom, 100)
        }
    }
}
```

- [ ] **Step 2: Add inlineStopHeader**

The old `peekHeader` was inside the sheet card. Now it's just below the map in the scroll view. Add this after `stopInlineContent`:

```swift
// MARK: - Inline Stop Header

private var inlineStopHeader: some View {
    HStack(alignment: .center, spacing: 12) {
        VStack(alignment: .leading, spacing: 3) {
            Text(stop.name)
                .font(.system(size: 22, weight: .bold))
                .foregroundStyle(AppTheme.textPrimary)

            HStack(spacing: 6) {
                ForEach(Array(stop.transitTypes).sorted(by: { $0.rawValue < $1.rawValue }), id: \.self) { type in
                    HStack(spacing: 4) {
                        type.icon.image
                            .font(.system(size: 11, weight: .medium))
                        Text(type.displayName)
                            .font(.system(size: 12, weight: .medium))
                    }
                    .foregroundStyle(AppTheme.textSecondary)
                }
            }
        }

        Spacer()

        Button { openInMaps() } label: {
            LucideIcon.navigation.image
                .font(.system(size: 18))
                .foregroundStyle(AppTheme.accent)
                .frame(width: 44, height: 44)
                .background(AppTheme.accent.opacity(0.1))
                .clipShape(Circle())
        }
        .accessibilityLabel(String(localized: "a11y_navigate_to_stop"))
        .accessibilityIdentifier("btn_navigate_sheet")
    }
    .padding(.horizontal, 20)
    .padding(.top, 20)
    .padding(.bottom, 12)
}
```

- [ ] **Step 3: Delete the old stopSheetContent property**

Delete the entire `var stopSheetContent: some View { ... }` property and its `peekHeader` property. They are fully replaced by `stopInlineContent` + `inlineStopHeader` + `mapHeader`.

---

## Task 6: Fix HomeTab SF Symbol bus icon

**Files:**
- Modify: `ios/TransitKit/Sources/Views/Home/HomeTab.swift`

- [ ] **Step 1: Replace Image(systemName: "bus") with LucideIcon**

At line 92, replace:
```swift
Image(systemName: "bus")
    .font(.system(size: 10, weight: .medium))
    .foregroundStyle(AppTheme.textSecondary)
```
with:
```swift
LucideIcon.bus.image
    .font(.system(size: 10, weight: .medium))
    .foregroundStyle(AppTheme.textSecondary)
```

---

## Task 7: Build and verify

- [ ] **Step 1: Build**

```bash
xcodebuild build \
  -project /Users/andreatoffanello/GitHub/transit-engine/ios/TransitKit.xcodeproj \
  -scheme TransitKit \
  -destination 'platform=iOS Simulator,id=2072D7B1-13D8-4D03-B483-F923CE59D6E3' \
  CODE_SIGNING_ALLOWED=NO 2>&1 | tail -20
```

Expected: `** BUILD SUCCEEDED **`

- [ ] **Step 2: Fix any compiler errors**

Common issues to watch for:
- `mapContent` or `peekHeader` references not yet removed → delete them
- `stopSheetContent` reference in body → replaced by `stopInlineContent`
- `cardExpanded` or `dragState` still referenced → delete remaining usages
- `showMap` referenced anywhere → delete

---

## Task 8: Commit

- [ ] **Step 1: Commit**

```bash
git add ios/TransitKit/Sources/Views/Orari/StopDetailView.swift \
        ios/TransitKit/Sources/Views/Home/HomeTab.swift \
        docs/superpowers/specs/2026-04-02-stopdetail-map-refactor-design.md \
        docs/superpowers/plans/2026-04-02-stopdetail-map-refactor.md
git commit -m "$(cat <<'EOF'
Refactor StopDetailView: inline 3D map header + hero expand overlay

Replace map+sheet pattern with standard ScrollView layout:
- 190pt 3D map header (pitch=50°, building extrusion) replaces fullscreen map
- matchedGeometryEffect hero animation to fullscreen overlay (pitch=65°)
- Expand (⤢) + close (✕) glass pill buttons; swipe-down to dismiss
- Eliminates scroll lock, drag gesture machinery, double-ScrollView nesting
- All NavigationLink-to-nearby-stop workarounds removed (works natively)
- Replace Image(systemName:"bus") with LucideIcon.bus in HomeTab

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"
```
