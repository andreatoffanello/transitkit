# transit-engine / TransitKit


## REALTIME PROXY (dipendenza esterna obbligatoria)

Le feature real-time (posizioni veicoli, ritardi live, alert di servizio) **non** colpiscono più l'upstream dell'operatore direttamente. Tutti i client (iOS, Android, web PWA) passano attraverso un proxy HTTP dedicato:

- **Repo:** https://github.com/andreatoffanello/transitkit-realtime-proxy
- **Prod URL:** `https://rt.transitkit.app`
- **Pattern endpoint:** `https://rt.transitkit.app/{operator_id}/{feed}.pb`
  - `{feed}` ∈ `vehicle-positions | trip-updates | alerts` (kebab-case)
- **Content-Type:** `application/x-protobuf` (passthrough bytes, zero trasformazione)
- **Cache:** Cloudflare edge 10s + `max-age=10` lato browser

**Quando aggiungi/modifichi un operatore con feed GTFS-RT:**

1. PR sul repo `transitkit-realtime-proxy` che aggiorna `operators.yaml` (schema: `label`, `used_by: [transitkit]`, `feeds.{vehicle_positions,trip_updates,alerts}` con `url` + `ttl`).
2. Merge su `main` → CI automatica fa build GHCR + SSH deploy sul VPS Hetzner.
3. In `transit-engine`, i `gtfs_rt` nei `config.json` (iOS + Android + `shared/operators/{op}/config.json` + eventualmente CDN `transitkit-data` per web) devono puntare a `https://rt.transitkit.app/{op}/{feed}.pb`, mai all'upstream diretto.

**Health e debug:**
- `curl https://rt.transitkit.app/healthz` → `ok`
- `curl https://rt.transitkit.app/status` → JSON con age + bytes per ogni feed
- Se un feed ritorna 503 con `Retry-After`: snapshot non ancora fetched (<1 TTL).
- Se header `X-Stale: true`: upstream giù da >5×TTL, dati potenzialmente vecchi.

**NEVER** inserire URL upstream dell'operatore (es. `s3.amazonaws.com/etatransit.gtfs/...`) nei `config.json` dei client: la regola è **tutti i GTFS-RT passano dal proxy**. Il solo feed ancora diretto è `gtfs_url` (static schedule zip, non real-time — non va nel proxy).

