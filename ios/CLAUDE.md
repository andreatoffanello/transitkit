# TransitKit iOS — Mappa per sessioni fresche

App iOS SwiftUI white-label per trasporto pubblico. Stack: SwiftUI + `@Observable` stores, MapKit, Metal shader, GTFS static (JSON da CDN) + GTFS-RT (protobuf via proxy). Identificatori progetto (UDID simulatore, scheme, bundle ID) sono nel CLAUDE.md root — non duplicati qui.

Root codice: `ios/TransitKit/Sources/`. Progetto Xcode: `ios/TransitKit.xcodeproj` (generato da `project.yml` via XcodeGen).

## Struttura cartelle

| Path | Contenuto |
|------|-----------|
| `App/` | Entry point (`TransitKitApp`), `ContentView` (TabView 5 tab), `DeepLinkRouter`, `Info.plist` |
| `Config/` | `OperatorConfig` — modello config.json operatore (theme, map, features, gtfs_rt, contact, fares) |
| `Models/` | `Schedule` (stops/routes/trips), `GtfsRtDecoder` (protobuf decoder custom) |
| `Services/` | `LocationManager` (CLLocation), `ScheduleLoader` (actor, CDN + disk cache) |
| `Stores/` | Fonte verità `@Observable` condivisa via `@Environment` |
| `Components/` | UI riutilizzabile (chip, card, badge, marquee, icone Lucide) |
| `Views/` | Schermate per tab: `Home/`, `Orari/`, `Mappa/`, `Info/`, `Settings/`, `Alerts/`, `Dev/` |
| `Shaders/` | `MapBackground.metal` — shader per sfondo |
| `Utils/` | `ColorUtils`, `HeadsignNormalizer` |
| `Resources/` | `config.json` dell'operatore bundled, asset catalog, dati GTFS seed |

## File grossi — splittare quando tocchi

Post-refactor (aprile 2026) i due view monstre principali sono stati splittati:

- `Views/Orari/StopDetailView.swift` — 495 LOC orchestrator + `Views/Orari/StopDetail/FullScheduleSheet.swift`, `ExpandedMapOverlay.swift`, `StopAlertsSection.swift` + `Components/DockIndicators.swift`, `FlowLayout.swift`
- `Views/Mappa/MappaTab.swift` — 383 LOC orchestrator + `MappaTab+Actions.swift` extension + `Components/MapSearchPill.swift`, `MapActiveRouteChip.swift`, `MapControlsColumn.swift`, `MapExpandedControls.swift`, `MapFloatingOverlays.swift`

Ancora da splittare quando li tocchi:

| File | LOC | Note / estraibile in |
|------|-----|----------------------|
| `Components/MarqueeLabel.swift` | 2160 | Legacy third-party (MIT). **Candidato sostituzione** con `MarqueeText.swift` nativo già presente — richiede design decision in cantiere dedicato. Non leggere intero — Grep mirato. |
| `Models/GtfsRtDecoder.swift` | 685 | Decoder protobuf manuale — splittare per message type (VehiclePosition, TripUpdate, Alert) |
| `Views/Orari/LinesListView.swift` | 514 | Splittare: header + search, row, sheet filtro direzione |
| `Views/Home/HomeTab.swift` | 514 | Splittare: hero card, nearby stops section, favorites section, alerts banner |

Pattern splitting: orchestrator `<Screen>View.swift` snello + sottocartella `<Screen>/` con subview + `Components/` per riusabili cross-feature. XcodeGen path glob `TransitKit/Sources` include tutto automaticamente — nessuna modifica a `project.yml` richiesta per nuovi file.

## Mappa — base UNICA SwiftUI (no UIKit)

**Tutte** le mappe usano SwiftUI `Map { }` (giu 2026: `TransitMapView` UIKit `MKMapView` eliminata). Pattern condiviso, parità Movete/DoVe:

- **Fermate** = `Annotation` dentro la `Map` (layer base). Vedi `MapLineFocusContent` (`MapContent` riusabile: polilinee + fermate).
- **Mezzi live + puck utente** = **overlay SwiftUI** sopra la `Map`, posizionati via `MapProxy.convert(coord, to:.local)`, NON `Annotation`/`UserAnnotation`. Componenti condivisi in `Views/Mappa/MapOverlayMarkers.swift` (`UserLocationDot`, `AnimatedVehicleMarker`). **WHY**: SwiftUI `Map` non ha `zPriority` e in 3D ordina per profondità → mezzi finirebbero sotto le fermate e `UserAnnotation()` viene resa nera/piatta. Gli overlay stanno sempre sopra e billboard (in piedi) anche in 3D.
- **Guardia obbligatoria** su ogni `proxy.convert` + `.position`: `point.x.isFinite && point.y.isFinite` — in 3D pitchato convert può dare NaN → `.position(NaN)` crasha SwiftUI (EXC_BAD_ACCESS).
- Mappa principale: `MappaTab` + `MappaTab+Actions.swift` (camera via `MapCameraPosition`, bridge da `mapRegion`/`is3D` con `onChange`). Mappa espansa: `Views/Orari/StopDetail/ExpandedMapOverlay.swift`.

