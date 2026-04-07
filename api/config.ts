import type { VercelRequest, VercelResponse } from '@vercel/node';
import { sql } from './_lib/db';

export default async function handler(req: VercelRequest, res: VercelResponse) {
  if (req.method !== 'GET') return res.status(405).end();

  const rows = await sql`SELECT id, name, url, timezone, features FROM operators LIMIT 1`;
  if (rows.length === 0) return res.status(404).json({ error: 'Operator not found' });

  const op = rows[0];
  res.setHeader('Cache-Control', 's-maxage=3600, stale-while-revalidate=86400');
  return res.status(200).json({
    id: op.id,
    name: op.name,
    url: op.url,
    timezone: op.timezone,
    features: op.features,
  });
}
