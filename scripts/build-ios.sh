#!/bin/bash
set -euo pipefail

# Usage: ./scripts/build-ios.sh <operator_id>
# Copies config.json and schedules.json into the Xcode project, then builds.

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IOS_DIR="$ROOT_DIR/ios"
RESOURCES_DIR="$IOS_DIR/TransitKit/Sources/Resources"

# ---------- Validate arguments ----------

if [ $# -lt 1 ]; then
    echo "Usage: $0 <operator_id>"
    echo "  operator_id  — folder name under shared/operators/ and output/"
    exit 1
fi

OPERATOR_ID="$1"
echo "==> Building TransitKit for operator: $OPERATOR_ID"

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
    -configuration Release \
    OPERATOR_ID="$OPERATOR_ID" \
    build \
    2>&1 | tail -20

echo ""
echo "==> Build complete for operator: $OPERATOR_ID"
