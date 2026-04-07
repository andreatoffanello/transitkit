import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from './_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const [operatorRows, routeRows, directionRows, stopRows, departureRows] = await Promise.all([
    sql`SELECT id, name, url, timezone, features FROM operators LIMIT 1`,
    sql`SELECT id, name, long_name, color, text_color, transit_type FROM routes ORDER BY name`,
    sql`SELECT route_id, direction_id, headsign, stop_ids, shape_polyline FROM route_directions`,
    sql`SELECT id, name, lat, lng, platform_code, dock_letter FROM stops`,
    sql`
      SELECT
        st.stop_id,
        st.departure_time,
        t.id   AS trip_id,
        t.route_id,
        t.headsign,
        t.service_days,
        r.name AS route_name,
        r.color,
        r.text_color
      FROM stop_times st
      JOIN trips t ON t.id = st.trip_id
      JOIN routes r ON r.id = t.route_id
      ORDER BY st.stop_id, st.departure_time
    `,
  ]);

  if (operatorRows.length === 0) return res.status(404).json({ error: 'No operator found' });

  const operator = operatorRows[0];

  // Group directions by route_id
  const directionsByRoute: Record<string, typeof directionRows> = {};
  for (const d of directionRows) {
    if (!directionsByRoute[d.route_id]) directionsByRoute[d.route_id] = [];
    directionsByRoute[d.route_id].push(d);
  }

  const routes = routeRows.map(r => ({
    id: r.id,
    name: r.name,
    longName: r.long_name,
    color: r.color,
    textColor: r.text_color,
    transitType: r.transit_type,
    directions: (directionsByRoute[r.id] ?? []).map(d => ({
      directionId: d.direction_id,
      headsign: d.headsign,
      stopIds: d.stop_ids,
      shapePolyline: d.shape_polyline,
    })),
  }));

  // Group departures by stop_id
  const departuresByStop: Record<string, typeof departureRows> = {};
  for (const d of departureRows) {
    if (!departuresByStop[d.stop_id]) departuresByStop[d.stop_id] = [];
    departuresByStop[d.stop_id].push(d);
  }

  const stops = stopRows.map(s => ({
    id: s.id,
    name: s.name,
    lat: s.lat,
    lng: s.lng,
    platformCode: s.platform_code,
    dockLetter: s.dock_letter,
    departures: (departuresByStop[s.id] ?? []).map(d => ({
      tripId: d.trip_id,
      routeId: d.route_id,
      routeName: d.route_name,
      routeColor: d.color,
      routeTextColor: d.text_color,
      headsign: d.headsign,
      departureTime: d.departure_time,
      serviceDays: d.service_days,
    })),
  }));

  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
  return res.status(200).json({
    operator: {
      id: operator.id,
      name: operator.name,
      url: operator.url,
      timezone: operator.timezone,
      features: operator.features,
    },
    routes,
    stops,
    lastUpdated: new Date().toISOString(),
  });
}
