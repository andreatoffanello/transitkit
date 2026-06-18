# Port DoVe → transit-engine — Gap Map

Riferimento per portare in transit-engine i miglioramenti maturati nella sezione
**muoviti** di DoVe (`~/GitHub/civici`) nella settimana 2026-06-15 → 2026-06-18.
Generato 2026-06-18. Non è una spec: è la mappa gap-by-gap per decidere l'ordine
di lavoro. Ogni cluster diventerà una spec separata quando lo si affronta.

Tre cluster × tre piattaforme (iOS native, Android native, web PWA).

## Findings che cambiano il quadro

- **Le card mappa mezzi esistono già come card, NON come sheet** su entrambe le
  native. `ios/.../Views/Mappa/VehicleDetailSheet.swift` è mal-nominato (è renderizzato
  nello ZStack di `MappaTab`, non in `.sheet`); Android usa `AnimatedVisibility` in
  `MappaScreen.kt`. La regola "no bottom sheets" è già rispettata → polish + pulsanti
  mancanti, non un rebuild.
- **Il gap funzionale più grosso è in Cluster B**: il ritardo RT è già fetchato e
  salvato sulle native (`VehicleStore.delayByTripId`) ma **non viene mai applicato a
  countdown e orari mostrati** — le partenze native mostrano sempre l'orario di
  schedule. **Il web lo fa già correttamente.** Più vicino a un bug di correttezza
  che a polish.
- **L'update-check è 100% greenfield** in transit-engine — non esiste nulla.
- **I mezzi "teletrasportano"** su entrambe le native (nessuna interpolazione glide).
- **La mappa web non esiste** — `web/pages/map.vue` è un placeholder "coming soon",
  quindi tutto il Cluster A su web è from-scratch (L).

## Commit DoVe di riferimento

Cluster A: `34e5c4ae` (LiveMapCard + glide), `597847a3` (box mezzo-live + tap-linea
Android), `36a8ba15` (tap palina → dettaglio filtrato), `02eba0ee` (2D-default/3D
opt-in), `197ed0b4` (mezzi RT con fallback ancorato).
Cluster B: `302f48a6` (strip mezzi live line detail), `fafbb50e` (trip detail pill
stato live), `514acb0a` (badge LIVE + countdown col ritardo), `07e7e6c8` (badge LIVE
su partenze GTFS-RT), `de007911` (GTFS-RT multioperatore).
Cluster C: `16d1c648` (banner soft store-driven iTunes/Play), `50ed14a0` (soft non
bloccante), `442bc0a9` (force-update 5 lingue), `f4c74bec` (config appUpdate),
`39f55038` (GPS on-demand), `c91cccf5`/`77d13dc1` (rendering centralizzato),
`5c7b9107` (esplosione banchine bitmap, no ANR), `4f0734f5` (anti-ANR teardown).

---

## ⚠️ Vincolo scoperto: mappa disabilitata per appalcart

`shared/operators/appalcart/config.json` → `features.enableMap: false`. La tab
Mappa è **spenta** nello stato canonico di appalcart. Conseguenze:
- **Cluster A** (card mappa, glide, follow/linea/corsa) non è validabile finché
  la mappa non viene riabilitata. Decisione utente necessaria: abilitare la
  mappa (Cluster A la rende pronta) o tenere Cluster A in pausa / testarlo su
  altro operatore.
- Il "Vedi su mappa" del box corsa (Cluster B) punterebbe a una tab assente →
  in caso, portare il box senza quella CTA finché la mappa è off.

## Avanzamento (loop porting)

- ✅ **B-data ritardo RT sui countdown — iOS** (`de79c75`): filtro plausibilità
  `-300..1800s`, countdown+orario shiftati e coerenti, pallino LIVE invariato
  (presenza veicolo). QA primary 4/4 PASS.
- ✅ **B-data ritardo RT sui countdown — Android** (`c21da69`): mirror iOS,
  additivo (campi RT prima mai popolati), pallino LIVE ora a parità. QA primary
  4/5 PASS — unico FAIL = verde condiviso soon/live (ereditato da DoVe, design
  decision aperta, non regressione).
