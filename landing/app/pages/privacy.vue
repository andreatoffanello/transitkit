<script setup lang="ts">
const { locale } = useI18n()
const { site } = useAppConfig()

const en = {
  "title": "Privacy Policy",
  "updated": "Last updated: July 2026",
  "intro": "This policy explains how TransitKit handles personal data across three distinct audiences: people who visit our website (transitkit.app), transit operators who subscribe to our service, and riders who use the branded apps we build for those operators. We designed the product to collect as little as possible, and this policy describes exactly what that means for each group. Governing law is Italy, and we honour data-protection rights under the GDPR, with equivalent rights extended to California residents under the CCPA.",
  "sections": [
    {
      "h": "Data controller and privacy contact",
      "p": "TransitKit is the data controller for its website and for the account and billing relationship with operator customers. For all data-protection matters — including access, copies, correction, or deletion — our dedicated privacy contact is andrea@transitkit.app. A formal Data Protection Officer is not required for an operation of this scale; this contact serves as the point of reference for every privacy request."
    },
    {
      "h": "Website visitors",
      "p": "The website transitkit.app runs no analytics, tracking pixels, or advertising, and collects nothing about a visitor unless the visitor chooses to email us. Where someone does email us, we use their message and address only to reply and to keep a record of the exchange."
    },
    {
      "h": "Operator customers and billing",
      "p": "For operators who subscribe, we process contact details (such as name and email) to provide the service, deliver support, and manage the account. Payment and billing data (billing name, email, and billing details) is collected and processed by Paddle.com as Merchant of Record, not stored on our servers."
    },
    {
      "h": "Riders using the apps",
      "p": "A rider's favourite stops and lines stay on the device and are never sent to our servers. Push notifications work by topic subscription, so we never receive a rider's favourites or identity; we do not track riders and never sell passenger data."
    },
    {
      "h": "Our role for rider data (processor)",
      "p": "By design, riders' favourites stay on the device and push works by topic, so very little rider personal data ever flows through the platform. Where any rider personal data is processed through the platform on behalf of an operator, TransitKit acts as a data processor and the operator as controller, under a data processing agreement (GDPR Art. 28). Business customers can request that agreement from the contact above."
    },
    {
      "h": "Legal bases for processing",
      "p": "Each purpose has its legal basis. Providing the service and managing subscriptions rests on performance of a contract. Replying to enquiries and keeping the service secure rest on our legitimate interest. Tax and accounting duties tied to billing rest on compliance with a legal obligation. Where any processing depends on consent, it rests on that consent, which may be withdrawn at any time."
    },
    {
      "h": "Payment processing via Paddle",
      "p": "Paddle.com is our Merchant of Record and acts as an independent controller for payment, invoicing, fraud-prevention, and tax data related to operator purchases. Purchases are also subject to Paddle's own buyer terms and privacy notice."
    },
    {
      "h": "Sub-processors and third parties",
      "p": "We rely on Apple (App Store and push delivery), Google (Google Play and Firebase Cloud Messaging), Vercel (web hosting), Hetzner (real-time data proxy), GitHub (static schedule data), and Paddle (payment processing and billing). Each is used only for the function described and receives only the data needed for it."
    },
    {
      "h": "International data transfers",
      "p": "Some of our sub-processors may process data outside the European Economic Area. Where that happens, we rely on appropriate safeguards, such as adequacy decisions or standard contractual clauses, to protect the data."
    },
    {
      "h": "Data retention",
      "p": "We keep personal data only for as long as needed to provide the service, answer requests, and meet our legal and accounting obligations. When data is no longer needed, we delete it or render it anonymous."
    },
    {
      "h": "Your rights",
      "p": "Under the GDPR you have the right to access, copy, correct, and delete your personal data, to restrict or object to processing, to data portability, and to withdraw consent where processing is based on it. To exercise any of these rights, write to andrea@transitkit.app. We also honour equivalent rights for California residents under the CCPA, including the right to know and to request deletion. We do not sell personal data."
    },
    {
      "h": "Complaints to a supervisory authority",
      "p": "Anyone who believes their data has been handled improperly may lodge a complaint with a data-protection supervisory authority. In Italy this is the Garante per la protezione dei dati personali."
    },
    {
      "h": "Cookies and tracking",
      "p": "Our website uses no cookies for analytics, advertising, or tracking, and sets no non-essential cookies. Because there are no non-essential cookies, no cookie-consent banner is required. We do not build profiles of visitors or riders, and we carry out no automated decision-making that produces legal or similarly significant effects."
    },
    {
      "h": "Security",
      "p": "We apply reasonable technical and organizational measures to protect personal data against loss, misuse, and unauthorized access. Keeping riders' favourites on the device by design means there is no central store of that data to expose."
    },
    {
      "h": "Children",
      "p": "Our service is not directed at children, and we do not knowingly collect personal data from children. If you believe a child has provided us data, contact us and we will delete it."
    },
    {
      "h": "Per-operator deployments",
      "p": "The apps we build are branded for individual transit operators, who may present their own additional terms or privacy notices to their riders. Those operator terms govern the operator's own relationship with its riders and are separate from this policy."
    },
    {
      "h": "Changes to this policy",
      "p": "We may update this policy from time to time. When we do, we will revise the date shown above, and material changes will be reflected on this page."
    }
  ],
  "contact": "Questions: "
}

