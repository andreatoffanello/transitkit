<template>
  <div class="min-h-screen" style="background-color: var(--bg-secondary)">
    <div class="max-w-lg mx-auto lg:max-w-2xl flex flex-col" style="min-height: 100vh">
      <!-- Header -->
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
        <!-- Intro card -->
        <header class="mb-6">
          <div
            class="w-12 h-12 rounded-2xl flex items-center justify-center mb-4"
            style="background-color: color-mix(in srgb, var(--color-primary) 12%, transparent)"
          >
            <Shield :size="22" :stroke-width="1.75" style="color: var(--color-primary)" />
          </div>
          <h2 class="text-2xl font-bold leading-tight" style="color: var(--text-primary); letter-spacing: -0.015em">
            {{ t.heading }}
          </h2>
          <p class="text-sm mt-1.5" style="color: var(--text-tertiary)">
            {{ t.lastUpdatedLabel }} {{ formattedDate }}
          </p>
          <p class="text-[15px] mt-4 leading-relaxed" style="color: var(--text-secondary)">
            {{ t.intro }}
          </p>
        </header>

        <!-- Sections -->
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

        <!-- Contact -->
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
import { ChevronLeft, Shield } from 'lucide-vue-next'

// Privacy è operator-agnostic: se l'host non matcha un operatore, niente fetch.
const operatorId = useState<string>('operatorId', () => '')
const hasOperator = computed(() => !!operatorId.value)

// Se c'è un operatore, prova a caricare la config per il locale + theme;
// se fallisce (o non c'è operatore) usa default IT.
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
type PrivacyText = {
  title: string
  back: string
  heading: string
  lastUpdatedLabel: string
  intro: string
  sections: Section[]
  contactLabel: string
}

const IT: PrivacyText = {
  title: 'Privacy',
  back: 'Indietro',
  heading: 'Privacy Policy',
  lastUpdatedLabel: 'Ultimo aggiornamento:',
  intro: 'TransitKit è un\'app per orari e informazioni in tempo reale sul trasporto pubblico. Questa policy spiega, in modo diretto, come vengono trattati i dati quando usi l\'app o questo sito.',
  sections: [
    {
      title: 'Dati che non raccogliamo',
      bullets: [
        'Non creiamo account utente: non serve registrarsi.',
        'Non raccogliamo nome, email, numero di telefono o altri dati identificativi.',
        'Non usiamo pixel pubblicitari né strumenti di tracciamento, e non colleghiamo nessun dato a una persona.',
        'Non vendiamo né condividiamo dati con inserzionisti o broker.',
      ],
    },
    {
      title: 'Statistiche d\'uso anonime',
      body: 'Nell\'app AppalRider raccogliamo statistiche d\'uso anonime: quante volte l\'app viene aperta, quali schermate vengono visitate e per quanto dura una sessione (in fasce, per esempio meno di 30 secondi), insieme a lingua, versione dell\'app e del sistema. Ogni installazione usa un identificativo casuale generato dall\'app, che non è collegato a te, al tuo dispositivo né a un account e non viene usato per tracciarti tra app o siti. Non raccogliamo posizione, nome o altri dati personali. I dati servono solo a migliorare l\'app. Sono ricevuti da un servizio su Cloudflare e conservati in un database Neon in Unione Europea (Francoforte). Puoi disattivarle in qualsiasi momento da Impostazioni → Statistiche d\'uso anonime: da quel momento non viene inviato più nulla e l\'identificativo locale viene cancellato.',
    },
    {
      title: 'Posizione',
      body: 'Se concedi il permesso di localizzazione, la tua posizione viene usata solo sul tuo dispositivo per calcolare le fermate vicine. Non viene inviata ai nostri server e non viene mai registrata.',
    },
    {
      title: 'Memoria locale',
      body: 'Le fermate preferite e quelle consultate di recente sono salvate localmente sul tuo dispositivo — nello storage del browser o dell\'app. Restano lì fino a quando non le cancelli tu o disinstalli l\'app.',
    },
    {
      title: 'Dati in tempo reale',
      body: 'Gli aggiornamenti real-time (posizioni veicoli, ritardi, avvisi) passano attraverso un proxy su rt.transitkit.app. Il proxy memorizza solo access log standard (indirizzo IP, data/ora, URL della richiesta) sull\'infrastruttura edge di Cloudflare. Questi log servono esclusivamente a diagnosticare disservizi e non vengono collegati a un\'identità né condivisi.',
    },
    {
      title: 'Dati di trasporto',
      body: 'Orari, linee e fermate provengono dai feed GTFS pubblici dell\'operatore di trasporto. Li ripubblichiamo così come sono, senza arricchirli con dati personali.',
    },
    {
      title: 'Servizi di terze parti',
      bullets: [
        'Mappe: tile serviti da MapLibre e dai collaboratori di OpenStreetMap.',
        'Distribuzione delle app: Apple App Store e Google Play applicano le loro privacy policy.',
        'Hosting del sito: Vercel applica la propria privacy policy per i log di infrastruttura.',
      ],
    },
    {
      title: 'Modifiche a questa policy',
      body: 'Se cambieranno il comportamento dell\'app o i servizi di terze parti, aggiorneremo questo documento. La data in alto riflette sempre l\'ultima revisione.',
    },
  ],
  contactLabel: 'Domande o richieste',
}

