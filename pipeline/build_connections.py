#!/usr/bin/env python3
"""
TransitKit — GTFS → connections.json.zlib builder.
Generates a compressed connections file for the on-device CSA trip planner.

Format matches RoutingEngine.swift expectations:
  connections: [[dep_stop_idx, arr_stop_idx, dep_sec, arr_sec, trip_idx, line_idx], ...]
  footpaths:   [[stop_a_idx, stop_b_idx, walk_sec], ...]
  stops:       [{"id", "name", "lat", "lng"}, ...]
  line_names:  ["BRT", "S", ...]
  trip_ids:    ["trip_1", ...]
  line_colors: ["AD2B3C", ...]  (no #)
  line_text_colors: ["FFFFFF", ...]

Usage:
    python pipeline/build_connections.py appalcart
    python pipeline/build_connections.py appalcart --output /tmp/connections.json.zlib
"""
from __future__ import annotations

import io
import json
import math
import re
import ssl
import sys
import zlib
import zipfile
from collections import defaultdict
from datetime import datetime, date
from pathlib import Path
from urllib.request import urlopen, Request

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
FOOTPATH_MAX_METERS = 400
FOOTPATH_WALK_SPEED_MPS = 1.2   # 4.3 km/h
FOOTPATH_MIN_METERS = 30


def haversine_m(lat1: float, lon1: float, lat2: float, lon2: float) -> float:
    R = 6_371_000
    phi1, phi2 = math.radians(lat1), math.radians(lat2)
    dp = math.radians(lat2 - lat1)
    dl = math.radians(lon2 - lon1)
    a = math.sin(dp/2)**2 + math.cos(phi1) * math.cos(phi2) * math.sin(dl/2)**2
    return 2 * R * math.asin(math.sqrt(a))


def time_to_sec(t: str) -> int | None:
    if not t or not t.strip():
        return None
    parts = t.strip().split(":")
    if len(parts) < 2:
        return None
    return int(parts[0]) * 3600 + int(parts[1]) * 60 + (int(parts[2]) if len(parts) > 2 else 0)


def load_gtfs(source: str) -> dict[str, list[dict]]:
    """Load GTFS from URL or local zip/directory."""
    tables = {}

    def parse_zip(data: bytes) -> dict[str, list[dict]]:
        with zipfile.ZipFile(io.BytesIO(data)) as zf:
            for name in zf.namelist():
                if not name.endswith(".txt"):
                    continue
                key = name.rsplit("/", 1)[-1][:-4]
                with zf.open(name) as f:
                    content = f.read().decode("utf-8-sig")
                    rows = list(csv.DictReader(content.splitlines()))
                    tables[key] = rows
        return tables

    import csv
    path = Path(source)
    if path.is_dir():
        for txt in path.glob("*.txt"):
            with open(txt, encoding="utf-8-sig") as f:
                tables[txt.stem] = list(csv.DictReader(f))
        return tables
    if path.is_file():
        return parse_zip(path.read_bytes())
    # URL
    ctx = ssl.create_default_context()
    ctx.check_hostname = False
    ctx.verify_mode = ssl.CERT_NONE
    req = Request(source, headers={"User-Agent": "TransitKit-Pipeline/1.0"})
    print(f"  Downloading {source}...")
    with urlopen(req, timeout=120, context=ctx) as resp:
        data = resp.read()
    print(f"  Downloaded: {len(data)//1024} KB")
    return parse_zip(data)


def _make_station_id(operator_id: str, name: str) -> str:
    slug = name.lower()
    slug = slug.replace(".", "").replace("'", "").replace('"', '')
    slug = re.sub(r'[^a-z0-9]+', '_', slug).strip('_')
    return f"{operator_id}_{slug}"


