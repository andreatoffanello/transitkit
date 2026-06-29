<script setup lang="ts">
const { locale } = useI18n()
const { site } = useAppConfig()

const en = {
  title: 'Privacy',
  updated: 'Last updated: June 2026',
  intro:
    'This policy covers transitkit.app (this website) and the branded transit apps TransitKit builds for operators. It describes what we actually do with data — nothing more.',
  sections: [
    {
      h: 'This website',
      p: 'We run no analytics, tracking pixels, or advertising on this site. We collect nothing unless you email us. If you do, we use your message only to reply.',
    },
    {
      h: 'The apps we build',
      p: "A rider's favourite stops and lines stay on their device and are never sent to our servers. Push notifications work by topic subscription, so we never receive a rider's favourites or identity. We do not track riders, and we never sell passenger data.",
    },
    {
      h: 'Sub-processors',
      p: 'We rely on: Apple (App Store, push), Google (Google Play, Firebase Cloud Messaging), Vercel (web hosting), Hetzner (real-time data proxy), GitHub (static schedule data). Each processes only what is needed to run the service.',
    },
    {
      h: 'Your rights',
      p: 'Under GDPR and CCPA you can ask what data we hold about you, request a copy, or request deletion. Write to ' + 'andrea@transitkit.app' + ' and we will respond.',
    },
    {
      h: 'Per-operator deployments',
      p: 'Each operator app may be covered by additional terms from that operator. Where a signed agreement exists, its terms govern that specific deployment.',
    },
  ],
  contact: 'Questions: ',
  note: 'This is a general policy and a working draft, pending legal review.',
}

const it = {
  title: 'Privacy',
  updated: 'Ultimo aggiornamento: giugno 2026',
  intro:
    "Questa policy riguarda transitkit.app (questo sito) e le app di trasporto brandizzate che TransitKit realizza per gli operatori. Descrive cosa facciamo davvero con i dati — niente di più.",
  sections: [
    {
      h: 'Questo sito',
      p: "Su questo sito non usiamo analytics, pixel di tracciamento o pubblicità. Non raccogliamo nulla, a meno che tu non ci scriva. In quel caso usiamo il tuo messaggio solo per risponderti.",
    },
    {
      h: 'Le app che realizziamo',
      p: "Le fermate e le linee preferite di un passeggero restano sul suo dispositivo e non vengono mai inviate ai nostri server. Le notifiche push funzionano per iscrizione a topic, quindi non riceviamo mai i preferiti né l'identità di un passeggero. Non tracciamo i passeggeri e non vendiamo mai i loro dati.",
    },
    {
      h: 'Sub-responsabili',
      p: 'Ci appoggiamo a: Apple (App Store, push), Google (Google Play, Firebase Cloud Messaging), Vercel (hosting web), Hetzner (proxy dati real-time), GitHub (dati orari statici). Ciascuno tratta solo quanto serve a far funzionare il servizio.',
    },
    {
      h: 'I tuoi diritti',
      p: 'Ai sensi di GDPR e CCPA puoi chiederci quali dati abbiamo su di te, ottenerne una copia o chiederne la cancellazione. Scrivi a ' + 'andrea@transitkit.app' + ' e ti risponderemo.',
    },
    {
      h: 'Deployment per operatore',
      p: "Ogni app per operatore può essere coperta da termini aggiuntivi di quell'operatore. Dove esiste un contratto firmato, valgono i suoi termini per quel deployment specifico.",
    },
  ],
  contact: 'Domande: ',
  note: 'Questa è una policy generale e una bozza di lavoro, in attesa di revisione legale.',
}

const c = computed(() => (locale.value === 'it' ? it : en))
useHead({ htmlAttrs: { lang: locale } })
useSeoMeta({ title: () => `${c.value.title} — ${site.name}`, robots: 'noindex' })
</script>

<template>
  <article class="legal section">
    <div class="legal__shell">
      <h1 class="legal__title text-sheen">{{ c.title }}</h1>
      <p class="legal__updated">{{ c.updated }}</p>
      <p class="legal__intro">{{ c.intro }}</p>

      <section v-for="(s, i) in c.sections" :key="i" class="legal__sec">
        <h2>{{ s.h }}</h2>
        <p>{{ s.p }}</p>
      </section>

      <p class="legal__contact">
        {{ c.contact }}<a :href="`mailto:${site.contactEmail}`">{{ site.contactEmail }}</a>
      </p>
      <p class="legal__note">{{ c.note }}</p>
    </div>
  </article>
</template>

<style scoped>
.legal { padding-top: clamp(110px, 14vw, 160px); }
.legal__shell { max-width: 720px; margin: 0 auto; padding-inline: 24px; }
.legal__title { font-size: clamp(2.2rem, 5vw, 3rem); font-weight: 800; letter-spacing: -0.04em; }
.legal__updated { font-family: 'Geist Mono', monospace; font-size: 0.8125rem; color: var(--text-3); margin-top: 10px; }
.legal__intro { color: var(--text-2); font-size: 1.0625rem; line-height: 1.6; margin-top: 24px; }
.legal__sec { margin-top: 36px; }
.legal__sec h2 { font-size: 1.1875rem; font-weight: 650; letter-spacing: -0.02em; margin-bottom: 10px; }
.legal__sec p { color: var(--text-2); font-size: 1rem; line-height: 1.65; }
.legal__contact { margin-top: 40px; color: var(--text-2); font-size: 1rem; }
.legal__contact a { color: var(--text); text-decoration: underline; text-underline-offset: 3px; }
.legal__note { margin-top: 16px; color: var(--text-3); font-size: 0.875rem; font-style: italic; }
</style>
