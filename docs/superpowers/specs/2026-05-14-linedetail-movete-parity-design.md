# LineDetail iOS — Movete parity + tab Avvisi + linee preferite

**Data:** 2026-05-14
**Scope:** iOS (TransitKit). Android segue come porting successivo.

## Obiettivo

Riallineare `LineDetailView` al pattern Movete: niente mappa hero, card vetture live, direction pills tinted, filtro per direzione. Aggiungere tab Avvisi dedicata con filtri (Tutti / Linee preferite / Linea singola). Aggiungere preferiti per linea.

## Decisioni di design

1. **Tab Avvisi 5ª:** sostituisce "Servizi". Servizi diventa quick block in Home.
2. **Mappa hero rimossa:** bottone toolbar (icona `maximize2`) apre tab Mappa con linea pre-selezionata (non più fullScreenCover).
3. **Card vetture live filtrate per direction id attiva.** Counter "N IN SERVIZIO" si aggiorna col picker.
4. **Direction picker pills tinted:** lista verticale, pill attiva `bg lineColor.opacity(0.15)`, border `lineColor.opacity(0.5)`.
5. **Tap card vettura:** switch tab 3 + `router.pendingMapPreviewVehicleId = vehicle.id` (esiste già).
6. **Preferiti per linea:** `FavoritesManager.favoriteRouteIds`, cuore in toolbar LineDetail.
7. **AlertListView filtri:** segmented "Tutti / Preferiti / Linea X" con picker linea contestuale.

## Layout LineDetail (top → bottom)

1. Alert section inline (solo se presenti — riusa logica esistente)
2. Direction pills (sopra il filtrato, sempre visibile se >1 direzione)
3. Live counter "N IN SERVIZIO" + horizontal scroll VehicleLiveCard
4. Fermate header + timeline (invariato)

Toolbar:
- Leading: back
- Principal: `[badge] longName`
- Trailing 1: `heart` / `heart.fill` toggle favorite
- Trailing 2: `maximize2` → apre tab Mappa con linea selezionata

## Risoluzione direction da tripId

`APIDeparture` NON ha `directionId`. Lookup via headsign matching:
- `route.directions.first { $0.headsign == departure.headsign }?.directionId`
- Fallback: directionId 0

Aggiunta a `ScheduleStore`:
```swift
private(set) var directionByTripId: [String: Int] = [:]
func direction(forTripId tripId: String) -> Int?
```
Popolato in `apply()` con il loop già esistente sulle departures.

## File modificati

| File | Change |
|------|--------|
| `Stores/ScheduleStore.swift` | + `directionByTripId` + `direction(forTripId:)` |
| `Stores/FavoritesManager.swift` | + `favoriteRouteIds` + `isFavoriteRoute/toggleRoute/etc` |
| `App/DeepLinkRouter.swift` | + `pendingMapPreviewRouteId: String?` |
| `App/ContentView.swift` | Tab 4: Servizi → Avvisi (AlertListView). Reazione a `pendingMapPreviewRouteId` |
| `Views/Orari/LineDetailView.swift` | Refactor completo, rimossa hero map |
| `Views/Orari/LineDetail/DirectionPills.swift` | **Nuovo** |
| `Views/Orari/LineDetail/LiveVehiclesSection.swift` | **Nuovo** (counter + cards) |
| `Views/Orari/LineDetail/VehicleLiveCard.swift` | **Nuovo** |
| `Views/Alerts/AlertListView.swift` | + filter bar (Tutti / Preferiti / Linea) |
| `Views/Home/HomeTab.swift` | + entry "Servizi" quick block |
| `Views/Mappa/MappaTab.swift` | + handler `pendingMapPreviewRouteId` (camera fit polyline) |

## Strings i18n (it/en/es)

Nuove chiavi da aggiungere a `Localizable.xcstrings`:
- `line_detail_in_service` → "%lld IN SERVIZIO" / "%lld IN SERVICE" / "%lld EN SERVICIO"
- `line_detail_no_buses_this_direction` → "Nessun bus in viaggio in questa direzione" / similar
- `line_detail_n_in_opposite_direction` → "%lld in direzione opposta" / etc.
- `vehicle_label_format` → "Vettura %@" / "Vehicle %@" / "Vehículo %@"
- `vehicle_next_stop` → "PROSSIMA FERMATA" / "NEXT STOP" / "PRÓXIMA PARADA"
- `alerts_filter_all` / `alerts_filter_favorites` / `alerts_filter_route`
- `line_favorite_a11y_add` / `line_favorite_a11y_remove`

## Empty states

**0 vetture in direzione attiva ma >0 in opposta:**
```
[icona bus.outline grigia]
Nessun bus in viaggio in questa direzione
2 in direzione opposta — tocca per passare →
```
Tap = switcha pill direzione.

**0 vetture totali:** sezione live counter+cards completamente nascosta.

**Tab Avvisi vuota:** usa già `EmptyStateView` esistente in AlertListView.

## Non in scope

- Android porting (separato, da fare dopo OK visivo iOS)
- TripUpdates per-stop ETA sulle card (ha già infrastruttura ma non lo metto in card v1)
- Filtro alert per stop/preferiti fermate (oggi solo banner home)
