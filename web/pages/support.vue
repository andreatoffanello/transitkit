<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <div class="max-w-lg mx-auto lg:max-w-2xl flex flex-col" style="min-height: 100vh">
      <header class="sticky top-0 z-30 flex items-center h-[52px] px-3" style="background-color: color-mix(in srgb, var(--bg-primary) 90%, transparent); backdrop-filter: blur(16px) saturate(180%); border-bottom: 1px solid var(--border)">
        <NuxtLink
          v-if="hasOperator"
          to="/"
          class="flex items-center gap-1.5 pl-1 pr-3 py-2 rounded-lg transition-opacity duration-150 active:opacity-60"
          style="color: var(--color-primary); font-size: 14px; font-weight: 500"
        >
          <ChevronLeft :size="20" :stroke-width="1.75" />
          <span>{{ t.back }}</span>
        </NuxtLink>
        <h1 class="flex-1 text-center text-[15px] font-semibold" style="color: var(--text-primary)">
          {{ t.title }}
        </h1>
        <div class="w-[80px]" />
      </header>

      <article class="px-5 pt-6 pb-12 flex-1">
        <header class="mb-6">
          <div
            class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4"
            style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
          >
            <LifeBuoy :size="22" :stroke-width="1.75" style="color: var(--color-primary)" />
          </div>
          <h2 class="text-2xl font-bold leading-tight" style="color: var(--text-primary); letter-spacing: -0.015em">
            {{ t.heading }}
          </h2>
          <p class="text-[15px] mt-4 leading-relaxed" style="color: var(--text-secondary)">
            {{ t.intro }}
          </p>
        </header>

        <section
          v-for="(section, i) in t.sections"
          :key="i"
          class="mb-6 rounded-2xl p-5"
          style="background-color: var(--bg-elevated); box-shadow: var(--shadow-sm)"
        >
          <h3 class="text-base font-semibold mb-2.5" style="color: var(--text-primary)">
            {{ section.title }}
          </h3>
          <p
            v-if="section.body"
            class="text-[15px] leading-relaxed"
            style="color: var(--text-secondary)"
          >
            {{ section.body }}
          </p>
          <ul
            v-if="section.bullets"
            class="space-y-2 mt-1"
          >
            <li
              v-for="(bullet, j) in section.bullets"
              :key="j"
              class="flex gap-2.5 text-[15px] leading-relaxed"
              style="color: var(--text-secondary)"
            >
              <span class="shrink-0 mt-2.5 w-1 h-1 rounded-full" style="background-color: var(--text-tertiary)" aria-hidden="true" />
              <span>{{ bullet }}</span>
            </li>
          </ul>
        </section>

        <section class="mt-8 text-center">
          <p class="text-sm" style="color: var(--text-tertiary)">
            {{ t.contactLabel }}
          </p>
          <a
            href="mailto:support@transitkit.app"
            class="inline-block mt-1 text-sm font-medium"
            style="color: var(--color-primary)"
          >
            support@transitkit.app
          </a>
        </section>
      </article>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ChevronLeft, LifeBuoy } from 'lucide-vue-next'

const operatorId = useState<string>('operatorId', () => '')
const hasOperator = computed(() => !!operatorId.value)

const configLocale = ref<string | null>(null)
if (hasOperator.value) {
  try {
    const { config } = await useOperator()
    configLocale.value = config.value?.locale?.[0] ?? null
  }
  catch {
    // Operator-agnostic fallback
  }
}

type Section = { title: string; body?: string; bullets?: string[] }
type SupportText = {
  title: string
  back: string
  heading: string
  intro: string
  sections: Section[]
  contactLabel: string
}

