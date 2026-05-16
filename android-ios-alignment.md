# Allineamento iOS — resoconto sessione Android

Da questa sessione su Android, ecco tutte le decisioni di design + implementazione che potrebbero valere su iOS. Organizzate per categoria, con file/posizione Android per riferimento.

---

## 1. Branding — naming

- **App label = "AppalRider"** (non "AppalCART"). Decisione legale: non possiamo usare "AppalCart" nel nome app finché non abbiamo l'autorizzazione dell'agenzia. AppalCART resta come **nome ufficiale dell'operatore/agenzia di trasporto** (mostrato in About, footer disclaimer, OperatorReferenceCard) — separazione netta tra "brand app" e "brand agenzia".
- **Android**: `strings.xml` (en/it/es) → `app_name = "AppalRider"`. Sul Home, il titolo in testa usa `stringResource(R.string.app_name)` invece di `config.name`. Settings `lingua_desc` ora ha `%s` placeholder per il nome app.
- **iOS**: stesso pattern. Bundle display name = "AppalRider". Operator config name = "AppalCART" intoccato.

---

## 2. Typography — Inter bundled

- **Decisione**: niente Google Fonts downloadable provider. Inter bundled in app come font statici (5 weights: Light/Regular/Medium/SemiBold/Bold).
- **Perché**: il downloadable provider richiede Play Services con font provider attivo. Su emulatori `google_apis` (senza Play Store) cade silenziosamente sul Roboto di sistema. Bundlato = deterministico ovunque.
- **Android**: file in `app/src/main/res/font/inter_{light,regular,medium,semibold,bold}.ttf`, `InterFontFamily` ora usa `Font(R.font.inter_*, weight)`. Costo ~700KB APK.
- **iOS**: bundlare gli stessi .ttf in `Resources/Fonts/`, registrarli in `Info.plist` (`UIAppFonts`), e impostare `Font.custom("Inter", size: ...)` in tutto il sistema typography. Stessa famiglia, stessi pesi, stesso costo trascurabile.

---

## 3. Departure time format — m/h, single token

- **Regola unificata in tutte le occorrenze** (Home Nearby, StopDetail, MapStopPreview, schedule list):
  - 0 min → pulsing dot + arrow + clock sotto (Departing state)
  - 1-59 min → "**5m**" (sopra) + clock (sotto)
  - 60+ min → "**1h**", "**2h**" (sopra) + clock (sotto)
  - Past → clock dimmed
- **Apostrofo prime `'` abolito** — convenzione italiana, in EN si usa m/h
- Countdown ≤5 min → colore `realtimeGreen`; sopra → `textPrimary`
- **Single TextView**: niente più due nodi `"5"` + `"'"` accostati. Il countdown è un'unica `Text("${minutes}m")` — riduce nodi A11y e fragility di layout.
- **Android**: `ui/components/TimeDisplay.kt` — sealed class `DepartureTimeState` con varianti `Departing`, `Minutes`, `Hours`, `Absolute`, `Passed`. Funzione `computeDepartureTimeState()` decide il bucket.
- **iOS**: già esisteva la parità (`TimeDisplay.swift`?). Verifica che il valore sia mostrato come `"\(n)m"` / `"\(n)h"` senza apostrofo, in **un solo `Text`**.

---

## 4. i18n leak — content descriptions in italiano su UI inglese

- **Trovato**: stringhe `"Linea X"`, `"direzione X"`, `"in partenza adesso"`, `"in X minuti"` hardcoded nel codice → finivano nel content-desc accessibility anche con UI in inglese, lette così dal TalkBack.
- **Fix**: 4 string resources `cd_line_format`, `cd_direction_format`, `cd_departing_now`, `cd_in_minutes` localizzate in en/it/es. Codice usa `stringResource`.
- **Android**: `ui/components/LineBadge.kt:124`, `ui/orari/StopDetailDepartureRow.kt`.
- **iOS**: cercare `accessibilityLabel` o `accessibilityIdentifier` con stringhe italiane hardcoded (es. "Linea", "direzione"). Spostarle in `Localizable.xcstrings`.

---

