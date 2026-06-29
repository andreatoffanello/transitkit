import sharp from 'sharp'
import toIco from 'to-ico'
import { readFile, writeFile } from 'node:fs/promises'

const icon = await readFile('public/icon.svg')
const og = await readFile('public/og.svg')

// apple-touch-icon 180
await sharp(icon).resize(180, 180).png().toFile('public/apple-touch-icon.png')
// maskable / android 192 + 512
await sharp(icon).resize(192, 192).png().toFile('public/icon-192.png')
await sharp(icon).resize(512, 512).png().toFile('public/icon-512.png')
// favicon source 32 + 16 -> .ico
const p32 = await sharp(icon).resize(32, 32).png().toBuffer()
const p16 = await sharp(icon).resize(16, 16).png().toBuffer()
await writeFile('public/favicon.ico', await toIco([p16, p32]))
// og 1200x630
await sharp(og).resize(1200, 630).png().toFile('public/og.png')

console.log('brand assets generated')
