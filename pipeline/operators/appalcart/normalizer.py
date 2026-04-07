"""
AppalCART (Appalachian District Transportation Authority) GTFS normalizer.

AppalCART provides standard GTFS feeds. This normalizer maps GTFS field
names to the transit-engine DB schema without applying custom transformations.
"""

from __future__ import annotations
import sys
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parents[3]))

from normalizer_base import OperatorNormalizer
from typing import Any


class AppalcartNormalizer(OperatorNormalizer):

    def normalize_stop(self, stop: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": stop["stop_id"],
            "name": stop["stop_name"].strip(),
            "lat": float(stop["stop_lat"]),
            "lng": float(stop["stop_lon"]),
            "platform_code": stop.get("platform_code") or None,
            "dock_letter": None,  # AppalCART does not use dock letters
        }

    def normalize_route(self, route: dict[str, Any]) -> dict[str, Any]:
        color = route.get("route_color", "").strip() or None
        text_color = route.get("route_text_color", "").strip() or None
        return {
            "id": route["route_id"],
            "name": route.get("route_short_name", "").strip() or route["route_id"],
            "long_name": route.get("route_long_name", "").strip() or None,
            "transit_type": int(route.get("route_type", 3)),
            "color": color,
            "text_color": text_color,
        }

    def normalize_trip(self, trip: dict[str, Any]) -> dict[str, Any]:
        return {
            "id": trip["trip_id"],
            "route_id": trip["route_id"],
            "direction_id": int(trip.get("direction_id", 0)),
            "headsign": trip.get("trip_headsign", "").strip() or None,
        }
