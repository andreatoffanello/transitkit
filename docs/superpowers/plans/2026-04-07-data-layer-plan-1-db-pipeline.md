# Data Layer — Plan 1: DB Schema + Pipeline

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Create the Neon DB schema and refactor the pipeline to write directly to Neon instead of producing a JSON file.

**Architecture:** The pipeline reads GTFS files, applies a per-operator normalizer, then writes to a Neon PostgreSQL database. The existing build.py orchestration is preserved but gains a `--output db` flag. A `db_writer.py` module handles all DB writes.

**Tech Stack:** Python 3.11+, psycopg2-binary, Neon PostgreSQL

---

## Task 1 — Create `pipeline/schema.sql`

Complete PostgreSQL schema with all tables, indexes, constraints, and a `schema_version` table. Use `CREATE TABLE IF NOT EXISTS` throughout for idempotency.

- [ ] Create file `pipeline/schema.sql` with the following content:

```sql
-- schema_version — single row sentinel for migrations
CREATE TABLE IF NOT EXISTS schema_version (
    version INTEGER NOT NULL
);
INSERT INTO schema_version (version)
SELECT 1
WHERE NOT EXISTS (SELECT 1 FROM schema_version);

-- Operator metadata and feature flags
CREATE TABLE IF NOT EXISTS operators (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    url         TEXT,
    timezone    TEXT NOT NULL,
    features    JSONB NOT NULL DEFAULT '{}'
);

-- Routes (transit lines)
CREATE TABLE IF NOT EXISTS routes (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    long_name     TEXT,
    color         TEXT,
    text_color    TEXT,
    transit_type  INTEGER NOT NULL
);

-- Stops (grouped stations — one row per logical stop, not per GTFS platform)
CREATE TABLE IF NOT EXISTS stops (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
    name          TEXT NOT NULL,
    lat           DOUBLE PRECISION NOT NULL,
    lng           DOUBLE PRECISION NOT NULL,
    platform_code TEXT,
    dock_letter   TEXT
);

-- Trips
CREATE TABLE IF NOT EXISTS trips (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id) ON DELETE CASCADE,
    route_id      TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    direction_id  INTEGER NOT NULL,
    headsign      TEXT,
    service_days  TEXT[] NOT NULL
);

-- Stop times
CREATE TABLE IF NOT EXISTS stop_times (
    trip_id         TEXT NOT NULL REFERENCES trips(id) ON DELETE CASCADE,
    stop_id         TEXT NOT NULL REFERENCES stops(id) ON DELETE CASCADE,
    arrival_time    TEXT NOT NULL,
    departure_time  TEXT NOT NULL,
    stop_sequence   INTEGER NOT NULL,
    PRIMARY KEY (trip_id, stop_sequence)
);

-- Route directions (canonical stop sequence + shape per direction)
CREATE TABLE IF NOT EXISTS route_directions (
    route_id        TEXT NOT NULL REFERENCES routes(id) ON DELETE CASCADE,
    direction_id    INTEGER NOT NULL,
    headsign        TEXT,
    stop_ids        TEXT[] NOT NULL,
    shape_polyline  TEXT,
    PRIMARY KEY (route_id, direction_id)
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_routes_operator        ON routes(operator_id);
CREATE INDEX IF NOT EXISTS idx_stops_operator         ON stops(operator_id);
CREATE INDEX IF NOT EXISTS idx_trips_operator         ON trips(operator_id);
CREATE INDEX IF NOT EXISTS idx_trips_route            ON trips(route_id);
CREATE INDEX IF NOT EXISTS idx_stop_times_stop        ON stop_times(stop_id);
CREATE INDEX IF NOT EXISTS idx_stop_times_trip        ON stop_times(trip_id);
CREATE INDEX IF NOT EXISTS idx_route_directions_route ON route_directions(route_id);
```

---

## Task 2 — Apply schema to Neon

Install psycopg2-binary and create the script that reads `DATABASE_URL` from the operator's `.env` file and applies `schema.sql`.

- [ ] Install the dependency:

```bash
pip install psycopg2-binary python-dotenv
```

- [ ] Create file `pipeline/apply_schema.py` with the following content:

