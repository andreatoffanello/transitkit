# TransitKit Web App — Design Spec

**Data:** 2026-04-03  
**Stato:** Approvato  
**Fase:** Phase 3 (post primo cliente)

---

## Obiettivo

Web app mobile-first in Nuxt che permette di:
1. Stampare QR code alle fermate fisiche → scansione → tabellone partenze branded per l'operatore
2. Offrire una presenza web indicizzabile (SEO) per ogni operatore
3. Funzionare senza che il rider scarichi nessuna app

---

## Scope

### Pagine incluse

| Pagina | URL | Priorità |
|--------|-----|----------|
| Tabellone fermata | `/stop/:stopId` | P0 — core del prodotto, target QR |
| Lista linee | `/lines` | P1 — SEO, discovery |
| Dettaglio linea | `/lines/:lineId` | P1 — SEO, lista fermate |
| Home operatore | `/` | P1 — presence page, link app store |

### Escluso da questo ciclo

- Mappa interattiva (Mapbox/Google Maps — costo e complessità non giustificati ora)
- Geolocalizzazione "fermate vicine" (funziona peggio nel browser che nell'app)
- Auth / favoriti (zero friction è il valore: nessun account)
- Dashboard operatore (Phase 2 — sistema separato)
- Push notifications (Phase 2 — richiede Neon + APNs/FCM)
- Proxy GTFS-RT server-side (Phase 4 — solo se CORS è un problema concreto)

---

## Architettura

```
GitHub Pages CDN (transitkit-data.github.io)
  /{operatorId}/config.json
  /{operatorId}/schedules.json
          ↓ fetch a build-time (SSG) + ogni ora (ISR revalidate: 3600)
Nuxt 3 su Vercel (single project)
  middleware globale: Host header → operatorId
  SSG per tutte le route statiche note
  ISR per stop pages (molte fermate, rebuild on-demand)
          ↓ client-side opzionale
  GTFS-RT protobuf feed → countdown live con fallback silenzioso
          
scripts/generate-qrs.ts (offline)
  input:  schedules.json
  output: qr/{operatorId}/stop-{id}-{name}.png
```

**Costo operativo stimato:** ~$0 aggiuntivi (Vercel pro già pagato, GitHub Pages gratuito).

---

## Host-based routing

Un solo Vercel project serve tutti gli operatori. Il middleware Nuxt legge `Host` e seleziona il contesto operatore:

```ts
// utils/operators.ts — importato dal middleware
export const OPERATOR_HOSTS: Record<string, string> = {
  "appalcart.transitkit.app": "appalcart",
  "fermate.appalcart.com": "appalcart",
  // aggiungere un operatore = una riga + redeploy
}
```

ISR configurato via `routeRules` in `nuxt.config.ts`:

```ts
routeRules: {
  '/stop/**': { isr: 3600 },
  '/lines/**': { isr: 3600 },
  '/': { isr: 3600 },
}
```

**Flusso aggiunta operatore:**
1. Aggiungere riga in `OPERATOR_HOSTS`
2. Aggiungere `/{operatorId}/` su GitHub Pages CDN con `config.json` + `schedules.json`
3. Redeploy Nuxt
4. (Quando firma) Cliente aggiunge CNAME `fermate.loro.com → cname.vercel-dns.com`

---

## CDN dati

Repository separato `transitkit-data` su GitHub, pubblicato via GitHub Pages.

```
transitkit-data/
  appalcart/
    config.json      ← stesso formato dell'iOS config.json
    schedules.json   ← output di pipeline/build.py (~3MB)
  tcat/
    config.json
    schedules.json
```

Nuxt fetcha i file a build-time. ISR garantisce che una visita dopo 1h trigger un revalidate in background — l'utente vede sempre la versione cached, mai un loading state per i dati base.

---

## Struttura progetto

```
web/                              ← nuova cartella nel monorepo
├── nuxt.config.ts                ← routeRules ISR, build config
├── app.config.ts                 ← theme defaults runtime
├── middleware/
│   └── operator.global.ts       ← Host → operatorId + carica config
├── composables/
│   ├── useOperator.ts           ← config + schedules, caricati una volta
│   └── useRealtime.ts           ← GTFS-RT client-side + fallback
├── utils/
│   ├── operators.ts             ← OPERATOR_HOSTS registry
│   └── schedule.ts              ← decode compact departure format (port da iOS)
├── components/
│   ├── LineBadge.vue            ← badge colorato linea
│   ├── DepartureRow.vue         ← riga partenza con countdown
│   └── DayGroupTabs.vue         ← tab lun-ven / sab / dom
└── pages/
    ├── index.vue                ← home operatore
    ├── lines/
    │   ├── index.vue            ← lista linee
    │   └── [lineId].vue        ← dettaglio linea
    └── stop/
        └── [stopId].vue        ← tabellone fermata (QR target)
```

---

## Pagine — dettaglio UX

### `/stop/:stopId` — Tabellone fermata

Struttura verticale mobile-first:

1. **Header** — logo/nome operatore, colori da `config.json`
2. **Nome fermata + badge linee** — nome in grande, badge colorati delle linee che passano
3. **Sezione "Adesso"** — prossime 4-6 partenze ordinate per orario, con countdown live se GTFS-RT disponibile (`● 2 min`) o orario statico (`07:35`) se no
4. **Sezione "Orari oggi"** — tab per day group (lun-ven / sab / dom), lista completa orari del giorno corrente
5. **Footer** — link "Apri in Google Maps" (coordinate fermata) + link App Store iOS

L'indicatore live (`● aggiornato X sec fa`) appare solo se `isLive === true`. Nessun errore mostrato se il feed non è disponibile.

### `/lines/:lineId` — Dettaglio linea

Header con badge grande e nome linea. Lista fermate in sequenza (direzione A), ogni fermata è link a `/stop/:id`. Prima/ultima corsa del giorno. Switch direzione A/B.

### `/lines` — Lista linee

Grid di badge raggruppati per `transitType` (bus, ferry, ecc.). Stesso raggruppamento di iOS `LinesListView`. Click → `/lines/:id`.

### `/` — Home operatore

Hero con logo + nome operatore. Link a `/lines`. Link App Store (se disponibile in `config.json`). Info contatti base. Metadata SEO ottimizzati per `"{operatorName} bus schedule"`.

---

## Real-time (client-side)

```ts
// composables/useRealtime.ts
useRealtime(stopId, departures, gtfsRtUrl?)
  → { departures: ComputedRef<Departure[]>, isLive: Ref<boolean> }
```

Flusso:
1. Se `gtfsRtUrl` è assente → return statico, `isLive = false`
2. Fetch protobuf ogni 30s
3. Parse con `protobufjs` (GTFS-RT schema)
4. Merge trip updates sui departures statici → aggiorna delay/countdown
5. Se fetch fallisce (CORS, 404, timeout) → `isLive = false`, dati statici rimangono

Nessun retry esponenziale — il polling ogni 30s è sufficiente e non aggressivo.

---

## QR generation (script offline)

```
scripts/generate-qrs.ts
  npx tsx scripts/generate-qrs.ts --operator appalcart
  
  legge: web/app.config.ts (host base) + CDN schedules.json
  genera: qr/appalcart/stop-{id}-{slugName}.png
  url:    https://appalcart.transitkit.app/stop/{stopId}
  size:   300×300px, error correction level H (resistente a danni fisici)
  output: zip consegnabile all'operatore
```

Dipendenze: `qrcode` (npm). Zero infrastruttura runtime.

---

## Design system web

- **Framework CSS:** Tailwind CSS v4
- **CSS variables:** settate dinamicamente da `config.json` al mount (`--color-primary`, `--color-accent`, `--color-text-on-primary`)
- **Stile:** glass morphism leggero coerente con iOS, ma nativo web — no port pixel-perfect di SwiftUI
- **Font:** system font stack (velocità, no CDN font esterno)
- **Dark mode:** `prefers-color-scheme` media query, stessi valori dell'iOS `AppTheme`

---

## SEO

Ogni stop page ha:
- `<title>`: `{stopName} — {operatorName}`
- `<meta description>`: `Orari e prossime partenze dalla fermata {stopName}. Linee: {lines}.`
- `og:title`, `og:description` per sharing

La home `/` è ottimizzata per `"{operatorName} bus schedule"` / `"{operatorName} orari autobus"`.

Le stop pages sono SSG quindi indexabili da Google. ISR garantisce freschezza senza SSR.

---

## Decisioni architetturali

| Decisione | Scelta | Motivo |
|-----------|--------|--------|
| Framework | Nuxt 3 | Andrea conosce Vue/Nuxt |
| Rendering | SSG + ISR (revalidate: 3600) | Performance + freschezza senza SSR cost |
| Routing multi-operatore | Host-based middleware | Un solo Vercel project, CNAME per operatore |
| CDN dati | GitHub Pages | Costo zero, upgrade a Vercel Blob quando serve |
| Real-time | Client-side fetch con fallback | No backend aggiuntivo, degrada gracefully |
| QR | Script offline → PNG | Zero infrastruttura, output consegnabile |
| Auth | Nessuna | Zero friction per rider occasionali |
| DB (Neon) | Non in questo ciclo | Serve solo per dashboard/push (Phase 2) |
| Mappa | Non in questo ciclo | Costo Mapbox + complessità non giustificati |
| CSS | Tailwind v4 + CSS vars | Theming dinamico per-operatore |
