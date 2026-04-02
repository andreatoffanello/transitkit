# TransitKit Wave 3 — Design Spec
**Date:** 2026-04-02  
**Starting from:** 8.5/10  
**Target:** 9+/10

---

## Batch 3A — Quick wins (S complexity, parallel)

### 3A-1: Tab bar trap fix
**File:** `Views/Orari/StopDetailView.swift`

Remove `.toolbar(.hidden, for: .tabBar)`. Instead, account for tab bar in the `mapContent` safe area inset:
- `mapContent` already has a `.safeAreaInset(edge: .bottom)` driven by `sheetDetent`
- Add `+ tabBarHeight` (~83pt) to that calculation
- The sheet's `.presentationBackgroundInteraction(.enabled(upThrough: .medium))` already allows map interaction
- Tab bar remains visible; rider can switch tabs while viewing a stop

### 3A-2: Headsign normalization
**New file:** `Sources/Utils/HeadsignNormalizer.swift`

```swift
enum HeadsignNormalizer {
    static func normalize(_ raw: String, map: [String: String]? = nil) -> String {
        // 1. Apply operator headsignMap if provided
        if let mapped = map?[raw] { return mapped }
        let caselessMap = map.flatMap { dict in dict.first { $0.key.caseInsensitiveCompare(raw) == .orderedSame } }
        if let mapped = caselessMap?.value { return mapped }
        // 2. Strip common prefixes
        var result = raw
        for prefix in ["To ", "Towards ", "Direction ", "Via "] {
            if result.lowercased().hasPrefix(prefix.lowercased()) {
                result = String(result.dropFirst(prefix.count))
            }
        }
        // 3. Title-case
        return result.capitalized
    }
}
```

Add `let headsignMap: [String: String]?` to `OperatorConfig` (top-level optional).
Apply in `ScheduleStore` wherever `headsign` is read from `data.headsigns[idx]`.

### 3A-3: First-run experience
**Files:** `Views/Home/HomeTab.swift`, `App/TransitKitApp.swift`

- `HomeTab`: when `favoritesManager.favoriteStopIds.isEmpty && !store.isLoading`: show `OnboardingCard` in favorites area with operator name, region, and "Trova la tua fermata →" CTA button that sets `selectedTab = 1` (Orari)
- `TransitKitApp` loading view: show operator initials avatar + operator name (from `ConfigLoader.load()` which succeeds before schedule loads) instead of generic spinner
- No `isFirstLaunch` flag needed — the empty favorites state IS the first-run state

---

## Batch 3B — Medium complexity

### 3B-1: Search recent + fuzzy
**New file:** `Sources/Stores/SearchHistoryStore.swift`
**Files:** `Views/Orari/OrariTab.swift`, `Views/Orari/StopsListView.swift`, `Views/Orari/LinesListView.swift`

- `SearchHistoryStore`: persists `[String]` of recent stop/line IDs in `UserDefaults`, max 8, newest first. `@Observable` so views update.
- `OrariTab`: when `searchQuery.isEmpty`, show "Recenti" section with recent stop/line rows above the full list
- `StopsListView`/`LinesListView`: implement subsequence fuzzy scoring when `searchQuery.count >= 2`; rank by score, exact prefix first
- Save to history when user navigates into a stop/line (`.navigationDestination` activation)

### 3B-2: GtfsRT live departures
**New file:** `Sources/Services/GtfsRtService.swift`
**Files:** `Stores/ScheduleStore.swift`, `App/TransitKitApp.swift`

- `GtfsRtService` actor: polls `config.gtfsRt?.tripUpdatesUrl` every 30s via `URLSession`
- Decodes GTFS-RT (JSON variant if available, protobuf requires SwiftProtobuf dependency)
- Builds `[String: Int]` delay map (`"routeId_stopId"` → seconds)
- `ScheduleStore.realtimeDelays` property (observable)
- `upcomingDepartures` adjusts `minutesFromMidnight` by delay when match exists
- `TimeDisplay` automatically shows live data (no changes needed)

---

## Acceptance criteria

**3A-1 tab bar:**
- [ ] Tab bar visible while viewing stop detail
- [ ] Map + sheet layout unchanged (no visual clash with tab bar)
- [ ] Can switch tabs while stop detail is open

**3A-2 headsigns:**
- [ ] "Inbound" → "Inbound" (no map = passthrough, title-cased)
- [ ] "To Central" → "Central"
- [ ] "TOWARDS AIRPORT" → "Airport"
- [ ] With config headsignMap: "Inbound" → "City Center" per operator definition

**3A-3 first-run:**
- [ ] Empty favorites state shows onboarding card with CTA
- [ ] Loading screen shows operator name/initials, not generic spinner
- [ ] Quick-access cards show "0 stop · 0 linee" during load (graceful)

**3B-1 search:**
- [ ] Recent searches visible when search field empty
- [ ] Tapping recent → navigates immediately (no full search needed)
- [ ] "cntrl" finds "Central Station" (fuzzy)
- [ ] Exact match ranks above fuzzy

**3B-2 RT:**
- [ ] Departures show adjusted times when RT feed available
- [ ] Falls back to static schedule when RT unavailable
- [ ] `TimeDisplay` shows `.minutes` with correct live value
