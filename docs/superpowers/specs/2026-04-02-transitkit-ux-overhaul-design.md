# TransitKit UX Overhaul — Design Spec
**Date:** 2026-04-02  
**Target:** Airbnb / Spotify / Citymapper quality tier  
**Project:** `/Users/andreatoffanello/GitHub/transit-engine/ios/TransitKit`

---

## Executive Summary

Nine categories of issues across the TransitKit iOS app ranging from broken localization keys (strings rendering raw key names instead of translated text), inconsistent icon sizing, missing color-contrast utility, redundant UI labels, broken filter reset state, missing Home tab, and incorrect tab icons. All must be resolved before the app is releasable at a top-tier quality level.

---

## Implementation Batches

### Batch A — Foundation: Color Utility + Icon Sizes + Brand (independent, highest priority)

**Files:** `Components/LineBadge.swift`, `Config/OperatorConfig.swift`

**A1 — WCAG contrast utility**
Add `ColorUtils.swift` with:
```swift
/// Returns "#FFFFFF" or "#000000" whichever has higher contrast ratio against the given hex.
/// Uses WCAG 2.1 relative luminance formula.
func contrastingTextColor(for backgroundHex: String) -> String
```
Luminance formula: L = 0.2126*R + 0.7152*G + 0.0722*B (linearized sRGB).
Contrast ratio > 4.5:1 target. Return `#FFFFFF` if white passes, else `#000000`.

**A2 — LineBadge auto text color**
In `LineBadge.init`, if `textColor` is `""`, `"000000"`, or `"FFFFFF"` (unset GTFS defaults), compute via `contrastingTextColor(for: color)` instead of using the raw value.
Rule: ONLY override if the textColor appears to be the GTFS default (black or white) AND the background is not white/very-light. If GTFS explicitly provides a non-default text_color, respect it always.

**A3 — BadgeSize iconSize fix**
```swift
var iconSize: CGFloat {
    case .tiny:   8
    case .small:  12
    case .medium: 16
}
```

**A4 — ThemeConfig secondaryColor**
Add `let secondaryColor: String?` to `OperatorConfig.ThemeConfig`. Add `AppTheme.secondary: Color` computed var defaulting to `accent.opacity(0.7)` if absent.

**A5 — Hide single-type filter**
In `OrariTab` (or wherever transit type filter chips are rendered): if `store.availableTransitTypes.count <= 1`, hide the filter row entirely. Add `availableTransitTypes: Set<TransitType>` computed prop to `ScheduleStore`.

**Acceptance criteria:**
- Dark background badges show white text; light background badges show black text automatically
- Badge icons are visibly larger than before at all sizes
- Filter chips for transit type are hidden when only buses exist

---

### Batch B — Lines List: Clean Subtitle + Localization (independent)

**Files:** `Views/Orari/LinesListView.swift`, `Resources/Localizable.xcstrings`

**B1 — Remove redundant "Bus" label**
In `LineRowContent`, the subtitle HStack shows `route.transitType.icon + displayName`. Hide this entire HStack if `store.availableTransitTypes.count <= 1` (inject via environment or pass as parameter).

**B2 — Fix directions_count**
Replace the current `directions_count \(route.directions.count)` text with:
- If `directions.count <= 1`: show nothing (remove the separator dot and text entirely)
- If `directions.count > 1`: show `"↔ \(route.directions.count)"` or localized `"directions_plural \(route.directions.count)"` = "2 directions" / "2 direzioni"

**B3 — Add missing localization keys to xcstrings:**
```
directions_plural %lld: IT="→ %lld direzioni" EN="→ %lld directions"
lines_loading: IT="Caricamento linee..." EN="Loading lines..."
lines_no_result: IT="Nessuna linea trovata" EN="No lines found"
lines_no_result_hint: IT="Prova con un altro termine" EN="Try a different search term"
lines_result_count %lld: IT="%lld linee" EN="%lld lines"
```

**Acceptance criteria:**
- Line rows show NO transit type label in single-type datasets
- Single-direction routes show no directions indicator
- Multi-direction routes show a compact directions indicator