## Store e Service

- `ScheduleStore` — fonte verità GTFS statico (stops, routes, departures computate). `Stores/ScheduleStore.swift`
- `VehicleStore` — GTFS-RT vehicle positions, poll 15s. `Stores/VehicleStore.swift`
- `AlertStore` — GTFS-RT service alerts, poll 60s, indice per stop_id/route_id. `Stores/AlertStore.swift`
- `FavoritesManager` — fermate preferite in UserDefaults. `Stores/FavoritesManager.swift`
- `SearchHistoryStore` — ricerche recenti per operatore. `Stores/SearchHistoryStore.swift`
- `LocationManager` — wrapper CLLocationManager. `Services/LocationManager.swift`
- `ScheduleLoader` — actor, scarica JSON da CDN + cache disco + freshness check. `Services/ScheduleLoader.swift`
- `DeepLinkRouter` — routing universal link / custom scheme. `App/DeepLinkRouter.swift`

Tutti gli store sono iniettati in `TransitKitApp.body` via `.environment(...)` e consumati con `@Environment(ScheduleStore.self)`.

## Pattern comuni

**Aggiungere una nuova schermata:**
1. Crea `Views/<Tab>/MyView.swift` con `@Environment` per gli store che servono.
2. Se è accessibile da un tab esistente: aggiungi `NavigationLink` o `.sheet` nella view parent del tab.
3. Se è un nuovo tab: aggiungi in `App/ContentView.swift` (TabView con 5 tab attualmente).

**Aggiungere un nuovo Store:**
1. `Stores/MyStore.swift` con `@Observable @MainActor final class`.
2. Istanzia in `TransitKitApp` (`@State`), inietta in ContentView con `.environment(...)`.
3. Configura post-init via metodo tipo `configure(with: OperatorConfig)` se serve.

**Aggiungere un componente riutilizzabile:**
- Drop-in in `Components/`, SwiftUI `View` puro, niente `@Environment` diretto su store (pass via init per riuso). Esempi di riferimento: `StopCard`, `DepartureRow`, `FilterChip`.

**Usare config operator:**
- Leggi `operatorConfig.theme.accent`, `operatorConfig.map.defaultCenter`, `operatorConfig.features.showAlerts` ecc.
- Config passato come parametro a `ContentView(config:)` e accessibile nelle view figlie via init o environment custom.

## White-label flow

1. Build setting `OPERATOR_ID=appalcart` (o altro) determina quale `Resources/<op>/config.json` viene bundlato.
2. `TransitKitApp.init` → `OperatorConfig.load()` legge il JSON dal bundle e parsa in `OperatorConfig` (`Config/OperatorConfig.swift`).
3. `ScheduleStore.configure(with: operatorConfig)` imposta URL CDN, filtri, GTFS-RT endpoints.
4. `AppTheme.configure(from: operatorConfig.theme)` applica accent, colori, font ramp (cerca `AppTheme` in `Utils/`/`Components/`).

## Realtime

Tutti gli endpoint GTFS-RT (vehicle-positions, trip-updates, alerts) puntano a `https://rt.transitkit.app/{operator_id}/{feed}.pb` — configurato nei file `Resources/**/config.json` (campo `gtfs_rt`). `VehicleStore` e `AlertStore` consumano da lì. **NEVER** hardcodare URL upstream dell'operatore — vedi CLAUDE.md root sezione proxy.

## Cosa NON fare

- **NEVER** leggere `Components/MarqueeLabel.swift` intero (2160 LOC, legacy third-party, candidato rimozione) — usa Grep mirato se devi.
- **NEVER** gonfiare `Views/Orari/StopDetailView.swift` (495 LOC orchestrator) o `Views/Mappa/MappaTab.swift` (383 LOC orchestrator) — aggiungi sottofile in `StopDetail/` o `Components/`, non inline.
- **NEVER** usare `booted` come target `xcrun simctl` — UDID pinned nel CLAUDE.md root.
- **NEVER** inventare pathData SVG per icone — usa `LucideIcon` (`Components/LucideIcon.swift`) con nome da libreria.
- **NEVER** usare `font-size`-equivalenti per dimensionare icone SVG — sempre `.frame(width:height:)` espliciti.
- **NEVER** chiamare upstream GTFS-RT diretti — passare sempre da `rt.transitkit.app`.
- **NEVER** mostrare logo/nome operatore (`config.name` "AppalCART") nella loading splash (`TransitKitApp.loadingView`) — sempre bus app + `config.brandName` ("AppalRider"). Rischio impersonazione/rejection. NB: l'imageset `OperatorLogo` è il bus dell'app (non l'operatore); il logo operatore è `SourceOperatorLogo`.
