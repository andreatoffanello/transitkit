# TransitKit Wave 2 — Design Spec
**Date:** 2026-04-02  
**Starting from:** 7.5/10  
**Target:** 9+/10 Airbnb/Spotify/Citymapper tier

---

## Batch 2A — High visibility, pure Swift (launch first, parallel)

### 2A-1: Home tab — Smart departure surfacing

**User value:** Answers "when is my bus" from the home screen without any taps.

**Changes to `Views/Home/HomeTab.swift`:**

1. **Favorite stop cards** — expand to show next 2 departures per stop:
   - `[LineBadge .small] [headsign truncated, 1 line] [countdown]`
   - Countdown: "Ora" if < 1 min, "1 min" if 1 min, "N min" if < 60 min, "HH:MM" if > 60 min
   - Use `TimelineView(.periodic(from: .now, by: 30))` for live ticking countdowns
   - Show up to 2 departures; if 0 departures → "Nessuna partenza oggi" in secondary text

2. **Hero card enrichment:**
   - Add time-of-day greeting: `hora < 12 → "Buongiorno"`, `12-17 → "Buon pomeriggio"`, `>17 → "Buonasera"`
   - Use `TimeZone(identifier: config.timezone)` for local time
   - Add secondary stats row: `"\(store.routes.count) linee · \(store.stops.count) fermate"` in caption/tertiary text

3. **Quick-access card subtitles:**
   - Orari card: `"\(store.stops.count) fermate"` subtitle
   - Linee card: `"\(store.routes.count) linee"` subtitle
   - Info card: `config.name` subtitle

**Localization keys to add:**
- `home_greeting_morning`: IT="Buongiorno" EN="Good morning"
- `home_greeting_afternoon`: IT="Buon pomeriggio" EN="Good afternoon"
- `home_greeting_evening`: IT="Buonasera" EN="Good evening"
- `home_stats`: IT="%lld linee · %lld fermate" EN="%lld lines · %lld stops"

---

### 2A-2: Config defensive decoding + hex validation

**User value:** Prevents silent failures when onboarding new operators with incomplete configs.

**Changes to `Config/OperatorConfig.swift`:**

1. **`FeaturesConfig`** — add custom `init(from:)` with `decodeIfPresent` + defaults:
   ```swift
   init(from decoder: Decoder) throws {
       let c = try decoder.container(keyedBy: CodingKeys.self)
       enableMap = try c.decodeIfPresent(Bool.self, forKey: .enableMap) ?? true
       enableGeolocation = try c.decodeIfPresent(Bool.self, forKey: .enableGeolocation) ?? false
       enableFavorites = try c.decodeIfPresent(Bool.self, forKey: .enableFavorites) ?? true
       enableNotifications = try c.decodeIfPresent(Bool.self, forKey: .enableNotifications) ?? false
   }
   ```

2. **`MapConfig`** — same pattern with defaults: `centerLat=0, centerLng=0, defaultZoom=12`

3. **Hex validation in `AppTheme.configure(from:)`:**
   ```swift
   static func isValidHex(_ hex: String) -> Bool {
       let clean = hex.trimmingCharacters(in: CharacterSet.alphanumerics.inverted)
       return clean.count == 6 || clean.count == 8
   }
   ```
   Before setting `accentHex`/`primaryHex`: if `!isValidHex(value)`, keep the current default.

4. **Add `gtfsRt` optional struct to `OperatorConfig`:**
   ```swift
   struct GtfsRtConfig: Codable {
       let vehiclePositionsUrl: String?
       let tripUpdatesUrl: String?
       let serviceAlertsUrl: String?
   }
   let gtfsRt: GtfsRtConfig?
   ```
   This enables real-time data in a future sprint without a model change.