---

### Batch C — Map: Direct Stop Navigation (independent)

**Files:** `Views/Mappa/MappaTab.swift`, `Views/Mappa/StopMapSheet.swift`

**C1 — Remove StopMapSheet, navigate directly**
When user taps a stop annotation in `MappaTab`, push directly to `StopDetailView(stop:)` via the `NavigationStack` that wraps `MappaTab` in `ContentView`. Use `@State private var selectedStop: ResolvedStop?` + `navigationDestination(item:)` pattern.

The `StopMapSheet` component can be kept in codebase but removed from `MappaTab`. This eliminates the 2-tap flow (tap annotation → see sheet → tap "See all") in favor of a single tap that opens the full stop view.

**Acceptance criteria:**
- Tap stop annotation → full StopDetailView opens immediately
- Back button returns to map at same position

---

### Batch D — Detail Views (needs Batch A for contrast utility)

**Files:** `Views/Orari/LineDetailView.swift`, `Views/Orari/StopDetailView.swift`, `Views/Orari/TripDetailView.swift`, `Components/FilterChip.swift`, `Resources/Localizable.xcstrings`

**D1 — LineDetailView header fix**
Remove `line_label` localization wrapper. The header should show:
- Badge (route.name) on left, large
- `route.longName` as prominent title text on right (no localization prefix)
- `route.transitType.displayName` as subtitle below longName
- Remove the static map (`lineMap` section) entirely from the scroll view

**D2 — Transfer stop icons in stops timeline**
In `stopsTimeline`, for each `ResolvedStop` that has `lineNames.count > 1` (or specifically lines != current route), prefix a `LucideIcon.arrowLeftRight` icon (small, accent color) before the badge row to signal "transfer here".

**D3 — StopDetailView filter chip unification**
Create `LineFilterChip` component in `Components/LineFilterChip.swift`:
```swift
struct LineFilterChip: View {
    let lineName: String
    let color: String      // GTFS hex
    let textColor: String  // computed via contrast utility
    let isSelected: Bool
    let action: () -> Void
}
```
When selected: pill with `Color(hex: color)` background, contrasting text. When unselected: glass morphism pill with colored text.

Replace the mixed `FilterChipView` + `LineBadge` usage in `nextDeparturesSection` with consistent `LineFilterChip` components for line chips and `FilterChip` for "Tutti".

**D4 — Filter empty state + reset**
When `filterLine != nil && upcomingDepartures.isEmpty`: show centered empty state with message and "Mostra tutte" / "Show all" button that sets `filterLine = nil`.

**D5 — minutes_away localization**
Add key: `"minutes_away %lld": IT="in %lld min" EN="in %lld min"`, `"departing_now": IT="Ora" EN="Now"`, `"one_minute": IT="1 min" EN="1 min"`

**D6 — show_more localization**
`"show_more %lld": IT="Mostra altri %lld" EN="Show %lld more"`

**D7 — filter_all_lines localization**
`"filter_all_lines": IT="Tutte" EN="All"`

**D8 — Stop toolbar: favorites + settings**
Add two `ToolbarItem(placement: .topBarTrailing)` in `StopDetailView`:
1. Favorites toggle: `LucideIcon.star` / `LucideIcon.starFilled` (check `FavoritesManager.isFavorite(stopId:)`)
2. Bell/alert prefs: `LucideIcon.bell` → opens `StopNotificationSheet` (simple placeholder sheet for now)

**D9 — FullScheduleSheet filter chips**
Apply same `LineFilterChip` pattern as D3. Fix `filter_all_lines` key usage.

**Acceptance criteria:**
- LineDetailView header looks clean: badge + name + type, no broken strings
- Static map gone from LineDetailView
- Transfer stops have a visual indicator
- All filter chips in StopDetailView and FullScheduleSheet are visually consistent
- Filtering to empty line shows message with reset button
- "in 16 min", "Ora", "Mostra altri 5" render correctly
- Star and bell icons in top-right of stop view

