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