```python
#!/usr/bin/env python3
"""
Apply pipeline/schema.sql to the operator's Neon database.

Usage:
    python pipeline/apply_schema.py rfta
"""

from __future__ import annotations

import os
import sys
from pathlib import Path

import psycopg2
from dotenv import load_dotenv

SCRIPT_DIR = Path(__file__).resolve().parent
REPO_ROOT = SCRIPT_DIR.parent
SCHEMA_FILE = SCRIPT_DIR / "schema.sql"


def main() -> None:
    if len(sys.argv) < 2:
        print("Usage: python pipeline/apply_schema.py <operator_id>")
        sys.exit(1)

    operator_id = sys.argv[1]
    env_path = REPO_ROOT / "shared" / "operators" / operator_id / ".env"

    if not env_path.exists():
        print(f"ERROR: .env file not found at {env_path}")
        print(f"       Copy {env_path.parent}/.env.example and fill in DATABASE_URL.")
        sys.exit(1)

    load_dotenv(env_path)
    database_url = os.environ.get("DATABASE_URL")

    if not database_url:
        print(f"ERROR: DATABASE_URL not set in {env_path}")
        sys.exit(1)

    schema_sql = SCHEMA_FILE.read_text(encoding="utf-8")

    print(f"Connecting to Neon for operator '{operator_id}'...")
    conn = psycopg2.connect(database_url)
    conn.autocommit = True
    try:
        with conn.cursor() as cur:
            cur.execute(schema_sql)
        print("Schema applied successfully.")

        # Verify schema_version
        with conn.cursor() as cur:
            cur.execute("SELECT version FROM schema_version;")
            row = cur.fetchone()
            print(f"schema_version = {row[0]}")
    finally:
        conn.close()


if __name__ == "__main__":
    main()
```

- [ ] Run it:

```bash
python pipeline/apply_schema.py rfta
```

---

## Task 3 — Create `pipeline/db_writer.py`

Module with functions for bulk-writing each entity type to Neon. Uses `psycopg2.extras.execute_batch` for performance. All functions accept an open `psycopg2` connection.

- [ ] Create file `pipeline/db_writer.py` with the following content:

