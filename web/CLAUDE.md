# TransitKit Web — Mappa per sessioni fresche

PWA Nuxt 3 + Vue 3 + TypeScript strict, SSG su Vercel, white-label per operatore via hostname → config JSON su CDN.

## Struttura cartelle

| Path | Ruolo |
|------|-------|
| `pages/` | Route file-based Nuxt (home, stop, lines, info, map, settings, privacy) |
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

- `pages/stop/[stopId].vue` — **615 righe**. Candidati estrazione: SchedulePanel, UpcomingPanel (merge realtime), ShareActions, DayGroupLogic.
- `pages/index.vue` — **599 righe**. Home con ricerca fermate, recenti, favoriti, download banner. Estrarre HomeSearch, RecentStopsSection, FavoritesSection.
- `utils/strings.ts` — **363 righe**. i18n home-grown; ok per ora, ma valutare split per namespace se cresce.
- `pages/lines/[lineId].vue` — **321 righe**. Dettaglio linea con schedule per direzione.
- `pages/lines/index.vue` — **276 righe**. Lista linee con filtro per transit type.
- `pages/info/services/[serviceId].vue` — **254 righe**.
- `utils/schedule.ts` — **222 righe**. Parsing orari + group by service day.
- `composables/useOperator.ts` — **165 righe**. Troppe responsabilità (fetch CDN + normalize iOS→web format + theme color lowercase + abort controller). Split candidato: `fetchOperatorConfig`, `normalizeSchedules`, `useOperator` sottile.

## Composables chiave

- `useOperator` — carica `config.json` + `schedules.json` da CDN per l'`operatorId` corrente; normalizza formato iOS→web; usa `useAsyncData` con chiavi stabili per evitare hydration mismatch.
- `useRealtime(departures, gtfsRtUrl)` — polling GTFS-RT ogni 30s via `rt.transitkit.app`; decode protobuf lazy; degrade silenzioso su CORS/404; espone `isLive`, `isLoading`, `lastUpdated`, `refresh`, `departures` merged.
- `useTheme` — applica colori operator come CSS custom properties.
- `useStrings` — wrapper thin su `utils/strings.ts` (i18n senza lib esterna, basato su operator `lang`).
- `useOperatorHead` — `useHead` per title/meta/OG per operatore.
- `useFavoriteStops` / `useRecentStops` — localStorage, SSR-safe (guard su `import.meta.client`).

## Components principali

- `AppLayout` — shell (sidebar + content).
- `AppSidebar` / `AppTabBar` — nav desktop / mobile.
- `PageHeader` — header route-aware con back button.
- `DepartureRow` — riga singola partenza con delay realtime, accent color linea.
- `LineBadge` — pill colorato con numero linea (color contrast WCAG via `utils/color`).
- `StopMapHeader` — hero mappa statica Mapbox per pagina fermata.
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
- Config + schedule live su CDN: `https://andreatoffanello.github.io/transitkit-data/{operatorId}/{config.json,schedules.json}` (override dev via env `CDN_BASE`).
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
- **Mai** aggiungere logica a `pages/stop/[stopId].vue` o `pages/index.vue` senza prima splittare — sono già oltre soglia manutenibile.
- **Mai** usare `font-size` per dimensionare icone SVG: sempre `width` + `height` espliciti.
- **Mai** leggere `localStorage` / `window` senza guard `import.meta.client` — rompe SSG.
- **Mai** modificare `OPERATOR_HOSTS` senza prevedere redeploy Vercel (non è runtime config).
