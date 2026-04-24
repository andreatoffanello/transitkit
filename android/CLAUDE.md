# TransitKit Android — Mappa per sessioni fresche

App Kotlin white-label per trasporto pubblico. Stack: Kotlin + Jetpack Compose + Mapbox (Maps SDK v11) + Moshi (KSP) + Hilt + Retrofit/OkHttp + AndroidX Navigation Compose.

Identificativi (UDID, package, AVD, regole simctl/adb) stanno nel CLAUDE.md root — non duplicare qui.

## Struttura cartelle

Root Kotlin: `app/src/main/java/com/transitkit/app/`.

| Cartella | Responsabilità |
|----------|----------------|
| `MainActivity.kt` | Single-activity host, NavHost, bottom bar, registrazione route |
| `TransitKitApp.kt` | `@HiltAndroidApp` entry point |
| `ui/home/` | Home redesign v2 + `OperatorMapBackground` (shader AGSL) + location primer |
| `ui/mappa/` | Schermata mappa Mapbox, annotations, line picker sheet |
| `ui/orari/` | Stop detail, trip detail, line detail, ricerca fermate |
| `ui/linee/` | Elenco linee |
| `ui/alerts/` | Lista + dettaglio service alerts |
| `ui/servizi/` | Info servizi (contatti, accessibility, fare, operator) |
| `ui/info/` | Info/about aggregata |
| `ui/settings/` | Settings + about |
| `ui/components/` | Componenti riusabili (oggi: LineBadge, StopIcon, TimeDisplay) |
| `config/` | `ConfigLoader`, `OperatorConfig`, `AppTheme`, `LucideIcons` |
| `data/api/` | `TransitApiService` (Retrofit) |
| `data/gtfsrt/` | `GtfsRtFetcher` (vehicle-positions, trip-updates, alerts via proxy) |
| `data/repository/` | `ScheduleRepository` (GTFS static + merge RT) |
| `data/store/` | `VehicleStore`, `AlertStore`, `FavoritesStore`, `SearchHistoryStore` |
| `data/model/` | Data classes Moshi |
| `di/` | `AppModule` (Hilt bindings unico modulo) |
| `assets/` | `config.json` operator (bundled a build time) |

## File grossi — splittare quando tocchi

Post-refactor (aprile 2026) i quattro screen monstre principali sono stati splittati in orchestrator + file locali allo screen:

- `ui/orari/StopDetailScreen.kt` — 383 LOC orchestrator + `StopDetailDeparturesList`, `FullScheduleSheet`, `DepartureRow`, `States`, `Alerts`, `LineBadgeRow`, `Helpers`, `Map`
- `ui/mappa/MappaScreen.kt` — 440 LOC orchestrator + `MappaMapLayers`, `TopBar`, `FabColumn`, `StopPreview`, `VehiclePreview`, `Helpers`
- `ui/orari/OrariScreen.kt` — 102 LOC orchestrator + `OrariSearchBar`, `StopsTab`, `StopCard`, `Helpers` (dead `LinesTab` rimosso)
- `ui/orari/LineDetailScreen.kt` — 285 LOC orchestrator + `LineDetailHeader`, `StopsTimeline`, `Helpers`

Nessun file >400 LOC attualmente. Se ne emergono, splittare in sottofile locali allo screen (naming `<Screen><Part>.kt`) o in `ui/components/` se riusabili cross-feature.

Regola: se aggiungi >50 righe a uno screen, prima estrai in file locali o in `ui/components/`.

## ViewModel e data layer

- `MappaViewModel` (`ui/mappa/`) — stato mappa, filtri linea, vehicles flow, camera state
- `OrariViewModel` (`ui/orari/`) — ricerca fermate + recenti
- `StopDetailViewModel` — partenze schedulate + merge RT, filtro linea/direzione
- `LineDetailViewModel` — timeline fermate + vehicles della linea
- `TripDetailViewModel` — singolo trip con stop_times + progresso RT
- `HomeViewModel` — location + nearby + operator branding
- `ScheduleRepository` — source of truth per orari: legge GTFS static (zip scaricato), merge con RT via `GtfsRtFetcher`
- `VehicleStore` / `AlertStore` — in-memory cache con `StateFlow`, refresh periodico
- `FavoritesStore` / `SearchHistoryStore` — persistenza DataStore
- `GtfsRtFetcher` — unica porta verso `rt.transitkit.app`, decode protobuf, espone Flow per feed

