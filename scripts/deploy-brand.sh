#!/usr/bin/env bash
set -euo pipefail

# deploy-brand.sh <operator_id>
#
# Deploys brand assets from shared/operators/<op>/brand/ to iOS and Android.
# Run from the project root: bash scripts/deploy-brand.sh appalcart
#
# ── Source files expected in shared/operators/<op>/brand/ ──────────────────
#
#   app-icon.png              ≥1000×1000, with background
#                             → iOS AppIcon.appiconset/AppIcon.png (1024×1024)
#                             → Android mipmap-*/ic_launcher.png (48–192px)
#                             → Android mipmap-*/ic_launcher_round.png (48–192px)
#
#   app-icon-foreground.png   ≥1000×1000, transparent background, subject in
#                             inner 66% of canvas (Android adaptive icon safe zone)
#                             → iOS OperatorLogo.imageset/ (header, 32pt circle)
#                             → Android mipmap-*/ic_launcher_foreground.png (108–432px)
#                             → Android drawable/app_logo.png (header, 32dp circle)
#
#   operator-logo.jpg|png     Logo reale dell'operatore (es. appalCART apple).
#                             Sfondo opaco OK — la card UI clippa con RoundedRect.
#                             → iOS SourceOperatorLogo.imageset/ (card 44pt tile)
#                             → Android drawable/operator_logo.png (card 40dp tile)
#
#   background.png            Immagine portrait (idealmente ~1320×2868).
#                             Usata come texture di sfondo nell'app e nelle onboarding.
#                             → iOS OperatorBackground.imageset/OperatorBackground.png
#                             → Android drawable/operator_background.png
#                               (rimuove operator_background.webp se presente)
#
# ── iOS mapping ────────────────────────────────────────────────────────────
#
#   Imageset                  Usato in
#   AppIcon                   Icona launcher app
#   OperatorLogo              HomeTab homeMinimalHeader — 32pt .clipShape(Circle())
#   SourceOperatorLogo        HomeTab operatorInfoCard — 44pt .clipShape(RoundedRect 12)
#   OperatorBackground        Shader background + onboarding
#
# ── Android mapping ────────────────────────────────────────────────────────
#
#   Resource                  Usato in
#   mipmap-*/ic_launcher      Icona launcher (legacy + adaptive background layer)
#   mipmap-*/ic_launcher_round Icona launcher round
#   mipmap-*/ic_launcher_foreground Adaptive icon foreground layer
#   drawable/app_logo         HomeMinimalHeader — 32dp CircleShape
#                             cercato via getIdentifier("app_logo", "drawable", ...)
#   drawable/operator_logo    OperatorReferenceCard — 40dp RoundedCornerShape(10)
#                             cercato via getIdentifier("operator_logo", "drawable", ...)
#   drawable/operator_background R.drawable.operator_background (OperatorMapBackground,
#                             LocationPrimerScreen)
#
# ── Web mapping ────────────────────────────────────────────────────────────
#
#   File                                  Usato in
#   web/public/favicon.ico                Tab icon browser (multi-size 16/32/48)
#   web/public/icons/icon-180.png         apple-touch-icon (iOS home screen PWA)
#   web/public/icons/icon-192.png         PWA manifest (Android home screen)
#   web/public/icons/icon-512.png         PWA manifest (splash + maskable)
#
# ── Requires ───────────────────────────────────────────────────────────────
#   sips     — built-in macOS, used for all PNG resize/convert operations
#   magick   — ImageMagick, only for favicon.ico multi-size generation.
#              Install: brew install imagemagick (graceful skip se assente)
# ───────────────────────────────────────────────────────────────────────────

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# ── Validate arguments ─────────────────────────────────────────────────────