5. **Cache expiry check in `ScheduleLoader`:**
   - If `validUntil` date in loaded schedule is in the past, log a warning (don't block the app — stale data is better than no data).

---

## Batch 2B — Stop detail + pipeline enrichment

### 2B-1: Nearby stops section in StopDetailView

**User value:** "I missed my bus, what else can I catch nearby?" — solved without leaving the stop view.

**Changes to `Stores/ScheduleStore.swift`:**
```swift
func nearbyStops(to stop: ResolvedStop, radiusMeters: Double = 400) -> [ResolvedStop] {
    // Flat-earth approximation (sufficient for <1km radius)
    let latDeg = radiusMeters / 111_320
    let lngDeg = radiusMeters / (111_320 * cos(stop.lat * .pi / 180))
    return stops
        .filter { $0.id != stop.id }
        .compactMap { candidate -> (ResolvedStop, Double)? in
            let dlat = candidate.lat - stop.lat
            let dlng = candidate.lng - stop.lng
            let dist = sqrt(dlat*dlat + lngDeg*lngDeg / latDeg*latDeg) * 111_320
            guard dist > 30 && dist <= radiusMeters else { return nil }  // min 30m floor
            return (candidate, dist)
        }
        .sorted { $0.1 < $1.1 }
        .prefix(5)
        .map(\.0)
}
```

**Changes to `Views/Orari/StopDetailView.swift`:**
- Add `@State private var nearbyStops: [ResolvedStop] = []` 
- Compute async in `.task { nearbyStops = store.nearbyStops(to: stop) }`
- Add section below linesSection in the sheet, only if `!nearbyStops.isEmpty`:
  - Section header: "Fermate vicine" / "Nearby stops"
  - Compact row: distance badge ("120 m") + stop name + line badges (up to 3)
  - Tapping pushes `StopDetailView(stop: nearbyStop)` via existing NavigationStack

**Localization:**
- `nearby_stops`: IT="Fermate vicine" EN="Nearby stops"
- `distance_meters %lld`: IT="%lld m" EN="%lld m"

---

### 2B-2: `route_url` in LineDetailView

**User value:** For university/campus operators, `route_url` links to the official PDF schedule — riders actively search for this.

**Changes:**
- Add `let url: String?` to `Route` model (or check if it exists already)
- In `LineDetailView`, if `route.url != nil`, add a "Orario ufficiale" / "Official schedule" link button at the bottom of the header section, styled like the "View on website" button in `OperatorInfoView`

---

## Batch 2C — Map clustering (M complexity, after 2A+2B stable)

### 2C-1: Coordinate-bin stop clustering

**Changes to `Views/Mappa/MappaTab.swift` + new `Models/StopCluster.swift`:**

1. **`StopCluster` model:**
   ```swift
   struct StopCluster: Identifiable {
       let id: String
       let centerLat: Double
       let centerLng: Double
       let count: Int
       let stops: [ResolvedStop]
   }
   ```

2. **Binning logic in `MappaTab`:**
   - Observe `@State private var mapZoomLevel: Double` from the `MapCameraPosition` onChange
   - At `zoomLevel < 11`: group stops into bins rounding lat/lng to 2 decimal places → show `ClusterAnnotationView` (circle + count)
   - At `zoomLevel >= 11`: show individual `StopAnnotationView`
   - `ClusterAnnotationView`: 36pt accent-colored circle with white count text; tapping fits map to cluster bounds

3. **Zoom-to-fit helper** (extract from existing `selectRoute` logic):
   ```swift
   func cameraPositionFitting(stops: [ResolvedStop]) -> MapCameraPosition
   ```

---

## Acceptance criteria (UX reviewer checklist)

**After 2A:**
- [ ] Home screen shows "Buongiorno/Buon pomeriggio/Buonasera" in hero card
- [ ] Favorite stop cards show next 2 departures with countdown
- [ ] Quick-access cards show stop/route counts as subtitles
- [ ] Countdown refreshes live (no stale "5 min" after a minute passes)

**After 2A-2:**
- [ ] App boots successfully with a config missing `enableGeolocation` field
- [ ] Invalid hex colors in config don't produce black-on-black themes

**After 2B-1:**
- [ ] Stop detail shows "Fermate vicine" with distance badges for stops within 400m
- [ ] Tapping a nearby stop pushes its detail view
- [ ] Section absent if no stops within 400m (no empty state)

**After 2C:**
- [ ] At full zoom-out, stops are clustered into ~10-15 blobs instead of 100+ dots
- [ ] Tapping a cluster zooms to show its member stops individually