## Components riutilizzabili

In `ui/components/`: `LineBadge` (pill colorato linea), `StopIcon`, `TimeDisplay`. Pochi — estrai nuovi componenti qui quando splitti gli screen monstre (candidati: DepartureRow, AlertBanner, FilterChip, EmptyState, ErrorState).

## Pattern comuni

**Aggiungere una schermata:** crea `ui/<feature>/<Feature>Screen.kt` + ViewModel Hilt. Registra in `MainActivity.kt` dentro `NavHost { composable(...) { ... } }` — route come costante o `sealed class Screen`. Per argomenti path: `composable("foo/{id}", arguments = listOf(navArgument("id") { type = NavType.StringType }))`.

**Aggiungere un ViewModel:** annota `@HiltViewModel`, costruttore `@Inject` con dipendenze da `AppModule`. Espone `StateFlow<UiState>` — mai `LiveData`.

**Collezionare un Flow in Composable:** sempre `collectAsStateWithLifecycle()` (import `androidx.lifecycle.compose.collectAsStateWithLifecycle`) — mai `collectAsState()` nudo.

**Caricare asset operator:** `ConfigLoader` legge `assets/config.json` via `context.assets.open(...)` + Moshi. Usa `OperatorConfig` injected via Hilt, non ri-parsare.

## Config operator e white-label

Attualmente **non** ci sono `productFlavors` Gradle. `applicationId = "com.transitkit.appalcart"` è hardcoded in `app/build.gradle.kts`. Il config operator è un singolo `app/src/main/assets/config.json` caricato runtime da `config/ConfigLoader.kt`. Per un nuovo operatore: swap `assets/config.json` + cambio `applicationId` + risorse brand (colori `config/AppTheme.kt`, icone mipmap). Quando si introdurranno veri flavors, aggiornare questa sezione.

## Realtime

Tutti i feed GTFS-RT passano da `https://rt.transitkit.app/{operator}/{feed}.pb`. Unico punto di accesso: `data/gtfsrt/GtfsRtFetcher.kt`. Non aggiungere URL upstream operatore nel `config.json`. Dettagli operativi nel CLAUDE.md root (sezione REALTIME PROXY).

## Regole non negoziabili Android

- NON edge-to-edge — Play Console lo segnala come warning. Status bar / navigation bar configurate in `MainActivity.kt`, non toccare senza motivo
- `adb` sempre con `-s $ANDROID_SERIAL`, mai nudo (altri progetti hanno emulatori attivi)
- AVD `transitkit-dev` unico consentito — vedi CLAUDE.md root
- Moshi: KSP già configurato (`moshi-kotlin-codegen`). Le data class usano `@JsonClass(generateAdapter = true)` — NON aggiungere `KotlinJsonAdapterFactory` a runtime
- Mapbox token in `local.properties` / `gradle.properties`, mai committato

## Cosa NON fare

- Non aggiungere logica a uno screen se già >400 LOC — prima estrai in file locali
- Non usare `KotlinJsonAdapterFactory` con riflessione — Moshi KSP è la strada
- Non collezionare Flow in Composable senza lifecycle (`collectAsStateWithLifecycle`)
- Non usare `LiveData` — tutto `StateFlow`/`SharedFlow`
- Non abilitare edge-to-edge "per simmetria con iOS"
- Non bypassare `GtfsRtFetcher` con chiamate dirette a URL upstream
- Non creare un secondo Hilt module senza motivo — `AppModule` è unico, tienilo tale finché non diventa ingestibile
- Non inserire pathData SVG inventati — icone solo da `LucideIcons` o risorse ufficiali
