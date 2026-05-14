# iOS → Android — Prospetto modifiche da allineare

Riassunto delle modifiche fatte in TransitKit iOS nelle ultime sessioni, organizzate per area. Ogni voce include path file iOS, motivazione e cosa cercare lato Android per la parità.

---

## 1. Mappa — Controlli unificati come pill singolo

**iOS:**
- `Views/Mappa/Components/MapControlsColumn.swift`
- `Views/Mappa/Components/MapExpandedControls.swift`

Pill verticale unico (cornerRadius 22, material) con celle 44×44 separate da divider 28×0.5pt. Bottoni: **2D/3D** (text label), **recenter** (navigation arrow), **reset bearing** (compass), **expand/close**. `showsResetBearing` opzionale — appare solo quando `abs(currentHeading) > 1°`. Stesso componente usato da Mappa tab e da `Views/Orari/StopDetail/ExpandedMapOverlay.swift`.

**Android:** unificare i controlli mappa in un unico Composable a pill verticale (al posto di FAB separati). Cella 44dp con divider sottile. Stesso pattern in MappaTab e nella fermata espansa. Reset bearing solo se `mapboxMap.cameraState.bearing` ≠ 0.

---

## 2. Mappa fermata espansa — Rimossi "Apri in mappe" e close inline

**iOS:** `Views/Orari/StopDetail/ExpandedMapOverlay.swift`

Rimossi: bottone `open_in_maps`, confirmation dialog (Apple/Google/Waze), bottone X in alto a destra. Sostituiti dal close FAB centrale di `MapExpandedControls`. Il "Naviga verso" resta solo nel Menu del toolbar di `StopDetailView` (in `.confirmationDialog`).

**Android:** togliere "Apri in mappe" dall'overlay mappa espansa. Il close deve essere il bottone del componente unificato. Eventuale navigazione esterna resta solo nel menu della toolbar.

---

## 3. Mappa — Pallino utente sempre blu e sopra tutto

**iOS:** `Views/Mappa/TransitMapView.swift`

- `map.tintColor = UIColor.systemBlue` in `makeUIView` (altrimenti era tintato col verde `AppTheme.accent` → appariva nero/scuro)
- `mapView(_:didAdd views:)` delegate: per ogni `MKUserLocation` setta `zPriority = .max` e `displayPriority = .required`
- Vehicle annotation `zPriority` abbassato da `.max` a `900` → sotto user, sopra stop (100)

**Android:** verificare il LocationComponent Mapbox — il dot deve essere blu di sistema, non tintato dal theme operatore. Il puck deve renderizzare al di sopra di stop + vehicle annotations. Su Mapbox: `locationComponentOptions.foregroundDrawable` esplicito + z-ordering tramite layer order.

---

## 4. Mappa picker planner — Gap dot/stem eliminato

**iOS:** `Views/Planner/LocationPickerMap.swift`

L'halo `Circle()` 48×48 era dentro lo ZStack del dot → occupava layout della VStack creando gap di 13pt tra fondo dot e top stem. Spostato come `.background` (non influenza layout). Stem si allunga da 12 → 20pt quando camera in movimento per coprire l'8pt di lift del dot.

**Android:** stessa logica nel pin centrale di `PlannerLocationPickerMap`. Halo deve essere reso fuori dal layout flow (es. `Box` overlay con `Modifier.fillMaxSize()` o `drawBehind`). Stem deve allungarsi per restare connesso al dot durante drag.

---

## 5. StopDetailView e LineDetailView — Mappa hero su tutte le versioni

**iOS:** `Views/Orari/StopDetailView.swift`, `Views/Orari/LineDetailView.swift`

Rimosso il gate `if #available(iOS 26, *)` che nascondeva il map hero su iOS 18. La `Map(position:)` SwiftUI è iOS 17+ quindi compatibile. Su iOS 18 `adaptiveNavBarBackground()` applica `.regularMaterial` quindi la nav bar resta leggibile sopra la mappa.

**Android:** Il map hero deve essere visibile sempre, non solo su Android 14+ o simili. Se c'era un gate API level, rimuoverlo.

---