const it = {
  "title": "Informativa sulla privacy",
  "updated": "Ultimo aggiornamento: luglio 2026",
  "intro": "La presente informativa spiega come TransitKit tratta i dati personali con riferimento a tre categorie distinte di soggetti: chi visita il nostro sito web (transitkit.app), gli operatori di trasporto che sottoscrivono il nostro servizio e i passeggeri che utilizzano le app personalizzate che realizziamo per tali operatori. Il prodotto è progettato per raccogliere il minimo indispensabile e la presente informativa descrive con precisione cosa ciò comporti per ciascuna categoria. La legge applicabile è quella italiana e TransitKit rispetta i diritti in materia di protezione dei dati previsti dal GDPR, estendendo diritti equivalenti ai residenti in California ai sensi del CCPA.",
  "sections": [
    {
      "h": "Titolare del trattamento e contatto privacy",
      "p": "TransitKit è il titolare del trattamento per il proprio sito web e per il rapporto di account e fatturazione con gli operatori clienti. Per ogni questione relativa alla protezione dei dati — incluse le richieste di accesso, copia, rettifica o cancellazione — il contatto privacy dedicato è andrea@transitkit.app. La nomina di un Responsabile della protezione dei dati non è richiesta per un'attività di queste dimensioni; tale contatto costituisce il punto di riferimento per ogni richiesta in materia di privacy."
    },
    {
      "h": "Visitatori del sito web",
      "p": "Il sito transitkit.app non utilizza strumenti di analisi, pixel di tracciamento o pubblicità e non raccoglie alcun dato relativo al visitatore, salvo che quest'ultimo scelga di inviarci un'email. In tal caso, il messaggio e l'indirizzo vengono utilizzati esclusivamente per rispondere e per conservare traccia dello scambio."
    },
    {
      "h": "Operatori clienti e fatturazione",
      "p": "Per gli operatori che sottoscrivono un abbonamento, trattiamo i dati di contatto (quali nome ed email) per erogare il servizio, fornire assistenza e gestire l'account. I dati di pagamento e fatturazione (nome di fatturazione, email e dettagli di fatturazione) sono raccolti e trattati da Paddle.com in qualità di Merchant of Record e non sono conservati sui nostri server."
    },
    {
      "h": "Passeggeri che usano le app",
      "p": "Le fermate e le linee preferite di un passeggero rimangono sul dispositivo e non vengono mai trasmesse ai nostri server. Le notifiche push funzionano tramite iscrizione a topic, per cui non riceviamo mai i preferiti né l'identità del passeggero; non tracciamo i passeggeri e non vendiamo mai i dati dei passeggeri."
    },
    {
      "h": "Il nostro ruolo per i dati dei passeggeri (responsabile)",
      "p": "Per progettazione, i preferiti dei passeggeri restano sul dispositivo e le notifiche funzionano tramite topic, cosicché attraverso la piattaforma transita pochissimo dato personale dei passeggeri. Qualora dati personali dei passeggeri vengano trattati tramite la piattaforma per conto di un operatore, TransitKit agisce in qualità di responsabile del trattamento e l'operatore in qualità di titolare, sulla base di un accordo sul trattamento dei dati (art. 28 GDPR). Gli operatori clienti possono richiedere tale accordo al contatto sopra indicato."
    },
    {
      "h": "Basi giuridiche del trattamento",
      "p": "A ciascuna finalità corrisponde la propria base giuridica. L'erogazione del servizio e la gestione degli abbonamenti si fondano sull'esecuzione di un contratto. La risposta alle richieste e il mantenimento della sicurezza del servizio si fondano sul nostro legittimo interesse. Gli adempimenti fiscali e contabili connessi alla fatturazione si fondano sull'adempimento di un obbligo legale. Ove un trattamento si basi sul consenso, esso si fonda su tale consenso, revocabile in qualsiasi momento."
    },
    {
      "h": "Trattamento dei pagamenti tramite Paddle",
      "p": "Paddle.com è il nostro Merchant of Record e agisce come titolare autonomo del trattamento per i dati di pagamento, fatturazione, prevenzione delle frodi e imposte relativi agli acquisti degli operatori. Gli acquisti sono inoltre soggetti alle condizioni per l'acquirente e all'informativa sulla privacy di Paddle."
    },
    {
      "h": "Responsabili del trattamento e terze parti",
      "p": "Ci avvaliamo di Apple (App Store e recapito delle notifiche push), Google (Google Play e Firebase Cloud Messaging), Vercel (hosting web), Hetzner (proxy per i dati in tempo reale), GitHub (dati statici degli orari) e Paddle (trattamento dei pagamenti e fatturazione). Ciascun soggetto è impiegato unicamente per la funzione descritta e riceve solo i dati necessari a tale scopo."
    },
    {
      "h": "Trasferimenti internazionali di dati",
      "p": "Alcuni dei nostri responsabili possono trattare dati al di fuori dello Spazio economico europeo. In tali casi ci basiamo su garanzie adeguate, quali decisioni di adeguatezza o clausole contrattuali tipo, per proteggere i dati."
    },
    {
      "h": "Conservazione dei dati",
      "p": "Conserviamo i dati personali solo per il tempo necessario a erogare il servizio, rispondere alle richieste e adempiere ai nostri obblighi legali e contabili. Quando i dati non sono più necessari, li cancelliamo o li rendiamo anonimi."
    },
    {
      "h": "I diritti dell'interessato",
      "p": "Ai sensi del GDPR, l'interessato ha il diritto di accedere ai propri dati personali, di ottenerne copia, di rettificarli e cancellarli, di limitare o opporsi al trattamento, alla portabilità dei dati e di revocare il consenso ove il trattamento su di esso si basi. Per esercitare tali diritti è possibile scrivere a andrea@transitkit.app. Riconosciamo inoltre diritti equivalenti ai residenti in California ai sensi del CCPA, incluso il diritto di conoscere i dati trattati e di richiederne la cancellazione. Non vendiamo dati personali."
    },
    {
      "h": "Reclamo all'autorità di controllo",
      "p": "Chiunque ritenga che i propri dati siano stati trattati in modo non conforme può presentare reclamo a un'autorità di controllo per la protezione dei dati. In Italia si tratta del Garante per la protezione dei dati personali."
    },
    {
      "h": "Cookie e tracciamento",
      "p": "Il nostro sito web non utilizza cookie per analisi, pubblicità o tracciamento e non imposta cookie non essenziali. Poiché non sono presenti cookie non essenziali, non è richiesto alcun banner di consenso ai cookie. Non creiamo profili dei visitatori o dei passeggeri e non effettuiamo alcun processo decisionale automatizzato che produca effetti giuridici o similmente significativi."
    },
    {
      "h": "Sicurezza",
      "p": "Adottiamo misure tecniche e organizzative ragionevoli per proteggere i dati personali da perdita, uso improprio e accesso non autorizzato. Mantenere per progettazione i preferiti dei passeggeri sul dispositivo significa che non esiste alcun archivio centrale di tali dati esposto a rischi."
    },
    {
      "h": "Minori",
      "p": "Il nostro servizio non è rivolto ai minori e non raccogliamo consapevolmente dati personali dei minori. Qualora si ritenga che un minore ci abbia fornito dei dati, è possibile contattarci e provvederemo a cancellarli."
    },
    {
      "h": "Distribuzioni per singolo operatore",
      "p": "Le app che realizziamo sono personalizzate per i singoli operatori di trasporto, i quali possono presentare ai propri passeggeri ulteriori condizioni o informative sulla privacy. Tali condizioni dell'operatore disciplinano il rapporto dell'operatore con i propri passeggeri e sono distinte dalla presente informativa."
    },
    {
      "h": "Modifiche alla presente informativa",
      "p": "Possiamo aggiornare periodicamente la presente informativa. In tal caso, aggiorneremo la data indicata sopra e le modifiche sostanziali saranno riportate in questa pagina."
    }
  ],
  "contact": "Domande: "
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
</style>
