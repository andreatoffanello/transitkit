"""Tests for build_ios_json() — verifies the JSON matches ScheduleResponse wire format."""
import sys, json, re
from pathlib import Path
sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from build import (
    build_ios_json,
    build_service_day_map,
    parse_stops,
    build_departures,
    build_route_directions,
    build_routes_list,
    parse_shapes,
    build_trip_terminus_map,
)


MINIMAL_FEED = {
    "agency": [{"agency_id": "A", "agency_name": "TestAgency", "agency_url": "https://test.example"}],
    "routes": [
        {"route_id": "R1", "route_short_name": "1", "route_long_name": "Test Route",
         "route_color": "165F9C", "route_text_color": "FFFFFF", "route_type": "3"}
    ],
    "stops": [
        {"stop_id": "S1", "stop_name": "Main St", "stop_lat": "36.2168", "stop_lon": "-81.6746"},
        {"stop_id": "S2", "stop_name": "Park Ave", "stop_lat": "36.2200", "stop_lon": "-81.6700"},
    ],
    "trips": [
        {"trip_id": "T1", "route_id": "R1", "service_id": "WD", "direction_id": "0",
         "trip_headsign": "Park Ave", "shape_id": ""}
    ],
    "stop_times": [
        {"trip_id": "T1", "stop_id": "S1", "departure_time": "07:35:00",
         "arrival_time": "07:35:00", "stop_sequence": "1"},
        {"trip_id": "T1", "stop_id": "S2", "departure_time": "07:45:00",
         "arrival_time": "07:45:00", "stop_sequence": "2"},
    ],
    "calendar": [
        {"service_id": "WD", "monday": "1", "tuesday": "1", "wednesday": "1",
         "thursday": "1", "friday": "1", "saturday": "0", "sunday": "0",
         "start_date": "20260101", "end_date": "20261231"}
    ],
    "shapes": [],
    "calendar_dates": [],
    "feed_info": [],
}

OPERATOR_CONFIG = {
    "id": "testop",
    "name": "Test Operator",
    "url": "https://test.example",
    "timezone": "America/New_York",
    "features": {
        "enableMap": True, "enableGeolocation": False,
        "enableFavorites": True, "enableNotifications": False,
    },
}


def _run_pipeline(feed, config):
    service_days = build_service_day_map(feed.get("calendar", []), feed.get("calendar_dates"))
    stations, stop_to_station = parse_stops(feed.get("stops", []), config["id"])
    valid_stop_ids = set(stop_to_station.keys())
    terminus_map = build_trip_terminus_map(feed.get("stop_times", []), feed.get("stops", []))
    departures = build_departures(
        feed.get("stop_times", []), feed.get("trips", []), feed.get("routes", []),
        service_days, valid_stop_ids, stop_to_station, stations, terminus_map=terminus_map,
    )
    shapes = parse_shapes(feed.get("shapes", []))
    directions = build_route_directions(
        feed.get("routes", []), feed.get("trips", []), feed.get("stop_times", []),
        shapes, stop_to_station, stations,
    )
    routes_list = build_routes_list(feed.get("routes", []), directions)
    return stations, departures, routes_list


def test_operator_fields():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    op = result["operator"]
    assert op["id"] == "testop"
    assert op["name"] == "Test Operator"
    assert op["timezone"] == "America/New_York"
    assert isinstance(op["features"], dict)
    assert op["features"]["enableMap"] is True


def test_last_updated_format():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    ts = result["lastUpdated"]
    assert re.match(r"\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z", ts), f"Bad timestamp: {ts}"


def test_route_transit_type_is_int():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    assert len(result["routes"]) >= 1
    for r in result["routes"]:
        assert isinstance(r["transitType"], int), f"transitType must be int, got {type(r['transitType'])}"
        assert r["transitType"] == 3


def test_route_color_no_hash():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    for r in result["routes"]:
        assert not r["color"].startswith("#"), f"color must not have #, got {r['color']}"
        assert not r["textColor"].startswith("#"), f"textColor must not have #, got {r['textColor']}"


def test_route_directions_shape():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    for r in result["routes"]:
        for d in r["directions"]:
            assert "directionId" in d
            assert "headsign" in d
            assert "stopIds" in d
            assert isinstance(d["stopIds"], list)
            assert "shapePolyline" in d


def test_stop_fields():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    assert len(result["stops"]) >= 1
    for s in result["stops"]:
        assert "id" in s
        assert "name" in s
        assert isinstance(s["lat"], float)
        assert isinstance(s["lng"], float)
        assert "platformCode" in s
        assert "dockLetter" in s
        assert isinstance(s["departures"], list)


def test_departure_fields():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    main_st_stops = [s for s in result["stops"] if "main" in s["name"].lower()]
    assert main_st_stops, "Main St stop not found in output"
    deps = main_st_stops[0]["departures"]
    assert len(deps) >= 1
    d = deps[0]
    assert "tripId" in d
    assert "routeId" in d
    assert "routeName" in d
    assert "routeColor" in d
    assert not d["routeColor"].startswith("#"), "routeColor must not have #"
    assert "routeTextColor" in d
    assert not d["routeTextColor"].startswith("#"), "routeTextColor must not have #"
    assert "headsign" in d
    assert "departureTime" in d
    assert re.match(r"\d{2}:\d{2}:\d{2}", d["departureTime"]), f"Bad time format: {d['departureTime']}"
    assert "serviceDays" in d
    assert isinstance(d["serviceDays"], list)
    assert len(d["serviceDays"]) >= 1
    valid_days = {"monday","tuesday","wednesday","thursday","friday","saturday","sunday"}
    for day in d["serviceDays"]:
        assert day in valid_days, f"Unknown service day: {day}"


def test_terminal_stop_excluded():
    stations, departures, routes_list = _run_pipeline(MINIMAL_FEED, OPERATOR_CONFIG)
    result = build_ios_json(OPERATOR_CONFIG, stations, departures, routes_list, MINIMAL_FEED)
    park_stops = [s for s in result["stops"] if "park" in s["name"].lower()]
    for s in park_stops:
        for dep in s["departures"]:
            assert dep["tripId"] != "T1", "Terminal stop should not have departure for T1"