## 6. Vehicle annotation pin — Gap badge/tail/ring eliminato

**iOS:** `Views/Mappa/VehicleAnnotationView.swift`

Bug originale: gap di 8pt tra badge pill, tail e ring dot a tier street. Fix:
- `centerY` del Canvas è conditional: `showBadge ? CGFloat(ringR) : size.height / 2`
- Balance spacer condizionale (solo con badge): `Spacer().frame(height: 11)`
- Math: in host 100×100 con `centerOffset = .zero`, dot deve stare a y=50; con badge pill ~20pt, spacer = pill_height − 9 = 11pt

**Android:** Verificare il rendering del vehicle marker custom (VehicleAnnotationView). Badge sopra → tail (8×5dp triangolo) → ring/dot devono essere flush. Se l'host è 100×100dp con anchor center, il dot deve cadere al centro per matchare la coordinata.

---

## 7. LineDetailView nav bar iOS 26 — Adaptive contrast

**iOS:** `Views/Orari/LineDetailView.swift`

Rimosso il modifier `DarkNavBarOnIOS26` che forzava `.toolbarColorScheme(.dark, for: .navigationBar)` su iOS 26 → tutti gli elementi nav bar diventavano bianchi indipendentemente dallo sfondo. Ora il glass nativo iOS 26 adatta automaticamente i colori in base al contenuto sottostante.

**Android:** Per Material 3 TopAppBar, i colori `titleContentColor` / `navigationIconContentColor` devono usare `onSurface` (adattivo) invece di un colore fisso. Se la barra è translucida sopra la mappa, evitare hard-coded white.

---

## 8. LineDetailView — Singolo pulsante expand

**iOS:** `Views/Orari/LineDetailView.swift`

C'erano due bottoni `maximize2`: uno nella toolbar e uno nell'overlay del map hero (iOS 26-only). Rimosso quello nell'overlay. Singolo expand nel toolbar. Hero map overlay ora ha solo il `LiveBadge` chip (leading, condizionale a `liveCount > 0`).

**Android:** Stesso problema da verificare in `LineDetailScreen.kt`. Espansione mappa solo dal toolbar, non dall'overlay sulla card mappa.

---

## 9. HomeTab — Chip "Ora" allineata a sinistra

**iOS:** `Views/Home/PlannerHomeBox.swift`

`HStack { WhenChipsRow(); Spacer() }` invece di `HStack { Spacer(); WhenChipsRow() }`. Chip parte da leading edge, sotto il card "Partenza/Destinazione".

**Android:** `HomePlannerBox.kt` o equivalente — la riga delle chip "quando" sotto i campi origin/destination deve essere `Arrangement.Start`.

---

## 10. HomeTab — Section header con icona e sentence case

**iOS:** `Views/Home/HomeTab.swift` — funzione `sectionHeader(_:icon:)`

- Aggiunto parametro `icon: LucideIcon? = nil`
- Font: `.system(size: 15, weight: .semibold)`, foregroundStyle `AppTheme.textPrimary`
- Icon: `.sized(14)`, `AppTheme.textSecondary`
- Niente `.textCase(.uppercase)` (era UPPERCASE prima)
- Nuovi keys i18n: `home_section_favorites` ("Fermate preferite"), `home_section_nearby` ("Fermate vicino a te"). Star icon per preferiti, mapPin per nearby

**Android:** Aggiornare gli header delle sezioni Home in `HomeScreen.kt`. Niente uppercase, sentence case con icona inline (12-14dp). Usare le nuove chiavi di stringhe ("Fermate preferite", "Fermate vicino a te").

---

## 11. HomeTab — Sezioni differenziate (preferite vs vicine)

**iOS:** `Views/Home/HomeTab.swift`

- **Preferite**: card piena `stopCard` con header transit icon + nome + elenco partenze
- **Vicine**: SINGOLA card glass con righe compatte `nearbyStopRow` (transit icon 14×14 + nome + distanza + chevron) → shortcut a `StopDetailView`, niente partenze

**Android:** Differenziazione visiva netta. Preferite con partenze inline; nearby compatte come liste shortcut.

---

## 12. TimeDisplay — Hours/minutes + wrap-around + threshold

