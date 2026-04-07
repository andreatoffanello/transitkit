"""
Base class for per-operator GTFS normalizers.

Each operator in pipeline/operators/{id}/normalizer.py must subclass
OperatorNormalizer and implement the three normalize_* methods.

The methods receive a dict representing one row from the GTFS feed
(already parsed from CSV) and return a modified dict. Returning the
input unchanged is valid (passthrough).
"""

from __future__ import annotations
from abc import ABC, abstractmethod
from typing import Any


class OperatorNormalizer(ABC):

    @abstractmethod
    def normalize_stop(self, stop: dict[str, Any]) -> dict[str, Any]:
        """
        Normalize a stop dict before writing to DB.

        Input keys (from GTFS stops.txt): stop_id, stop_name, stop_lat, stop_lon,
        plus any operator-specific extras.

        Expected output keys: id, name, lat, lng, platform_code (opt), dock_letter (opt).
        """

    @abstractmethod
    def normalize_route(self, route: dict[str, Any]) -> dict[str, Any]:
        """
        Normalize a route dict before writing to DB.

        Input keys (from GTFS routes.txt): route_id, route_short_name, route_long_name,
        route_type, route_color, route_text_color.

        Expected output keys: id, name, long_name, transit_type, color (opt), text_color (opt).
        """

    @abstractmethod
    def normalize_trip(self, trip: dict[str, Any]) -> dict[str, Any]:
        """
        Normalize a trip dict before writing to DB.

        Input keys (from GTFS trips.txt): trip_id, route_id, service_id,
        trip_headsign, direction_id, shape_id.

        Expected output keys: id, route_id, direction_id, headsign (opt).
        """