---

### Batch E — Home Tab + Info Icon Fix (independent)

**Files:** `App/ContentView.swift`, new `Views/Home/HomeTab.swift`

**E1 — Info tab icon**
`LucideIcon.ticket` → `LucideIcon.info` in ContentView.swift tab item for Info tab.

**E2 — Home Tab**
New tab inserted at index 0. All existing tabs shift +1.

```
HomeTab layout:
├── Alert Banner (conditional, driven by future config.alerts array — hidden if nil/empty)
├── Operator Hero Section
│   ├── Operator logo placeholder (colored circle with initials)
│   ├── Operator name (large, bold)
│   └── Region/country subtitle
├── Quick Access Cards (horizontal scroll)
│   ├── "Orari" card → navigates to OrariTab
│   ├── "Mappa" card → navigates to MappaTab  
│   └── "Info" card → navigates to InfoTab
└── Favorites Section (if favorites exist)
    └── Compact stop cards with next departure
```

Use `AppTheme` colors throughout. Glass morphism cards. The tab icon: `LucideIcon.home`.

**E3 — Add tab_home localization key**
`"tab_home": IT="Home" EN="Home"`

**Acceptance criteria:**
- Home tab appears first
- Alert banner slot exists (empty for now)
- Operator name/branding displayed
- Quick access cards navigate correctly
- Favorites section shows if favorites exist

---

## Missing Localization Keys (add to xcstrings)

```
tab_home: IT="Home" EN="Home"
directions_plural %lld: IT="→ %lld direzioni" EN="→ %lld directions"
minutes_away %lld: IT="in %lld min" EN="in %lld min"  
one_minute: IT="1 min" EN="1 min"
departing_now: IT="Ora" EN="Now"
show_more %lld: IT="Mostra altri %lld" EN="Show %lld more"
filter_all_lines: IT="Tutte" EN="All"
line_label %@: IT="Linea %@" EN="Line %@"  (keep for a11y only, not visible UI)
no_departures_for_line %@: IT="Nessuna partenza per la linea %@" EN="No departures for line %@"
show_all_departures: IT="Mostra tutte" EN="Show all"
transfer_here: IT="Cambio" EN="Transfer"
add_to_favorites: IT="Aggiungi ai preferiti" EN="Add to favorites"
remove_from_favorites: IT="Rimuovi dai preferiti" EN="Remove from favorites"
stop_notifications: IT="Notifiche fermata" EN="Stop notifications"
lines_loading: IT="Caricamento linee..." EN="Loading lines..."
lines_no_result: IT="Nessuna linea trovata" EN="No lines found"
lines_no_result_hint: IT="Prova con un altro termine" EN="Try a different search term"
lines_result_count %lld: IT="%lld linee" EN="%lld lines"
home_quick_access: IT="Accesso rapido" EN="Quick access"
home_favorites: IT="Preferiti" EN="Favorites"
home_no_favorites: IT="Nessun preferito ancora" EN="No favorites yet"
```

---

## UX Review Checklist (for senior reviewer with simulator)

After each batch, the senior UX reviewer must verify:

**Visual quality:**
- [ ] No text is unreadable on any badge (contrast ratio ≥ 4.5:1)
- [ ] Icons are appropriately sized (not all the same tiny size)
- [ ] No raw localization key strings visible anywhere in the UI
- [ ] Filter chips are visually consistent (same shape, same size, same style logic)

**Behavior:**
- [ ] Filter reset works from any filtered state
- [ ] Empty state when filtered line has no departures
- [ ] Map tap → single tap to open stop (no intermediate sheet)
- [ ] Favorites star toggles correctly

**Information architecture:**
- [ ] Redundant "Bus" label hidden in single-type datasets
- [ ] Redundant "directions_count 1" hidden
- [ ] Home tab provides genuine navigation value (not just a duplicate of Orari)

**Branding:**
- [ ] Tab bar shows correct icons (home, clock, map, info, settings)
- [ ] Info tab shows info icon (not ticket)