```python
"""
DB writer for TransitKit pipeline.

All public functions accept an open psycopg2 connection and operator_id.
They are designed to be called after clear_operator_data() for a clean
re-import. Use psycopg2.extras.execute_batch for bulk inserts.
"""

from __future__ import annotations

import psycopg2
import psycopg2.extras
from typing import Any


def clear_operator_data(conn: psycopg2.extensions.connection, operator_id: str) -> None:
    """Delete all data for an operator. Cascades to routes, stops, trips, stop_times,
    route_directions via ON DELETE CASCADE. The operator row itself is also deleted
    so write_operator() can re-insert it fresh."""
    with conn.cursor() as cur:
        # Cascade via FK: deleting operator removes routes, stops, trips,
        # stop_times (via trips), route_directions (via routes).
        cur.execute("DELETE FROM operators WHERE id = %s;", (operator_id,))
    conn.commit()
    print(f"  Cleared existing data for operator '{operator_id}'.")


def write_operator(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    config: dict[str, Any],
) -> None:
    """Insert or update the operator row.

    config must have: name (str), timezone (str).
    Optional: url (str), features (dict).
    """
    sql = """
        INSERT INTO operators (id, name, url, timezone, features)
        VALUES (%(id)s, %(name)s, %(url)s, %(timezone)s, %(features)s)
        ON CONFLICT (id) DO UPDATE SET
            name     = EXCLUDED.name,
            url      = EXCLUDED.url,
            timezone = EXCLUDED.timezone,
            features = EXCLUDED.features;
    """
    with conn.cursor() as cur:
        cur.execute(sql, {
            "id": operator_id,
            "name": config.get("name", operator_id),
            "url": config.get("url"),
            "timezone": config.get("timezone", "UTC"),
            "features": psycopg2.extras.Json(config.get("features", {})),
        })
    conn.commit()
    print(f"  Wrote operator '{operator_id}'.")


def write_routes(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    routes_list: list[dict[str, Any]],
) -> None:
    """Bulk-insert routes.

    Each dict must have: id, name, transit_type (int).
    Optional: long_name, color, text_color.
    transit_type is the raw GTFS route_type integer.
    """
    ROUTE_TYPE_INT = {
        "tram": 0, "metro": 1, "rail": 2, "bus": 3, "ferry": 4,
        "cable_tram": 5, "gondola": 6, "funicular": 7,
        "trolleybus": 11, "monorail": 12,
    }

    sql = """
        INSERT INTO routes (id, operator_id, name, long_name, color, text_color, transit_type)
        VALUES (%(id)s, %(operator_id)s, %(name)s, %(long_name)s, %(color)s, %(text_color)s, %(transit_type)s)
        ON CONFLICT (id) DO UPDATE SET
            operator_id  = EXCLUDED.operator_id,
            name         = EXCLUDED.name,
            long_name    = EXCLUDED.long_name,
            color        = EXCLUDED.color,
            text_color   = EXCLUDED.text_color,
            transit_type = EXCLUDED.transit_type;
    """
    rows = []
    for r in routes_list:
        transit_type_raw = r.get("transit_type", r.get("transitType", "bus"))
        if isinstance(transit_type_raw, str):
            transit_type_int = ROUTE_TYPE_INT.get(transit_type_raw, 3)
        else:
            transit_type_int = int(transit_type_raw)

        rows.append({
            "id": r["id"],
            "operator_id": operator_id,
            "name": r.get("name", r["id"]),
            "long_name": r.get("long_name") or r.get("longName"),
            "color": r.get("color"),
            "text_color": r.get("text_color") or r.get("textColor"),
            "transit_type": transit_type_int,
        })

    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, sql, rows, page_size=500)
    conn.commit()
    print(f"  Wrote {len(rows)} routes.")


def write_stops(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    stops_list: list[dict[str, Any]],
) -> None:
    """Bulk-insert stops.

    Each dict must have: id, name, lat (float), lng (float).
    Optional: platform_code (str), dock_letter (str).
    """
    sql = """
        INSERT INTO stops (id, operator_id, name, lat, lng, platform_code, dock_letter)
        VALUES (%(id)s, %(operator_id)s, %(name)s, %(lat)s, %(lng)s, %(platform_code)s, %(dock_letter)s)
        ON CONFLICT (id) DO UPDATE SET
            operator_id   = EXCLUDED.operator_id,
            name          = EXCLUDED.name,
            lat           = EXCLUDED.lat,
            lng           = EXCLUDED.lng,
            platform_code = EXCLUDED.platform_code,
            dock_letter   = EXCLUDED.dock_letter;
    """
    rows = [
        {
            "id": s["id"],
            "operator_id": operator_id,
            "name": s["name"],
            "lat": float(s["lat"]),
            "lng": float(s["lng"]),
            "platform_code": s.get("platform_code"),
            "dock_letter": s.get("dock_letter"),
        }
        for s in stops_list
    ]

    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, sql, rows, page_size=500)
    conn.commit()
    print(f"  Wrote {len(rows)} stops.")


def write_trips_and_stop_times(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    trips_data: list[dict[str, Any]],
    stop_times_data: list[dict[str, Any]],
) -> None:
    """Bulk-insert trips and their stop_times.

    trips_data: each dict must have id, route_id, direction_id (int),
                service_days (list[str]). Optional: headsign.

    stop_times_data: each dict must have trip_id, stop_id, arrival_time,
                     departure_time (both HH:MM:SS), stop_sequence (int).
    """
    trip_sql = """
        INSERT INTO trips (id, operator_id, route_id, direction_id, headsign, service_days)
        VALUES (%(id)s, %(operator_id)s, %(route_id)s, %(direction_id)s, %(headsign)s, %(service_days)s)
        ON CONFLICT (id) DO UPDATE SET
            operator_id  = EXCLUDED.operator_id,
            route_id     = EXCLUDED.route_id,
            direction_id = EXCLUDED.direction_id,
            headsign     = EXCLUDED.headsign,
            service_days = EXCLUDED.service_days;
    """
    trip_rows = [
        {
            "id": t["id"],
            "operator_id": operator_id,
            "route_id": t["route_id"],
            "direction_id": int(t.get("direction_id", 0)),
            "headsign": t.get("headsign"),
            "service_days": t.get("service_days", []),
        }
        for t in trips_data
    ]

    st_sql = """
        INSERT INTO stop_times (trip_id, stop_id, arrival_time, departure_time, stop_sequence)
        VALUES (%(trip_id)s, %(stop_id)s, %(arrival_time)s, %(departure_time)s, %(stop_sequence)s)
        ON CONFLICT (trip_id, stop_sequence) DO UPDATE SET
            stop_id        = EXCLUDED.stop_id,
            arrival_time   = EXCLUDED.arrival_time,
            departure_time = EXCLUDED.departure_time;
    """
    st_rows = [
        {
            "trip_id": st["trip_id"],
            "stop_id": st["stop_id"],
            "arrival_time": st["arrival_time"],
            "departure_time": st["departure_time"],
            "stop_sequence": int(st["stop_sequence"]),
        }
        for st in stop_times_data
    ]

    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, trip_sql, trip_rows, page_size=500)
    conn.commit()
    print(f"  Wrote {len(trip_rows)} trips.")

    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, st_sql, st_rows, page_size=1000)
    conn.commit()
    print(f"  Wrote {len(st_rows)} stop_times.")


def write_route_directions(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    directions_data: list[dict[str, Any]],
) -> None:
    """Bulk-insert route_directions.

    Each dict must have: route_id, direction_id (int), stop_ids (list[str]).
    Optional: headsign (str), shape_polyline (str encoded polyline).
    """
    sql = """
        INSERT INTO route_directions (route_id, direction_id, headsign, stop_ids, shape_polyline)
        VALUES (%(route_id)s, %(direction_id)s, %(headsign)s, %(stop_ids)s, %(shape_polyline)s)
        ON CONFLICT (route_id, direction_id) DO UPDATE SET
            headsign       = EXCLUDED.headsign,
            stop_ids       = EXCLUDED.stop_ids,
            shape_polyline = EXCLUDED.shape_polyline;
    """
    rows = [
        {
            "route_id": d["route_id"],
            "direction_id": int(d["direction_id"]),
            "headsign": d.get("headsign"),
            "stop_ids": d["stop_ids"],
            "shape_polyline": d.get("shape_polyline"),
        }
        for d in directions_data
    ]

    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(cur, sql, rows, page_size=500)
    conn.commit()
    print(f"  Wrote {len(rows)} route_directions.")
```

