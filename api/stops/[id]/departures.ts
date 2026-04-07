import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from '../../_lib/db';

const DAY_NAMES = ['sunday','monday','tuesday','wednesday','thursday','friday','saturday'];

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const stopId = req.query.id as string;
  const dateStr = (req.query.date as string) ?? new Date().toISOString().slice(0, 10);
  const limit = parseInt((req.query.limit as string) ?? '50');

  const date = new Date(dateStr + 'T00:00:00Z');
  const dayName = DAY_NAMES[date.getUTCDay()];

  const rows = await sql`
    SELECT
      st.departure_time,
      t.id   AS trip_id,
      t.route_id,
      t.headsign,
      t.service_days,
      r.name AS route_name,
      r.color,
      r.text_color,
      r.transit_type
    FROM stop_times st
    JOIN trips t ON t.id = st.trip_id
    JOIN routes r ON r.id = t.route_id
    WHERE st.stop_id = ${stopId}
      AND ${dayName} = ANY(t.service_days)
    ORDER BY st.departure_time
    LIMIT ${limit}
  `;

  if (rows.length === 0) {
    // Check if stop exists at all
    const stopCheck = await sql`SELECT id FROM stops WHERE id = ${stopId}`;
    if (stopCheck.length === 0) return res.status(404).json({ error: 'Stop not found' });
  }

  res.setHeader('Cache-Control', 's-maxage=60, stale-while-revalidate=300');
  return res.status(200).json(rows.map(r => ({
    tripId: r.trip_id,
    routeId: r.route_id,
    routeName: r.route_name,
    routeColor: r.color,
    routeTextColor: r.text_color,
    transitType: r.transit_type,
    headsign: r.headsign,
    departureTime: r.departure_time,
    serviceDays: r.service_days,
  })));
}