const EN: PrivacyText = {
  title: 'Privacy',
  back: 'Back',
  heading: 'Privacy Policy',
  lastUpdatedLabel: 'Last updated:',
  intro: 'TransitKit is a public transit app for schedules and real-time information. This policy explains, plainly, how data is handled when you use the app or this site.',
  sections: [
    {
      title: 'Data we do not collect',
      bullets: [
        'We do not create user accounts — no sign-up required.',
        'We do not collect names, email addresses, phone numbers, or other identifying information.',
        'We do not use ad pixels or tracking tools, and we do not link any data to a person.',
        'We do not sell or share data with advertisers or data brokers.',
      ],
    },
    {
      title: 'Anonymous usage statistics',
      body: 'In the AppalRider app we collect anonymous usage statistics: how often the app is opened, which screens are visited and how long a session lasts (in ranges, such as under 30 seconds), together with language, app version and OS version. Each installation uses a random identifier generated by the app, which is not linked to you, your device or any account and is not used to track you across apps or sites. We do not collect location, name or any other personal data. The data is used only to improve the app. It is received by a service running on Cloudflare and stored in a Neon database in the European Union (Frankfurt). You can turn it off at any time in Settings → Anonymous usage statistics: from then on nothing is sent and the local identifier is deleted.',
    },
    {
      title: 'Location',
      body: 'If you grant location permission, your position is used only on your device to find nearby stops. It is never sent to our servers and is never logged.',
    },
    {
      title: 'Local storage',
      body: 'Your favorite and recent stops are saved locally on your device — in browser storage or app storage. They stay there until you clear them or uninstall the app.',
    },
    {
      title: 'Real-time data',
      body: 'Real-time updates (vehicle positions, delays, alerts) are delivered through a proxy at rt.transitkit.app. The proxy stores only standard access logs (IP address, timestamp, request URL) on Cloudflare\'s edge infrastructure. These logs are used solely to diagnose outages and are not linked to any identity or shared with third parties.',
    },
    {
      title: 'Transit data',
      body: 'Schedule, line, and stop data come from the transit operator\'s public GTFS feeds. We republish this data as-is without enriching it with personal information.',
    },
    {
      title: 'Third-party services',
      bullets: [
        'Maps: tiles are served by MapLibre and the OpenStreetMap contributors.',
        'App distribution: Apple App Store and Google Play apply their own privacy policies.',
        'Website hosting: Vercel applies its own privacy policy for infrastructure logs.',
      ],
    },
    {
      title: 'Changes to this policy',
      body: 'If the app\'s behavior or third-party services change, this document will be updated. The date above always reflects the most recent revision.',
    },
  ],
  contactLabel: 'Questions or requests',
}