- [ ] Write a test file `pipeline/tests/test_db_writer.py` that inserts mock data and reads it back. This test requires a real DATABASE_URL (set in env) and is skipped if not present:

```python
"""
Integration test for db_writer.py.

Requires DATABASE_URL in environment (e.g. from shared/operators/rfta/.env).
Run with: pytest pipeline/tests/test_db_writer.py -v

Skipped automatically if DATABASE_URL is not set.
"""

import os
import pytest
import psycopg2

TEST_OPERATOR = "_test_dbwriter"


@pytest.fixture(scope="module")
def conn():
    db_url = os.environ.get("DATABASE_URL")
    if not db_url:
        pytest.skip("DATABASE_URL not set — skipping DB integration test.")
    c = psycopg2.connect(db_url)
    yield c
    # Teardown: remove test operator (cascades to all child rows)
    c.autocommit = True
    with c.cursor() as cur:
        cur.execute("DELETE FROM operators WHERE id = %s;", (TEST_OPERATOR,))
    c.close()


def test_write_operator(conn):
    import sys, pathlib
    sys.path.insert(0, str(pathlib.Path(__file__).resolve().parents[2]))
    from pipeline import db_writer

    db_writer.write_operator(conn, TEST_OPERATOR, {
        "name": "Test Operator",
        "url": "https://example.com",
        "timezone": "UTC",
        "features": {"realtime": False},
    })
    with conn.cursor() as cur:
        cur.execute("SELECT name, timezone FROM operators WHERE id = %s;", (TEST_OPERATOR,))
        row = cur.fetchone()
    assert row is not None
    assert row[0] == "Test Operator"
    assert row[1] == "UTC"


def test_write_routes(conn):
    from pipeline import db_writer

    db_writer.write_routes(conn, TEST_OPERATOR, [
        {"id": f"{TEST_OPERATOR}_r1", "name": "1", "long_name": "Test Line 1",
         "color": "#FF0000", "text_color": "#FFFFFF", "transit_type": "bus"},
    ])
    with conn.cursor() as cur:
        cur.execute("SELECT name FROM routes WHERE id = %s;", (f"{TEST_OPERATOR}_r1",))
        row = cur.fetchone()
    assert row is not None
    assert row[0] == "1"


def test_write_stops(conn):
    from pipeline import db_writer

    db_writer.write_stops(conn, TEST_OPERATOR, [
        {"id": f"{TEST_OPERATOR}_s1", "name": "Main St", "lat": 39.35, "lng": -107.0},
        {"id": f"{TEST_OPERATOR}_s2", "name": "Oak Ave", "lat": 39.36, "lng": -107.01,
         "dock_letter": "A"},
    ])
    with conn.cursor() as cur:
        cur.execute("SELECT COUNT(*) FROM stops WHERE operator_id = %s;", (TEST_OPERATOR,))
        count = cur.fetchone()[0]
    assert count == 2


def test_write_trips_and_stop_times(conn):
    from pipeline import db_writer

    db_writer.write_trips_and_stop_times(
        conn, TEST_OPERATOR,
        trips_data=[
            {"id": f"{TEST_OPERATOR}_t1", "route_id": f"{TEST_OPERATOR}_r1",
             "direction_id": 0, "headsign": "Downtown", "service_days": ["monday", "tuesday"]},
        ],
        stop_times_data=[
            {"trip_id": f"{TEST_OPERATOR}_t1", "stop_id": f"{TEST_OPERATOR}_s1",
             "arrival_time": "08:00:00", "departure_time": "08:00:00", "stop_sequence": 1},
            {"trip_id": f"{TEST_OPERATOR}_t1", "stop_id": f"{TEST_OPERATOR}_s2",
             "arrival_time": "08:10:00", "departure_time": "08:10:00", "stop_sequence": 2},
        ],
    )
    with conn.cursor() as cur:
        cur.execute(
            "SELECT COUNT(*) FROM stop_times WHERE trip_id = %s;",
            (f"{TEST_OPERATOR}_t1",),
        )
        count = cur.fetchone()[0]
    assert count == 2


def test_write_route_directions(conn):
    from pipeline import db_writer

    db_writer.write_route_directions(conn, TEST_OPERATOR, [
        {
            "route_id": f"{TEST_OPERATOR}_r1",
            "direction_id": 0,
            "headsign": "Downtown",
            "stop_ids": [f"{TEST_OPERATOR}_s1", f"{TEST_OPERATOR}_s2"],
            "shape_polyline": None,
        },
    ])
    with conn.cursor() as cur:
        cur.execute(
            "SELECT stop_ids FROM route_directions WHERE route_id = %s AND direction_id = 0;",
            (f"{TEST_OPERATOR}_r1",),
        )
        row = cur.fetchone()
    assert row is not None
    assert f"{TEST_OPERATOR}_s1" in row[0]
```

