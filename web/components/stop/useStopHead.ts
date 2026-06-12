import { computed } from 'vue'
import type { ComputedRef, Ref } from 'vue'
import type { OperatorConfig, Departure, Route, ScheduleStop } from '~/types'
import type { AppStrings } from '~/utils/strings'
import { computeNowMin } from '~/utils/schedule'

interface StopHeadArgs {
  stop: ComputedRef<ScheduleStop | null>
  config: Ref<OperatorConfig | null | undefined>
  s: ComputedRef<AppStrings>
  pending: ComputedRef<boolean>
  upcomingDepartures: ComputedRef<Departure[]>
  servingRoutes: ComputedRef<Route[]>
  now: Ref<number>
  canonicalHref: ComputedRef<string>
}

/**
 * Composes the `useHead` payload for the stop page: canonical link,
 * JSON-LD BusStop data, dynamic title with next-departure countdown,
 * OG title + description + robots directive.
 */
export function useStopHead(args: StopHeadArgs) {
  const { stop, config, s, pending, upcomingDepartures, servingRoutes, now, canonicalHref } = args

  useHead({
    link: [{ rel: 'canonical', href: canonicalHref }],
    script: [{
      type: 'application/ld+json',
      innerHTML: computed(() => {
        if (!stop.value) return '{}'
        const data: Record<string, unknown> = {
          '@context': 'https://schema.org',
          '@type': 'BusStop',
          name: stop.value.name,
          identifier: stop.value.id,
        }
        if (stop.value.lat != null && stop.value.lng != null) {
          data.geo = { '@type': 'GeoCoordinates', latitude: stop.value.lat, longitude: stop.value.lng }
        }
        return JSON.stringify(data)
      }),
    }],
    title: computed(() => {
      const opName = config.value?.brandName ?? config.value?.name ?? ''
      if (!pending.value && !stop.value) return `${s.value.stopNotFound} — ${opName}`
      const stopName = stop.value?.name ?? ''
      const base = stopName ? `${stopName} — ${opName}` : opName
      if (!upcomingDepartures.value.length) return base
      const next = upcomingDepartures.value[0]!
      const titleNowMin = computeNowMin(now.value, config.value?.timezone)
      let diffMin = next.minutesFromMidnight - titleNowMin
      if (next.realtimeDelay !== undefined) diffMin += Math.round(next.realtimeDelay / 60)
      if (diffMin < 0 || diffMin >= 60) return base
      if (diffMin === 0) return `${stopName} · ${next.lineName} ${s.value.now} — ${opName}`
      return `${stopName} · ${next.lineName} ${s.value.nextDepartureIn} ${diffMin} ${s.value.minutesShort} — ${opName}`
    }),
    meta: [
      { property: 'og:title', content: computed(() => stop.value?.name ?? config.value?.brandName ?? config.value?.name ?? '') },
      {
        name: 'description',
        content: computed(() => {
          const stopName = stop.value?.name ?? ''
          const op = config.value?.fullName ?? config.value?.name ?? ''
          const count = servingRoutes.value.length
          let lineCountPart = ''
          if (count > 0) {
            const lineWord = count === 1 ? s.value.lineSingular : s.value.linePlural
            lineCountPart = ` · ${count} ${lineWord}`
          }
          return `${stopName}${lineCountPart} · ${s.value.schedulesAndDepartures}${op ? ` — ${op}` : ''}`
        }),
      },
      { name: 'robots', content: computed(() => (!pending.value && !stop.value) ? 'noindex, nofollow' : 'index, follow') },
    ],
  })
}
