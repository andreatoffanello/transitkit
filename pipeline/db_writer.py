"""
DB writer for TransitKit pipeline.

All public functions accept an open psycopg2 connection and operator_id.
Call clear_operator_data() first for a clean re-import.
Uses psycopg2.extras.execute_batch for bulk inserts.
"""

from __future__ import annotations

import psycopg2
import psycopg2.extras
from typing import Any


def clear_operator_data(conn: psycopg2.extensions.connection, operator_id: str) -> None:
    """Delete all data for operator. Cascades to routes, stops, trips, stop_times,
    route_directions via ON DELETE CASCADE."""
    with conn.cursor() as cur:
        cur.execute("DELETE FROM operators WHERE id = %s;", (operator_id,))
    conn.commit()
    print(f"  Cleared existing data for operator '{operator_id}'.")


def write_operator(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    name: str,
    url: str | None,
    timezone: str,
    features: dict[str, Any],
) -> None:
    with conn.cursor() as cur:
        cur.execute(
            """
            INSERT INTO operators (id, name, url, timezone, features)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                url = EXCLUDED.url,
                timezone = EXCLUDED.timezone,
                features = EXCLUDED.features
            """,
            (operator_id, name, url, timezone, psycopg2.extras.Json(features)),
        )
    conn.commit()
    print(f"  Wrote operator '{operator_id}'.")


def write_routes(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    routes: list[dict[str, Any]],
) -> None:
    """
    Each route dict must have: id, name, long_name, color, text_color, transit_type.
    transit_type is an int (GTFS route_type).
    """
    rows = [
        (
            r["id"],
            operator_id,
            r["name"],
            r.get("long_name"),
            r.get("color"),
            r.get("text_color"),
            int(r["transit_type"]),
        )
        for r in routes
    ]
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(
            cur,
            """
            INSERT INTO routes (id, operator_id, name, long_name, color, text_color, transit_type)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                long_name = EXCLUDED.long_name,
                color = EXCLUDED.color,
                text_color = EXCLUDED.text_color,
                transit_type = EXCLUDED.transit_type
            """,
            rows,
        )
    conn.commit()
    print(f"  Wrote {len(rows)} routes.")


def write_stops(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    stops: list[dict[str, Any]],
) -> None:
    """
    Each stop dict must have: id, name, lat, lng.
    Optional: platform_code, dock_letter.
    """
    rows = [
        (
            s["id"],
            operator_id,
            s["name"],
            float(s["lat"]),
            float(s["lng"]),
            s.get("platform_code"),
            s.get("dock_letter"),
        )
        for s in stops
    ]
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(
            cur,
            """
            INSERT INTO stops (id, operator_id, name, lat, lng, platform_code, dock_letter)
            VALUES (%s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                name = EXCLUDED.name,
                lat = EXCLUDED.lat,
                lng = EXCLUDED.lng,
                platform_code = EXCLUDED.platform_code,
                dock_letter = EXCLUDED.dock_letter
            """,
            rows,
        )
    conn.commit()
    print(f"  Wrote {len(rows)} stops.")


def write_trips_and_stop_times(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    trips: list[dict[str, Any]],
    stop_times: list[dict[str, Any]],
) -> None:
    """
    Each trip dict: id, route_id, direction_id (int), headsign (str|None), service_days (list[str]).
    Each stop_time dict: trip_id, stop_id, arrival_time, departure_time, stop_sequence (int).
    """
    trip_rows = [
        (
            t["id"],
            operator_id,
            t["route_id"],
            int(t["direction_id"]),
            t.get("headsign"),
            t["service_days"],  # list[str] — psycopg2 maps to TEXT[]
        )
        for t in trips
    ]
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(
            cur,
            """
            INSERT INTO trips (id, operator_id, route_id, direction_id, headsign, service_days)
            VALUES (%s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO UPDATE SET
                route_id = EXCLUDED.route_id,
                direction_id = EXCLUDED.direction_id,
                headsign = EXCLUDED.headsign,
                service_days = EXCLUDED.service_days
            """,
            trip_rows,
        )
    conn.commit()
    print(f"  Wrote {len(trip_rows)} trips.")

    st_rows = [
        (
            st["trip_id"],
            st["stop_id"],
            st["arrival_time"],
            st["departure_time"],
            int(st["stop_sequence"]),
        )
        for st in stop_times
    ]
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(
            cur,
            """
            INSERT INTO stop_times (trip_id, stop_id, arrival_time, departure_time, stop_sequence)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (trip_id, stop_sequence) DO UPDATE SET
                stop_id = EXCLUDED.stop_id,
                arrival_time = EXCLUDED.arrival_time,
                departure_time = EXCLUDED.departure_time
            """,
            st_rows,
        )
    conn.commit()
    print(f"  Wrote {len(st_rows)} stop times.")


def write_route_directions(
    conn: psycopg2.extensions.connection,
    operator_id: str,
    directions: list[dict[str, Any]],
) -> None:
    """
    Each direction dict: route_id, direction_id (int), headsign (str|None),
    stop_ids (list[str]), shape_polyline (str|None).
    """
    rows = [
        (
            d["route_id"],
            int(d["direction_id"]),
            d.get("headsign"),
            d["stop_ids"],  # list[str] — psycopg2 maps to TEXT[]
            d.get("shape_polyline"),
        )
        for d in directions
    ]
    with conn.cursor() as cur:
        psycopg2.extras.execute_batch(
            cur,
            """
            INSERT INTO route_directions (route_id, direction_id, headsign, stop_ids, shape_polyline)
            VALUES (%s, %s, %s, %s, %s)
            ON CONFLICT (route_id, direction_id) DO UPDATE SET
                headsign = EXCLUDED.headsign,
                stop_ids = EXCLUDED.stop_ids,
                shape_polyline = EXCLUDED.shape_polyline
            """,
            rows,
        )
    conn.commit()
    print(f"  Wrote {len(rows)} route directions.")