const ES: PrivacyText = {
  title: 'Privacidad',
  back: 'Atrás',
  heading: 'Política de privacidad',
  lastUpdatedLabel: 'Última actualización:',
  intro: 'TransitKit es una app de transporte público con horarios e información en tiempo real. Esta política explica, de forma directa, cómo se tratan los datos cuando usas la app o este sitio.',
  sections: [
    {
      title: 'Datos que no recopilamos',
      bullets: [
        'No creamos cuentas de usuario: no hace falta registrarse.',
        'No recopilamos nombre, correo electrónico, número de teléfono ni otros datos identificativos.',
        'No usamos píxeles publicitarios ni herramientas de seguimiento, y no vinculamos ningún dato a una persona.',
        'No vendemos ni compartimos datos con anunciantes ni intermediarios de datos.',
      ],
    },
    {
      title: 'Estadísticas de uso anónimas',
      body: 'En la app AppalRider recopilamos estadísticas de uso anónimas: cuántas veces se abre la app, qué pantallas se visitan y cuánto dura una sesión (por rangos, por ejemplo menos de 30 segundos), junto con el idioma y las versiones de la app y del sistema. Cada instalación usa un identificador aleatorio generado por la app, que no está vinculado a ti, a tu dispositivo ni a ninguna cuenta y no se usa para seguirte entre apps o sitios. No recopilamos ubicación, nombre ni otros datos personales. Los datos sirven solo para mejorar la app. Los recibe un servicio en Cloudflare y se guardan en una base de datos Neon en la Unión Europea (Fráncfort). Puedes desactivarlas en cualquier momento en Ajustes → Estadísticas de uso anónimas: desde ese momento no se envía nada y el identificador local se borra.',
    },
    {
      title: 'Ubicación',
      body: 'Si concedes el permiso de ubicación, tu posición se usa solo en tu dispositivo para encontrar las paradas cercanas. Nunca se envía a nuestros servidores y nunca se registra.',
    },
    {
      title: 'Almacenamiento local',
      body: 'Tus paradas favoritas y recientes se guardan localmente en tu dispositivo, en el almacenamiento del navegador o de la app. Permanecen ahí hasta que las borres o desinstales la app.',
    },
    {
      title: 'Datos en tiempo real',
      body: 'Las actualizaciones en tiempo real (posiciones de vehículos, retrasos, avisos) pasan por un proxy en rt.transitkit.app. El proxy guarda solo registros de acceso estándar (dirección IP, fecha y hora, URL de la solicitud) en la infraestructura de Cloudflare. Estos registros sirven únicamente para diagnosticar fallos y no se vinculan a ninguna identidad ni se comparten.',
    },
    {
      title: 'Datos de transporte',
      body: 'Los horarios, líneas y paradas provienen de los feeds GTFS públicos del operador de transporte. Los republicamos tal cual, sin añadirles datos personales.',
    },
    {
      title: 'Servicios de terceros',
      bullets: [
        'Mapas: las teselas las sirven MapLibre y los colaboradores de OpenStreetMap.',
        'Distribución de las apps: Apple App Store y Google Play aplican sus propias políticas de privacidad.',
        'Alojamiento del sitio: Vercel aplica su propia política de privacidad para los registros de infraestructura.',
      ],
    },
    {
      title: 'Cambios en esta política',
      body: 'Si cambian el comportamiento de la app o los servicios de terceros, actualizaremos este documento. La fecha de arriba refleja siempre la última revisión.',
    },
  ],
  contactLabel: 'Preguntas o solicitudes',
}

// 2026-10-11 — keep in sync with content above when it changes.
const LAST_UPDATED = '2026-10-11'

const locale = computed<'it' | 'en' | 'es'>(() => {
  const raw = configLocale.value?.split('-')[0]?.toLowerCase()
  return raw === 'en' ? 'en' : raw === 'es' ? 'es' : 'it'
})

const t = computed(() => (locale.value === 'en' ? EN : locale.value === 'es' ? ES : IT))

const formattedDate = computed(() => {
  const d = new Date(LAST_UPDATED)
  const loc = locale.value === 'en' ? 'en-US' : locale.value === 'es' ? 'es-ES' : 'it-IT'
  return d.toLocaleDateString(loc, { day: 'numeric', month: 'long', year: 'numeric' })
})

useHead(() => ({
  title: `${t.value.heading} — TransitKit`,
  meta: [
    { name: 'description', content: t.value.intro },
    { name: 'robots', content: 'index,follow' },
  ],
}))
</script>
