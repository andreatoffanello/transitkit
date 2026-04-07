import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from '../_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const routeId = req.query.id as string;

  const [routeRows, directionRows] = await Promise.all([
    sql`SELECT id, name, long_name, color, text_color, transit_type FROM routes WHERE id = ${routeId}`,
    sql`SELECT direction_id, headsign, stop_ids, shape_polyline FROM route_directions WHERE route_id = ${routeId} ORDER BY direction_id`,
  ]);

  if (routeRows.length === 0) return res.status(404).json({ error: 'Route not found' });

  const route = routeRows[0];
  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
  return res.status(200).json({
    id: route.id,
    name: route.name,
    longName: route.long_name,
    color: route.color,
    textColor: route.text_color,
    transitType: route.transit_type,
    directions: directionRows.map(d => ({
      directionId: d.direction_id,
      headsign: d.headsign,
      stopIds: d.stop_ids,
      shapePolyline: d.shape_polyline,
    })),
  });
}