## 5. Marquee → static ellipsis

- **Decisione**: niente più marquee orizzontale sui sottotitoli delle departure row (catena di fermate intermedie). Multiple righe animate simultaneamente = noise visivo, illeggibile.
- **Sostituito con**: `maxLines = 1, overflow = Ellipsis` statico. L'utente che vuole vedere le fermate intermedie tocca per espandere (nella vista trip detail).
- **Android**: rimosso `Modifier.basicMarquee()` da `StopDetailDepartureRow.kt:106-114` e `LineeScreen.kt:481`.
- **iOS**: cercare `.scrollClipDisabled` o `marquee` su righe departure. Sostituire con `.lineLimit(1)` + truncation tail.

---

## 6. Card consistency — niente più border, solo elevation

- **Audit ha trovato**: alcune card avevano `BorderStroke` 0.5dp grigio, altre erano flat. Inconsistenza grossa: Nearby card outlined, Planner From/To e Favorites card senza border.
- **Decisione utente**: card devono essere **senza border, solo elevation**.
- **Android**: 10 `border()` / `BorderStroke()` rimossi da 6 file: HomeScreen (Nearby + planner box), SettingsScreen (2), AboutScreen, JourneyCard, ServiziScreen (5 card). Bumped elevation da 0 → 1dp dove serviva.
- **iOS**: cercare `.overlay(RoundedRectangle().stroke(...))` su card e `.background()` con border. Sostituire con `.shadow(radius: 1, y: 1)` o `.background(.regularMaterial)`. Pattern visivo: **card senza outline**.

---

## 7. Bottom sheet → full-screen page

- **Regola ferrea utente**: **NIENTE bottom sheet** mai, da nessuna parte. Niente `ModalBottomSheet`, niente `presentationDetents`, niente sheet con drag handle.
- **Sostituiti**:
  - LinePicker (era `ModalBottomSheet` → ora `Surface(fillMaxSize)` full-screen con header `←` + titolo)
  - StopDetailFullSchedule (era sheet → ora full-screen)
- Stop preview che era già una floating card overlay (non sheet) è OK.
- Time picker piccolo (Now / Depart at / Arrive by) → `DropdownMenu` popup (non sheet), va bene.
- **iOS**: scansiona ogni `.sheet { }` e `presentationDetents`. Sostituisci con `NavigationLink` / `.fullScreenCover` o navigation push tradizionale. Drag handles aboliti.
- Salvato come memory persistente: `feedback_no_sheets.md`.

---

## 8. Map default view — 3D + posizione utente + near zoom

- **Decisione**: aprire la Mappa default su **posizione dispositivo** (last known) a zoom **"near"** (street-level) e con **pitch 45° (3D)**. Fallback: centro operatore + zoom city overview, sempre con pitch 45°.
- **Costanti**:
  - `userDefaultEntry` = zoom 15.0 (near)
  - `cityDefaultEntry` = zoom 12.5 (overview)
  - Pitch default = 45°
- **Android**: `MappaScreen.kt:134-170`. Legge `LocationManager.getLastKnownLocation(GPS)` poi NETWORK come fallback, sincrono in `remember{}` per evitare jump. `MapZoomLevels.kt` ha già le costanti.
- **iOS**: stesso pattern. `CLLocationManager.location?.coordinate` sincrono al setup della camera. `MKMapCamera` con `pitch: 45`, `centerCoordinateDistance` ~ near zoom. Fallback su operator center.

---

## 9. User location puck — sempre presente, sempre sopra, blu classico + bearing

- **Regola**: ogni mappa deve avere il pin posizione dispositivo: **blu classico Mapbox**, **sempre sopra ogni altro layer**, con **bearing cone**.
- **Android**: composable `UserLocationPuck` in `ui/mappa/UserLocationPuck.kt`. Abilita la `LocationComponent` nativa Mapbox via `MapEffect { mapView.location.updateSettings { enabled=true; locationPuck = createDefault2DPuck(withBearing=true); puckBearing = HEADING } }`. Mapbox gestisce z-order automaticamente. Aggiunto a 3 mappe: `MappaScreen`, `JourneyMapView`, `LocationPickerMapScreen`. Rimossa la `UserPuck` manuale ViewAnnotation che era in JourneyMapView (sostituita dalla nativa).
- **iOS**: usa la `MKUserLocation` standard di Mapbox iOS SDK (`mapView.showsUserLocation = true` o equivalente in MapboxMaps iOS). Conferma che `puckType: .puck2D(.makeDefault(showBearing: true))` sia attivo su tutte le `MapView` (StopDetail map, JourneyMap, PlannerLocationMap, MainMap). Bearing puck = cono triangolare blu attorno al dot.