**iOS:** `Components/TimeDisplay.swift`, `Components/DepartureRow.swift`

- Nuovo caso `.hoursMinutes(Int, Int)` → render "1h 23'" (h bold subheadline, m footnote semibold textSecondary)
- Init `TimeDisplay(departure:now:relativeThreshold:)` con default 60
- Home preferite usa `relativeThreshold: 1440` → sempre relativo (mai orario fisso)
- Wrap-around: `if diff < -60 { diff += 1440 }` → partenze "passate" più di 1h sono trattate come prossima occorrenza il giorno successivo
- Nuovo param `liveDot: Bool` su TimeDisplay → pallino verde inline a sinistra di "3'" / "17:42" dentro lo stesso frame minWidth=52 (no gap tra dot e digit)

**Android:** Aggiornare il componente TimeDisplay/DepartureTimeText. Threshold configurabile; case hours+minutes per departures fra 1h e 24h; wrap-around per gestire schedule del giorno successivo. Live dot inline col countdown.

---

## 13. DepartureRow — Live dot inline col countdown

**iOS:** `Components/DepartureRow.swift`

Pallino live era posizionato come elemento separato fra `destinationStack` e `timeStack` → appariva fluttuante a metà riga. Ora è dentro `TimeDisplay` (via `liveDot: true`) inline col countdown: "● 3'" — nessun gap.

**Android:** Stessa modifica in `DepartureRow.kt`. Il pallino live va affiancato direttamente al countdown (es. "● 3'" baseline-aligned), non in colonna separata.

---

## 14. LineBadge — Fix clipping per nomi lunghi (4-5 char)

**iOS:** `Components/LineBadge.swift`

Bug: `.fixedSize(horizontal: true)` era solo sul Text interno. Il parent (es. `destinationStack` con `layoutPriority(1)` + `maxWidth: .infinity`) schiacciava la cornice del badge → il `RoundedRectangle` background restava ristretto e il testo veniva clippato ("SFPLS" → "FPLS"). Fix: `.fixedSize(horizontal: true, vertical: false)` applicato all'ESTERNO del badge (dopo background).

**Android:** Verificare che il `LineBadge` Composable usi `Modifier.wrapContentWidth()` o `Modifier.width(IntrinsicSize.Max)` e che il container parent non lo schiacci con `weight`. Test con nomi a 5 char tipo "SFPLS", "NOPOP".

---

## 15. Home — Operator info card con branding reale

**iOS:**
- `Resources/Assets.xcassets/SourceOperatorLogo.imageset` (nuovo asset)
- `Views/Home/HomeTab.swift` — `operatorInfoCard`

Sostituito il gradient + LucideIcon.bus con `Image("SourceOperatorLogo")` (logo AppalCART vero da `shared/operators/appalcart/logo.png`). Testo card: `config.name` ("AppalCART") invece di `config.brandName` ("AppalRider") → la card è l'attribuzione alla fonte dati, non al brand white-label dell'app.

**Android:** Aggiungere asset `source_operator_logo.png/webp` in `res/drawable-*` (dalla stessa sorgente). Card info operatore mostra `config.name` con logo reale, non il brand white-label.

---

## 16. LocationPrimerView — Background readability

**iOS:** `Views/Home/LocationPrimerView.swift`

Sfondo era: `Image("OperatorBackground").opacity(0.18).overlay(Color.black.opacity(0.15))` → testo poco leggibile. Fix:
- Base solida `AppTheme.background`
- Ghost map a `opacity(0.08)` (era 0.18 + overlay nero)
- `RadialGradient` centrale (start 60, end 360) da `AppTheme.background.opacity(0.85)` a 0 → "isola di leggibilità" attorno a icona/titolo
- Niente overlay scuro

**Android:** Stesso pattern nel primer location di Android. Background mappa molto attenuato (≤10% opacity), radial fade verso `surface` color al centro per garantire contrasto del testo titolo/body.

---

## 17. LinesListView — Live count badge per linea

**iOS:** `Views/Orari/LinesListView.swift`