- 🔶 **Decisione aperta:** differenziare il verde soon vs live? (cross-platform)
- 🔶 **Decisione aperta:** abilitare la mappa per Cluster A?
- ▶️ **Prossimo:** Cluster C — update-check (indipendente da mappa/colore).

## Cluster A — Card mappa mezzi + pulsanti (follow/linea/corsa) + glide

**DoVe:** al tap su un mezzo compare una card SwiftUI flottante nello ZStack (non
sheet): line badge + headsign direzione + progress a 3 stati (fermo/in transito/in
arrivo) + chip metadata. In basso 3 azioni: **Follow** icon-only (camera segue il
mezzo), **Linea** (attiva `activeLineForMap` — filtro mappa, NON push), **Corsa**
(push TripDetail). Marker con glide 900ms linear, per-vehicle state. Mappa apre in
**2D** (pitch 0), 3D opt-in via toggle.

| Sub-feature | iOS | Android | Web |
|---|---|---|---|
| On-map card (non sheet) | ✅ `VehicleDetailSheet.swift` (mal-nominato) | ✅ `MappaScreen.kt` `VehiclePreviewContent` | ❌ nessuna mappa |
| Follow (camera segue) | ✅ icon+label, `MappaTab+Actions.swift` | ✅ `LaunchedEffect` fly-to | ❌ |
| **Linea (filtro mappa)** | ❌ route si auto-attiva ma è silente | ❌ idem | ❌ |
| Corsa (apri TripDetail) | ✅ `fullScreenCover` | ✅ `onOpenTripDetail` | ❌ |
| **Glide marker** | ❌ teleport — serve tween CADisplayLink (L) | ❌ teleport — serve migrazione a SymbolLayer+`VehicleMarkerAnimator` (L) | ❌ |
| **2D-default / 3D opt-in** | ❌ default `is3D = true` | ⚠️ apre 3D quando localizzato (pitch 50) | ❌ |

**Pesanti:** glide marker (L su entrambe), intera mappa web (L).
**Decisione di design su "Linea":** in DoVe è filtro mappa (`activeLineForMap`), non
push a `LineDetailView`. Va deciso quale semantica si vuole in transit-engine.

### Rischi di porting (Cluster A)
- **Map SDK diverso su iOS.** DoVe muoviti usa Mapbox; transit-engine iOS usa
  MapKit (`MKMapView` in `UIViewRepresentable`, `TransitMapView.swift`). Il glide
  `AnimatedVehicleMarker` di DoVe è portabile a MapKit ma il tap-handler stop
  (Mapbox `queryRenderedFeatures`) non ha equivalente — implementazione distinta.
- **Android: ViewAnnotation vs SymbolLayer.** DoVe ha migrato i mezzi da
  `ViewAnnotation` a `SymbolLayer + VehicleMarkerAnimator` proprio perché
  ViewAnnotation non interpola (teleport, rischio ANR a scala). transit-engine usa
  ancora `ViewAnnotation` (`MappaMapLayers.kt`). Glide = adottare il path
  SymbolLayer (scelta giusta) o forzare animatable dentro la ViewAnnotation (fragile).
- **TripDetail "mezzo live box" assente** (commit `597847a3`): box in cima a
  TripDetail con back-link "Vedi su mappa". Richiede di propagare `vehicleId` nella
  navigazione trip (oggi non passato sul path Android).
- **Policy 3D divergente:** DoVe ha standardizzato 2D-all'apertura/3D-opt-in
  (`02eba0ee`); transit-engine apre 3D. La motivazione DoVe (3D a scala-città = rumore)
  vale anche per reti bus.
- **Mappa web = foglio bianco:** nessun SDK integrato. Scelta SDK (Mapbox GL JS /
  MapLibre / Leaflet) + integrazione realtime prima di qualsiasi sub-feature.

---

## Cluster B — Viste corse & linee con mezzi live

**DoVe:** singleton `RealtimeProvider` che fa il join VP→TU e pubblica indici, tra
cui `rtDelaysByRouteStop` (`"routeId|gtfsStopId" → [(sched_min, delay_min)]`) per
correlare partenze statiche e trip-update **bypassando il mismatch di namespace
trip_id**. Il modello `Departure` porta `rtDelayMinutes`, `liveMinutesUntil()`,
`liveTime`, `isRealtime` → una sola fonte di verità per departure row / trip detail /
strip live. Filtro `reliableDelay` (−5…+30 min) anti ghost-trip (countdown fantasma
+93 min). `isRealtime` guida il badge LIVE. Box `liveVehicleBox` in trip detail solo
se mezzo live risolto (via `liveVehicleId` passato da "Apri corsa", o fallback
`vehicleByTripId`).

