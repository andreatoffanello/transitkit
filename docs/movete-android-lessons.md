# TransitKit Android — Lessons learned per Movete

Sintesi di un audit completo (3 agenti opus + counter-audit + 3 sprint di fix in-place) eseguito sull'app Android di TransitKit (Kotlin + Jetpack Compose + Mapbox v11 + Hilt + Moshi KSP + Retrofit + Navigation Compose). Stessa stack di Movete, gli smell trovati sono altamente trasferibili.

Lo scopo è darti una checklist concreta: ognuno dei punti sotto è un grep o un controllo di 30 secondi sul tuo codice. Non leggerlo come "fix da fare", leggilo come "dove guardare se non l'hai mai guardato".

---

## TL;DR — i 10 smell che valgono il controllo

1. **`KotlinJsonAdapterFactory` in `Moshi.Builder()` quando hai già `@JsonClass(generateAdapter = true)`** → riflessione runtime inutile + cold start lento + viola la rule "Moshi KSP è la strada"
2. **Sync I/O in `@Provides @Singleton`** di Hilt: aprire `assets`, `BufferedReader`, parse JSON nel main thread durante `MainActivity.onCreate` injection
3. **`collectAsState()` nudo invece di `collectAsStateWithLifecycle()`** — sottoscrizione che sopravvive a `STOPPED`, batteria + dati sprecati
4. **`subscribeStyleLoaded { }` Mapbox il cui `Cancelable` viene scartato** — listener leak ad ogni recompose / cambio theme
5. **`buildList { }` non `remember`ato in Composable** che alimenta una `remember(items) { ... }` → la chiave cambia ad ogni recompose, cascata di invalidation sul `Scaffold`/`NavHost`
6. **Composable privati gonfiati**: file Compose >400 LOC è quasi sempre un sintomo di pigrizia, non di complessità irriducibile
7. **Fuzzy search subsequence-match senza gate** — query corte ("dog") matchano per caratteri sparsi su parole diverse ("Highland Crossing")
8. **Empty state ≠ "Not found" state**: stop/route inesistente dal deeplink che mostra "no departures" o titolo = ID raw
9. **AlertDetail con sola description card** — manca metadata GTFS-RT (active period, cause, effect, "affects all service")
10. **Deeplink coverage parziale** — manifest e `navDeepLink` su una manciata di pattern, gli altri silently re-launch in Home senza errore

---

## 1. Cold-start & Hilt sync I/O

**Sintomo**: `Choreographer Skipped 305 frames` (~5s main-thread block) al primo `am start` cold, splash visibile 7-8s su emulatore.

**Causa**: in `AppModule`

```kotlin
@Provides @Singleton
fun provideOperatorConfig(@ApplicationContext ctx: Context, moshi: Moshi): OperatorConfig {
    val json = ctx.assets.open("config.json").bufferedReader().use { it.readText() }
    return moshi.adapter(OperatorConfig::class.java).fromJson(json)!!
}
```

Hilt resolve i `@Singleton` **sincroni** alla prima iniezione, e quella prima iniezione è `MainActivity.onCreate` (`@Inject lateinit var operatorConfig: OperatorConfig`). Risultato: assets I/O + JSON parse + (se hai messo `KotlinJsonAdapterFactory`) riflessione, tutto sul main thread prima del primo frame.

