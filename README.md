# TransitKit Engine

White-label transit app engine. Single codebase, per-operator config → branded app on stores.

## Structure

```
transit-engine/
├── pipeline/          # Python: GTFS → processed JSON
├── ios/               # SwiftUI white-label app
├── android/           # Compose white-label app
├── web/               # Nuxt white-label web app
├── shared/            # Operator configs, design tokens
│   └── operators/     # Per-operator config.json + assets
├── scripts/           # Build, deploy, store upload automation
├── data/              # Raw GTFS feeds (gitignored)
└── MARKET_RESEARCH.md # Operator targets and market analysis
```

## Quick start

```bash
# 1. Download operator GTFS
./scripts/download-gtfs.sh rfta

# 2. Process GTFS → JSON
python pipeline/build.py rfta

# 3. Build iOS app
./scripts/build-ios.sh rfta
```