| Sub-feature | iOS | Android | Web |
|---|---|---|---|
| Strip mezzi live in line detail | ✅ `LineDetailView` `LiveVehiclesSection` | ✅ `LineDetailScreen` `LazyRow` | ❌ (web non consuma feed VP) |
| **Box stato live in trip detail** ("In transito / +N min" + Vedi su mappa) | ❌ mancante (M) | ❌ mancante (M) | n/a (no trip page) |
| Badge LIVE in departure row | ⚠️ pulsing dot, non capsula "LIVE" | ⚠️ da verificare | ✅ fatto (dot) |
| **Countdown col ritardo RT applicato** | ❌ **full gap** (M) — `timeState` ignora il delay | ❌ **full gap** (M) — `tripDelays` non applicato | ✅ fatto |
| Orario schedule-vs-realtime | ❌ mostra solo schedule | ❌ idem | ✅ strikethrough fatto |
| Filtro `reliableDelay` anti ghost-trip | ❌ mancante | ❌ mancante | ❌ mancante |
| Join route+stop+time (`rtDelaysByRouteStop`) | ❌ solo trip_id (L) | ❌ idem (L) | ❌ idem |

**Fondamenta:** ritardo RT applicato ai countdown + filtro `reliableDelay`. È il
vero valore utente e sblocca box stato live e ritardo sulla card. Urgenza minore per
AppalCART (single-operator, trip_id puliti) ma è la spina dorsale di correttezza.

### Rischi di porting (Cluster B)
- **`reliableDelay` assente ovunque** in transit-engine. Senza, operatori che
  riusano trip_id intraday producono countdown assurdi (+93 min osservati in DoVe).
- **Join route+stop+time è il contributo core di DoVe.** `delayByTripId` assume
  stesso namespace trip_id statico/RT. Regge per feed single-source (AppalCART pulito)
  ma rompe se l'RT emette trip_id da service_id/block diversi. Item più complesso (L).
- **iOS `TripDetailView` manca il pass-through `liveVehicleId`.** DoVe passa
  `liveVehicleId` come parametro nav aprendo la corsa da una card mezzo, bypassando il
  join trip_id fragile. transit-engine porta solo `(departure, fromStop)`.
- **Android `TripDetailViewModel` ha `hasLiveVehicle` ma non `liveDelayMinutes`.** Il
  box stato live non si rende senza prima portare il calcolo `reliableDelay`.
- **Feed vehicle-positions web inesplorato.** `useRealtime` decodifica solo
  `TripUpdate`. Strip mezzi live web richiede estendere il decode a `VehiclePosition`.
- **iOS `DepartureRow` ignora `VehicleStore.delay(forTripId:)`.** Il delay c'è già ma
  `ScheduleStore.timeState(for:)` non lo legge. Fix più piccolo (single-file) ma da
  decidere se applicarlo subito (trip_id join, rischio stale) o dopo il route+stop index.

---

## Cluster C — Sotto al cofano (update-check + performance)

**DoVe:** update split in due meccanismi indipendenti — *force gate*
(`appUpdate.ios.minVersion` / `android.minVersionCode` + `force:true` → schermo
bloccante `ForceUpdateView`/`ForceUpdateScreen`) e *soft banner* dismissibile
per-versione (iTunes Lookup su iOS, Play In-App Updates su Android), copy in 5 lingue
nel config. Performance: GPS refcount on-demand; rendering fermate/mezzi estratto in
componenti condivisi; esplosione banchine da `ViewAnnotation` a `Bitmap` Canvas +
`style.addImage` (no inflation main-thread); `MapSurfaceTeardownGuard` che drena la
render queue prima della distruzione (no `destroyCondition.await()` block); glide
mezzi 900ms su entrambe.

### C1 — Update check (interamente greenfield, port pulito)

