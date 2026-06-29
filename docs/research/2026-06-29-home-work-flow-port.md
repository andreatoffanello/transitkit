# Port flusso Casa/Lavoro da movete → transit-engine

Data: 2026-06-29

## Obiettivo

Portare il nuovo flusso movete per definire i luoghi Casa/Lavoro, con **pagine di
ricerca dedicate** e **disaccoppiamento SET/USE**, + **ricerca indirizzi** (forward
geocoding) nelle search page. Parità iOS + Android.

## Il valore del flusso movete

- `LocationPicker` con due modalità: `.select` (scegli per il viaggio) e
  `.assign(key)` (pagina dedicata che imposta la scorciatoia **senza toccare il
  viaggio**).
- `SavedPlaceRow`: stato vuoto "Imposta indirizzo" → apre pagina assign; stato
  impostato → tap per usare nel viaggio + menu ⋯ Modifica / Rimuovi.
- Risultati partizionati FERMATE / LUOGHI (fermate GTFS + indirizzi geocodati).
- Reverse-geocode in background quando si salva da GPS/mappa.

## Stato di partenza transit-engine

| | iOS | Android |
|---|---|---|
| Persistenza saved places | ✅ `SavedPlacesStore` (UserDefaults, operator-scoped) | ❌ solo stato di sessione in `PlannerViewModel` |
| SavedPlaceRow | ✅ esiste ma con **banner inline** (vecchio pattern) | ❌ assente |
| Flusso assign dedicato | ❌ banner `assigningPlaceKey` nella stessa schermata | ❌ |
| Ricerca | solo fermate GTFS (`store.stops.filter`) | solo fermate GTFS (`allStops.filter`) |
| Geocoding | CLGeocoder **reverse** (map picker) | Mapbox **reverse** (`MapboxGeocodingService`) |
| Forward geocoding (indirizzi) | ❌ | ❌ |

## Decisioni tecniche

- **Forward geocoding iOS** = `MKLocalSearch` (MapKit, nativo, nessun token; iOS NON
  usa Mapbox). Reverse resta CLGeocoder.
- **Forward geocoding Android** = estendere `MapboxGeocodingService` con
  `forwardGeocode()` (Mapbox Search API v6, token già presente). Reverse resta Mapbox.
- Entrambi i provider: **bias all'area operatore** (centro da config).
- **No bottom sheet / no Dialog full-screen su Android** (regola progetto): la pagina
  assign è una **push full-screen** via Navigation, NON un Dialog annidato come movete.
  iOS: NavigationStack push (come movete).
- Modello dati condiviso concettualmente: `GeocodeResult { id, kind(stop|place),
  name, lat, lon, stopGtfsId? }`. `SavedPlace { name, lat, lon }`, chiavi `home`/`work`.
- Localizzazione **EN + IT**, zero stringhe hardcoded.

## Piano per batch

### Batch 0 — Fondazione: geocoding + persistenza Android
- **iOS**: nuovo `GeocodingProvider` (MKLocalSearch forward + bias operatore) →
  `[GeocodeResult]`. Modello `GeocodeResult`.
- **Android**: estendere `MapboxGeocodingService.forwardGeocode()`; modello
  `GeocodeResult`; nuovo `SavedPlacesStore` (DataStore, chiave `{operatorId}_saved_places`)
  + `SavedPlace` + `SavedPlaceKeys`.

### Batch 1 — iOS: restructure flusso
- `LocationPickerView`: rimuovere banner inline, introdurre modalità `.assign(key)`
  come pagina pushata dedicata (SET/USE disaccoppiato).
- Risultati uniti e partizionati FERMATE / LUOGHI (stop + MKLocalSearch).
- `savedPlaceRow`: vuoto→push assign; impostato→usa; menu ⋯ Modifica/Rimuovi.
- Reverse-geocode in background su salvataggio da GPS/mappa.
- Icone da libreria (no hand-crafted).

### Batch 2 — Android: flusso + persistenza
- `SavedPlaceRow` composable (stato vuoto + DropdownMenu Modifica/Rimuovi).
- Modalità assign in `LocationPickerScreen` come **push full-screen** dedicata.
- Risultati partizionati FERMATE / LUOGHI (stop + Mapbox forward).
- `HomePlannerBox` legge i saved places persistiti; SET/USE disaccoppiato.

### Batch 3 — QA
- Build iOS + Android puliti.
- QA visiva indipendente (designer-eye-qa) su iOS 18/26 + emulator.
- Verifica EN + IT, accessibility identifiers, no crash.

## Gotcha noti
- `NavigationStack` iOS 18: coalescing pop+push concorrenti → hop runloop tra il
  dismiss della pagina assign e l'eventuale push successivo.
- Android: niente edge-to-edge; rispettare safe area.
- Operator-scoped storage su entrambe le piattaforme.