**Fix che abbiamo applicato**:
1. Rimosso `KotlinJsonAdapterFactory()` da `Moshi.Builder()` — le data class avevano già `@JsonClass(generateAdapter = true)` (KSP). Il factory faceva riflessione **ridondante** e bypassava il codegen
2. Mantenuto il parse sync (l'unica reale alternativa robusta è pre-parsing async via `Application.attachBaseContext` con `Lazy<OperatorConfig>` — refactor non triviale)

**Da controllare su Movete**:
- `grep -rn "KotlinJsonAdapterFactory" app/src/main/`
- `grep -rn "@Provides.*Singleton" app/src/main/` e per ognuno: c'è `context.assets.open` o `File(...)` o `runBlocking` nel body?

**Impatto reale**: il cold start su emulatore è passato da 16.6s a 3.3s dopo Sprint completo (rimozione dead code + split file pesanti + i 2 fix sopra). Su device reale ci si aspetta sub-2s.

---

## 2. Mapbox `Cancelable` leak

**Sintomo**: ad ogni recompose o cambio `isSystemInDarkTheme()`, un nuovo `OnStyleLoadedListener` si registra senza che il vecchio venga cancellato. Quando hai N hero map nel back stack, finisci con N listener vivi.

**Codice rotto**:
```kotlin
MapEffect(isDark) { mapView ->
    val applied = mapView.mapboxMap.style
    if (applied != null) {
        applyStyleConfig(applied, isDark)
    } else {
        mapView.mapboxMap.subscribeStyleLoaded { // ⚠️ ritorna Cancelable, viene SCARTATO
            mapView.mapboxMap.style?.let { applyStyleConfig(it, isDark) }
        }
    }
}
```

**Fix**:
```kotlin
MapEffect(isDark) { mapView ->
    mapView.mapboxMap.style?.let { applyStyleConfig(it, isDark) }
    val cancelable = mapView.mapboxMap.subscribeStyleLoaded {
        mapView.mapboxMap.style?.let { applyStyleConfig(it, isDark) }
    }
    try {
        awaitCancellation()
    } finally {
        cancelable.cancel()
    }
}
```

**Da controllare su Movete**:
- `grep -rn "subscribeStyleLoaded" app/src/main/` — ogni call site che NON cattura il return in una variabile poi cancellata in cleanup è un leak

---

## 3. `collectAsState` vs `collectAsStateWithLifecycle`

Da regola Compose moderna: in un Composable host `StateFlow`/`SharedFlow` dovrebbero essere collezionati con `collectAsStateWithLifecycle()` di `androidx.lifecycle.compose.*`. La versione nuda continua a raccogliere quando l'Activity è `STOPPED`, sprecando CPU + dati + batteria.

Trovati 13 siti su TransitKit (5 in MainActivity, 8 in PlannerScreen). Fix meccanico:

```kotlin
// Import
import androidx.lifecycle.compose.collectAsStateWithLifecycle

// Usage
val foo by viewModel.fooFlow.collectAsStateWithLifecycle()
```

**Da controllare su Movete**:
```bash
grep -rn '\.collectAsState()' app/src/main/java/
```
Ogni risultato è una violazione. Lo stesso vale per `observeAsState` di LiveData (regola: niente LiveData).

---

## 4. Recomposition cascade in MainActivity

**Sintomo**: 442 frame skipped osservati durante navigazione home. Cause-effetto:

```kotlin
val items = buildList {                            // ⚠️ non-remember
    add(Triple(Screen.Home, ...))
    if (config.features.enableMap) add(...)
}
val tabRoutes = remember(items) {                  // chiave instabile
    items.map { it.first.route }.toSet()
}
val showBottomBar = currentRoute in tabRoutes      // invalida ad ogni recompose
```

`buildList { }` produce una nuova `List` ad ogni recompose → `remember(items)` invalida → `tabRoutes` cambia → `showBottomBar` cambia → `Scaffold.bottomBar` recompose → `NavigationBarItem`×5 recompose. Cascata.

**Fix**:
```kotlin
val mapEnabled = config.features.enableMap
val items = remember(mapEnabled, labelHome, labelOrari, ...) {
    buildList { ... }
}
```

**Da controllare su Movete**:
- Cerca `buildList`, `listOf(...)` con elementi non-stabili (es. lambda, Triple di Composable refs), `Map<>`, `Set<>` costruiti inline nei Composable senza `remember`
- Specialmente in `MainActivity.kt` / `App.kt` dove definisci la tab bar — è il hotspot tipico

---

## 5. File Compose >400 LOC

Sintomo "tecnico": il file Compose monstre rende impossibile fare ragionamento locale su recomposition + lambda stability + state.

Su TransitKit avevamo:
- `HomeScreen.kt` 1394 LOC
- `JourneyDetailScreen.kt` 805 LOC
- `MainActivity.kt` 669 LOC
- 14 file >400 LOC totali

**Split pattern usato**:
- Orchestrator (`HomeScreen.kt`) <300 LOC, chiama solo sub-composable
- Sub-composable in file paralleli: `HomeMinimalHeader.kt`, `HomeAlertChip.kt`, `HomeFavoritesSection.kt`, `HomeNearbySection.kt`, `HomeStopRow.kt`, ecc.
- Composable estratti hanno visibilità `internal` (non `private`) per essere visibili nello stesso package senza export
- Helpers non-Composable (formattatori, costanti) in `HomeFormatting.kt`

Risultato TransitKit: HomeScreen 1394→294 LOC, JourneyDetailScreen 805→169 LOC.

**Da controllare su Movete**:
```bash
find app/src/main/java -name "*.kt" -exec wc -l {} + | sort -rn | head -20
```
Tutti i file >400 LOC sono candidati. Quelli >700 sono priorità.

---

## 6. Fuzzy search subsequence rumoroso

**Sintomo**: la ricerca "dog" su AppalCART ritornava "Highland Crossing", "Meadowview Dr / Greenway Rd" oltre a "ASU Dogwood Hall". Causa:

```kotlin
private fun fuzzyScore(text: String, query: String): Int {
    val t = text.lowercase()
    val q = query.lowercase()
    if (t.startsWith(q)) return 100
    if (t.contains(q)) return 80
    var qi = 0
    for (char in t) {
        if (qi < q.length && char == q[qi]) qi++
    }
    return if (qi == q.length) 50 else 0   // ⚠️ subsequence match sempre attivo
}
```

"Highland Crossing" contiene D-O-G come subsequence (Hig**hl**an**d** crossin**g** → no aspetta, "Highland" non ha 'd' all'inizio… lasciamo perdere — il punto è che con query 3-char il subsequence ha falsi positivi enormi).

**Fix**: aggiunto word-prefix match (score 90) + gate del subsequence solo per query ≥5 char:

```kotlin
if (q.isEmpty()) return 0
if (t.startsWith(q)) return 100
if (t.split(' ', '-', '/').any { it.startsWith(q) }) return 90  // word-prefix
if (t.contains(q)) return 80
if (q.length < 5) return 0                                       // GATE
// subsequence solo per query lunghe
```

Test post-fix: "dog" → solo "ASU Dogwood Hall". "hardin" → "Hardin Street Crosswalk" + "Howard St / Hardin St". Clean.

**Da controllare su Movete**: se la tua search fa subsequence sempre, prova a digitare 3-4 caratteri comuni (es. "tre", "via", "san") e vedi se i risultati hanno rumore. Se sì, applica il gate.

---

## 7. Empty state ≠ NotFound state

**Sintomo**: `transitkit://stop/1` con `1` non esistente → schermo "Stop detail" con titolo "1" (raw id) e "no departures today". Confonde l'utente: l'errore è un link rotto, non un orario vuoto.

**Fix pattern**: aggiungere `NotFound` state esplicito al sealed class:

```kotlin
sealed class DeparturesState {
    object Loading : DeparturesState()
    data class Success(val departures: List<Departure>) : DeparturesState()
    object Empty : DeparturesState()         // stop OK, no upcoming today
    object NotFound : DeparturesState()      // stop NOT in the schedule
    data class Error(val message: String) : DeparturesState()
}

fun loadDepartures() {
    viewModelScope.launch {
        scheduleRepository.load()
        val stopExists = scheduleRepository.scheduleResponse.value
            ?.stops?.any { it.id == stopId } == true
        if (!stopExists) {
            _loadState.value = DeparturesState.NotFound
            return@launch
        }
        // ... normale path
    }
}
```

E un `NotFoundState` composable con icona + sottotitolo + CTA "Back to home". Stesso pattern per `LineDetail` (`routeNotFound: StateFlow<Boolean>` derivato da `combine(scheduleResponse, route)`).

Nel topbar, condiziona il titolo:
```kotlin
val titleText = when {
    state is DeparturesState.NotFound -> stringResource(R.string.stop_not_found_title)
    resolvedName != null -> resolvedName
    else -> stopName  // fallback display name passato dal caller
}
```

**Da controllare su Movete**: lancia un deeplink a una fermata/linea con ID inventato. Se la UI non distingue empty da not-found, è il fix da fare.

---

## 8. AlertDetail metadata GTFS-RT

**Sintomo**: alert "Memorial Day" mostrava solo title + description. I campi GTFS-RT `activePeriods`, `cause`, `effect` erano nel data model ma non renderizzati. Quando l'alert non ha route/stop affetti specifici (system-wide), nessun indicatore "affects all".

**Fix pattern**: card metadata sotto la description, con 4 row condizionali:

```
┌────────────────────────────────────┐
│  🕐  WHEN                          │
│      19 May, 12:00 — 26 May, 05:55 │
│                                    │
│  ⚠️  EFFECT                        │
│      Reduced service               │
│                                    │
│  ℹ️  CAUSE                         │
│      Holiday                       │
│                                    │
│  🚌  Affects all service           │
└────────────────────────────────────┘
```

Code:
```kotlin
private fun LazyListScope.metadataCardItem(
    alert: ServiceAlert,
    affectedRoutes: List<ScheduleRoute>,
    affectedStops: List<ResolvedStop>,
) {
    val hasPeriod = alert.activePeriods.any { it.start != null || it.end != null }
    val hasCause = alert.cause != AlertCause.UNKNOWN_CAUSE
    val hasEffect = alert.effect != AlertEffect.UNKNOWN_EFFECT && alert.effect != AlertEffect.NO_EFFECT
    val affectsAll = affectedRoutes.isEmpty() && affectedStops.isEmpty()
    if (!hasPeriod && !hasCause && !hasEffect && !affectsAll) return
    item { /* render card */ }
}
```

`MetadataRow` riusabile per ognuna delle 4 righe. Skip silenzioso se non c'è nulla da mostrare (no card vuota).

**Da controllare su Movete**: apri un service alert. Vedi metadata o solo description? Il GTFS-RT del tuo operatore espone cause/effect/active_period?

---

## 9. Deeplink coverage

TransitKit Android partiva con 7 pattern su 28 dichiarati su iOS (25%). Tutti gli altri risolvono silenziosamente alla `startDestination` (Home) — `am start -W` ritorna sempre `Status: ok` perché l'Activity esiste, ma il routing Compose Navigation non matcha.

**Fix pattern**:

A. Per le rotte semplici (Home/Settings/Planner/Alerts/...) aggiungi `deepLinks = listOf(navDeepLink { uriPattern = "..." })` al composable:
```kotlin
composable(
    Screen.Home.route,
    deepLinks = listOf(
        navDeepLink { uriPattern = "transitkit://home" },
        navDeepLink { uriPattern = "transitkit://favorites" },
    ),
) { HomeScreen(...) }
```

B. Per rotte con query (`planner?when=14:30`, `search?q=&scope=`) Compose Navigation NON propaga i query params se non li dichiari come arguments — soluzione `PendingPrefillStore`:
```kotlin
object PendingPlannerPrefillStore {
    private val _pending = MutableStateFlow<PendingPlannerPrefill?>(null)
    val pending: StateFlow<PendingPlannerPrefill?> = _pending
    fun set(p: PendingPlannerPrefill) { _pending.value = p }
    fun consume(): PendingPlannerPrefill? {
        val v = _pending.value
        _pending.value = null
        return v
    }
}
```

In MainActivity.onCreate e `onNewIntent`:
```kotlin
private fun handlePlannerDeepLink(intent: Intent?) {
    val data = intent?.data ?: return
    if (data.scheme != "transitkit" || data.host != "planner") return
    val whenStr = data.getQueryParameter("when") ?: return
    PendingPlannerPrefillStore.set(PendingPlannerPrefill(whenStr = whenStr))
}
```

E nel ViewModel `init`:
```kotlin
init {
    applyPendingPrefill()
}
private fun applyPendingPrefill() {
    val p = PendingPlannerPrefillStore.consume() ?: return
    // parse whenStr "HH:MM" usando OPERATOR timezone, NON device tz
    val date = parseHhMmInOperatorTz(p.whenStr, config.timezone) ?: return
    _whenSelection.value = WhenSelection(mode = 1, date = date)
}
```

**Punto chiave timezone**: HH:MM da deeplink deve essere parsato nella TZ dell'operatore. Su iOS avevamo lo stesso bug — `14:30` per AppalCART (NY) interpretato come `14:30 Europe/Rome` mostrava `20:30` o `08:30` lato UTC. Su Android, usa `Calendar.getInstance(TimeZone.getTimeZone(operatorTz))`.

**Da controllare su Movete**:
```bash
grep -c "navDeepLink" app/src/main/java/...MainActivity.kt
```
Confronta con la matrice iOS. Tutti i pattern iOS gestiti? Per quelli con query, hai il PendingStore + TZ-aware parse?

---

## 10. Bottom-nav tap su Home tab no-op

**Sintomo perceptually critico**: dalla schermata Schedules (o qualunque altro tab), tap su "Home" non riporta a Home. Tap su Routes, Map, Alerts funzionano regolarmente. Solo Home — cioè **il tab che punta alla start destination** — è insensibile.

**Causa**:
```kotlin
onClick = {
    navController.navigate(screen.route) {
        popUpTo(navController.graph.startDestinationId) { saveState = true }  // ⚠️
        launchSingleTop = true
        restoreState = true
    }
}
```

`graph.startDestinationId` ritorna l'ID grezzo dal `NavGraph`, che con nested graph annidati o overlay (`AlertToastHost`-like `Box`/`Scaffold` wrapping) **non sempre matcha** l'ID che Navigation Compose usa per identificare la start route. Quando il tab cliccato punta a quella stessa route, `popUpTo` silently no-op → `launchSingleTop` no-op → nessuna navigazione.

**Fix**:
```kotlin
import androidx.navigation.NavGraph.Companion.findStartDestination

// ...
popUpTo(navController.graph.findStartDestination().id) { saveState = true }
```

`findStartDestination()` (extension su `NavGraph`) attraversa ricorsivamente i nested graph per trovare il vero innermost start destination. È il pattern raccomandato da Material Design + Compose Navigation docs.

**Da controllare su Movete**:
```bash
grep -rn "graph\.startDestinationId" app/src/main/java/
```
Ogni occorrenza in un `popUpTo` è un candidato per il bug. Sostituisci tutte con `graph.findStartDestination().id`. È un find-and-replace di 10 secondi che evita un bug perceptually critico.

**Reproduzione**: avvia l'app, naviga a un altro tab (es. Schedules), tap su Home tab nella bottom bar. Se resta su Schedules invece di tornare a Home — hai il bug.

---

## 11. Pattern minori (quick wins)

### Hex color parsing duplicato
Trovati 10 siti che facevano `Color(android.graphics.Color.parseColor("#$hex"))` ad-hoc invece di usare una funzione `parseHexColor` centralizzata. Vector di bug futuri (blank string, `#FFF` 3-char hex non supportato, alpha channel ambiguo).

**Fix**: una sola funzione esportata da un componente UI:
```kotlin
fun parseHexColor(hex: String?, fallback: Color = Color(0xFF3B82F6)): Color {
    val s = hex?.trim()?.removePrefix("#") ?: return fallback
    val expanded = when (s.length) {
        3, 4 -> s.map { "$it$it" }.joinToString("")   // 3-char shorthand
        6, 8 -> s
        else -> return fallback
    }
    return runCatching {
        val argb = if (expanded.length == 6) "FF$expanded" else expanded
        Color(argb.toLong(16).toInt())
    }.getOrDefault(fallback)
}
```

### TripDetailViewModel O(N²) on main
Per ogni apertura del trip detail, 3 scan separati `schedule.stops.flatMap { ... }` su Main. Su 500 stops × 10 deps = 5000 op × 3 nel main thread.

**Fix**:
```kotlin
val result = withContext(Dispatchers.Default) {
    val stopById = schedule.stops.associateBy { it.id }
    val tripStops = mutableListOf<StopTime>()
    var currentRouteId: String? = null
    schedule.stops.forEach { stop ->
        stop.departures.forEach { dep ->
            if (dep.tripId == tripId) {
                if (currentRouteId == null) currentRouteId = dep.routeId
                tripStops += StopTime(...)
            }
        }
    }
    val coincidences = tripStops.associate { st ->
        st.stopId to (stopById[st.stopId]?.departures
            ?.filter { it.routeId != currentRouteId }
            ?.map { it.routeName }?.distinct() ?: emptyList())
    }
    Triple(tripStops.sortedBy { it.sequenceNumber }, originIdx, coincidences)
}
```
Singolo pass + cache stopById + spostato off-main.

### Polling lifecycle awareness
Pattern corretto già adottato su TransitKit (`ProcessLifecycleOwner.get().lifecycle.addObserver(DefaultLifecycleObserver { ... })` con start/stop polling). Punto di duplicazione: lo stesso `lifecycleObserver` definito sia in `HomeViewModel` sia in `MappaViewModel`. Sarebbe meglio centralizzato dentro `VehicleStore` / `AlertStore`.

### ProMotion 120Hz
Mapbox SDK v11 default a 60Hz. Su device con 90/120Hz dispaly, override opt-in:
```kotlin
fun MapView.applyMaximumFps(target: Int = 120) {
    runCatching { setMaximumFps(target) }
}
```
Chiamato da `MapEffect(Unit) { mapView -> mapView.applyMaximumFps() }`. Safe no-op sui 60Hz.

### Dead code
Eliminati 466 LOC totali:
- `ConfigLoader.kt` (16 LOC) — mai chiamato, `AppModule.provideOperatorConfig` faceva già il parse
- `InfoScreen.kt` (~400 LOC) + `InfoViewModel.kt` — sostituiti da `ServiziScreen` ma orfani non rimossi
- Sub-composable privati morti dentro file vivi

Check banale: `grep -rn "ClassName\|funName" app/src/main/` su ogni classe pubblica del codebase. Zero risultati esterni al file di definizione = candidata morte.

### Smart-casing acronimi
Pattern bug iOS-tipo: `name.replaceFirstChar { it.uppercase() }.capitalize()` su stop name rovina acronimi tipo "ASU SRC" → "Asu Src". Su Android TransitKit **non era presente** (good), ma da verificare se hai logica simile su Movete (es. stop names da feed in lowercase che ri-capitalizzi prima del display).

Fix corretto: preservare token ≤3 caratteri in uppercase.

---

## Strumenti d'audit (riusabili)

Quello che ha funzionato meglio per scoprire questi smell:

1. **3 agent paralleli con ruoli distinti**:
   - Agent A (visivo+funzionale): emulator + Maestro + uiautomator dump + screenshot
   - Agent B (architettura+codice): read-only filesystem, file LOC ranking, grep pattern violations
   - Agent C (performance+IA): logcat + Mapbox lifecycle + recomposition pattern + deeplink coverage matrix

2. **Counter-audit obbligatorio**: per ogni finding, verificare nel codice se è VERO/FALSO/PARZIALE prima di crederci. Sui 30+ finding iniziali, 5-6 erano falsi positivi (es. "map controls in alto-destra" → era già centrato vertical, agente aveva guardato lo screenshot male).

3. **Build verde dopo ogni batch**: `./gradlew assembleDebug` rapido (cached <20s), facile non rompere il branch. Per i refactor monstre (split HomeScreen), build dopo ogni 3-4 file estratti.

4. **Smoke test deeplink-driven**: invece di tap manuali, lancia `adb shell am start -W -a android.intent.action.VIEW -d "transitkit://stop/badId"` e screenshot. Fast, ripetibile, copre i 28 schemi in 2 minuti.

5. **Delega ai sub-agent per il lavoro meccanico**: split file (Sonnet) e i18n (Sonnet). Tieni Opus sul main thread per giudizio + counter-audit.

---

## Metriche before/after TransitKit (orientative)

| Metrica | Pre-audit | Post-fix |
|---------|-----------|----------|
| File >400 LOC | 17 | 9 |
| `collectAsState()` nudo | 13 | 0 |
| `KotlinJsonAdapterFactory` | 2 | 0 |
| Hex `parseColor("#")` ad-hoc | 11 | 1 (legittimo, raw Int) |
| Leak `Cancelable` Mapbox | 2 | 0 |
| Dead code LOC | ~466 | 0 |
| Deeplink coverage iOS-parity | 7/28 (25%) | 28/28 (100%) |
| Cold start emulator | 11-16s | 3.3s |
| Frame skipped cold start | 442 | <50 |

I numeri sono dell'emulator Pixel 6 API 34 — su device reale vanno scalati di un fattore 3-4× più favorevole.

---

## Punto finale

Niente di quanto sopra è esoterico — sono tutti pattern Compose/Hilt/Mapbox documentati ma facili da dimenticare quando il codebase cresce. La cura più efficace è far girare un audit periodico (trimestrale?) con la stessa metodologia: 3 agent paralleli + counter-audit + fix in-place senza branch.

Se hai dubbi su come applicare un pattern a Movete specifico, scrivi — il vantaggio di lavorare su entrambi i codebase è che le lezioni di uno aiutano l'altro.
