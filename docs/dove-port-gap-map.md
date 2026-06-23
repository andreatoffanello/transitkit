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

## Nuovi candidati da DoVe + Movete (scan 2026-06-20)

Portabili in transit-engine, in ordine di priorità. (Skip Venezia-specifico: banchine,
feste/sagre, vaporetti, CMS, farmacie.)

1. ✅ **PERF — loop 60fps eliminati — iOS** (`32e015c`): alone statico (pulse solo
   sul selezionato) + shader Home 30fps + gate per tab. Idle = zero loop 60fps.
   (Android: coperto dall'alone static della migrazione SymbolLayer held.)
2. **Planner (MOTIS)** — ✅ scorciatoie Casa/Lavoro **iOS** (`bd2722d`, Movete
   `7e164c8570`); ⏳ parità Android; ⏳ indicatore RT per-leg "In orario" nel
   dettaglio viaggio (`2a6225a8b1`).
3. **Ricerca smart** fermate/linee — Movete `8cc1a9fe74` (token order-free, abbrevi).
   NB utente: dizionario abbreviazioni **Boone/operatore-specifico, non Venezia**
   ("P.le" non esiste qui) → portare il meccanismo + dict adatto/config-driven.
4. **StopDetail** — partenze raggruppate per direzione always-on (Movete `133091db2f`);
   overlay selezione linea sulla mappa StopDetail (Movete `915dca364c`); ritardo sotto
   orari nella sequenza-fermate linea + ordine/scroll (DoVe `983d3721`).
5. **Robustezza** — ✅ sanitize C1/CP1252 nel testo avvisi iOS+Android (`b7ec41d`,
   Movete `c55183bfb3`); ⏳ conditional GET sul refresh dati (Movete `dddf21da49`).

### Stato porting non-vehicle (sessione 2026-06-20)
Fatti: perf 60fps iOS (`32e015c`), planner Casa/Lavoro iOS (`bd2722d` + fix icona
`e80a8bb`), sanitize avvisi iOS+Android (`b7ec41d`).
Held (aspetta bus a Boone): glide Android SymbolLayer (5 file non committati).
Rimane (non-vehicle): parità Android planner Casa/Lavoro · planner per-leg RT "In
orario" · ricerca smart (dict Boone) · StopDetail per-direzione + overlay linea ·
conditional GET refresh.

Già portati (cross-conferma DoVe+Movete): clamp delay `[-300,+1800]s`, in-app update,
badge live su DepartureRow.

## ✅ Risolto: mappa nativa riabilitata (era un flag condiviso)

`enableMap` era `false` (commit `f4d7f40`, 12 giu) per nascondere la mappa
**web** placeholder, ma è un flag condiviso → spegneva anche la mappa nativa
(Android la perdeva a ogni build via `build-android.sh`). Fix `e1226c8`:
canonical `enableMap: true` (nativo ON), web gateata su `WEB_MAP_READY`
(`web/utils/features.ts`, =false) finché la PWA map non è reale. Mappa Android
verificata funzionante (Mapbox, marker, toggle 2D, no crash). **Cluster A
sbloccato.** Il "Vedi su mappa" del box corsa ora ha una destinazione valida.

## Avanzamento (loop porting)

- ✅ **B-data ritardo RT sui countdown — iOS** (`de79c75`): filtro plausibilità
  `-300..1800s`, countdown+orario shiftati e coerenti, pallino LIVE invariato
  (presenza veicolo). QA primary 4/4 PASS.
- ✅ **B-data ritardo RT sui countdown — Android** (`c21da69`): mirror iOS,
  additivo (campi RT prima mai popolati), pallino LIVE ora a parità. QA primary
  4/5 PASS — unico FAIL = verde condiviso soon/live (ereditato da DoVe, design
  decision aperta, non regressione).
- ✅ **Mappa nativa riabilitata** (`e1226c8`): Cluster A sbloccato; web placeholder
  nascosto via `WEB_MAP_READY`.
- ✅ **Cluster A — pulsante "Linea" card mezzo — iOS** (`8afb3ce`): layout DoVe
  Follow(icon-only)/Linea(ghost)/Corsa(filled); Linea apre LineDetailView (push,
  parallelo a Corsa→TripDetail), fedele a DoVe (onShowLine=navigateToRoute, NON
  overlay). Marker mezzo ora accessibili (a11y id). QA primary 5/5 PASS.
  - Correzione importante: la prima lettura di DoVe (Linea=filtro overlay) era
    sbagliata; verificato a mano: DoVe Linea = push line detail.
- ✅ **Cluster A — pulsante "Linea" — Android** (`8d51439`): mirror iOS, riusa nav
  `line/{routeId}`, layout 3 pulsanti, testTag. + completati in IT i 9 testi della
  card mezzo (erano fallback EN). Card mezzo ora interamente EN+IT. QA: layout OK,
  truncation OK; Linea/Corsa/Follow code-verified (feed appalcart vuoto fuori
  orario → ri-QA in orario di servizio).
- ✅ **i18n IT completo** (`094bd00`): EN+IT ora 1:1 su entrambe (26 chiavi IT
  Android aggiunte; iOS già completo). Regola EN+IT rispettata. (es resta indietro,
  fuori scope.)
- ✅ **Update-check app — iOS** (`1363102`): banner soft (iTunes Lookup, bundleId
  parametrizzato) + force screen, schema `appUpdate` in config. force OFF per
  appalcart (no brick). Build+launch verificati. QA visiva banner/force =
  condizionale (serve versione store più alta / force=true per innescarli).
- ✅ **Fix marker mezzo tappabile** (`1c0f22f`): tappare il bus ora apre la card
  (era rotto: a11y id non rinfrescato + hit-area 100×100 che faceva rubare il tap
  dalle fermate). Verificato live.
- ✅ **QA LIVE iOS (orario di servizio, `2026-06-19_1400`)**: card 3 pulsanti,
  Linea→line detail, Corsa→trip detail, Follow, **box stato live**, countdown
  ritardo RT — **tutti PASS su dati live**. Backlog QA mezzo-live iOS CHIUSO.
- 📌 **Da QA, pre-esistenti fuori scope:** M1 righe partenza con destinazione
  duplicata (headsign printed twice — ricorrente), M2 "live" reso in 2 modi
  (dot vs pill), P1 icona Follow (arrow vs crosshair).
- ✅ **Box stato live in TripDetail — iOS** (`8054da5`) **+ Android** (`86e1adb`):
  "In transito" + pill ritardo + CTA "Vedi su mappa" (riusa deep-link
  map/vehicle/{id}); additiva, nascosta senza mezzo live. Stringhe EN+IT+es.
- 🔶 **Decisione aperta:** differenziare il verde soon vs live? (cross-platform)
- ⚠️ **Debito i18n:** ~26 chiavi IT mancanti nell'app (viola EN+IT completi).
- 📌 **Fuori scope flaggati:** a11y marker fermata/mezzo (Mapbox symbol layer),
  polish controlli mappa.
- 🕑 **Backlog QA "in orario di servizio"** (appalcart non ha mezzi live di notte →
  feed vuoto, UI a mezzo-live non visibili): card mezzo Android (Linea/Corsa/Follow),
  box stato live TripDetail iOS+Android, glide marker quando fatti.
- ✅ **Update-check app — Android** (`a3a5d06`): mirror iOS, config-only, force gate
  OFF (no brick), banner soft + force screen, EN+IT+es. Build+launch OK. **Cluster C
  completo iOS+Android.**
- ▶️ **Prossimo:** glide marker Android (iOS ha `scheduleVehicleTween`; verificare
  se Android salta tra refresh e portare il tween) · QA live Android in orario di
  servizio (card mezzo + box stato live) · [opz] fix pre-esistente M1 (righe
  partenza con destinazione duplicata — headsign, segnalato 3× dalle QA).
- 🔁 **Loop:** l'heartbeat in-memory (wakeup/cron) NON scatta affidabilmente
  unattended (provato: wakeup 12:53 non partito per 53 min nonostante caffeinate).
  Continuazione affidabile = sessione attiva / ping utente, oppure grind nello
  stesso turno.

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
