import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from '../_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const rows = await sql`
    SELECT id, name, long_name, color, text_color, transit_type
    FROM routes
    ORDER BY name
  `;

  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
  return res.status(200).json(rows.map(r => ({
    id: r.id,
    name: r.name,
    longName: r.long_name,
    color: r.color,
    textColor: r.text_color,
    transitType: r.transit_type,
  })));
}