---

## Task 4 — Create normalizer interface and rfta passthrough

Define a base abstract class that every operator normalizer implements. Provide an rfta passthrough as the concrete example.

- [ ] Create file `pipeline/normalizer_base.py`:

```python
"""
Base class for operator-specific GTFS normalizers.

Each operator's normalizer lives at pipeline/operators/{id}/normalizer.py
and must subclass OperatorNormalizer.

The default implementations are identity functions (passthrough).
Override only the methods that need operator-specific logic.
"""

from __future__ import annotations
from abc import ABC


class OperatorNormalizer(ABC):
    """Operator-specific GTFS data normalizer.

    Methods receive a single dict representing one GTFS row (already parsed
    by base_transformer). Return the same dict, mutated or replaced.
    Returning None signals that the row should be dropped entirely.
    """

    def normalize_stop(self, stop: dict) -> dict | None:
        """Normalize a stop dict.

        stop keys: id, name, lat, lng, platform_code, dock_letter.
        Return None to exclude this stop from the output.
        """
        return stop

    def normalize_route(self, route: dict) -> dict | None:
        """Normalize a route dict.

        route keys: id, name, long_name, color, text_color, transit_type.
        Return None to exclude this route from the output.
        """
        return route

    def normalize_trip(self, trip: dict) -> dict | None:
        """Normalize a trip dict.

        trip keys: id, route_id, direction_id, headsign, service_days.
        Return None to exclude this trip from the output.
        """
        return trip
```

