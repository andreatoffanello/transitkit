import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from '../_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const lat = parseFloat(req.query.lat as string);
  const lng = parseFloat(req.query.lng as string);
  const radius = parseFloat((req.query.radius as string) ?? '500');

  if (isNaN(lat) || isNaN(lng)) {
    return res.status(400).json({ error: 'lat and lng are required' });
  }

  // Haversine approximation via bounding box + distance filter using subquery
  const latDelta = radius / 111320;
  const lngDelta = radius / (111320 * Math.cos((lat * Math.PI) / 180));

  const rows = await sql`
    SELECT * FROM (
      SELECT
        id, name, lat, lng, platform_code, dock_letter,
        (6371000 * acos(
          cos(radians(${lat})) * cos(radians(lat)) *
          cos(radians(lng) - radians(${lng})) +
          sin(radians(${lat})) * sin(radians(lat))
        )) AS distance_m
      FROM stops
      WHERE
        lat BETWEEN ${lat - latDelta} AND ${lat + latDelta}
        AND lng BETWEEN ${lng - lngDelta} AND ${lng + lngDelta}
    ) AS candidates
    WHERE distance_m <= ${radius}
    ORDER BY distance_m
    LIMIT 20
  `;

  res.setHeader('Cache-Control', 's-maxage=60');
  return res.status(200).json(rows.map(r => ({
    id: r.id,
    name: r.name,
    lat: r.lat,
    lng: r.lng,
    platformCode: r.platform_code,
    dockLetter: r.dock_letter,
    distanceM: Math.round(r.distance_m as number),
  })));
}