if [ $# -lt 1 ]; then
    echo "Usage: $0 <operator_id>"
    echo "  operator_id — folder name under shared/operators/ (e.g. appalcart)"
    exit 1
fi

OP="$1"
BRAND="$ROOT_DIR/shared/operators/$OP/brand"
XCASSETS="$ROOT_DIR/ios/TransitKit/Sources/Resources/Assets.xcassets"
ANDROID_RES="$ROOT_DIR/android/app/src/main/res"
WEB_PUBLIC="$ROOT_DIR/web/public"

# ── Validate source files ──────────────────────────────────────────────────

echo "→ Checking brand sources for operator: $OP"

[[ -d "$BRAND" ]] || { echo "✗ Missing brand directory: $BRAND"; exit 1; }

for f in app-icon.png app-icon-foreground.png background.png; do
    [[ -f "$BRAND/$f" ]] || { echo "✗ Missing: $BRAND/$f"; exit 1; }
done

# operator-logo accepts both .jpg and .png
OPERATOR_LOGO=""
for ext in jpg jpeg png; do
    [[ -f "$BRAND/operator-logo.$ext" ]] && OPERATOR_LOGO="$BRAND/operator-logo.$ext" && break
done
[[ -n "$OPERATOR_LOGO" ]] || { echo "✗ Missing: $BRAND/operator-logo.{jpg,png}"; exit 1; }

echo "  ✓ app-icon.png"
echo "  ✓ app-icon-foreground.png"
echo "  ✓ $OPERATOR_LOGO"
echo "  ✓ background.png"

# ── iOS ───────────────────────────────────────────────────────────────────

echo ""
echo "→ Deploying iOS assets"

# AppIcon — single universal 1024×1024 (Contents.json already configured)
sips -z 1024 1024 "$BRAND/app-icon.png" \
    --out "$XCASSETS/AppIcon.appiconset/AppIcon.png" -s formatOptions best > /dev/null
echo "  ✓ AppIcon.appiconset/AppIcon.png (1024×1024)"

# OperatorLogo — bus senza sfondo, usato a 32pt con .clipShape(Circle())
sips -z 100 100 "$BRAND/app-icon-foreground.png" \
    --out "$XCASSETS/OperatorLogo.imageset/logo.png" > /dev/null
sips -z 200 200 "$BRAND/app-icon-foreground.png" \
    --out "$XCASSETS/OperatorLogo.imageset/logo@2x.png" > /dev/null
echo "  ✓ OperatorLogo.imageset/ (100px 1x, 200px 2x)"

# SourceOperatorLogo — logo reale operatore, usato a 44pt con .clipShape(RoundedRect 12)
sips -z 100 100 -s format png "$OPERATOR_LOGO" \
    --out "$XCASSETS/SourceOperatorLogo.imageset/logo.png" > /dev/null
sips -z 200 200 -s format png "$OPERATOR_LOGO" \
    --out "$XCASSETS/SourceOperatorLogo.imageset/logo@2x.png" > /dev/null
echo "  ✓ SourceOperatorLogo.imageset/ (100px 1x, 200px 2x)"

# OperatorBackground — texture fullscreen per shader e onboarding
cp "$BRAND/background.png" \
    "$XCASSETS/OperatorBackground.imageset/OperatorBackground.png"
echo "  ✓ OperatorBackground.imageset/OperatorBackground.png"

# ── Android ───────────────────────────────────────────────────────────────

echo ""
echo "→ Deploying Android assets"

# ic_launcher + ic_launcher_round (icona con sfondo)
declare -A LAUNCHER_SIZES=([mdpi]=48 [hdpi]=72 [xhdpi]=96 [xxhdpi]=144 [xxxhdpi]=192)
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    size="${LAUNCHER_SIZES[$density]}"
    sips -z "$size" "$size" "$BRAND/app-icon.png" \
        --out "$ANDROID_RES/mipmap-$density/ic_launcher.png" > /dev/null
    sips -z "$size" "$size" "$BRAND/app-icon.png" \
        --out "$ANDROID_RES/mipmap-$density/ic_launcher_round.png" > /dev/null
done
echo "  ✓ mipmap-*/ic_launcher.png + ic_launcher_round.png (48–192px)"

# ic_launcher_foreground (bus trasparente, adaptive icon — safe zone 66% del canvas)
declare -A FOREGROUND_SIZES=([mdpi]=108 [hdpi]=162 [xhdpi]=216 [xxhdpi]=324 [xxxhdpi]=432)
for density in mdpi hdpi xhdpi xxhdpi xxxhdpi; do
    size="${FOREGROUND_SIZES[$density]}"
    sips -z "$size" "$size" "$BRAND/app-icon-foreground.png" \
        --out "$ANDROID_RES/mipmap-$density/ic_launcher_foreground.png" > /dev/null
done
echo "  ✓ mipmap-*/ic_launcher_foreground.png (108–432px)"

# drawable/app_logo — bus senza sfondo per HomeMinimalHeader (32dp CircleShape)
sips -z 200 200 "$BRAND/app-icon-foreground.png" \
    --out "$ANDROID_RES/drawable/app_logo.png" > /dev/null
echo "  ✓ drawable/app_logo.png"

# drawable/operator_logo — logo reale operatore per OperatorReferenceCard (40dp tile)
sips -z 200 200 -s format png "$OPERATOR_LOGO" \
    --out "$ANDROID_RES/drawable/operator_logo.png" > /dev/null
echo "  ✓ drawable/operator_logo.png"

# drawable/operator_background — texture fullscreen (R.drawable.operator_background)
# Rimuove eventuale .webp precedente per evitare duplicate resource error a compile time.
[[ -f "$ANDROID_RES/drawable/operator_background.webp" ]] && \
    rm "$ANDROID_RES/drawable/operator_background.webp"
cp "$BRAND/background.png" "$ANDROID_RES/drawable/operator_background.png"
echo "  ✓ drawable/operator_background.png"

# ── Web ───────────────────────────────────────────────────────────────────

if [[ -d "$WEB_PUBLIC" ]]; then
    echo ""
    echo "→ Deploying Web assets"

    mkdir -p "$WEB_PUBLIC/icons"

    # PWA + apple-touch icons — generati direttamente dall'app-icon master
    for size in 180 192 512; do
        sips -Z "$size" "$BRAND/app-icon.png" \
            --out "$WEB_PUBLIC/icons/icon-$size.png" > /dev/null
    done
    echo "  ✓ icons/icon-{180,192,512}.png"

    # favicon.ico multi-size (richiede ImageMagick — graceful skip se assente)
    if command -v magick >/dev/null 2>&1; then
        magick "$BRAND/app-icon.png" -resize 256x256 \
            \( -clone 0 -resize 16x16 \) \
            \( -clone 0 -resize 32x32 \) \
            \( -clone 0 -resize 48x48 \) \
            -delete 0 "$WEB_PUBLIC/favicon.ico" 2>/dev/null
        echo "  ✓ favicon.ico (16+32+48)"
    else
        echo "  ⚠ favicon.ico skipped — install ImageMagick (brew install imagemagick) per generarlo"
    fi
fi

# ── Done ──────────────────────────────────────────────────────────────────

echo ""
echo "✓ Brand deployed for operator: $OP"
echo ""
echo "  Next steps:"
echo "  • iOS     — build e verifica visiva con: bash scripts/build-ios.sh $OP"
echo "  • Android — build e verifica visiva con: bash scripts/build-android.sh $OP"
echo "  • Web     — npm run build (cd web/) o vercel deploy"
