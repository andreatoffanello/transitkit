#!/bin/bash
set -euo pipefail

# Usage: ./scripts/upload-ios.sh <operator_id>
#   operator_id — folder name under shared/operators/
#
# Archives the TransitKit iOS app for the given operator, exports an
# App Store-signed IPA, and uploads it to App Store Connect via
# `xcrun altool` using the ASC API key configured in ~/.zshrc.
#
# Required env (already set in ~/.zshrc):
#   ASC_API_KEY_ID         — key id (e.g. 4RM9VCXDCC)
#   ASC_API_ISSUER_ID      — issuer UUID
#   ASC_API_KEY_PATH       — path to AuthKey_<id>.p8
#
# Existing infra (already configured, do not touch):
#   ios/ExportOptions.plist — method=app-store-connect, team=R56VCP5L45
#   Provisioning profile "iOS Team Store Provisioning Profile:
#     com.transitkit.<op>" installed in
#     ~/Library/Developer/Xcode/UserData/Provisioning Profiles/

if [ $# -lt 1 ]; then
    echo "Usage: $0 <operator_id>"
    exit 1
fi

OPERATOR_ID="$1"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"
IOS_DIR="$ROOT_DIR/ios"
BUILD_DIR="$IOS_DIR/build"
ARCHIVE_PATH="$BUILD_DIR/TransitKit-$OPERATOR_ID.xcarchive"
EXPORT_DIR="$BUILD_DIR/export-$OPERATOR_ID"

# Load ASC API env vars. They live in ~/.zshrc (zsh syntax), but this
# script runs under bash. Cherry-pick the three `export ASC_API_*=...`
# lines and eval them — they're our own well-formed exports, safe to source.
if [ -z "${ASC_API_KEY_ID:-}" ] && [ -f "$HOME/.zshrc" ]; then
    while IFS= read -r line; do
        eval "$line"
    done < <(grep -E '^export ASC_API_(KEY_ID|ISSUER_ID|KEY_PATH)=' "$HOME/.zshrc")
fi

: "${ASC_API_KEY_ID:?ASC_API_KEY_ID not set — check ~/.zshrc}"
: "${ASC_API_ISSUER_ID:?ASC_API_ISSUER_ID not set — check ~/.zshrc}"
: "${ASC_API_KEY_PATH:?ASC_API_KEY_PATH not set — check ~/.zshrc}"

# ---------- Stage resources (config, schedules, Firebase plist) ----------
echo "==> Staging resources for $OPERATOR_ID"
RESOURCES_DIR="$IOS_DIR/TransitKit/Sources/Resources"

CONFIG_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/config.json"
[ -f "$CONFIG_SRC" ] || { echo "ERROR: missing $CONFIG_SRC"; exit 1; }
cp "$CONFIG_SRC" "$RESOURCES_DIR/config.json"

# ScheduleLoader reads memory → disk cache → CDN; it never falls back to the
# bundle. `resources:` in project.yml globs this whole directory, so a copy
# left here by an older build would ship ~6 MB of dead weight.
rm -f "$RESOURCES_DIR/schedules.json"

FIREBASE_SRC="$ROOT_DIR/shared/operators/$OPERATOR_ID/firebase/GoogleService-Info.plist"
if [ -f "$FIREBASE_SRC" ]; then
    cp "$FIREBASE_SRC" "$RESOURCES_DIR/GoogleService-Info.plist"
    echo "  ✓ Firebase plist copied"
else
    rm -f "$RESOURCES_DIR/GoogleService-Info.plist"
    echo "  ⚠ No Firebase plist — push disabled in this build"
fi

# ---------- Regenerate xcodeproj from project.yml ----------
echo "==> Regenerating Xcode project"
(cd "$IOS_DIR" && xcodegen generate >/dev/null)

# ---------- Archive ----------
rm -rf "$ARCHIVE_PATH" "$EXPORT_DIR"
mkdir -p "$BUILD_DIR"

echo "==> Archiving Release build (this takes a few minutes)"
xcodebuild \
    -project "$IOS_DIR/TransitKit.xcodeproj" \
    -scheme TransitKit \
    -configuration Release \
    -destination "generic/platform=iOS" \
    -archivePath "$ARCHIVE_PATH" \
    -allowProvisioningUpdates \
    -authenticationKeyPath "$ASC_API_KEY_PATH" \
    -authenticationKeyID "$ASC_API_KEY_ID" \
    -authenticationKeyIssuerID "$ASC_API_ISSUER_ID" \
    OPERATOR_ID="$OPERATOR_ID" \
    archive 2>&1 | \
    grep --line-buffered -E "error:|warning:|Code Sign error|ARCHIVE SUCCEEDED|ARCHIVE FAILED|\*\* ARCHIVE" || true

if [ ! -d "$ARCHIVE_PATH" ]; then
    echo "ARCHIVE FAILED — no xcarchive produced at $ARCHIVE_PATH"
    exit 1
fi
echo "  ✓ Archive: $ARCHIVE_PATH"

# ---------- Export + upload to App Store Connect ----------
#
# ExportOptions.plist sets `destination: upload`, so -exportArchive signs AND
# uploads in one step, leaving no .ipa on disk. Don't look for one: an
# "IPA missing" check here reports EXPORT FAILED on a *successful* upload.
#
# Fallback if Apple 500s on this path (it has before): flip ExportOptions to
# `destination: export`, then upload the emitted .ipa with
#   xcrun altool --upload-app --type ios --file "$EXPORT_DIR"/*.ipa \
#     --apiKey "$ASC_API_KEY_ID" --apiIssuer "$ASC_API_ISSUER_ID"
# altool reads the key from those flags; keep the two in sync.
#
# grep alone can't gate this: it exits 1 when a clean run matches none of its
# patterns, so `| grep ... || true` (and pipefail) can't tell success from
# failure. Take xcodebuild's own status via PIPESTATUS and keep the full log —
# the interesting errors (e.g. "bundle version ... already been used") are
# server-side and appear nowhere else.
EXPORT_LOG="$BUILD_DIR/export-$OPERATOR_ID.log"
echo "==> Exporting + uploading to App Store Connect"
set +e
xcodebuild \
    -exportArchive \
    -archivePath "$ARCHIVE_PATH" \
    -exportPath "$EXPORT_DIR" \
    -exportOptionsPlist "$IOS_DIR/ExportOptions.plist" \
    -allowProvisioningUpdates \
    -authenticationKeyPath "$ASC_API_KEY_PATH" \
    -authenticationKeyID "$ASC_API_KEY_ID" \
    -authenticationKeyIssuerID "$ASC_API_ISSUER_ID" 2>&1 | \
    tee "$EXPORT_LOG" | \
    grep --line-buffered -E "error:|Starting upload|EXPORT SUCCEEDED|EXPORT FAILED|\*\* EXPORT"
EXPORT_STATUS=${PIPESTATUS[0]}
set -e

if [ "$EXPORT_STATUS" -ne 0 ]; then
    echo "EXPORT/UPLOAD FAILED (xcodebuild exit $EXPORT_STATUS) — full log: $EXPORT_LOG"
    exit 1
fi
echo "  ✓ Uploaded to App Store Connect"

echo ""
echo "==> Build uploaded. Apple processing takes 5–15 min; it does not appear"
echo "    in the API immediately after upload, so a missing build right after"
echo "    this step means 'not indexed yet', not 'upload failed'. Track at"
echo "    https://appstoreconnect.apple.com/apps"
