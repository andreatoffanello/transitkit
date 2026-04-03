# TransitKit Engine

White-label transit app engine. Single codebase, per-operator config → branded iOS app + Android app + web stop pages.

## Struttura

```
transit-engine/
├── pipeline/          # Python: GTFS → schedules.json
├── ios/               # SwiftUI white-label app (✅ funzionante)
├── android/           # Jetpack Compose (🔲 da costruire)
├── web/               # Nuxt web stop pages (🔲 da costruire)
├── shared/
│   └── operators/     # Per-operator: config.json + assets
│       ├── rfta/
│       └── tcat/
├── scripts/           # Build, deploy, store automation
├── data/              # Raw GTFS feeds (gitignored)
├── STRATEGY.md        # Product & business strategy
└── MARKET_RESEARCH.md # Pipeline operatori target
```

## Quick start

```bash
# Processa GTFS → JSON
python pipeline/build.py rfta

# Build iOS
./scripts/build-ios.sh rfta

# Web (futuro)
cd web && npm run dev
```

## Aggiungere un operatore

1. Crea `shared/operators/<id>/config.json`
2. `python pipeline/build.py <id>` → genera `output/<id>/schedules.json`
3. Carica `schedules.json` su CDN
4. iOS: build con `operatorId = "<id>"`
5. Web: aggiungi entry in `web/server/middleware/operator.ts` + custom domain su Vercel

Vedi `STRATEGY.md` per il quadro completo.

## Deployment (Web)

The web app deploys automatically to Vercel via GitHub Actions on every push to `main`.

### Required GitHub Secrets

Add these in **GitHub → Settings → Secrets and variables → Actions**:

| Secret | Value |
|--------|-------|
| `VERCEL_TOKEN` | Generate with `vercel tokens create` |
| `VERCEL_ORG_ID` | `team_DCUI79bqy35aMbSETneHbCQ7` |
| `VERCEL_PROJECT_ID` | `prj_fJqGIF3Aq4c9w0Gz3081dfnAoOcL` |

### Workflows

- `.github/workflows/deploy.yml` — builds and deploys on push to `main` (production) or any PR (preview). Preview URLs are posted as PR comments.
- `.github/workflows/test.yml` — runs type check and unit tests on every push/PR.