---

## 10. FAB controls mappa — layout fix

- **Bug Android trovato**: la `Column` interna alla FAB pill conteneva un `HorizontalDivider` default `fillMaxWidth` → la Column wrappava al widest child = il divider → Surface esplodeva a piena larghezza, coprendo metà mappa al centro.
- **Fix**: `Column(modifier = Modifier.width(44.dp))` esplicito, vincola la larghezza.
- **iOS**: probabilmente non ha lo stesso bug (layout diverso), ma vale la regola: i map controls (FAB stack) sono una pill **verticale ~44pt larga, ancorata al right edge** con padding 16pt, e **al centro verticale** dell'area mappa. Mai full-width, mai vicino ai bordi top/bottom (collisione con nav bar / tab bar).

---

## 11. Settings — back chevron

- **Audit**: la schermata Settings era pushed da Home (cog icon) ma non aveva back chevron — l'utente dipendeva dalla gesture back di sistema.
- **Fix**: aggiunta IconButton + ChevronLeft a sinistra del titolo "Settings", `onBack = { popBackStack() }`.
- **iOS**: verificare che Settings (e qualunque detail pushed) abbia sempre `NavigationBackButton` esplicito con chevron + label opzionale.

---

## 12. Bottom nav — selected state più visibile

- **Audit**: il pill indicator dello stato selezionato era troppo light (alpha 0.15). Difficile distinguere il tab attivo.
- **Fix**: bumped `indicatorColor = accent.copy(alpha = 0.22f)`. Più verde, più leggibile.
- **iOS**: se usate TabView custom o `UITabBar` con tint, controllare contrasto del selected state. Pillola di sfondo deve essere percepibile a colpo d'occhio.

---

## 13. Services tab icon — Grid2x2Plus

- **Cambio**: icona della tab Services da `Info` (cerchio i) a **`grid-2x2-plus`** Lucide (griglia 2×2 con piccolo "+"). Semantica migliore: "servizi multipli" + "aggiuntivi".
- **Android**: drawable `ic_lucide_grid_2x2_plus.xml` con i path data UFFICIALI Lucide (presi da `web/node_modules/lucide-vue-next/dist/esm/icons/grid-2x2-plus.js` — niente pathData inventato). Aggiunto a `LucideIcons.kt` come `Grid2x2Plus`.
- **iOS**: stesso swap. Se usate SF Symbols, c'è `square.grid.2x2` ma manca il "+". L'opzione cleaner è bundlare l'SVG Lucide come asset (stesso path data, vettoriale).

---

## 14. Home — top header AppalRider + OperatorReferenceCard bottom

- **Top header**: ora "AppalRider" (app brand), subtitle resta region (es. "Boone, North Carolina"). Settings cog top-right.
- **Bottom card** (Movete-style, nuova): `OperatorReferenceCard`. Prima del disclaimer footer. Contiene:
  - Avatar gradient `AP` (accent → primary) 40dp
  - Nome operatore vero: "**AppalCART**"
  - Indicatore live verde pulsante + "**X vehicles live**" (legge `vehicleStore.vehicles.size`)
  - `·` separatore + region
  - Chevron right → tap apre About operator
  - Fallback: "No live vehicles" se count = 0
- **Razionale**: il top dice cosa è l'app (AppalRider), il bottom dice da chi vengono i dati e in tempo reale quanto è "vivo" il network. Tap fluido per dettagli.
- **Android**: `HomeScreen.kt` (nuova composable + insertion before disclaimer), `HomeViewModel.liveVehicleCount: StateFlow<Int>`.
- **iOS**: stesso pattern. Top header → app name display. Bottom → operator card con live count letto dal feed GTFS-RT. Tap → About.

