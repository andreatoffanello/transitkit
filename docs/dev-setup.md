# Setup ambiente di sviluppo (una volta per Mac)

Gli identificativi pinnati (UDID, bundle ID, AVD, porta console) sono in
`CLAUDE.md` root, sezione `IDENTIFICATIVI PROGETTO`: leggili da lì, mai da
lookup runtime.

## Script

```bash
bash scripts/setup-dev.sh [operator_id]
```

Applica anche la location dei simulatori leggendo
`shared/operators/<op>/config.json` (default `appalcart` → Boone, NC).
Su iOS la location è in-memory: rilancia lo script dopo ogni reboot del simulatore.

## Simulatori iOS

```bash
xcrun simctl create "transitkit-ios18" "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro" "com.apple.CoreSimulator.SimRuntime.iOS-18-5"
xcrun simctl create "transitkit-ios26" "com.apple.CoreSimulator.SimDeviceType.iPhone-16-Pro" "com.apple.CoreSimulator.SimRuntime.iOS-26-5"
```

Dopo la creazione (o dopo un wipe) l'UDID cambia: ri-pinnarlo a mano in
`CLAUDE.md` root.

## Emulatori Android

```bash
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
avdmanager create avd --name "transitkit-android" --device "pixel_6" \
  --package "system-images;android-34;google_apis;arm64-v8a" --sdcard "512M"

# Solo per verificare edge-to-edge / window insets su Android 15
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home" \
avdmanager create avd --name "transitkit-android-api35" --device "pixel_6" \
  --package "system-images;android-35;google_apis;arm64-v8a" --sdcard "512M"
```

L'avvio va SEMPRE fatto su porta fissa (`-port 5600`, e `5602` per l'API 35):
il serial deve restare deterministico. Comandi di avvio e verifica difensiva
del serial: `CLAUDE.md` root, sezione SIMULATORI DI RIFERIMENTO.

## Maestro

```bash
which maestro || curl -Ls "https://get.maestro.mobile.dev" | bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"  # maestro non trova Java da solo
```
