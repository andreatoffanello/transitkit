# Data Layer — Plan 2: API Layer

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a Vercel Functions API that serves schedule data from Neon to iOS and web clients.

**Architecture:** A standalone `api/` directory deployable as a separate Vercel project. Each endpoint queries Neon directly using @neondatabase/serverless. The /schedule endpoint returns bulk data for iOS; other endpoints serve granular queries for web SSR.

**Tech Stack:** TypeScript, Vercel Functions, @neondatabase/serverless, zod for validation

---

## Task 1 — Initialize `api/` project

Create the `api/` directory with all scaffolding files. This is a standalone project — it has its own `package.json` and is linked to a separate Vercel project per operator.

- [ ] Create file `api/package.json`:

```json
{
  "name": "transitkit-api",
  "version": "1.0.0",
  "private": true,
  "scripts": {
    "dev": "vercel dev",
    "build": "tsc --noEmit",
    "typecheck": "tsc --noEmit"
  },
  "dependencies": {
    "@neondatabase/serverless": "^0.9.0",
    "zod": "^3.22.4"
  },
  "devDependencies": {
    "@vercel/node": "^3.0.0",
    "typescript": "^5.3.3"
  }
}
```

- [ ] Create file `api/tsconfig.json`:

```json
{
  "compilerOptions": {
    "target": "ES2022",
    "module": "ESNext",
    "moduleResolution": "bundler",
    "strict": true,
    "esModuleInterop": true,
    "skipLibCheck": true,
    "outDir": ".vercel/output",
    "rootDir": ".",
    "baseUrl": "."
  },
  "include": ["./**/*.ts"],
  "exclude": ["node_modules", ".vercel"]
}
```

- [ ] Create file `api/vercel.json`:

```json
{
  "rewrites": [
    { "source": "/schedule",                       "destination": "/routes/schedule" },
    { "source": "/config",                         "destination": "/routes/config" },
    { "source": "/stops/nearby",                   "destination": "/routes/stops/nearby" },
    { "source": "/stops/:id/departures",           "destination": "/routes/stops/[id]/departures" },
    { "source": "/routes",                         "destination": "/routes/routes/index" },
    { "source": "/routes/:id",                     "destination": "/routes/routes/[id]" },
    { "source": "/trips/:id",                      "destination": "/routes/trips/[id]" }
  ]
}
```

- [ ] Install dependencies:

```bash
cd api && npm install
```

---

## Task 2 — Create `api/lib/db.ts`

Neon serverless connection helper. All endpoints import `sql` from this module.

- [ ] Create file `api/lib/db.ts`:

```typescript
import { neon } from "@neondatabase/serverless";

if (!process.env.DATABASE_URL) {
  throw new Error("DATABASE_URL environment variable is not set.");
}

/**
 * Tagged template literal for querying Neon.
 * Usage: const rows = await sql`SELECT * FROM stops WHERE id = ${stopId}`;
 */
export const sql = neon(process.env.DATABASE_URL);
```

---

## Task 3 — Create `api/lib/types.ts`

TypeScript interfaces matching the DB schema exactly. These are the clean output types returned to clients — no compact arrays, no index lookups.

- [ ] Create file `api/lib/types.ts`:

```typescript
// OperatorFeatures — mirrors the operators.features JSONB column
export interface OperatorFeatures {
  docks: boolean;
  realtime: boolean;
  alerts: boolean;
  bikes: boolean;
  shapes: boolean;
}

// Operator — from the operators table
export interface Operator {
  id: string;
  name: string;
  url: string | null;
  timezone: string;
  features: Partial<OperatorFeatures>;
}

// Route — from the routes table
export interface Route {
  id: string;
  operatorId: string;
  name: string;
  longName: string | null;
  color: string | null;
  textColor: string | null;
  transitType: number;
}

// Stop — from the stops table
export interface Stop {
  id: string;
  operatorId: string;
  name: string;
  lat: number;
  lng: number;
  platformCode: string | null;
  dockLetter: string | null;
}

// StopWithDistance — Stop extended with a computed distance field
export interface StopWithDistance extends Stop {
  distanceMeters: number;
}

// Trip — from the trips table
export interface Trip {
  id: string;
  operatorId: string;
  routeId: string;
  directionId: number;
  headsign: string | null;
  serviceDays: string[];
}

// StopTime — from the stop_times table
export interface StopTime {
  tripId: string;
  stopId: string;
  arrivalTime: string;   // HH:MM:SS, may exceed 24:00
  departureTime: string;
  stopSequence: number;
}

// StopTimeWithStop — stop_time joined with stop name and coords
export interface StopTimeWithStop extends StopTime {
  stopName: string;
  stopLat: number;
  stopLng: number;
}

// RouteDirection — from the route_directions table
export interface RouteDirection {
  routeId: string;
  directionId: number;
  headsign: string | null;
  stopIds: string[];
  shapePolyline: string | null;
}

// RouteWithDirections — route joined with its directions
export interface RouteWithDirections extends Route {
  directions: RouteDirection[];
}

// Departure — a single upcoming departure from a stop
export interface Departure {
  tripId: string;
  routeId: string;
  routeName: string;
  routeColor: string | null;
  routeTextColor: string | null;
  headsign: string | null;
  departureTime: string;  // HH:MM:SS
  serviceDays: string[];
}

// TripDetail — full trip with stop sequence
export interface TripDetail extends Trip {
  routeName: string;
  routeColor: string | null;
  routeTextColor: string | null;
  stopTimes: StopTimeWithStop[];
}

// ScheduleResponse — bulk payload for iOS /schedule endpoint
export interface ScheduleResponse {
  operator: Operator;
  lastUpdated: string;       // ISO 8601
  routes: RouteWithDirections[];
  stops: Stop[];
  departuresByStop: Record<string, Departure[]>;  // stopId → departures for today
}
```

---

## Task 4 — Create `/schedule` endpoint

Bulk JSON payload for iOS. Queries all routes, route_directions, stops, and today's departures in parallel. Assembles a single `ScheduleResponse`.

