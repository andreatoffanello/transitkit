// "Book a demo" lead capture → email to team@ via Resend.
// Runs as a Vercel Function (nitro preset: 'vercel'). No lead is ever persisted
// server-side beyond the outbound email.

interface LeadBody {
  name?: string
  agency?: string
  email?: string
  message?: string
  plan?: string
  locale?: string
  company_website?: string // honeypot
}

const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
const clip = (v: unknown, max: number) => String(v ?? '').trim().slice(0, max)
const esc = (s: string) =>
  s.replace(/[&<>"]/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;' }[c] as string))

export default defineEventHandler(async (event) => {
  const body = await readBody<LeadBody>(event)

  // Honeypot: a bot filled the hidden field. Ack success, send nothing.
  if (clip(body?.company_website, 200)) return { ok: true }

  const name = clip(body?.name, 120)
  const agency = clip(body?.agency, 160)
  const email = clip(body?.email, 200)
  const message = clip(body?.message, 4000)
  const plan = clip(body?.plan, 40)
  const locale = clip(body?.locale, 8) || 'en'

  if (!name || !agency || !EMAIL_RE.test(email)) {
    throw createError({ statusCode: 400, statusMessage: 'Invalid lead payload' })
  }

  const config = useRuntimeConfig(event)
  const apiKey = config.resendApiKey || process.env.RESEND_API_KEY || process.env.NUXT_RESEND_API_KEY
  if (!apiKey) {
    console.error('[lead] RESEND_API_KEY not configured — cannot send lead email')
    throw createError({ statusCode: 500, statusMessage: 'Email not configured' })
  }

  const to = config.leadTo || 'team@transitkit.app'
  const from = config.leadFrom || 'TransitKit <leads@transitkit.app>'

  const subject = plan
    ? `New demo request — ${agency} (${plan})`
    : `New demo request — ${agency}`

  const rows: [string, string][] = [
    ['Name', name],
    ['Agency', agency],
    ['Email', email],
    ...(plan ? ([['Plan', plan]] as [string, string][]) : []),
    ['Locale', locale],
  ]
  const text =
    rows.map(([k, v]) => `${k}: ${v}`).join('\n') +
    (message ? `\n\nMessage:\n${message}` : '')
  const html =
    `<div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;font-size:15px;line-height:1.6;color:#111">` +
    `<h2 style="margin:0 0 14px;font-size:18px">New demo request</h2>` +
    `<table style="border-collapse:collapse">` +
    rows
      .map(
        ([k, v]) =>
          `<tr><td style="padding:2px 14px 2px 0;color:#666;vertical-align:top">${esc(k)}</td>` +
          `<td style="padding:2px 0"><strong>${esc(v)}</strong></td></tr>`,
      )
      .join('') +
    `</table>` +
    (message
      ? `<p style="margin:16px 0 4px;color:#666">Message</p><p style="margin:0;white-space:pre-wrap">${esc(message)}</p>`
      : '') +
    `</div>`

  const res = await $fetch.raw('https://api.resend.com/emails', {
    method: 'POST',
    headers: {
      Authorization: `Bearer ${apiKey}`,
      'Content-Type': 'application/json',
    },
    body: { from, to: [to], reply_to: email, subject, html, text },
    ignoreResponseError: true,
  })

  if (res.status >= 300) {
    console.error('[lead] Resend send failed', res.status, res._data)
    throw createError({ statusCode: 502, statusMessage: 'Could not send lead email' })
  }

  return { ok: true }
})