Chip statico (no animazione, no material) accanto alla chevron quando `vehicleStore.liveCount(forRouteId:) > 0`:
```
● {count} live
```
Capsule verde tenue (`AppTheme.realtimeGreen.opacity(0.12)`). Inietta `@Environment(VehicleStore.self)`. **Importante**: usato badge STATICO (no pulse `repeatForever`) perché dentro `LazyVStack` con re-render ogni 15s causava layout loop con animazioni infinite.

**Android:** Aggiungere live badge nelle righe di `LinesListScreen.kt`. Versione statica (no animazione pulse) per evitare instabilità in LazyColumn con re-compose periodici. Verde tenue, posizione: prima della chevron.

---

## 18. Favorite star — Filled + brand color quando attivo

**iOS:**
- `Resources/Assets.xcassets/Icons/lucide-star-filled.imageset` (nuovo, path Lucide ufficiale con `fill="currentColor"`)
- `Components/LucideIcon.swift` — case `starFilled`
- `Views/Orari/StopDetailView.swift` — toolbar favorite button

Stella outline vs filled non era distinguibile (cambiava solo colore). Ora:
- Favorito: `LucideIcon.starFilled` riempita con `AppTheme.accent`
- Non favorito: `LucideIcon.star` outline `.secondary`
- `.contentTransition(.symbolEffect(.replace))` per swap fluido

**Android:** Stessa cosa con il drawable `ic_star` (outline) vs `ic_star_filled`. Tint con colore brand quando attivo. Animazione di swap (es. `AnimatedContent` o crossfade).

---

## 19. iOS-specific cleanup (non rilevante Android)

Cose strettamente iOS che NON serve portare:
- `DarkNavBarOnIOS26` removal
- `adaptiveNavBarBackground()` modifier
- `IgnoreTopSafeAreaOnIOS26` modifier
- `MKUserLocationView` zPriority handling
- `MapCameraPosition`/`MKMapCamera` differences (Android usa Mapbox SDK)

---

## Convenzioni globali da rispettare

- **Nessuna animazione `repeatForever` dentro liste lazy** che rerendono periodicamente (causa layout instability). Live badge statico nelle liste, animato solo in punti singoli (es. detail view hero)
- **Icone**: niente pathData inventati. Solo Lucide ufficiali o asset operatore
- **Touch target** ≥ 44pt / 48dp
- **Sentence case** per header e CTA, NO uppercase forzato
- **Tint user location** = sempre blu sistema, non operator accent

---

## File modificati (iOS) — quick reference

```
Components/
  LineBadge.swift                 (fixedSize esterno)
  TimeDisplay.swift               (hoursMinutes + liveDot + threshold + wrap-around)
  DepartureRow.swift              (live dot inline via TimeDisplay)
  LucideIcon.swift                (case starFilled)

Views/Home/
  HomeTab.swift                   (sectionHeader+icon, nearbyStopRow compact, operatorInfoCard logo+name, time threshold 1440)
  PlannerHomeBox.swift            (WhenChipsRow leading)
  LocationPrimerView.swift        (background readability)

Views/Orari/
  LineDetailView.swift            (rimosso DarkNavBarOnIOS26, expand singolo, map hero universal)
  LinesListView.swift             (live badge statico per linea)
  StopDetailView.swift            (map hero universal, star filled, confirmationDialog open_in_maps)
  StopDetail/ExpandedMapOverlay.swift   (MapExpandedControls + niente open_in_maps + 3D toggle)

Views/Mappa/
  TransitMapView.swift            (tintColor blue, user location zPriority max, vehicle 900)
  VehicleAnnotationView.swift     (gap badge/tail/ring fix)
  MappaTab.swift                  (state is3D, nuovi callback)
  Components/MapControlsColumn.swift     (pill design)
  Components/MapExpandedControls.swift   (pill design + 3D + reset bearing condizionale)

Views/Planner/
  LocationPickerMap.swift         (halo background, stem stretch)

Resources/
  Localizable.xcstrings           (home_section_favorites, home_section_nearby, map_switch_to_2d/3d)
  Assets.xcassets/SourceOperatorLogo.imageset (nuovo)
  Assets.xcassets/Icons/lucide-star-filled.imageset (nuovo)
```
