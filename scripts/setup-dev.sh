#!/usr/bin/env bash
# scripts/setup-dev.sh — applica la posizione GPS della città di riferimento
# dell'operatore ai simulatori iOS e all'emulatore Android.
#
# Legge shared/operators/<op>/config.json → map.centerLat / map.centerLng,
# poi imposta:
#   - iOS sim transitkit-dev    (UDID 4302AFD9-…, iOS 18.5)
#   - iOS sim transitkit-dev-26 (UDID E25FE58E-…, iOS 26.2)
#   - Android emulator AVD transitkit-dev (se in esecuzione)
#
# UDID/AVD sono *pinned* al CLAUDE.md root del progetto. Idempotente:
# rilanciare quando vuoi cambiare operatore o sincronizzare i sim.
#
# Uso:
#   scripts/setup-dev.sh                 # default: appalcart (Boone, NC)
#   scripts/setup-dev.sh tcat            # TCAT (Ithaca, NY)
#   scripts/setup-dev.sh rfta            # RFTA (Roaring Fork, CO)

set -euo pipefail

OPERATOR_ID="${1:-appalcart}"
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CFG="$ROOT/shared/operators/$OPERATOR_ID/config.json"

if [[ ! -f "$CFG" ]]; then
    echo "✗ config.json non trovato per operatore '$OPERATOR_ID': $CFG" >&2
    echo "  Operatori disponibili:" >&2
    ls "$ROOT/shared/operators/" 2>/dev/null | sed 's/^/    - /' >&2
    exit 1
fi

LAT=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['map']['centerLat'])" "$CFG")
LON=$(python3 -c "import json,sys; print(json.load(open(sys.argv[1]))['map']['centerLng'])" "$CFG")

echo "==> operator=$OPERATOR_ID  center=$LAT,$LON"

# ---------- iOS ----------

IOS_UDIDS=(
    "4302AFD9-496E-4586-A5D0-D6BAC735FFFD"   # transitkit-dev    (iOS 18.5)
    "E25FE58E-7059-457F-A0A9-8B1E3D59145D"   # transitkit-dev-26 (iOS 26.2)
)

for UDID in "${IOS_UDIDS[@]}"; do
    STATE=$(xcrun simctl list devices 2>/dev/null | grep "$UDID" | grep -oE "Booted|Shutdown" | head -1 || true)
    if [[ -z "$STATE" ]]; then
        echo "   ⚠ iOS UDID $UDID non trovato in simctl — creare il simulatore manualmente"
        echo "     (vedi CLAUDE.md sezione SIMULATORI DI RIFERIMENTO)"
        continue
    fi
    if [[ "$STATE" == "Shutdown" ]]; then
        echo "   booting iOS sim $UDID..."
        xcrun simctl boot "$UDID" 2>/dev/null || true
        xcrun simctl bootstatus "$UDID" -b >/dev/null 2>&1 || true
    fi
    xcrun simctl location "$UDID" set "$LAT,$LON"
    echo "   ✓ iOS  $UDID → $LAT,$LON"
done

# ---------- Android ----------

ADB="/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb"
AVD_NAME="transitkit-dev"

EMU=""
for s in $("$ADB" devices 2>/dev/null | grep -E "^emulator-" | awk '{print $1}'); do
    name=$("$ADB" -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r' || true)
    if [[ "$name" == "$AVD_NAME" ]]; then
        EMU="$s"
        break
    fi
done

if [[ -z "$EMU" ]]; then
    echo "   ⚠ AVD '$AVD_NAME' non in esecuzione — avvialo con:"
    echo "       /Users/andreatoffanello/Library/Android/sdk/emulator/emulator \\"
    echo "         -avd $AVD_NAME -no-snapshot-load -no-audio &"
    echo "     poi rilancia: scripts/setup-dev.sh $OPERATOR_ID"
    exit 0
fi

# adb emu geo fix vuole LONGITUDE LATITUDE (in quest'ordine — opposto a simctl)
"$ADB" -s "$EMU" emu geo fix "$LON" "$LAT" >/dev/null
echo "   ✓ Android $EMU → $LAT,$LON"
