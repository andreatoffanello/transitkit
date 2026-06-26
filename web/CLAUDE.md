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

- `pages/stop/[stopId].vue` — **305 righe** orchestrator + `components/stop/` (`StopHeader`, `UpcomingPanel`, `SchedulePanel`, `ShareActions`, `useStopHead`).
- `composables/useOperator.ts` — **61 righe** facade + `useOperatorConfig.ts` + `useOperatorSchedule.ts`. **API pubblica invariata** — consumer continuano a usare `useOperator()`.

Ancora da splittare quando li tocchi:

- `pages/index.vue` — **~599 righe**. Home con ricerca fermate, recenti, favoriti, download banner. Estrarre in `components/home/` (HomeSearch, RecentStopsSection, FavoritesSection, DownloadBanner).
- `utils/strings.ts` — **363 righe**. i18n home-grown; ok per ora, valutare split per namespace se cresce.
- `pages/lines/[lineId].vue` — **321 righe**.
- `pages/lines/index.vue` — **276 righe**.
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

- `AppLayout` — shell (sidebar + content).
- `AppSidebar` / `AppTabBar` — nav desktop / mobile.
- `PageHeader` — header route-aware con back button.
- `DepartureRow` — riga singola partenza con delay realtime, accent color linea.
- `LineBadge` — pill colorato con numero linea (color contrast WCAG via `utils/color`).
- `DayGroupTabs` — selettore servizio (feriale/festivo/ecc).
- `AppDownloadBanner` — CTA app store iOS/Android.

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

- **Unit** (Vitest, `tests/`): 19 file. Coprono composables (useOperator, useRealtime, useFavoriteStops, useRecentStops, useOperatorHead), utils (color, schedule, strings, highlight, fetchWithRetry), componenti (DepartureRow), server routes (sitemap, robots, manifest, jsonld), business logic (linesFilter, operators).
- **E2E** (Playwright, `e2e/`): hydration mismatch, JSON-LD validity, filtro linee, smoke test, back button su pagina stop.
- Run: `npm run test` (vitest), `npm run test:e2e` (playwright). Config: `vitest.config.ts`, `playwright.config.ts`.

## Cosa NON fare

- **Mai** inserire URL upstream GTFS-RT (es. `s3.amazonaws.com/...`, endpoint operatore diretto) in config — tutto real-time passa da `rt.transitkit.app/{op}/{feed}.pb`. Solo `gtfs_url` (zip static schedule) resta diretto.
- **Mai** ignorare hydration mismatch sulle pagine realtime o sulle bindings `:style` con colori operator. I colori hex devono essere lowercase lato server e client (vedi comment in `useOperator.ts`).
- **Mai** aggiungere logica a `pages/index.vue` (~599 righe) senza prima splittare in `components/home/`. Su `pages/stop/[stopId].vue` (305 orchestrator) aggiungi sottocomponenti in `components/stop/`, non inline.
- **Mai** usare `font-size` per dimensionare icone SVG: sempre `width` + `height` espliciti.
- **Mai** far cadere il `tripId` nella normalizzazione schedule. Il formato CDN iOS porta `tripId` su ogni partenza; `normalizeSchedules` ([useOperatorSchedule.ts](composables/useOperatorSchedule.ts)) DEVE costruire `tripIds[]` ed emettere la tupla compatta con `tripIdIdx` all'**indice 5** (`[time, lineIdx, headsignIdx, dock, _, tripIdIdx]`) — è ciò che `decodeDepartures` legge e da cui dipendono righe cliccabili + `reconstructTrip`. Senza, lo svolgimento corsa è morto ("Trip details unavailable") e le righe non linkano. Coperto da `tests/tripReconstruction.test.ts`.
- **Svolgimento corsa** (`pages/trip/[tripId].vue`): ricostruito offline dallo schedule (`reconstructTrip` in [schedule.ts](utils/schedule.ts)) — porting di iOS `TripDetailView`, nessun endpoint dedicato. `?from=<stopId>` evidenzia l'origine ("Ora").
- **Mai** leggere `localStorage` / `window` senza guard `import.meta.client` — rompe SSG.
- **Mai** modificare `OPERATOR_HOSTS` senza prevedere redeploy Vercel (non è runtime config).