def build_connections(gtfs: dict, config: dict) -> dict:
    stops_gtfs = gtfs.get("stops", [])
    routes_gtfs = gtfs.get("routes", [])
    trips_gtfs = gtfs.get("trips", [])
    stop_times_gtfs = gtfs.get("stop_times", [])

    operator_id = config.get("id", "")

    # Build stop index: gtfs_stop_id → {id (station_id), name, lat, lng}
    # Use same ID format as build.py: {operator_id}_{slugified_name}
    stop_map: dict[str, dict] = {}
    for s in stops_gtfs:
        sid = s.get("stop_id", "").strip()
        if not sid:
            continue
        name = s.get("stop_name", "").strip()
        station_id = _make_station_id(operator_id, name) if operator_id else sid
        stop_map[sid] = {
            "id": station_id,
            "name": name,
            "lat": float(s.get("stop_lat", 0) or 0),
            "lng": float(s.get("stop_lon", 0) or 0),
        }

    # Build route index: route_id → {name, color, text_color}
    route_map: dict[str, dict] = {}
    for r in routes_gtfs:
        rid = r.get("route_id", "").strip()
        route_map[rid] = {
            "name": (r.get("route_short_name") or r.get("route_long_name", "")).strip(),
            "color": r.get("route_color", "").strip().lstrip("#"),
            "text_color": r.get("route_text_color", "").strip().lstrip("#"),
        }

    # Build trip→route map
    trip_to_route: dict[str, str] = {}
    for t in trips_gtfs:
        tid = t.get("trip_id", "").strip()
        rid = t.get("route_id", "").strip()
        if tid and rid:
            trip_to_route[tid] = rid

    # Group stop_times by trip_id
    print("  Grouping stop times by trip...")
    trip_stop_times: dict[str, list[dict]] = defaultdict(list)
    for st in stop_times_gtfs:
        tid = st.get("trip_id", "").strip()
        if tid:
            trip_stop_times[tid].append(st)

    # Sort each trip's stop_times by stop_sequence
    for tid in trip_stop_times:
        trip_stop_times[tid].sort(key=lambda x: int(x.get("stop_sequence", 0)))

    # Collect all unique stop IDs that are actually used in stop_times
    used_stop_ids: set[str] = set()
    for sts in trip_stop_times.values():
        for st in sts:
            used_stop_ids.add(st.get("stop_id", "").strip())

    # Build compact stops array (only used stops)
    stops_list: list[dict] = []
    stop_idx: dict[str, int] = {}
    for sid in sorted(used_stop_ids):  # sorted for determinism
        if sid in stop_map:
            stop_idx[sid] = len(stops_list)
            stops_list.append(stop_map[sid])

    print(f"  Used stops: {len(stops_list)}")

    # Build compact line/trip arrays
    line_names: list[str] = []
    line_name_idx: dict[str, int] = {}
    line_colors: list[str] = []
    line_text_colors: list[str] = []
    trip_ids: list[str] = []
    trip_id_idx: dict[str, int] = {}

    # Build connections
    print("  Building connections...")
    connections: list[list[int]] = []
    skipped = 0

    for tid, sts in trip_stop_times.items():
        if len(sts) < 2:
            continue
        rid = trip_to_route.get(tid, "")
        route = route_map.get(rid, {"name": rid, "color": "000000", "text_color": ""})
        lname = route["name"] or rid

        if lname not in line_name_idx:
            line_name_idx[lname] = len(line_names)
            line_names.append(lname)
            line_colors.append(route["color"])
            line_text_colors.append(route["text_color"])
        lidx = line_name_idx[lname]

        if tid not in trip_id_idx:
            trip_id_idx[tid] = len(trip_ids)
            trip_ids.append(tid)
        tidx = trip_id_idx[tid]

        for i in range(len(sts) - 1):
            dep_st = sts[i]
            arr_st = sts[i + 1]
            dep_sid = dep_st.get("stop_id", "").strip()
            arr_sid = arr_st.get("stop_id", "").strip()
            dep_sec = time_to_sec(dep_st.get("departure_time", "") or dep_st.get("arrival_time", ""))
            arr_sec = time_to_sec(arr_st.get("arrival_time", "") or arr_st.get("departure_time", ""))

            if dep_sid not in stop_idx or arr_sid not in stop_idx:
                skipped += 1; continue
            if dep_sec is None or arr_sec is None:
                skipped += 1; continue
            if arr_sec <= dep_sec:
                # Midnight crossover: add 86400
                if arr_sec < dep_sec:
                    arr_sec += 86400
                else:
                    skipped += 1; continue

            connections.append([
                stop_idx[dep_sid],
                stop_idx[arr_sid],
                dep_sec, arr_sec,
                tidx, lidx
            ])

    # Sort by dep_sec (required by CSA forward scan)
    connections.sort(key=lambda c: c[2])
    print(f"  Connections: {len(connections):,}  (skipped: {skipped})")

    # Build footpaths: pairs of stops within FOOTPATH_MAX_METERS
    print("  Building footpaths...")
    footpaths: list[list[int]] = []
    n = len(stops_list)
    # Use a grid bucket approach for performance
    for i in range(n):
        si = stops_list[i]
        for j in range(i + 1, n):
            sj = stops_list[j]
            # Fast lat/lng bounding box pre-filter (1° lat ≈ 111km)
            if abs(si["lat"] - sj["lat"]) > 0.004:
                continue
            if abs(si["lng"] - sj["lng"]) > 0.006:
                continue
            d = haversine_m(si["lat"], si["lng"], sj["lat"], sj["lng"])
            if FOOTPATH_MIN_METERS <= d <= FOOTPATH_MAX_METERS:
                walk_sec = max(60, int(d / FOOTPATH_WALK_SPEED_MPS))
                footpaths.append([i, j, walk_sec])
                footpaths.append([j, i, walk_sec])

    print(f"  Footpaths: {len(footpaths):,}")

    return {
        "stops": stops_list,
        "line_names": line_names,
        "trip_ids": trip_ids,
        "line_colors": line_colors,
        "line_text_colors": line_text_colors,
        "connections": connections,
        "footpaths": footpaths,
        "generated_at": datetime.utcnow().isoformat() + "Z",
    }


