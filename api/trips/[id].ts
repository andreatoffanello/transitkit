import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from '../_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const tripId = req.query.id as string;

  const [tripRows, stopTimeRows] = await Promise.all([
    sql`
      SELECT t.id, t.route_id, t.direction_id, t.headsign, t.service_days,
             r.name AS route_name, r.color, r.text_color, r.transit_type
      FROM trips t
      JOIN routes r ON r.id = t.route_id
      WHERE t.id = ${tripId}
    `,
    sql`
      SELECT st.stop_sequence, st.arrival_time, st.departure_time,
             s.id AS stop_id, s.name AS stop_name, s.lat, s.lng
      FROM stop_times st
      JOIN stops s ON s.id = st.stop_id
      WHERE st.trip_id = ${tripId}
      ORDER BY st.stop_sequence
    `,
  ]);

  if (tripRows.length === 0) return res.status(404).json({ error: 'Trip not found' });

  const trip = tripRows[0];
  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
  return res.status(200).json({
    id: trip.id,
    routeId: trip.route_id,
    routeName: trip.route_name,
    routeColor: trip.color,
    routeTextColor: trip.text_color,
    transitType: trip.transit_type,
    directionId: trip.direction_id,
    headsign: trip.headsign,
    serviceDays: trip.service_days,
    stopTimes: stopTimeRows.map(st => ({
      stopSequence: st.stop_sequence,
      stopId: st.stop_id,
      stopName: st.stop_name,
      lat: st.lat,
      lng: st.lng,
      arrivalTime: st.arrival_time,
      departureTime: st.departure_time,
    })),
  });
}
