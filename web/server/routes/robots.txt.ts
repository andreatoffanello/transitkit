import { resolveOperatorId } from '~/utils/operators'

export function buildRobotsTxt(host: string, isKnownOperator: boolean): string {
  const lines = ['User-agent: *', 'Allow: /']
  if (isKnownOperator) {
    lines.push(`Sitemap: https://${host}/sitemap.xml`)
  }
  return lines.join('\n') + '\n'
}

export default defineEventHandler((event) => {
  const host = getRequestHeader(event, 'host') ?? ''
  const operatorId = resolveOperatorId(host)

  setResponseHeader(event, 'content-type', 'text/plain; charset=utf-8')
  setResponseHeader(event, 'cache-control', 'public, max-age=86400')

  return buildRobotsTxt(host, operatorId !== null)
})
