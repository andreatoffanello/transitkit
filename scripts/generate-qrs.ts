/**
 * Genera QR code PNG per ogni fermata di un operatore.
 *
 * Uso:
 *   npx tsx scripts/generate-qrs.ts --operator appalcart
 *   npx tsx scripts/generate-qrs.ts --operator appalcart --host fermate.appalcart.com
 *
 * Output: qr/{operatorId}/stop-{stopId}-{stopName}.png
 *
 * Variabili d'ambiente:
 *   CDN_BASE  — URL base CDN (default: https://andreatoffanello.github.io/transitkit-data)
 */

import QRCode from 'qrcode'
import { promises as fs } from 'fs'
import path from 'path'

// ---- CLI arg parsing ----

function getArg(flag: string): string | undefined {
  const args = process.argv.slice(2)
  const idx = args.indexOf(flag)
  if (idx === -1 || idx + 1 >= args.length) return undefined
  const value = args[idx + 1]
  return value !== undefined && !value.startsWith('--') ? value : undefined
}

const operatorId = getArg('--operator')
const hostOverride = getArg('--host')

if (!operatorId) {
  console.error('Usage: npx tsx scripts/generate-qrs.ts --operator <operatorId> [--host <hostname>]')
  process.exit(1)
}

if (!/^[a-z0-9_-]+$/i.test(operatorId)) {
  console.error('Error: operatorId must be alphanumeric (letters, numbers, hyphens, underscores only)')
  process.exit(1)
}

// ---- Config ----

const CDN_BASE = process.env['CDN_BASE'] ?? 'https://andreatoffanello.github.io/transitkit-data'
const WEB_HOST = hostOverride ?? `${operatorId}.transitkit.app`
const OUTPUT_DIR = path.join(process.cwd(), 'qr', operatorId)

// ---- Helpers ----

function slugify(name: string): string {
  return (
    name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '') // strip combining diacritics
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/^-|-$/g, '') || 'unnamed'
  )
}

// ---- Main ----

async function main(): Promise<void> {
  console.log(`Operator:    ${operatorId}`)
  console.log(`CDN:         ${CDN_BASE}/${operatorId}/schedules.json`)
  console.log(`Web host:    ${WEB_HOST}`)
  console.log(`Output dir:  ${OUTPUT_DIR}`)
  console.log()

  // 1. Fetch schedules from CDN
  const res = await fetch(`${CDN_BASE}/${operatorId}/schedules.json`)
  if (!res.ok) {
    throw new Error(`CDN fetch failed: ${res.status} ${res.statusText}`)
  }
  const data = (await res.json()) as { stops: Array<{ id: string; name: string }> }

  if (!Array.isArray(data.stops) || data.stops.length === 0) {
    throw new Error('No stops found in schedules.json')
  }

  // 2. Create output directory
  await fs.mkdir(OUTPUT_DIR, { recursive: true })

  // 3. Generate one QR per stop
  let generated = 0
  const errors: Array<{ stopId: string; name: string; error: string }> = []
  for (const stop of data.stops) {
    if (!stop.id || typeof stop.name !== 'string') {
      errors.push({ stopId: stop.id ?? 'unknown', name: String(stop.name ?? ''), error: 'Missing id or name' })
      continue
    }

    try {
      const url = `https://${WEB_HOST}/stop/${stop.id}`
      const filename = `stop-${stop.id}-${slugify(stop.name)}.png`
      const filepath = path.join(OUTPUT_DIR, filename)

      await QRCode.toFile(filepath, url, {
        type: 'png',
        width: 300,
        margin: 2,
        errorCorrectionLevel: 'H',
        color: { dark: '#000000', light: '#FFFFFF' },
      })

      generated++
    } catch (err) {
      errors.push({
        stopId: stop.id,
        name: stop.name,
        error: err instanceof Error ? err.message : String(err),
      })
    }

    if (generated % 50 === 0 && generated > 0) {
      process.stdout.write(`\r  ${generated}/${data.stops.length} fermate...`)
    }
  }

  // Final progress clear
  if (generated > 0) process.stdout.write('\n')

  if (errors.length > 0) {
    console.error(`\n⚠ ${errors.length} QR non generati:`)
    for (const e of errors) console.error(`  [${e.stopId}] ${e.name}: ${e.error}`)
    console.log(`\n✓ ${generated} QR code generati in ${OUTPUT_DIR}/`)
    process.exit(1)
  }

  console.log(`✓ ${generated} QR code generati in ${OUTPUT_DIR}/`)
  console.log(`  Formato URL: https://${WEB_HOST}/stop/{stopId}`)
  console.log()
  console.log("Consegna la cartella qr/ all'operatore (zip o Google Drive).")
}

main().catch((err: unknown) => {
  console.error('\nErrore:', err instanceof Error ? err.message : err)
  process.exit(1)
})