- [ ] Create file `api/routes/schedule.ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sql } from "../lib/db";
import type {
  Operator,
  Route,
  RouteDirection,
  RouteWithDirections,
  Stop,
  Departure,
  ScheduleResponse,
} from "../lib/types";

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  try {
    const [operatorRows, routeRows, directionRows, stopRows, departureRows] =
      await Promise.all([
        sql`
          SELECT id, name, url, timezone, features
          FROM operators
          LIMIT 1
        `,
        sql`
          SELECT id, operator_id, name, long_name, color, text_color, transit_type
          FROM routes
          ORDER BY name
        `,
        sql`
          SELECT route_id, direction_id, headsign, stop_ids, shape_polyline
          FROM route_directions
          ORDER BY route_id, direction_id
        `,
        sql`
          SELECT id, operator_id, name, lat, lng, platform_code, dock_letter
          FROM stops
          ORDER BY name
        `,
        sql`
          SELECT DISTINCT ON (st.stop_id, t.route_id, t.headsign, st.departure_time)
            st.stop_id,
            st.departure_time,
            t.id        AS trip_id,
            t.route_id,
            t.headsign,
            t.service_days,
            r.name      AS route_name,
            r.color     AS route_color,
            r.text_color AS route_text_color
          FROM stop_times st
          JOIN trips t   ON t.id = st.trip_id
          JOIN routes r  ON r.id = t.route_id
          ORDER BY st.stop_id, t.route_id, t.headsign, st.departure_time
        `,
      ]);

    if (operatorRows.length === 0) {
      res.status(404).json({ error: "Operator not found" });
      return;
    }

    const operatorRow = operatorRows[0];
    const operator: Operator = {
      id: operatorRow.id,
      name: operatorRow.name,
      url: operatorRow.url,
      timezone: operatorRow.timezone,
      features: operatorRow.features ?? {},
    };

    // Build routes map with directions
    const directionsMap = new Map<string, RouteDirection[]>();
    for (const d of directionRows) {
      const dir: RouteDirection = {
        routeId: d.route_id,
        directionId: d.direction_id,
        headsign: d.headsign,
        stopIds: d.stop_ids,
        shapePolyline: d.shape_polyline,
      };
      if (!directionsMap.has(d.route_id)) {
        directionsMap.set(d.route_id, []);
      }
      directionsMap.get(d.route_id)!.push(dir);
    }

    const routes: RouteWithDirections[] = routeRows.map((r) => ({
      id: r.id,
      operatorId: r.operator_id,
      name: r.name,
      longName: r.long_name,
      color: r.color,
      textColor: r.text_color,
      transitType: r.transit_type,
      directions: directionsMap.get(r.id) ?? [],
    }));

    const stops: Stop[] = stopRows.map((s) => ({
      id: s.id,
      operatorId: s.operator_id,
      name: s.name,
      lat: parseFloat(s.lat),
      lng: parseFloat(s.lng),
      platformCode: s.platform_code,
      dockLetter: s.dock_letter,
    }));

    // Group departures by stop_id
    const departuresByStop: Record<string, Departure[]> = {};
    for (const d of departureRows) {
      const dep: Departure = {
        tripId: d.trip_id,
        routeId: d.route_id,
        routeName: d.route_name,
        routeColor: d.route_color,
        routeTextColor: d.route_text_color,
        headsign: d.headsign,
        departureTime: d.departure_time,
        serviceDays: d.service_days ?? [],
      };
      if (!departuresByStop[d.stop_id]) {
        departuresByStop[d.stop_id] = [];
      }
      departuresByStop[d.stop_id].push(dep);
    }

    const response: ScheduleResponse = {
      operator,
      lastUpdated: new Date().toISOString(),
      routes,
      stops,
      departuresByStop,
    };

    res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
    res.status(200).json(response);
  } catch (err) {
    console.error("/schedule error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 5 — Create `/config` endpoint

Returns operator metadata and feature flags from the `operators` table.

- [ ] Create file `api/routes/config.ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sql } from "../lib/db";
import type { Operator } from "../lib/types";

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  try {
    const rows = await sql`
      SELECT id, name, url, timezone, features
      FROM operators
      LIMIT 1
    `;

    if (rows.length === 0) {
      res.status(404).json({ error: "Operator not found" });
      return;
    }

    const row = rows[0];
    const operator: Operator = {
      id: row.id,
      name: row.name,
      url: row.url,
      timezone: row.timezone,
      features: row.features ?? {},
    };

    res.setHeader("Cache-Control", "public, s-maxage=86400, stale-while-revalidate=604800");
    res.status(200).json(operator);
  } catch (err) {
    console.error("/config error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 6 — Create `/stops/nearby` endpoint

Accepts `lat`, `lng`, `radius` (metres) query params. Uses Haversine formula in SQL to return stops within the given radius, sorted by distance.

- [ ] Create directory `api/routes/stops/` (create `nearby.ts` inside it):

- [ ] Create file `api/routes/stops/nearby.ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";
import { sql } from "../../lib/db";
import type { StopWithDistance } from "../../lib/types";

const QuerySchema = z.object({
  lat:    z.string().transform(Number).pipe(z.number().min(-90).max(90)),
  lng:    z.string().transform(Number).pipe(z.number().min(-180).max(180)),
  radius: z.string().transform(Number).pipe(z.number().positive().max(50000)).optional(),
});

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  const parsed = QuerySchema.safeParse(req.query);
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid query params", details: parsed.error.flatten() });
    return;
  }

  const { lat, lng, radius = 500 } = parsed.data;

  try {
    // Haversine formula in SQL — returns distance in metres
    const rows = await sql`
      SELECT
        id,
        operator_id,
        name,
        lat,
        lng,
        platform_code,
        dock_letter,
        (
          6371000 * acos(
            LEAST(1.0, cos(radians(${lat})) * cos(radians(lat))
            * cos(radians(lng) - radians(${lng}))
            + sin(radians(${lat})) * sin(radians(lat)))
          )
        ) AS distance_meters
      FROM stops
      WHERE
        lat BETWEEN ${lat - radius / 111320.0} AND ${lat + radius / 111320.0}
        AND lng BETWEEN ${lng - radius / (111320.0 * Math.cos((lat * Math.PI) / 180))}
                    AND ${lng + radius / (111320.0 * Math.cos((lat * Math.PI) / 180))}
      HAVING (
          6371000 * acos(
            LEAST(1.0, cos(radians(${lat})) * cos(radians(lat))
            * cos(radians(lng) - radians(${lng}))
            + sin(radians(${lat})) * sin(radians(lat)))
          )
        ) <= ${radius}
      ORDER BY distance_meters
      LIMIT 20
    `;

    const stops: StopWithDistance[] = rows.map((r) => ({
      id: r.id,
      operatorId: r.operator_id,
      name: r.name,
      lat: parseFloat(r.lat),
      lng: parseFloat(r.lng),
      platformCode: r.platform_code,
      dockLetter: r.dock_letter,
      distanceMeters: Math.round(parseFloat(r.distance_meters)),
    }));

    res.setHeader("Cache-Control", "public, s-maxage=60, stale-while-revalidate=300");
    res.status(200).json(stops);
  } catch (err) {
    console.error("/stops/nearby error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 7 — Create `/stops/[id]/departures` endpoint

Accepts `date` (YYYY-MM-DD) and `limit` query params. Filters stop_times by the day of week matching `date`, returns upcoming departures sorted by departure_time.

- [ ] Create directory `api/routes/stops/[id]/` (create `departures.ts` inside it):

- [ ] Create file `api/routes/stops/[id]/departures.ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { z } from "zod";
import { sql } from "../../../lib/db";
import type { Departure } from "../../../lib/types";

const QuerySchema = z.object({
  date:  z.string().regex(/^\d{4}-\d{2}-\d{2}$/).optional(),
  limit: z.string().transform(Number).pipe(z.number().int().min(1).max(200)).optional(),
});

// Map JS getDay() (0=Sun) to GTFS day name
const JS_DAY_TO_GTFS: Record<number, string> = {
  0: "sunday",
  1: "monday",
  2: "tuesday",
  3: "wednesday",
  4: "thursday",
  5: "friday",
  6: "saturday",
};

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  const stopId = req.query.id as string;
  if (!stopId) {
    res.status(400).json({ error: "Missing stop id" });
    return;
  }

  const parsed = QuerySchema.safeParse(req.query);
  if (!parsed.success) {
    res.status(400).json({ error: "Invalid query params", details: parsed.error.flatten() });
    return;
  }

  const { date, limit = 50 } = parsed.data;

  // Determine which day of week to filter by
  const targetDate = date ? new Date(`${date}T00:00:00Z`) : new Date();
  const gtfsDay = JS_DAY_TO_GTFS[targetDate.getUTCDay()];

  try {
    // Verify stop exists
    const stopRows = await sql`
      SELECT id FROM stops WHERE id = ${stopId}
    `;
    if (stopRows.length === 0) {
      res.status(404).json({ error: `Stop '${stopId}' not found` });
      return;
    }

    const rows = await sql`
      SELECT
        st.departure_time,
        t.id         AS trip_id,
        t.route_id,
        t.headsign,
        t.service_days,
        r.name       AS route_name,
        r.color      AS route_color,
        r.text_color AS route_text_color
      FROM stop_times st
      JOIN trips  t ON t.id  = st.trip_id
      JOIN routes r ON r.id  = t.route_id
      WHERE
        st.stop_id = ${stopId}
        AND ${gtfsDay} = ANY(t.service_days)
      ORDER BY st.departure_time
      LIMIT ${limit}
    `;

    const departures: Departure[] = rows.map((r) => ({
      tripId:         r.trip_id,
      routeId:        r.route_id,
      routeName:      r.route_name,
      routeColor:     r.route_color,
      routeTextColor: r.route_text_color,
      headsign:       r.headsign,
      departureTime:  r.departure_time,
      serviceDays:    r.service_days ?? [],
    }));

    res.setHeader("Cache-Control", "public, s-maxage=60, stale-while-revalidate=300");
    res.status(200).json(departures);
  } catch (err) {
    console.error(`/stops/${stopId}/departures error:`, err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 8 — Create `/routes` endpoint

Returns all routes with basic info, sorted by name.

- [ ] Create directory `api/routes/routes/`:

- [ ] Create file `api/routes/routes/index.ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sql } from "../../lib/db";
import type { Route } from "../../lib/types";

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  try {
    const rows = await sql`
      SELECT id, operator_id, name, long_name, color, text_color, transit_type
      FROM routes
      ORDER BY name
    `;

    const routes: Route[] = rows.map((r) => ({
      id:          r.id,
      operatorId:  r.operator_id,
      name:        r.name,
      longName:    r.long_name,
      color:       r.color,
      textColor:   r.text_color,
      transitType: r.transit_type,
    }));

    res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
    res.status(200).json(routes);
  } catch (err) {
    console.error("/routes error:", err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 9 — Create `/routes/[id]` endpoint

Returns a single route with its route_directions (ordered stop_ids + shape polyline).

- [ ] Create file `api/routes/routes/[id].ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sql } from "../../lib/db";
import type { RouteWithDirections, RouteDirection } from "../../lib/types";

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  const routeId = req.query.id as string;
  if (!routeId) {
    res.status(400).json({ error: "Missing route id" });
    return;
  }

  try {
    const [routeRows, directionRows] = await Promise.all([
      sql`
        SELECT id, operator_id, name, long_name, color, text_color, transit_type
        FROM routes
        WHERE id = ${routeId}
      `,
      sql`
        SELECT route_id, direction_id, headsign, stop_ids, shape_polyline
        FROM route_directions
        WHERE route_id = ${routeId}
        ORDER BY direction_id
      `,
    ]);

    if (routeRows.length === 0) {
      res.status(404).json({ error: `Route '${routeId}' not found` });
      return;
    }

    const r = routeRows[0];
    const directions: RouteDirection[] = directionRows.map((d) => ({
      routeId:       d.route_id,
      directionId:   d.direction_id,
      headsign:      d.headsign,
      stopIds:       d.stop_ids,
      shapePolyline: d.shape_polyline,
    }));

    const route: RouteWithDirections = {
      id:          r.id,
      operatorId:  r.operator_id,
      name:        r.name,
      longName:    r.long_name,
      color:       r.color,
      textColor:   r.text_color,
      transitType: r.transit_type,
      directions,
    };

    res.setHeader("Cache-Control", "public, s-maxage=3600, stale-while-revalidate=86400");
    res.status(200).json(route);
  } catch (err) {
    console.error(`/routes/${routeId} error:`, err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 10 — Create `/trips/[id]` endpoint

Returns a single trip with all stop_times joined with stop names and coordinates.

- [ ] Create directory `api/routes/trips/`:

- [ ] Create file `api/routes/trips/[id].ts`:

```typescript
import type { VercelRequest, VercelResponse } from "@vercel/node";
import { sql } from "../../lib/db";
import type { TripDetail, StopTimeWithStop } from "../../lib/types";

export default async function handler(
  req: VercelRequest,
  res: VercelResponse
): Promise<void> {
  if (req.method !== "GET") {
    res.status(405).json({ error: "Method Not Allowed" });
    return;
  }

  const tripId = req.query.id as string;
  if (!tripId) {
    res.status(400).json({ error: "Missing trip id" });
    return;
  }

  try {
    const [tripRows, stopTimeRows] = await Promise.all([
      sql`
        SELECT
          t.id,
          t.operator_id,
          t.route_id,
          t.direction_id,
          t.headsign,
          t.service_days,
          r.name       AS route_name,
          r.color      AS route_color,
          r.text_color AS route_text_color
        FROM trips t
        JOIN routes r ON r.id = t.route_id
        WHERE t.id = ${tripId}
      `,
      sql`
        SELECT
          st.trip_id,
          st.stop_id,
          st.arrival_time,
          st.departure_time,
          st.stop_sequence,
          s.name AS stop_name,
          s.lat  AS stop_lat,
          s.lng  AS stop_lng
        FROM stop_times st
        JOIN stops s ON s.id = st.stop_id
        WHERE st.trip_id = ${tripId}
        ORDER BY st.stop_sequence
      `,
    ]);

    if (tripRows.length === 0) {
      res.status(404).json({ error: `Trip '${tripId}' not found` });
      return;
    }

    const t = tripRows[0];
    const stopTimes: StopTimeWithStop[] = stopTimeRows.map((st) => ({
      tripId:        st.trip_id,
      stopId:        st.stop_id,
      arrivalTime:   st.arrival_time,
      departureTime: st.departure_time,
      stopSequence:  st.stop_sequence,
      stopName:      st.stop_name,
      stopLat:       parseFloat(st.stop_lat),
      stopLng:       parseFloat(st.stop_lng),
    }));

    const trip: TripDetail = {
      id:            t.id,
      operatorId:    t.operator_id,
      routeId:       t.route_id,
      directionId:   t.direction_id,
      headsign:      t.headsign,
      serviceDays:   t.service_days ?? [],
      routeName:     t.route_name,
      routeColor:    t.route_color,
      routeTextColor: t.route_text_color,
      stopTimes,
    };

    // Trip details are stable — cache aggressively
    res.setHeader("Cache-Control", "public, s-maxage=86400, stale-while-revalidate=604800");
    res.status(200).json(trip);
  } catch (err) {
    console.error(`/trips/${tripId} error:`, err);
    res.status(500).json({ error: "Internal Server Error" });
  }
}
```

---

## Task 11 — Deploy to Vercel

Each operator gets its own Vercel project linked to the `api/` directory. The only required environment variable is `DATABASE_URL`.

- [ ] Navigate to the api directory and link it to a new Vercel project:

```bash
cd api
vercel link
```

When prompted:
- "Set up and deploy?" → Y
- "Which scope?" → select your team/account
- "Link to existing project?" → N (create new)
- "Project name?" → e.g. `transitkit-api-rfta`
- "In which directory is your code located?" → `./` (the `api/` directory)

- [ ] Add the `DATABASE_URL` environment variable for Production, Preview, and Development:

```bash
vercel env add DATABASE_URL
```

When prompted, paste the Neon `DATABASE_URL` from `shared/operators/rfta/.env`. Select all three environments (Production, Preview, Development).

- [ ] Pull the env vars locally for `vercel dev`:

```bash
vercel env pull .env.local
```

- [ ] Test locally:

```bash
vercel dev
```

Then in a separate terminal:

```bash
# Test /config
curl http://localhost:3000/config

# Test /routes
curl http://localhost:3000/routes

# Test /stops/nearby
curl "http://localhost:3000/stops/nearby?lat=39.35&lng=-107.0&radius=1000"

# Test /schedule (may take a few seconds on first call)
curl http://localhost:3000/schedule | head -c 500
```

- [ ] Deploy to production:

```bash
vercel --prod
```

Vercel will print the production URL, e.g. `https://transitkit-api-rfta.vercel.app`.

- [ ] Verify the production deployment:

```bash
curl https://transitkit-api-rfta.vercel.app/config
curl https://transitkit-api-rfta.vercel.app/routes
```

- [ ] Add `apiUrl` to `shared/operators/rfta/config.json` (done in Plan 3 — just note it here):

```json
"apiUrl": "https://transitkit-api-rfta.vercel.app"
```
