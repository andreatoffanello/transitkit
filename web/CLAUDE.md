# TransitKit Web — Mappa per sessioni fresche

PWA **Nuxt 4** (4.4.x) + Vue 3.5 + TypeScript strict, SSR + ISR su Vercel (preset `vercel`), white-label per operatore via hostname → config JSON su CDN. Nitro 2.13 sotto.

**Routing & cache.** `/stop/**` ha `routeRules.isr: 60` con `stale-while-revalidate=600` ([nuxt.config.ts](nuxt.config.ts)) — primo scan QR paga il SSR cold, tutti i successivi 60s servono dalla edge Vercel. `/lines/**` ISR 300s. Resto SSR per richiesta.

## Struttura cartelle

**SCOPE (non negoziabile).** Questa NON è l'app in versione web. Unico caso d'uso: da QR → pagina fermata con le partenze (RT marcato vs no) → tap partenza → svolgimento corsa → tabellone per giorni → ricerca altra fermata / ricerca linea → fermata. NIENTE mappe, settings, info, onboarding app-like. Pagine `map`/`settings`/`info` ELIMINATE (giu 2026). Se serve altro, l'utente scarica l'app (banner). Non re-introdurre superfici fuori da questo flusso.

| Path | Ruolo |
|------|-------|
| `pages/` | Route file-based Nuxt: `index` (ricerca), `stop/[stopId]`, `trip/[tripId]` (svolgimento corsa), `lines`, `privacy`/`support` (legali, store) |
| `components/` | UI riusabile (no logica dati) |
| `composables/` | State + logica (fetch, realtime, tema, i18n, favorites) |
| `utils/` | Pure functions (operator resolution, schedule parse, color, fetchWithRetry) |
| `types/index.ts` | TS types condivisi (OperatorConfig, ScheduleData, Departure, TransitType) |
| `middleware/operator.global.ts` | Risolve `operatorId` da hostname a ogni navigazione |
| `server/routes/` | Route server: `manifest.json`, `og.svg`, `robots.txt`, `sitemap.xml` |
| `tests/` | Vitest unit (19 file, composables + utils + componenti) |
| `e2e/` | Playwright E2E (hydration, jsonld, lines-filter, smoke, stop-backbutton) |
| `proto/` | `gtfs-realtime.proto` caricato lazy da `useRealtime` |
| `public/` | Asset statici + icone PWA |
| `plugins/` | Plugin Nuxt |

## File grossi — splittare quando tocchi

Post-refactor (aprile 2026):

- `pages/stop/[stopId]/` — **directory**, non un file singolo: `index.vue` (210 righe) + `schedule.vue` (214 righe). Sottocomponenti in `components/stop/`: `StopHeader.vue`, `UpcomingPanel.vue`, `useStopHead.ts`.
- `composables/useOperator.ts` — **61 righe** facade + `useOperatorConfig.ts` + `useOperatorSchedule.ts`. **API pubblica invariata** — consumer continuano a usare `useOperator()`.

Ancora da splittare quando li tocchi:

- `pages/index.vue` — **558 righe**. Home con ricerca fermate, recenti, favoriti, brand header. Estrarre in `components/home/` (HomeSearch, RecentStopsSection, FavoritesSection, OperatorCard).
- `utils/strings.ts` — **363 righe**. i18n home-grown; ok per ora, valutare split per namespace se cresce.
- `pages/lines/[lineId].vue` — **327 righe**.
- `pages/lines/index.vue` — **294 righe**.
- `utils/schedule.ts` — **222 righe**.

Pattern splitting: orchestrator `pages/<route>.vue` snello + sottocartella `components/<feature>/` con subview + composables dedicati per logica stateful.

## Composables chiave