---

## 15. Punti dell'audit ancora aperti (per discussione cross-platform)

Cose che ho lasciato fuori scope ma vanno decise prima o poi (alcune le aveva già detto l'utente di NON toccare):

- **Stop pin design su mappa**: utente esplicito "non toccare, hai logica che non capisci". Lasciato com'era. **iOS allineare al pattern Android attuale** (qualunque sia).
- **Icon style mix** (filled green pin + outlined grey star nello StopDetail header) — semantica diversa: pin = action, star = toggle. Non unificato.
- **Bottom nav 5 tab vs 4** (Schedules + Routes consolidamento) — design discussion aperta.
- **Route detail header colorato** (header pieno con linea color) — outlier visivo rispetto altri detail (chiari). Da decidere se diventa pattern globale o eccezione.
- **Privacy "Location access" dot indicator passivo vs Notifications toggle** in Settings — mismatch di controllo. iOS attento al pattern.
- **Lettera "A" beside clock time** in StopDetail (significato non documentato — scheduled? approximate?). Da spiegare o togliere.
- **"Powered by TransitKit" card** in About — etichetta duplicata, da pulire.
- **Service tab top padding** — c'era ~140px di empty space sopra il titolo (LargeTopAppBar dynamic, ma sembrava troppo).

---

## 16. Memory persistenti salvate (per future sessioni)

- `feedback_no_sheets.md` — vietati i bottom sheet, sostituire con push navigation o full-screen overlay
- `feedback_no_handcrafted_icons.md` — già esisteva, riaffermato: mai pathData SVG inventato, sempre fetch da Lucide/SF Symbols/Heroicons ufficiali

---

## File chiave Android modificati (per riferimento incrociato)

| File | Cambiamento |
|------|------------|
| `res/font/inter_*.ttf` | 5 file Inter bundled |
| `res/drawable/ic_lucide_grid_2x2_plus.xml` | Nuovo |
| `res/values{,-it,-es}/strings.xml` | app_name AppalRider, cd_line_format, home_operator_card_live, settings_lingua_desc con %s |
| `config/AppTheme.kt` | Inter bundled invece di GoogleFont downloadable |
| `config/LucideIcons.kt` | + `Grid2x2Plus` |
| `MainActivity.kt` | Servizi icon, nav onBack Settings + onNavigateToAbout per Home |
| `ui/components/TimeDisplay.kt` | DepartureTimeState con Hours, single TextView "m"/"h" |
| `ui/components/LineBadge.kt` | cd via stringResource |
| `ui/home/HomeScreen.kt` | Header app_name, OperatorReferenceCard, no border |
| `ui/home/HomeViewModel.kt` | liveVehicleCount |
| `ui/mappa/MappaScreen.kt` | Camera default 3D + user location + near zoom, UserLocationPuck |
| `ui/mappa/MappaFabColumn.kt` | Width esplicita per fix layout |
| `ui/mappa/UserLocationPuck.kt` | Nuovo |
| `ui/mappa/LinePickerSheet.kt` | Sheet → full-screen Surface |
| `ui/orari/StopDetailFullScheduleSheet.kt` | Sheet → full-screen Surface |
| `ui/orari/StopDetailDepartureRow.kt` | i18n a11y, no marquee, ellipsis |
| `ui/orari/StopDetailScreen.kt` | (intoccato esplicitamente) |
| `ui/planner/JourneyMapView.kt` | UserLocationPuck nativo invece di ViewAnnotation manuale |
| `ui/planner/JourneyCard.kt` | No border |
| `ui/planner/LocationPickerMapScreen.kt` | UserLocationPuck |
| `ui/settings/SettingsScreen.kt` | Back chevron header |
| `ui/settings/AboutScreen.kt` | No border |
| `ui/servizi/ServiziScreen.kt` | 5 card no border |
| `ui/linee/LineeScreen.kt` | No marquee |

Tutto fatto e verificato su `transitkit-dev` (Pixel 6, API 34).
