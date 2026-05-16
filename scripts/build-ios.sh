#!/bin/bash
set -euo pipefail

# Usage: ./scripts/build-ios.sh <operator_id> [configuration]
#   operator_id    — folder name under shared/operators/ and output/
#   configuration  — Debug | Release (default: Debug — required for local CMS smoke test)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IOS_DIR="$ROOT_DIR/ios"
RESOURCES_DIR="$IOS_DIR/TransitKit/Sources/Resources"

# ---------- Validate arguments ----------

if [ $# -lt 1 ]; then
    echo "Usage: $0 <operator_id> [Debug|Release]"
    echo "  operator_id    — folder name under shared/operators/ and output/"
    echo "  configuration  — defaults to Debug (so #if DEBUG hooks engage —"
    echo "                   PushApiClient targets http://localhost:3000)."
    echo "                   Pass 'Release' for production-style validation."
    exit 1
fi

OPERATOR_ID="$1"
CONFIGURATION="${2:-Debug}"
case "$CONFIGURATION" in
    Debug|Release) ;;
    *) echo "ERROR: configuration must be Debug or Release (got '$CONFIGURATION')"; exit 1 ;;
esac
echo "==> Building TransitKit for operator: $OPERATOR_ID (configuration: $CONFIGURATION)"

# ---------- Copy config.json ----------

CONFIG_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/config.json"
if [ ! -f "$CONFIG_SRC" ]; then
    echo "ERROR: Config not found at $CONFIG_SRC"
    exit 1
fi

mkdir -p "$RESOURCES_DIR"
cp "$CONFIG_SRC" "$RESOURCES_DIR/config.json"
echo "  ✓ Copied config.json"

# ---------- Copy schedules.json ----------

SCHEDULES_SRC="$ROOT_DIR/output/$OPERATOR_ID/schedules.json"
if [ ! -f "$SCHEDULES_SRC" ]; then
    echo "ERROR: Schedules not found at $SCHEDULES_SRC"
    exit 1
fi

cp "$SCHEDULES_SRC" "$RESOURCES_DIR/schedules.json"
echo "  ✓ Copied schedules.json"

# ---------- Copy GoogleService-Info.plist (optional — push notifications) ----------
#
# Each operator that has push notifications enabled drops a copy of their
# Firebase iOS app's plist at:
#   shared/operators/<op>/firebase/GoogleService-Info.plist
#
# Missing file is non-fatal: the app builds and runs, but
# PushNotificationManager logs a warning and disables push.

FIREBASE_PLIST_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/firebase/GoogleService-Info.plist"
FIREBASE_PLIST_DST="$RESOURCES_DIR/GoogleService-Info.plist"
if [ -f "$FIREBASE_PLIST_SRC" ]; then
    cp "$FIREBASE_PLIST_SRC" "$FIREBASE_PLIST_DST"
    echo "  ✓ Copied GoogleService-Info.plist"
else
    rm -f "$FIREBASE_PLIST_DST"
    echo "  ⚠ No Firebase plist for $OPERATOR_ID — push notifications will be disabled in the build"
    echo "    Drop one at: $FIREBASE_PLIST_SRC"
fi

# ---------- Run XcodeGen (if available and project.yml exists) ----------

if [ -f "$IOS_DIR/project.yml" ]; then
    if command -v xcodegen &>/dev/null; then
        echo "==> Running xcodegen..."
        (cd "$IOS_DIR" && xcodegen generate)
        echo "  ✓ Xcode project generated"
    else
        echo "  ⚠ xcodegen not found — skipping project generation"
        echo "    Install with: brew install xcodegen"
    fi
fi

# ---------- Build ----------

PROJECT_PATH="$IOS_DIR/TransitKit.xcodeproj"
if [ ! -d "$PROJECT_PATH" ]; then
    echo "ERROR: Xcode project not found at $PROJECT_PATH"
    echo "  Run 'brew install xcodegen' and try again."
    exit 1
fi

echo "==> Building TransitKit..."
xcodebuild \
    -project "$PROJECT_PATH" \
    -scheme TransitKit \
    -sdk iphonesimulator \
    -destination "generic/platform=iOS Simulator" \
    -configuration "$CONFIGURATION" \
    OPERATOR_ID="$OPERATOR_ID" \
    CODE_SIGNING_ALLOWED=NO \
    build \
    2>&1 | tail -20

echo ""
echo "==> Build complete for operator: $OPERATOR_ID ($CONFIGURATION)"
