# StopDetailView Map Refactor — Design Spec
**Date:** 2026-04-02
**From:** 7.5/10 (map+sheet pattern, scroll lock, UX friction)
**Target:** 9+/10

---

## Problem

The current `StopDetailView` uses a fullscreen map + draggable bottom card overlay. This pattern creates:
- Scroll lock at medium card height (inner ScrollView inert until card expanded)
- Tab bar visibility hacks (fixed with override but fragile)
- NavigationLink-inside-sheet failures requiring dismiss+delay workarounds
- Every navigation out of the stop requires workarounds fighting the framework

The pattern is correct for navigation apps (Apple Maps) where the map IS the content. For a transit app where the primary question is "when does my bus come?", departures are the content and the map is supporting context.

## Solution: Option A — Inline Map Header + Expandable Fullscreen Overlay

Standard NavigationStack push → scrollable `ScrollView`. Map lives as a fixed-height header (190pt) at the top of the scroll. An expand button opens a fullscreen overlay using `matchedGeometryEffect` for a hero-style fluid animation.

---

## Layout

```
NavigationStack push → StopDetailView (no sheet)
┌─────────────────────────────────────┐
│  [Map 190pt — 3D, non-interactive]  │  pitch=50°, distance=400m
│                               [⤢]  │  expand button, glass pill, bottom-trailing
├─────────────────────────────────────┤
│  Stop Name                   ★  🔔  │  bold 22pt + line badges row
│  [B] [E] [G]                        │  (via existing linesSection)
├─────────────────────────────────────┤
│  Next Departures                    │  section header
│  [Tutti] [B] [E] [G]                │  filter chips
│  → 2 min   [B] ASU Peacock...       │  departure rows
│  → 5 min   [E] Boone Mall           │
├─────────────────────────────────────┤
│  Fermate vicine                     │  horizontal scroll
│  [card 140pt] [card] [card]  →      │
├─────────────────────────────────────┤
│  [Orario completo]                  │  full schedule button
└─────────────────────────────────────┘
```

Fullscreen map overlay (when expanded):
```
┌─────────────────────────────────────┐  ignoresSafeArea(.all)
│                               [✕]   │  glass close pill, top-trailing, safe area aware
│                                     │
│   [MapKit — 3D interactive]         │  pitch=65°, distance=350m, full interaction
│                                     │
│   stop pin + dock pins visible      │
│                                     │
└─────────────────────────────────────┘
```

---

## Map Style: 3D with Building Extrusion

Both collapsed and expanded use:
```swift
.mapStyle(.standard(elevation: .realistic, pointsOfInterest: .excludingAll))
```

Camera positions:
- **Collapsed header:** `MapCamera(centerCoordinate: stop, distance: 400, heading: 0, pitch: 50)`
- **Expanded fullscreen:** `MapCamera(centerCoordinate: stop, distance: 350, heading: 0, pitch: 65)`

For stops with multiple docks: adjust distance to fit bounding box, maintain pitch.

---

## Animation Mechanism: matchedGeometryEffect

```swift
@Namespace private var mapNamespace
@State private var mapExpanded = false
```

- Collapsed: `Map(...).matchedGeometryEffect(id: "stopMap", in: mapNamespace).frame(height: 190).disabled(true)`
- Expanded: `Map(...).matchedGeometryEffect(id: "stopMap", in: mapNamespace).ignoresSafeArea(.all)`
- SwiftUI interpolates position + size + corner radius automatically
- Spring: `.spring(response: 0.42, dampingFraction: 0.82)`
- Map stays in hierarchy → no camera re-init on expand

---

## Expand / Close Controls

| Element | Spec |
|---|---|
| Expand button | `LucideIcon.maximize2`, 32×32pt, glass pill (`.regularMaterial`), positioned `.overlay(alignment: .bottomTrailing)` on the map header, padding 10pt |
| Close button | `LucideIcon.x`, same glass pill, `.overlay(alignment: .topTrailing)` on fullscreen map, top safe area inset aware |
| Swipe-to-close | `DragGesture` on fullscreen map: if `translation.height > 80` → `mapExpanded = false` |
| Tap outside | Not needed — fullscreen overlay covers everything, close is explicit |

---

## What Gets Removed

- `GeometryReader` wrapper
- `bottomCard(height:screenHeight:)` function + drag gesture state
- `cardExpanded`, `cardHeightOverride`, `dragState` state variables
- `showMap` delayed reveal + `.easeIn` transition
- `mapContent` as separate `@ViewBuilder` (inlined as `mapHeader`)
- Double `ScrollView` nesting (root `ScrollView` + `stopSheetContent` inner `ScrollView`)
- `.safeAreaInset` driven by card height

---

## What Gets Added

- `@Namespace private var mapNamespace`
- `@State private var mapExpanded: Bool = false`
- `mapHeader` view: Map + expand button overlay, frame(height: 190), clip to RoundedRectangle(16)
- `expandedMapOverlay`: fullscreen Map + close button + swipe gesture, shown via `if mapExpanded` in ZStack
- Single root `ScrollView` with all sections stacked vertically

---

## Navigation

`nearbyStopsSection` tap → `selectedNearbyStop = nearbyStop` → `.navigationDestination(item:)` on outer NavigationStack. No sheet involvement — works natively.

---

## Additional Fixes (same agent, same PR)

### Nav bar title truncation
Long stop names ("ASU College of Health Sciences") truncate to "..." in the inline nav bar title. Fix:
- Use custom `.principal` `ToolbarItem` with two-line `VStack` (name + transit type)
- Or reduce font to `.subheadline.weight(.semibold)` via `Text` in principal slot
- Remove `toolbarRole(.editor)` if it conflicts with custom principal

### SF Symbol bus → LucideIcon
`HomeTab.swift:92` — `Image(systemName: "bus")` is the only remaining SF Symbol in the app. Replace with `LucideIcon.bus.image` to match the rest of the visual language.

---

## Acceptance Criteria

- [ ] Scroll through departures without dragging the card up first
- [ ] Tap expand (⤢) → map animates hero-style to fullscreen edge-to-edge
- [ ] Tap close (✕) or swipe down → map animates back to 190pt header
- [ ] Map in both states shows 3D buildings (elevation: .realistic)
- [ ] Collapsed: non-interactive (scroll page, not pan map)
- [ ] Expanded: fully interactive (pinch zoom, rotate, pan)
- [ ] Tab bar remains visible throughout
- [ ] NavigationLink to nearby stop works without dismiss hack
- [ ] Long stop names no longer truncate in nav bar
- [ ] No SF Symbols in hero card (bus icon is LucideIcon)
