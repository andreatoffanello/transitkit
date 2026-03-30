#!/usr/bin/env python3
"""
TransitKit — Output validation suite.

Validates the generated schedules.json for data integrity.
Run after build.py to ensure output is correct.

Usage:
    python pipeline/validate.py <operator_id>
    python pipeline/validate.py rfta
    python pipeline/validate.py --all
"""

from __future__ import annotations

import csv
import json
import sys
from collections import defaultdict
from pathlib import Path

REPO_ROOT = Path(__file__).resolve().parent.parent


def validate(operator_id: str) -> tuple[list[str], list[str]]:
    """Validate output for an operator. Returns (errors, warnings)."""
    errors = []
    warnings = []

    output_file = REPO_ROOT / "output" / operator_id / "schedules.json"
    if not output_file.exists():
        return [f"Output file not found: {output_file}"], []

    with open(output_file) as f:
        data = json.load(f)

    # --- 1. Structure ---
    required_keys = ["operator", "lastUpdated", "headsigns", "lineNames", "routeIds", "tripIds", "routes", "stops"]
    for key in required_keys:
        if key not in data:
            errors.append(f"Missing top-level key: {key}")

    if errors:
        return errors, warnings

    # --- 2. Routes ---
    for r in data["routes"]:
        color = r["color"].lstrip("#")
        if len(color) != 6 or not all(c in "0123456789abcdefABCDEF" for c in color):
            errors.append(f"Invalid route color: {r['name']} → {r['color']}")
        if not r["name"].strip():
            errors.append(f"Empty route name for id={r['id']}")
        if r["transitType"] not in ("bus", "tram", "metro", "rail", "ferry", "cable_tram", "gondola", "funicular", "trolleybus", "monorail"):
            warnings.append(f"Unknown transit type: {r['transitType']} for {r['name']}")
        for d in r.get("directions", []):
            if len(d.get("stopIds", [])) < 2:
                warnings.append(f"Direction with <2 stops: {r['name']} dir={d['id']}")
            if not d.get("headsign", "").strip():
                warnings.append(f"Empty headsign: {r['name']} dir={d['id']}")

    # --- 3. Stops ---
    if len(data["stops"]) < 5:
        errors.append(f"Too few stops: {len(data['stops'])} (expected ≥5)")

    lats = [s["lat"] for s in data["stops"]]
    lngs = [s["lng"] for s in data["stops"]]
    if min(lats) < -90 or max(lats) > 90:
        errors.append(f"Invalid latitude: {min(lats)} to {max(lats)}")
    if min(lngs) < -180 or max(lngs) > 180:
        errors.append(f"Invalid longitude: {min(lngs)} to {max(lngs)}")

    empty_stops = [s for s in data["stops"] if not s["departures"]]
    if empty_stops:
        errors.append(f"{len(empty_stops)} stops with zero departures")

    # Duplicate station IDs
    ids = [s["id"] for s in data["stops"]]
    if len(ids) != len(set(ids)):
        dups = [i for i in ids if ids.count(i) > 1]
        errors.append(f"Duplicate stop IDs: {set(dups)}")

    # --- 4. Departures ---
    valid_days = {"mon", "tue", "wed", "thu", "fri", "sat", "sun"}
    total_deps = 0
    bad_times = 0
    bad_refs = 0

    for s in data["stops"]:
        for day_key, deps in s["departures"].items():
            for d in day_key.split(","):
                if d not in valid_days:
                    errors.append(f"Invalid day '{d}' in stop {s['name']}")

            for dep in deps:
                total_deps += 1
                # Time format
                if len(dep[0]) != 5 or dep[0][2] != ":":
                    bad_times += 1
                # Index bounds
                if dep[1] < 0 or dep[1] >= len(data["lineNames"]):
                    bad_refs += 1
                if dep[2] < 0 or dep[2] >= len(data["headsigns"]):
                    bad_refs += 1
                if len(dep) >= 6 and (dep[5] < 0 or dep[5] >= len(data["tripIds"])):
                    bad_refs += 1

    if bad_times:
        errors.append(f"{bad_times} departures with invalid time format")
    if bad_refs:
        errors.append(f"{bad_refs} departures with invalid index references")
    if total_deps < 100:
        errors.append(f"Too few departures: {total_deps} (expected ≥100)")

    # --- 5. Cross-references ---
    if len(data["lineNames"]) != len(data["routeIds"]):
        errors.append(f"lineNames/routeIds length mismatch: {len(data['lineNames'])} vs {len(data['routeIds'])}")

    # --- 6. Stats (informational) ---
    stats = {
        "stops": len(data["stops"]),
        "routes": len(data["routes"]),
        "departures": total_deps,
        "headsigns": len(data["headsigns"]),
        "patterns": len(data.get("stopPatterns", [])),
        "lat_range": f"{min(lats):.3f} to {max(lats):.3f}",
        "lng_range": f"{min(lngs):.3f} to {max(lngs):.3f}",
    }

    return errors, warnings, stats


def main():
    if len(sys.argv) < 2:
        print("Usage: python pipeline/validate.py <operator_id> | --all")
        sys.exit(1)

    if sys.argv[1] == "--all":
        output_dir = REPO_ROOT / "output"
        operators = [d.name for d in output_dir.iterdir() if d.is_dir()]
    else:
        operators = [sys.argv[1]]

    all_pass = True
    for op in sorted(operators):
        result = validate(op)
        errors, warnings = result[0], result[1]
        stats = result[2] if len(result) > 2 else {}

        print(f"\n{'='*50}")
        print(f"  {op.upper()}")
        print(f"{'='*50}")

        if stats:
            print(f"  Stops: {stats['stops']} | Routes: {stats['routes']} | Departures: {stats['departures']}")
            print(f"  Headsigns: {stats['headsigns']} | Patterns: {stats['patterns']}")
            print(f"  Lat: {stats['lat_range']} | Lng: {stats['lng_range']}")

        if errors:
            all_pass = False
            print(f"\n  ❌ {len(errors)} ERRORS:")
            for e in errors:
                print(f"    ✘ {e}")

        if warnings:
            print(f"\n  ⚠️  {len(warnings)} WARNINGS:")
            for w in warnings:
                print(f"    ⚠ {w}")

        if not errors:
            print(f"\n  ✅ PASSED")

    print(f"\n{'='*50}")
    if all_pass:
        print(f"✅ All {len(operators)} operator(s) passed validation.")
    else:
        print(f"❌ Some operators failed validation.")
        sys.exit(1)


if __name__ == "__main__":
    main()