- `useOperator` — carica `config.json` + `schedules.json` da CDN per l'`operatorId` corrente; normalizza formato iOS→web; usa `useAsyncData` con chiavi stabili per evitare hydration mismatch.
- `useRealtime(departures, gtfsRtUrl)` — polling GTFS-RT ogni 30s via `rt.transitkit.app`; decode protobuf lazy; degrade silenzioso su CORS/404; espone `isLive`, `isLoading`, `lastUpdated`, `refresh`, `departures` merged.
- `useTheme` — segue lo schema di sistema (no toggle manuale: l'app è iOS-aligned). `initTheme` in `app.vue`.
- `useStrings` — wrapper thin su `utils/strings.ts` (i18n senza lib esterna, basato su operator `lang`).
- `useOperatorHead` — `useHead` per title/meta/OG per operatore.
- `useFavoriteStops` / `useRecentStops` — localStorage, SSR-safe (guard su `import.meta.client`).

## Components principali

- `AppLayout` — shell (topbar + content + tab bar).
- `AppTopBar` / `AppTabBar` — nav desktop (sticky, blur+vibrancy) / mobile. La sidebar `lg:ml-60` è stata sostituita dalla topbar (lug 2026) per parity con l'header nativo iOS.
- `PageHeader` — header route-aware con back button.
- `DepartureRow` — riga singola partenza con delay realtime, accent color linea.
- `LineBadge` — pill colorato con numero linea (color contrast WCAG via `utils/color`).
- `DayGroupTabs` — selettore servizio (feriale/festivo/ecc).
- `AppDownloadBanner` — CTA store. Su iOS/Android **l'intera riga** è un link allo store del dispositivo (UA sniffing; iPadOS 13+ si dichiara `Macintosh` → discriminato via `navigator.maxTouchPoints`); su desktop restano entrambe le icone. Coperto da `e2e/app-banner.spec.ts`.

## Brand — app ≠ operatore (GOTCHA)

Sono due identità diverse, con due loghi e due nomi. Sbagliarli è già successo due volte:

| Superficie | Immagine | Testo |
|---|---|---|
| Topbar desktop + header mobile home | bus dell'**app** (`/brand/{op}/app-logo.png`) | `config.brandName` → "AppalRider" |
| Card "chi muove la città" | logo **reale operatore** (`/brand/{op}/operator-logo.jpg`) | `config.name` → "AppalCART" |

- **`config.logoUrl` NON è un logo**: punta a `app-icon.png`, cioè l'icona launcher **col fondo**. Usarla in un header dà un'icona squadrata al posto del bus. Il bus trasparente è `app-icon-foreground.png`.
- Gli asset di brand **non sono sul CDN** (lì c'è solo `app-icon.png`): il web li serve da `web/public/brand/{operatorId}/`. Nuovo operatore → copiare lì `app-logo.png` (= `app-icon-foreground.png`) e `operator-logo.jpg` da `shared/operators/{op}/brand/`, altrimenti scatta il fallback, che **non è unico**: bus Lucide solo in `AppTopBar.vue` (topbar desktop), `/icons/icon-180.png` per l'app-logo dell'header mobile in `pages/index.vue`, `/icons/icon-192.png` per l'operator-logo della card.
- Parity con gli imageset iOS: `OperatorLogo` = bus app, `SourceOperatorLogo` = logo operatore (vedi CLAUDE.md di root).

## Pattern comuni

- **Aggiungere una pagina**: file in `pages/`, routing automatico. Per dinamica: `[param].vue`. Accesso param: `useRoute().params.param`.
- **Aggiungere un composable**: nuovo file in `composables/`, auto-imported. SSR-safe: guardare `import.meta.server` / `import.meta.client`; cleanup in `onUnmounted` (timer, listeners).
- **SEO/meta**: usare `useOperatorHead` o `useHead` direttamente. JSON-LD in `useOperatorHead` per structured data.
- **Accesso operator**: `const { config, schedules } = await useOperator()`. `operatorId` grezzo via `useState<string>('operatorId')`.
- **Fetch con retry**: `utils/fetchWithRetry` (3 retry, backoff, AbortSignal).

## White-label e operator config

- Host → `operatorId` mappato in `utils/operators.ts` (`OPERATOR_HOSTS`). Unica mappa: aggiornare qui per nuovo operatore.
- `middleware/operator.global.ts` risolve a ogni nav e setta `useState('operatorId')`. 404 se host sconosciuto (eccezione: `/privacy`, operator-agnostic).
- Config + schedule live su CDN: `https://andreatoffanello.github.io/transitkit/{operatorId}/{config.json,schedules.json}` (override dev via env `CDN_BASE`). **NON** `transitkit-data` — quello è il repo del mock iniziale (4 fermate finte italiane): puntarci in prod è già successo e ha servito dati demo agli utenti.
- Dev: senza hostname matching usa `NUXT_OPERATOR` env o il primo operatore registrato.

## Realtime

- `useRealtime` polla `{gtfsRtUrl}` (dal `config.json` dell'operatore — deve puntare a `rt.transitkit.app/{op}/trip-updates.pb`).
- Interval 30s via `setInterval`; skip se `document.hidden` (Visibility API light, no pause/resume esplicito).
- Protobuf decoder caricato lazy (`protobufjs` + `proto/gtfs-realtime.proto`), cached module-level.
- **TODO**: gestione 503 `Retry-After` del proxy non implementata — attualmente degrade silenzioso su qualsiasi errore. Header `X-Stale: true` ignorato.

## Test

- **Unit** (Vitest, `tests/`): 20 file (341 test). Coprono composables (useOperator, useRealtime, useFavoriteStops, useRecentStops, useOperatorHead), utils (color, schedule, strings, highlight, fetchWithRetry), componenti (DepartureRow), server routes (sitemap, robots, manifest, jsonld), business logic (linesFilter, operators).
- **E2E** (Playwright, `e2e/`): hydration mismatch, JSON-LD validity, filtro linee, smoke test, back button su pagina stop, link store del banner (`app-banner`).
- **GOTCHA porta**: `playwright.config.ts` punta a `localhost:3000` con `reuseExistingServer` — se lì gira il dev server di un ALTRO progetto (la landing sta spesso sulla 3000), i test girano contro quello e falliscono in modo incomprensibile. Verificare cosa risponde sulla 3000 prima di sbattere la testa.
- Run: `npm run test` (vitest), `npm run test:e2e` (playwright). Config: `vitest.config.ts`, `playwright.config.ts`.

## Cosa NON fare

- **Mai** inserire URL upstream GTFS-RT (es. `s3.amazonaws.com/...`, endpoint operatore diretto) in config — tutto real-time passa da `rt.transitkit.app/{op}/{feed}.pb`. Solo `gtfs_url` (zip static schedule) resta diretto.
- **Mai** ignorare hydration mismatch sulle pagine realtime o sulle bindings `:style` con colori operator. I colori hex devono essere lowercase lato server e client (vedi comment in `useOperator.ts`).
- **Mai** aggiungere logica a `pages/index.vue` (558 righe) senza prima splittare in `components/home/`. Su `pages/stop/[stopId]/` (`index.vue` 210 + `schedule.vue` 214) aggiungi sottocomponenti in `components/stop/`, non inline.
- **Mai** usare `font-size` per dimensionare icone SVG: sempre `width` + `height` espliciti.
- **Mai** stampare in UI campi di `config` che sono metadata.** Il blocco `store` (`title`/`subtitle`/`keywords`) è stato **rimosso** dai config (lug 2026): nessuno ne *usava* il valore, il web lo mostrava in una card in home. Quella card stampava `store.title` = "Boone Bus — Community App": una stringa che **non è mai esistita su nessuno store** (il listing è "AppalRider"), con icona telefono e link a `/lines`. Il nome dell'app viene da `brandName`, quello dell'operatore da `name`. Se serve copy per lo store, vive in `docs/business/store/` e nelle console — non in `config.json`.
- **GOTCHA — "nessuno lo legge" ≠ "si può togliere dal JSON":** la rimozione di `store` (commit `82efc7e`, lug 2026) ha rotto l'app iOS per 13 ore. Il commit dichiarava "iOS non l'aveva nel modello": **falso**. `OperatorConfig.swift` lo dichiarava `let store: StoreConfig` **non-optional** — nessuno ne leggeva il valore, ma il decoder lo *pretendeva*. Tolto dal JSON → `DecodingError.keyNotFound` → `ConfigLoader.load()` lancia → `TransitKitApp.bootstrap()` finisce nel `catch` e l'app si apre su "Unable to load data" invece che sulla home. Salvata solo dal caso: la build 1.2.6(16) era stata caricata 51 minuti prima del commit. Due lezioni: (1) un campo Codable non-optional è un **requisito di schema** anche se il valore è morto — prima di togliere una chiave dal JSON, apri il modello di **ogni** client, non fidarti dell'assunzione; (2) quel commit ha verificato Android (build) e web (vue-tsc + 341 test) ma **non ha mai buildato iOS**, cioè l'unica piattaforma che poteva smentirlo. Se una modifica tocca i config condivisi, la verifica va fatta su iOS **e** Android **e** web.
- **Mai** far cadere il `tripId` nella normalizzazione schedule. Il formato CDN iOS porta `tripId` su ogni partenza; `normalizeSchedules` ([useOperatorSchedule.ts](composables/useOperatorSchedule.ts)) DEVE costruire `tripIds[]` ed emettere la tupla compatta con `tripIdIdx` all'**indice 5** (`[time, lineIdx, headsignIdx, dock, _, tripIdIdx]`) — è ciò che `decodeDepartures` legge e da cui dipendono righe cliccabili + `reconstructTrip`. Senza, lo svolgimento corsa è morto ("Trip details unavailable") e le righe non linkano. Coperto da `tests/tripReconstruction.test.ts`.
- **Svolgimento corsa** (`pages/trip/[tripId].vue`): ricostruito offline dallo schedule (`reconstructTrip` in [schedule.ts](utils/schedule.ts)) — porting di iOS `TripDetailView`, nessun endpoint dedicato. `?from=<stopId>` evidenzia l'origine ("Ora").
- **Mai** leggere `localStorage` / `window` senza guard `import.meta.client` — rompe SSG.
- **Mai** modificare `OPERATOR_HOSTS` senza prevedere redeploy Vercel (non è runtime config).
