#!/usr/bin/env bash
# Run iOS Maestro flows with auto-recovery on crash/hang.
# On failure: kills app, reboots simulator, reinstalls, retries once.
set -euo pipefail

UDID="4302AFD9-496E-4586-A5D0-D6BAC735FFFD"
BUNDLE="com.transitkit.appalcart"
FLOW_TIMEOUT=45  # seconds per flow before kill
JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export JAVA_HOME

PASS=0; FAIL=0; SKIP=0
FAILED_FLOWS=()

recover() {
  echo "  [recover] terminating app..."
  xcrun simctl terminate "$UDID" "$BUNDLE" 2>/dev/null || true
  sleep 1
  echo "  [recover] relaunching app..."
  xcrun simctl launch "$UDID" "$BUNDLE" 2>/dev/null || true
  sleep 2
}

run_flow() {
  local flow="$1"
  local name
  name=$(basename "$flow" .yaml)

  printf "%-50s " "$name"

  # Run with a hard timeout — if Maestro hangs, kill it
  if timeout "$FLOW_TIMEOUT" maestro --device "$UDID" test "$flow" \
       2>&1 | grep -qE "FAILED|error:"; then
    # Assertion failure (flow ran but a step failed)
    echo "FAIL"
    recover
    # Retry once
    printf "%-50s " "$name (retry)"
    if timeout "$FLOW_TIMEOUT" maestro --device "$UDID" test "$flow" \
         2>&1 | grep -qE "FAILED|error:"; then
      echo "FAIL (gave up)"
      FAILED_FLOWS+=("$name")
      FAIL=$((FAIL + 1))
    else
      echo "PASS (after retry)"
      PASS=$((PASS + 1))
    fi
  elif [ $? -eq 124 ]; then
    # timeout(1) killed the process — hard hang
    echo "TIMEOUT"
    recover
    FAILED_FLOWS+=("$name [TIMEOUT]")
    FAIL=$((FAIL + 1))
  else
    echo "PASS"
    PASS=$((PASS + 1))
  fi
}

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== TransitKit iOS E2E — $(date '+%H:%M:%S') ==="
echo ""

for flow in \
  "$SCRIPT_DIR/ios_smoke.yaml" \
  "$SCRIPT_DIR/ios_stop_detail_filter_chips.yaml" \
  "$SCRIPT_DIR/ios_stop_detail_planner_origin.yaml" \
  "$SCRIPT_DIR/ios_stop_detail_planner_destination.yaml" \
  "$SCRIPT_DIR/ios_line_detail_map_hero.yaml" \
  "$SCRIPT_DIR/ios_line_detail_map_expand.yaml" \
  "$SCRIPT_DIR/ios_stop_detail_full_schedule.yaml" \
  "$SCRIPT_DIR/ios_mappa_stop_preview.yaml"; do
  run_flow "$flow"
done

echo ""
echo "=== Results: $PASS passed, $FAIL failed ==="
if [ ${#FAILED_FLOWS[@]} -gt 0 ]; then
  echo "Failed:"
  for f in "${FAILED_FLOWS[@]}"; do echo "  - $f"; done
  exit 1
fi
