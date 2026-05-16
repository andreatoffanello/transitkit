#!/bin/bash
set -euo pipefail

# Usage: ./scripts/build-android.sh <operator_id> [debug|release]
#   operator_id  — folder name under shared/operators/
#   variant      — debug (default) | release

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
ANDROID_DIR="$ROOT_DIR/android"

# Android Studio bundles a Java 17 JBR. macOS's stub `/usr/bin/java` throws
# "Unable to locate a Java Runtime" when no JDK is installed system-wide, so
# point JAVA_HOME at the bundled one if it exists.
if [ -z "${JAVA_HOME:-}" ] && [ -d "/Applications/Android Studio.app/Contents/jbr/Contents/Home" ]; then
    export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
fi

if [ $# -lt 1 ]; then
    echo "Usage: $0 <operator_id> [debug|release]"
    echo "  operator_id  — folder name under shared/operators/"
    echo "  variant      — debug (default) | release"
    exit 1
fi

OPERATOR_ID="$1"
VARIANT="${2:-debug}"
case "$VARIANT" in
    debug)   GRADLE_TASK="assembleDebug";   APK_VARIANT="debug" ;;
    release) GRADLE_TASK="assembleRelease"; APK_VARIANT="release" ;;
    *) echo "ERROR: variant must be debug or release (got '$VARIANT')"; exit 1 ;;
esac

echo "==> Building TransitKit Android for operator: $OPERATOR_ID ($VARIANT)"

# ---------- Copy operator config.json ----------

CONFIG_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/config.json"
CONFIG_DST="$ANDROID_DIR/app/src/main/assets/config.json"
if [ ! -f "$CONFIG_SRC" ]; then
    echo "ERROR: Config not found at $CONFIG_SRC"
    exit 1
fi
cp "$CONFIG_SRC" "$CONFIG_DST"
echo "  ✓ Copied config.json"

# ---------- Copy google-services.json (optional, push notifications) ----------
#
# Each operator that has push notifications drops a copy of their Firebase
# Android app's plist at:
#   shared/operators/<op>/firebase/google-services.json
#
# Missing file is non-fatal — the google-services plugin only applies when
# app/google-services.json exists (guarded in app/build.gradle.kts).

GSV_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/firebase/google-services.json"
GSV_DST="$ANDROID_DIR/app/google-services.json"
if [ -f "$GSV_SRC" ]; then
    cp "$GSV_SRC" "$GSV_DST"
    echo "  ✓ Copied google-services.json"
else
    rm -f "$GSV_DST"
    echo "  ⚠ No Firebase config for $OPERATOR_ID — push notifications will be disabled"
    echo "    Drop one at: $GSV_SRC"
fi

# ---------- Build ----------

cd "$ANDROID_DIR"
./gradlew $GRADLE_TASK 2>&1 | tail -30

echo ""
echo "==> Build complete for operator: $OPERATOR_ID ($VARIANT)"

APK_PATH="$ANDROID_DIR/app/build/outputs/apk/$APK_VARIANT/app-$APK_VARIANT.apk"
if [ -f "$APK_PATH" ]; then
    echo "==> APK: $APK_PATH"
fi
