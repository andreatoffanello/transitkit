#!/usr/bin/env bash
# TransitKit — install + launch + verify smoke su emulator transitkit-android.
#
# Dismette qualunque dialog ANR/system bloccante, force-stop dell'app,
# launch pulito, attesa first paint, screenshot.
#
# Uso:
#   scripts/android-smoke.sh                              # install + launch + screenshot home
#   scripts/android-smoke.sh --no-install                 # solo launch (build già installato)
#   scripts/android-smoke.sh --screenshot path            # custom output path
#   scripts/android-smoke.sh --wait-text "Linee"          # attende testo specifico (oltre lo splash)
#   scripts/android-smoke.sh --grace 8                    # grace post-foreground (default 4s)
#   scripts/android-smoke.sh --no-screenshot              # solo install + launch
#   scripts/android-smoke.sh --operator appalcart         # override OPERATOR_ID (default: appalcart)
#
# Esci con exit code != 0 se: emulator non online, install fail.

set -euo pipefail

ADB="/Users/andreatoffanello/Library/Android/sdk/platform-tools/adb"
AVD_NAME="transitkit-android"
OPERATOR_ID="appalcart"
NAMESPACE="com.transitkit.app"
ACT="${NAMESPACE}.MainActivity"
APK="/Users/andreatoffanello/GitHub/transit-engine/android/app/build/outputs/apk/debug/app-debug.apk"
DO_INSTALL=1
DO_SCREENSHOT=1
OUT="/tmp/transitkit_smoke.png"
WAIT_TEXT=""
GRACE=4

while [[ $# -gt 0 ]]; do
    case "$1" in
        --no-install) DO_INSTALL=0; shift ;;
        --no-screenshot) DO_SCREENSHOT=0; shift ;;
        --screenshot) OUT="$2"; shift 2 ;;
        --wait-text) WAIT_TEXT="$2"; shift 2 ;;
        --grace) GRACE="$2"; shift 2 ;;
        --operator) OPERATOR_ID="$2"; shift 2 ;;
        -h|--help)
            grep -E "^#" "$0" | sed 's/^# \?//' | head -22
            exit 0 ;;
        *) echo "unknown flag: $1" >&2; exit 1 ;;
    esac
done

PKG="com.transitkit.${OPERATOR_ID}"

# Risolvi serial dell'emulator transitkit-android (non usare emulator-XXXX hardcoded:
# altri progetti hanno emulatori attivi con porte diverse)
EMU=""
for s in $("$ADB" devices | grep -E "^emulator-" | awk '{print $1}'); do
    name=$("$ADB" -s "$s" emu avd name 2>/dev/null | head -1 | tr -d '\r' || true)
    if [[ "$name" == "$AVD_NAME" ]]; then
        EMU="$s"
        break
    fi
done

if [[ -z "$EMU" ]]; then
    echo "✗ emulator '$AVD_NAME' non online. Avvia con:" >&2
    echo "    /Users/andreatoffanello/Library/Android/sdk/emulator/emulator -avd $AVD_NAME -no-snapshot-load -no-audio &" >&2
    exit 1
fi

# 0a) Preflight: misure di rumore-ANR sull'emulatore (idempotenti).
#     Disabilita Digital Wellbeing (ANR fantoccio ricorrente) e abilita
#     hide_error_dialogs = 1 (system non mostra dialog ANR/crash che bloccano
#     foreground). Da applicare una volta per AVD; rieseguire dopo wipe.
"$ADB" -s "$EMU" shell pm disable-user --user 0 com.google.android.apps.wellbeing >/dev/null 2>&1 || true
"$ADB" -s "$EMU" shell settings put global hide_error_dialogs 1 >/dev/null 2>&1 || true

# 0b) Wake + unlock
"$ADB" -s "$EMU" shell input keyevent KEYCODE_WAKEUP >/dev/null 2>&1 || true
sleep 1

# 1) Dismetti qualunque system dialog di ANR/crash bloccante.
for _ in 1 2 3; do
    "$ADB" -s "$EMU" shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
    sleep 0.3
done

# 2) Force-stop dell'app
"$ADB" -s "$EMU" shell am force-stop "$PKG" >/dev/null 2>&1 || true

# 3) Install
if [[ $DO_INSTALL -eq 1 ]]; then
    if [[ ! -f "$APK" ]]; then
        echo "✗ APK non trovato: $APK — esegui prima: cd android && ./gradlew assembleDebug" >&2
        exit 1
    fi
    out=$("$ADB" -s "$EMU" install -r "$APK" 2>&1)
    if ! echo "$out" | grep -q "Success"; then
        echo "✗ install failed:" >&2
        echo "$out" >&2
        exit 1
    fi
fi

# 4) Launch — applicationId/namespace possono differire (white-label):
#    package = applicationId (com.transitkit.appalcart), activity = namespace.MainActivity
"$ADB" -s "$EMU" shell am start -n "${PKG}/${ACT}" >/dev/null

# 5) Attendi foreground (max 25s)
START=$(date +%s)
while true; do
    NOW=$(date +%s)
    if (( NOW - START > 25 )); then
        echo "⚠ timeout 25s: foreground non raggiunto" >&2
        break
    fi
    TOP=$("$ADB" -s "$EMU" shell dumpsys window 2>/dev/null | grep -E "mCurrentFocus|mFocusedApp" | head -1 || true)
    if echo "$TOP" | grep -q "$PKG"; then
        break
    fi
    if echo "$TOP" | grep -qE "android|google.*wellbeing|errordialog"; then
        "$ADB" -s "$EMU" shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
    fi
    sleep 0.5
done

# 6) Wait-text (opzionale)
if [[ -n "$WAIT_TEXT" ]]; then
    WSTART=$(date +%s)
    FOUND=0
    while true; do
        NOW=$(date +%s)
        if (( NOW - WSTART > 25 )); then
            echo "⚠ timeout 25s: testo '$WAIT_TEXT' non apparso" >&2
            break
        fi
        "$ADB" -s "$EMU" shell uiautomator dump /sdcard/_smoke.xml >/dev/null 2>&1 || true
        if "$ADB" -s "$EMU" shell cat /sdcard/_smoke.xml 2>/dev/null | grep -q "$WAIT_TEXT"; then
            FOUND=1
            break
        fi
        sleep 1
    done
    if [[ $FOUND -eq 1 ]]; then
        echo "✓ testo '$WAIT_TEXT' visibile"
    fi
else
    sleep "$GRACE"
fi

# 7) Screenshot
if [[ $DO_SCREENSHOT -eq 1 ]]; then
    "$ADB" -s "$EMU" exec-out screencap -p > "$OUT"
    SZ=$(wc -c < "$OUT")
    echo "✓ screenshot: $OUT (${SZ} bytes) — serial=$EMU package=$PKG"
fi