def main():
    import argparse
    ap = argparse.ArgumentParser()
    ap.add_argument("operator_id")
    ap.add_argument("--output", help="Output path for .zlib file")
    args = ap.parse_args()

    op_id = args.operator_id
    data_dir = REPO_ROOT / "data" / op_id
    config_file = next(
        (p for p in [
            REPO_ROOT / "transitkit-data" / op_id / "config.json",
            REPO_ROOT / "shared" / "operators" / op_id / "config.json",
            data_dir / "config.json",
        ] + list((REPO_ROOT / "pipeline" / "operators").glob(f"{op_id}/*.json"))
        if p.exists()),
        None
    )

    # Load operator config
    config = {}
    if config_file and config_file.exists():
        config = json.loads(config_file.read_text())

    gtfs_url = config.get("gtfs_url")
    gtfs_dir = data_dir / "gtfs"

    print(f"=== TransitKit — Connections builder for '{op_id}' ===\n")

    if gtfs_dir.exists() and any(gtfs_dir.glob("*.txt")):
        print(f"[1/3] Loading GTFS from {gtfs_dir}...")
        gtfs = load_gtfs(str(gtfs_dir))
    elif gtfs_url:
        print(f"[1/3] Downloading GTFS from {gtfs_url}...")
        gtfs = load_gtfs(gtfs_url)
    else:
        # Try CDN config
        cdn_config_url = f"https://andreatoffanello.github.io/transitkit/{op_id}/config.json"
        try:
            import ssl as _ssl, urllib.request as _ur
            ctx = _ssl.create_default_context(); ctx.check_hostname=False; ctx.verify_mode=_ssl.CERT_NONE
            with _ur.urlopen(cdn_config_url, timeout=10, context=ctx) as r:
                cdn_cfg = json.loads(r.read())
            gtfs_url = cdn_cfg.get("gtfs_url")
            if gtfs_url:
                print(f"[1/3] Downloading GTFS from {gtfs_url} (via CDN config)...")
                gtfs = load_gtfs(gtfs_url)
            else:
                raise ValueError(f"No GTFS URL found for {op_id}")
        except Exception as e:
            print(f"ERROR: {e}"); sys.exit(1)

    print(f"\n[2/3] Building connections...")
    result = build_connections(gtfs, config)

    out_json = json.dumps(result, separators=(",", ":"), ensure_ascii=False).encode()
    out_zlib = zlib.compress(out_json, level=9)

    output_path = Path(args.output) if args.output else \
        REPO_ROOT / "transitkit-data" / op_id / "connections.json.zlib"
    output_path.parent.mkdir(parents=True, exist_ok=True)
    output_path.write_bytes(out_zlib)

    print(f"\n[3/3] Output: {output_path}")
    print(f"  JSON: {len(out_json)//1024} KB → Zlib: {len(out_zlib)//1024} KB")
    print(f"  Stops: {len(result['stops'])}")
    print(f"  Lines: {len(result['line_names'])}")
    print(f"  Connections: {len(result['connections']):,}")
    print(f"  Footpaths: {len(result['footpaths']):,}")
    print("\nDone.")


if __name__ == "__main__":
    main()