const IT: SupportText = {
  title: 'Assistenza',
  back: 'Indietro',
  heading: 'Assistenza',
  intro: 'Risposte rapide alle domande più comuni. Se non trovi quello che cerchi, scrivici e proviamo a sistemare.',
  sections: [
    {
      title: 'Gli autobus non si muovono sulla mappa',
      body: 'L\'app mostra le posizioni in tempo reale solo quando l\'operatore le sta trasmettendo. In notturna, durante festività o pause accademiche il servizio può essere ridotto. Verifica gli orari della linea per confermare che sia in servizio in quel momento.',
    },
    {
      title: 'I tempi di arrivo sembrano sbagliati',
      body: 'I tempi sono basati sulla combinazione dell\'orario pianificato e degli aggiornamenti live del veicolo. Quando il segnale live manca temporaneamente, l\'app torna all\'orario pianificato. Se la differenza è persistente, segnalalo all\'operatore: spesso si tratta di un problema lato feed.',
    },
    {
      title: 'Non vedo le fermate vicine',
      body: 'Controlla che il permesso di localizzazione sia attivo. Su iOS: Impostazioni → Privacy e Sicurezza → Localizzazione → AppalRider → Mentre usi l\'app. Su Android: Impostazioni → App → AppalRider → Autorizzazioni → Posizione.',
    },
    {
      title: 'Le notifiche push non arrivano',
      body: 'Apri l\'app, vai in Impostazioni e attiva le notifiche. Su iOS controlla anche Impostazioni → Notifiche → AppalRider. Su alcuni dispositivi Android risparmio energetico o ottimizzazione batteria possono bloccare le push: aggiungi AppalRider alle app non ottimizzate.',
    },
    {
      title: 'Domande sul servizio di trasporto',
      bullets: [
        'AppalCART Dispatch: 828-297-1300',
        'TDD: 1-800-735-2962',
        'Per Paratransit e Rural Services contatta direttamente Dispatch.',
      ],
    },
    {
      title: 'Bug o suggerimenti',
      body: 'Apriamo volentieri segnalazioni dettagliate: modello del dispositivo, versione iOS/Android, cosa stavi facendo, cosa ti aspettavi e cosa è successo. Più contesto ci dai, più velocemente possiamo intervenire.',
    },
  ],
  contactLabel: 'Per qualsiasi altra cosa',
}

const EN: SupportText = {
  title: 'Support',
  back: 'Back',
  heading: 'Support',
  intro: 'Quick answers to the most common questions. If you do not find what you need, drop us a line and we will look into it.',
  sections: [
    {
      title: 'Buses are not moving on the map',
      body: 'The app shows live positions only while the operator is broadcasting them. Service can be reduced overnight, on holidays, or during academic breaks. Check the line schedule to confirm the route is running at that time.',
    },
    {
      title: 'Arrival times look off',
      body: 'Times combine the scheduled timetable with live vehicle updates. When the live feed drops out temporarily, the app falls back to the scheduled time. If the gap is persistent, report it to the operator — it is usually a feed-side issue.',
    },
    {
      title: 'I do not see nearby stops',
      body: 'Make sure location permission is granted. iOS: Settings → Privacy & Security → Location Services → AppalRider → While Using the App. Android: Settings → Apps → AppalRider → Permissions → Location.',
    },
    {
      title: 'Push notifications are not arriving',
      body: 'Open the app, go to Settings, and enable notifications. On iOS also check Settings → Notifications → AppalRider. On some Android devices, battery optimization can block push delivery — add AppalRider to your unrestricted apps.',
    },
    {
      title: 'Questions about the bus service itself',
      bullets: [
        'AppalCART Dispatch: 828-297-1300',
        'TDD: 1-800-735-2962',
        'For Paratransit and Rural Services bookings, call Dispatch directly.',
      ],
    },
    {
      title: 'Bugs or feature requests',
      body: 'We welcome detailed reports: device model, iOS or Android version, what you were doing, what you expected, and what happened instead. The more context you share, the faster we can act.',
    },
  ],
  contactLabel: 'For anything else',
}

const locale = computed<'it' | 'en'>(() => {
  const raw = configLocale.value?.split('-')[0]?.toLowerCase()
  return raw === 'en' ? 'en' : 'it'
})

const t = computed(() => (locale.value === 'en' ? EN : IT))

useHead(() => ({
  title: `${t.value.heading} — TransitKit`,
  meta: [
    { name: 'description', content: t.value.intro },
    { name: 'robots', content: 'index,follow' },
  ],
}))
</script>