- [ ] Create file `pipeline/operators/rfta/normalizer.py`:

```python
"""
RFTA (Roaring Fork Transportation Authority) normalizer.

RFTA feeds are clean standard GTFS — no patches needed.
All methods pass through unchanged.
"""

from __future__ import annotations
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[3]))
from pipeline.normalizer_base import OperatorNormalizer


class RFTANormalizer(OperatorNormalizer):
    """Passthrough normalizer for RFTA — GTFS data requires no transformation."""
    pass


# Module-level instance used by build.py dynamic loader
normalizer = RFTANormalizer()
```

---

## Task 5 — Update `pipeline/build.py`

Add `--output` flag, dynamic normalizer loading, and DB write path. The existing JSON output path is unchanged when `--output json` (default).

- [ ] Open `pipeline/build.py` and apply the following changes.

**5a. Replace the `main()` argument parsing block** — change:

```python
def main():
    if len(sys.argv) < 2:
        print("Usage: python pipeline/build.py <operator_id>")
        print("Example: python pipeline/build.py rfta")
        sys.exit(1)

    operator_id = sys.argv[1]
    print(f"=== TransitKit — GTFS → JSON for '{operator_id}' ===\n")
```

To:

```python
def load_normalizer(operator_id: str):
    """Dynamically load pipeline/operators/{id}/normalizer.py.

    Returns the module-level `normalizer` instance if found,
    or a passthrough OperatorNormalizer if the file does not exist.
    """
    import importlib.util
    from pipeline.normalizer_base import OperatorNormalizer

    normalizer_path = SCRIPT_DIR / "operators" / operator_id / "normalizer.py"
    if not normalizer_path.exists():
        print(f"  No normalizer found at {normalizer_path} — using passthrough.")
        return OperatorNormalizer()

    spec = importlib.util.spec_from_file_location(
        f"pipeline.operators.{operator_id}.normalizer",
        normalizer_path,
    )
    module = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(module)
    instance = getattr(module, "normalizer", None)
    if instance is None:
        print(f"  WARNING: {normalizer_path} has no 'normalizer' instance — using passthrough.")
        return OperatorNormalizer()
    return instance


def load_operator_env(operator_id: str) -> str:
    """Load DATABASE_URL from shared/operators/{id}/.env.

    Returns the DATABASE_URL string. Exits with error if not found.
    """
    import os
    from dotenv import load_dotenv

    env_path = REPO_ROOT / "shared" / "operators" / operator_id / ".env"
    if not env_path.exists():
        print(f"ERROR: .env not found at {env_path}")
        print(f"       Copy {env_path.parent}/.env.example and fill in DATABASE_URL.")
        sys.exit(1)

    load_dotenv(env_path, override=True)
    db_url = os.environ.get("DATABASE_URL")
    if not db_url:
        print(f"ERROR: DATABASE_URL not set in {env_path}")
        sys.exit(1)
    return db_url


def main():
    import argparse
    parser = argparse.ArgumentParser(description="TransitKit — GTFS pipeline")
    parser.add_argument("operator_id", help="Operator ID (e.g. rfta)")
    parser.add_argument(
        "--output",
        choices=["json", "db"],
        default="json",
        help="Output target: 'json' (default, writes output/{id}/schedules.json) or 'db' (writes to Neon)",
    )
    args = parser.parse_args()

    operator_id = args.operator_id
    output_mode = args.output
    print(f"=== TransitKit — GTFS → {'Neon DB' if output_mode == 'db' else 'JSON'} for '{operator_id}' ===\n")
```

**5b. At the end of `main()`, replace the Save block** — change:

```python
    # 5. Save
    print("\n[4/4] Saving...")
    output_dir = REPO_ROOT / "output" / operator_id
    output_dir.mkdir(parents=True, exist_ok=True)

    output_file = output_dir / "schedules.json"
    with open(output_file, "w", encoding="utf-8") as f:
        json.dump(output, f, ensure_ascii=False, separators=(",", ":"))

    size_mb = output_file.stat().st_size / (1024 * 1024)
    print(f"  {output_file.relative_to(REPO_ROOT)} ({size_mb:.1f} MB)")

    print(f"\n✓ Done! {len(output['stops'])} stops, {len(output['routes'])} routes.")
```

To:

```python
    # 5. Save
    if output_mode == "json":
        print("\n[4/4] Saving JSON...")
        output_dir = REPO_ROOT / "output" / operator_id
        output_dir.mkdir(parents=True, exist_ok=True)

        output_file = output_dir / "schedules.json"
        with open(output_file, "w", encoding="utf-8") as f:
            json.dump(output, f, ensure_ascii=False, separators=(",", ":"))

        size_mb = output_file.stat().st_size / (1024 * 1024)
        print(f"  {output_file.relative_to(REPO_ROOT)} ({size_mb:.1f} MB)")
        print(f"\n✓ Done! {len(output['stops'])} stops, {len(output['routes'])} routes.")

    elif output_mode == "db":
        print("\n[4/4] Writing to Neon DB...")
        import psycopg2
        from pipeline import db_writer

        database_url = load_operator_env(operator_id)
        normalizer_instance = load_normalizer(operator_id)

        conn = psycopg2.connect(database_url)
        try:
            db_writer.clear_operator_data(conn, operator_id)
            db_writer.write_operator(conn, operator_id, config)

            # Apply normalizer to routes
            normalized_routes = []
            for r in output["routes"]:
                result = normalizer_instance.normalize_route(r)
                if result is not None:
                    normalized_routes.append(result)
            db_writer.write_routes(conn, operator_id, normalized_routes)

            # Apply normalizer to stops
            normalized_stops = []
            for s in output["stops"]:
                result = normalizer_instance.normalize_stop(s)
                if result is not None:
                    normalized_stops.append(result)
            db_writer.write_stops(conn, operator_id, normalized_stops)

            # Build trips and stop_times from raw feed for DB output
            DAY_IDX_TO_NAME = ["monday", "tuesday", "wednesday", "thursday",
                                "friday", "saturday", "sunday"]
            service_days_by_id: dict[str, list[str]] = {
                sid: [DAY_IDX_TO_NAME[i] for i in sorted(days)]
                for sid, days in service_days.items()
            }

            valid_stop_ids_db: set[str] = {s["id"] for s in normalized_stops}
            valid_route_ids_db: set[str] = {r["id"] for r in normalized_routes}

            trips_for_db = []
            for t in feed.get("trips", []):
                route_id = t["route_id"].strip()
                if route_id not in valid_route_ids_db:
                    continue
                trip_dict = {
                    "id": t["trip_id"].strip(),
                    "route_id": route_id,
                    "direction_id": int(t.get("direction_id", 0)),
                    "headsign": t.get("trip_headsign", "").strip().strip('"'),
                    "service_days": service_days_by_id.get(t["service_id"].strip(), []),
                }
                result = normalizer_instance.normalize_trip(trip_dict)
                if result is not None:
                    trips_for_db.append(result)

            valid_trip_ids_db: set[str] = {t["id"] for t in trips_for_db}

            stop_times_for_db = []
            for st in feed.get("stop_times", []):
                trip_id = st["trip_id"].strip()
                stop_id = st["stop_id"].strip()
                station_id = stop_to_station.get(stop_id)
                if trip_id not in valid_trip_ids_db:
                    continue
                if not station_id or station_id not in valid_stop_ids_db:
                    continue
                arr = st.get("arrival_time", st.get("departure_time", "00:00:00")).strip()
                dep = st.get("departure_time", st.get("arrival_time", "00:00:00")).strip()
                stop_times_for_db.append({
                    "trip_id": trip_id,
                    "stop_id": station_id,
                    "arrival_time": arr if arr else "00:00:00",
                    "departure_time": dep if dep else "00:00:00",
                    "stop_sequence": int(st.get("stop_sequence", 0)),
                })

            db_writer.write_trips_and_stop_times(
                conn, operator_id, trips_for_db, stop_times_for_db,
            )

            # Build route_directions for DB
            directions_for_db = []
            for route_id, dir_list in directions.items():
                if route_id not in valid_route_ids_db:
                    continue
                for d in dir_list:
                    directions_for_db.append({
                        "route_id": route_id,
                        "direction_id": d["id"],
                        "headsign": d.get("headsign"),
                        "stop_ids": d["stopIds"],
                        "shape_polyline": None,  # encode polyline conversion is optional
                    })
            db_writer.write_route_directions(conn, operator_id, directions_for_db)

        finally:
            conn.close()

        print(f"\n✓ Done! {len(normalized_stops)} stops, {len(normalized_routes)} routes written to Neon.")
```