Documentazione operativa completa: [README](https://github.com/andreatoffanello/transitkit-realtime-proxy#readme), [Runbook](https://github.com/andreatoffanello/transitkit-realtime-proxy/blob/main/docs/RUNBOOK.md), [Troubleshooting](https://github.com/andreatoffanello/transitkit-realtime-proxy/blob/main/docs/TROUBLESHOOTING.md).

---

## IDENTIFICATIVI PROGETTO (pinned — non toccare senza aggiornare la macchina)

- Scheme: `TransitKit`
- Bundle ID: `com.transitkit.{OPERATOR_ID}` (white-label: es. `com.transitkit.appalcart`)
- iOS xcodeproj: `ios/TransitKit.xcodeproj`
- UDID simulatore iOS 18: `4302AFD9-496E-4586-A5D0-D6BAC735FFFD` (`transitkit-dev`, iPhone 16 Pro)
- UDID simulatore iOS 26: `E25FE58E-7059-457F-A0A9-8B1E3D59145D` (`transitkit-dev-26`, iPhone 16 Pro)
- Location simulatori/emulatore: applicata da `scripts/setup-dev.sh [operator_id]` leggendo `shared/operators/<op>/config.json` → `map.centerLat/centerLng`. Default `appalcart` → Boone, NC (`36.2168,-81.6746`). Su iOS la posizione `simctl location set` è in-memory: rilanciare lo script dopo ogni reboot del simulatore.
- AVD Android: `transitkit-dev` (Pixel 6, API 34 — white-label: es. com.transitkit.appalcart)
- Package Android: `com.transitkit.{OPERATOR_ID}`

**Regole ferree:**
- MAI lanciare/installare app su simulatori diversi da `transitkit-dev` e `transitkit-dev-26`.
- MAI usare `booted` come target simctl — sempre `$UDID`.
- Il Bundle ID cambia per operatore: verificare sempre `{OPERATOR_ID}` prima di `simctl launch`.
- MAI lanciare/installare app su emulatori Android diversi da `transitkit-dev`.
- MAI usare `adb` senza `-s <serial>` esplicito — su più emulatori ambiguo.
- MAI usare `emulator -avd <name>` con `<name>` diverso dall'AVD pinned del progetto.

---

## REGOLE AGENTE — OBBLIGATORIE

Queste regole hanno priorità su qualsiasi skill o plugin caricato nella sessione.

### Qualità visiva

Livello di riferimento: Airbnb, Uber, Spotify, Telegram. La UI di sistema è il pavimento, non il soffitto.

**Sì:**
- Spring animations con damping studiato — mai linear o ease generico
- Shader/Metal backgrounds per profondità e atmosfera — parte del prodotto, non un template
- Micro-interazioni su ogni elemento interattivo: tap feedback, stato pressed, transizioni di stato
- Blur e vibrancy per gerarchia spaziale — non decorazione
- Haptic feedback calibrato — non su ogni tap, solo dove aggiunge significato
- Transizioni tra schermate con logica di continuità visiva

**NEVER:**
- Animazioni decorative senza trigger funzionale
- Lottie/GIF al posto di animazioni native
- Gradienti saturi o combinazioni cromatiche senza ragione
- Effetti che rallentano la percezione di velocità dell'app
- Shimmer/skeleton quando i dati arrivano in meno di 300ms
- Contatori, badge statistici, filler UI — ogni pixel risponde a una domanda reale dell'utente

**Tipografia e layout:** gerarchia intenzionale, allineamento ottico (non solo matematico). Spacing da sistema (4/8/16/24/32). Contrasto WCAG AA minimo, AA+ preferito per testo body.

### Gotcha implementazione UI

**Icone SVG da librerie** (Heroicons, Lucide, SF Symbols custom, ecc.):
**ALWAYS** `width` e `height` espliciti — **NEVER** `font-size` o `em`. Le dimensioni intrinseche SVG ignorano i valori font-based.

**Safe area e edge-to-edge:**
- iOS: ok per mappe e immagini hero — testare SEMPRE su iOS 18 oltre che iOS 26

**Mappe:**
- Dettaglio luogo (fermata, POI, indirizzo): default **3D** — su iOS il rendering è scenico
- Overview (città, percorso): 2D per leggibilità
- Controlli mappa: verticalmente al **centro** dell'area mappa — mai vicino ai bordi (collidono con nav bar e tab bar)

### Strumento giusto per il task giusto

| Task | Strumento |
|------|-----------|
| Flusso E2E / smoke / regressione | `maestro test flows/<nome>.yaml` |
| Ispezione elemento singolo / debug | `idb ui describe-all --udid $UDID` |
| Tap / input / swipe puntuali | `idb ui tap` / `idb ui text` |
| Verifica estetica / design review | Screenshot compresso + crop |

**NEVER** sequenze manuali idb per simulare un flusso che esiste in `flows/`.
**NEVER** screenshot per verificare navigazione o logica.

### Screenshot — solo per design review

```bash
# Full screen compresso
xcrun simctl io $UDID screenshot /tmp/s.png && sips -Z 800 /tmp/s.png --out /tmp/s_small.png
# Crop componente singolo
xcrun simctl io $UDID screenshot /tmp/s.png && sips -c <H> <W> /tmp/s.png --out /tmp/component.png
```
Allega sempre "prima" e "dopo" con descrizione esplicita. Per animazioni/shader: descrivi il comportamento dinamico, non il frame statico.

### Definition of done — feature UI

- [ ] Build senza errori né warning nuovi
- [ ] Elementi interattivi nuovi: `.accessibilityIdentifier()`
- [ ] Screenshot "prima" e "dopo" allegati e commentati
- [ ] Flow Maestro in `flows/` aggiornato se il flusso utente è cambiato
- [ ] Se tocca safe area, edge-to-edge, o mappa: testato su simulatore iOS 18
- [ ] Nessun crash: `xcrun simctl spawn $UDID log stream --level error`

---

Framework iOS white-label per app di trasporto pubblico.

- Progetto Xcode: `ios/TransitKit.xcodeproj`
- Scheme: `TransitKit`
- Bundle ID: `com.transitkit.{OPERATOR_ID}` — es. `com.transitkit.appalcart`
- Build con OPERATOR_ID: `xcodebuild ... OPERATOR_ID=appalcart`

## SIMULATORI DI RIFERIMENTO

### iOS

| Ruolo | Nome progetto | Modello | iOS |
|-------|--------------|---------|-----|
| Principale | `transitkit-dev` | iPhone 16 Pro | 18.5 |
| iOS 26 test | `transitkit-dev-26` | iPhone 16 Pro | 26.2 |

```bash
# Setup tramite script (una volta per Mac) — usa questo
bash scripts/setup-dev.sh

# Oppure manuale con identifier completi:
xcrun simctl create "transitkit-dev" "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro" "com.apple.CoreSimulator.SimRuntime.iOS-18-5"

# Lookup UDID
UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"      # transitkit-dev (iOS 18.5 — pinned)
UDID26="E25FE58E-7059-457F-A0A9-8B1E3D59145D"    # transitkit-dev-26 (iOS 26.2 — pinned)
```

### Android

| Ruolo | AVD name | Device | API |
|-------|----------|--------|-----|
| Principale | `transitkit-dev` | Pixel 6 | 34 |

```bash
# Crea AVD (una volta per Mac)
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
avdmanager create avd --name "transitkit-dev" --device "pixel_6" \
  --package "system-images;android-34;google_apis;arm64-v8a" --sdcard "512M"

# Avvia emulatore
/Users/andreatoffanello/Library/Android/sdk/emulator/emulator -avd transitkit-dev -no-snapshot-load -no-audio &

# Lookup serial (usare QUESTO — non altri emulator-XXXX)
ADB=/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb
ANDROID_SERIAL=$($ADB devices | grep emulator | awk '{print $1}' | while read s; do
  $ADB -s $s emu avd name 2>/dev/null | grep -q "transitkit-dev" && echo $s && break
done)
```

**NEVER** usare altri emulatori (DoVe_Pixel6, alilaguna-android, movete-android) — appartengono ad altri progetti.

## BUILD

```bash
UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"   # transitkit-dev (pinned — evita ambiguità con cloni omonimi)
xcodebuild -project ios/TransitKit.xcodeproj -scheme TransitKit \
  -sdk iphonesimulator \
  -destination "platform=iOS Simulator,id=$UDID" \
  build OPERATOR_ID=appalcart CODE_SIGNING_ALLOWED=NO 2>&1 | tail -5
xcrun simctl install $UDID $(find ~/Library/Developer/Xcode/DerivedData -name "TransitKit.app" -path "*/Debug-iphonesimulator/*" | head -1)
xcrun simctl launch $UDID com.transitkit.appalcart
```

## WORKFLOW E2E — Maestro

```bash
maestro test flows/smoke_test.yaml
maestro test flows/
maestro test --watch flows/nome_flow.yaml
```

## IMPORTANTE

**Questo non è il progetto DoVe/civici.** Non usare skill `dove-ios-workflow`.