| Sub-feature | iOS | Android | Web |
|---|---|---|---|
| Discovery ultima versione | ❌ iTunes Lookup (M) | ❌ Play In-App Updates (M) | ❌ |
| Config schema `appUpdate.*` + `message`/`whatsNew` | ❌ assente (S) — riusa infra `locale` | ❌ idem | ❌ |
| Soft banner dismissibile | ❌ (M) | ❌ (M) | ❌ |
| Force-update screen | ❌ (S) | ❌ (S) | ❌ |
| i18n — **EN + IT** completi (no altre lingue per ora) | ⚠️ infra locale c'è, servono key | ⚠️ idem | — |
| Dove parte il check | ⚠️ `scenePhase` esiste, manca la call | ⚠️ `repeatOnLifecycle` esiste, manca la call | — |

### C2 — Performance / stabilità

| Sub-feature | iOS | Android | Web |
|---|---|---|---|
| GPS on-demand | ✅ già one-shot (`LocationManager.swift`), refinements minori | ❌ legacy `LocationManager` inline in `HomeScreen.kt`, non Fused (M) | — |
| Rendering fermate/mezzi centralizzato | ✅ già centralizzato (MKMapView Coordinator) | ⚠️ ok per 1 sola map surface, S pre-emptive | — |
| Bitmap SymbolLayer anti-ANR (pin) | n/a | ✅ `StopMarkerBitmap.kt` già usa Bitmap+`addImage` | — |
| **Anti-ANR teardown guard** | n/a | ❌ **mancante** — rischio block su back-nav mappa (M) | — |
| Glide mezzi | ⚠️ verificare CADisplayLink in `VehicleAnnotationView.swift` | ❌ probabilmente mancante (M) | — |

### Vincolo localizzazione (tutti i cluster)
**Solo EN + IT per ora**, ma **completi al 100% — zero stringhe hardcoded o non
localizzate**. DoVe porta copy in 5 lingue; in transit-engine si portano solo EN e IT,
e ogni nuova stringa UI deve avere entrambe le key (niente literal in View/Composable).

### Rischi di porting (Cluster C)
- **iTunes Lookup / Play packageName per-operatore.** DoVe hardcoda un solo
  bundleId/package. transit-engine è white-label (`com.transitkit.{op}`): parametrizzare
  da `OPERATOR_ID`/`config.json`, facile da sbagliare copiando da DoVe.
- **Config `appUpdate` via CDN vs bundled.** Il force-gate che legge config remoto
  deve passare da `DataSyncManager`, non `CDN_BASE` diretto (gotcha 2 CDN:
  transitkit reale vs transitkit-data mock).
- **Bitmap marker su main thread.** `StopMarkerBitmap` chiamato in `MapEffect`
  (main). Per operatori con molte fermate (RFTA ~300) il costo one-time su cambio
  colore può far frame-drop: valutare pre-generazione su `Dispatchers.Default`.
- **Legacy LocationManager vs Fused.** Migrare a `FusedLocationProviderClient`
  aggiunge `play-services-location` (attenzione a target non-GMS).
- **Force-update + Play IMMEDIATE flow.** Il flow IMMEDIATE prende l'Activity:
  gestire l'activity result con `ActivityResultLauncher` per non lasciare schermo
  bianco dopo l'update (specie con deep-link in back-stack).
- **Audit glide iOS CADisplayLink.** Verificare se il tween esistente in
  `VehicleAnnotationView.swift` copre già il glide 900ms prima di portarlo; se sì il
  gap iOS è chiuso e resta solo l'`VehicleMarkerAnimator` Android.

---

## Ordine consigliato (quando si scriveranno le spec)

1. **B-data** (S/M, alto valore): ritardo RT applicato ai countdown + `reliableDelay`
   + polish badge LIVE. Fixa il gap di correttezza, alimenta tutto il resto. Solo
   native (web già a posto).
2. **A** (headline): glide marker + pulsante Linea + 2D-default + box mezzo-live in
   TripDetail. Lavoro mappa/interazione, ora capace di mostrare il ritardo live.
3. **C update-check**: indipendente, isolato, basso rischio — anche in parallelo o
   per primo come quick win. Includere anti-ANR teardown Android + Fused GPS.
4. **Mappa web (Cluster A su web):** progetto a sé, from-scratch (L), nessun prior
   art DoVe.