**5c. Expose `service_days` and `stop_to_station` from the processing block** — ensure these variables are accessible at the save step. In the existing `# 3. Process` section, `service_days` is already assigned from `build_service_day_map(...)` and `stop_to_station` from `parse_stops(...)`. No change needed — they are already in scope.

---

## Task 6 — Create `shared/operators/rfta/.env.example`

- [ ] Create file `shared/operators/rfta/.env.example`:

```bash
# DATABASE_URL — Neon PostgreSQL connection string for the rfta operator project.
# Format: postgresql://user:password@host/dbname?sslmode=require
# Get this from: Neon console → Project → Connection string
DATABASE_URL=
```

- [ ] Verify `shared/operators/rfta/.env` is listed in `.gitignore`. Check the root `.gitignore`:

```bash
grep -r "\.env" /Users/andreatoffanello/GitHub/transit-engine/.gitignore
```

If `.env` is not ignored, add the pattern:

```bash
echo "shared/operators/**/.env" >> /Users/andreatoffanello/GitHub/transit-engine/.gitignore
```

---

## Task 7 — Test the pipeline end-to-end

- [ ] Set up the environment:

```bash
# Copy .env.example and fill in your Neon DATABASE_URL
cp shared/operators/rfta/.env.example shared/operators/rfta/.env
# Edit the file and add your DATABASE_URL
```

- [ ] Apply the schema:

```bash
python pipeline/apply_schema.py rfta
```

- [ ] Run the pipeline with `--output db`:

```bash
python pipeline/build.py rfta --output db
```

Expected output ends with:
```
✓ Done! N stops, N routes written to Neon.
```

- [ ] Verify data landed in Neon with a verification query (run via `psql $DATABASE_URL` or the Neon console SQL editor):

```sql
-- Check operator row
SELECT id, name, timezone FROM operators WHERE id = 'rfta';

-- Count entities
SELECT 'routes'          AS entity, COUNT(*) FROM routes          WHERE operator_id = 'rfta'
UNION ALL
SELECT 'stops'           AS entity, COUNT(*) FROM stops            WHERE operator_id = 'rfta'
UNION ALL
SELECT 'trips'           AS entity, COUNT(*) FROM trips            WHERE operator_id = 'rfta'
UNION ALL
SELECT 'stop_times'      AS entity, COUNT(*) FROM stop_times
    WHERE trip_id IN (SELECT id FROM trips WHERE operator_id = 'rfta')
UNION ALL
SELECT 'route_directions' AS entity, COUNT(*) FROM route_directions
    WHERE route_id IN (SELECT id FROM routes WHERE operator_id = 'rfta');

-- Spot-check a stop with its stop_times count
SELECT s.name, COUNT(st.trip_id) AS departure_count
FROM stops s
JOIN stop_times st ON st.stop_id = s.id
WHERE s.operator_id = 'rfta'
GROUP BY s.name
ORDER BY departure_count DESC
LIMIT 10;

-- Schema version
SELECT version FROM schema_version;
```

- [ ] Run the integration tests (requires DATABASE_URL in environment):

```bash
export $(grep -v '^#' shared/operators/rfta/.env | xargs)
pytest pipeline/tests/test_db_writer.py -v
```

All 5 tests should pass.
