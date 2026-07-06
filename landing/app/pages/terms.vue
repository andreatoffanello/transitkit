<script setup lang="ts">
const { locale } = useI18n()
const { site } = useAppConfig()

const en = {
  "title": "Terms of Service",
  "updated": "Last updated: July 2026",
  "intro": "These Terms of Service govern your use of TransitKit, a white-label platform that builds and hosts branded native iOS and Android public-transit apps for transit operators, together with web stop pages, operator-controlled push notifications, and an operator console. By purchasing, accessing, or using the Service, you agree to these Terms. If you accept on behalf of an organization, you confirm you have authority to bind it.",
  "sections": [
    {
      "h": "Parties and acceptance",
      "p": "\"TransitKit\", \"we\", or \"us\" refers to the provider of the Service; \"you\" or \"Customer\" refers to the transit operator that subscribes. Using or purchasing the Service constitutes acceptance of these Terms. These Terms are between businesses; the offering is business-to-business and the Customer is a transit operator, not an individual consumer."
    },
    {
      "h": "The Service",
      "p": "TransitKit builds and hosts branded native iOS and Android transit apps, web stop pages with QR codes, operator-controlled push notifications, and an operator console at console.transitkit.app. The Service is built on top of the GTFS and GTFS-RT feeds you already publish. We may add, change, or remove features over time to improve the Service."
    },
    {
      "h": "Account and eligibility",
      "p": "You must be a transit operator, provide accurate account and billing information, and keep it current. You are responsible for safeguarding access credentials and for all activity under your account. You must promptly notify us of any unauthorized use."
    },
    {
      "h": "Subscriptions, auto-renewal and billing",
      "p": "The Service is a recurring subscription with nothing due upfront and no setup fee, billed monthly or annually. The Standard plan is $599/mo, or $499/mo billed annually (annual equals two months free); a Custom plan is priced to scope. The subscription auto-renews each period until cancelled."
    },
    {
      "h": "Payments via Paddle (Merchant of Record)",
      "p": "Payments are processed by Paddle.com as our authorized reseller and Merchant of Record. Paddle handles the transaction, invoicing, and applicable sales taxes/VAT, and your purchase is also subject to Paddle's buyer terms. TransitKit is the software provider and licensor; Paddle is the seller of record for the payment."
    },
    {
      "h": "Price changes",
      "p": "We may change subscription prices. We will give you reasonable notice before a change takes effect, and any new price applies from your next renewal period. If you do not accept a price change, you may cancel before it takes effect."
    },
    {
      "h": "Acceptable use and Customer responsibilities",
      "p": "You must provide accurate GTFS and GTFS-RT feeds and brand assets and warrant that you hold all rights to them and to their use in the branded apps. You must use the Service lawfully and must not resell it, reverse-engineer it, scrape it, or abuse its APIs or real-time endpoints. One subscription covers one operator organization."
    },
    {
      "h": "Customer content and license grant",
      "p": "You retain ownership of your feeds, brand assets, and other content you provide. You grant us a non-exclusive license to host, process, and display that content solely to operate and deliver the Service, including publishing your branded apps and web pages. This license ends when the relevant content is removed or the subscription terminates."
    },
    {
      "h": "Intellectual property",
      "p": "TransitKit owns the platform, engine, software, and all related intellectual property and brand. We grant you a non-exclusive, non-transferable right to use the Service during your subscription. You keep ownership of your own data, feeds, and brand; nothing here transfers your IP to us or our IP to you."
    },
    {
      "h": "Third-party platforms and data",
      "p": "The Service depends on third parties including Apple (App Store, push), Google (Google Play, Firebase Cloud Messaging), and the transit data feeds you and others publish. Distribution of the branded apps is subject to Apple's and Google's rules. We do not guarantee the accuracy or continuous availability of upstream transit data or third-party platforms."
    },
    {
      "h": "Availability and maintenance",
      "p": "We work to keep the Service available and perform routine maintenance and updates as part of the subscription. Access may be interrupted for maintenance, updates, or factors outside our control. The Service is provided without a specific availability commitment."
    },
    {
      "h": "Suspension",
      "p": "We may suspend all or part of the Service if you fail to pay, materially breach these Terms, or if your use threatens the security, integrity, or lawful operation of the Service or third-party platforms. Where practicable we will give notice and an opportunity to resolve the issue."
    },
    {
      "h": "Term, cancellation and termination",
      "p": "The subscription continues for each paid period and auto-renews until cancelled. You may cancel at any time to stop future renewals through your account or Paddle; the Service keeps running until the end of the period already paid for, with no cancellation fee. We may terminate for material breach that remains uncured after notice. Refunds are governed by our Refund Policy."
    },
    {
      "h": "Effect of termination",
      "p": "On termination, your right to use the Service ends. We stop publishing and hosting your branded apps and web pages and remove the deployment, and delisting from the App Store and Google Play follows their processes. You remain responsible for charges accrued up to the end of the paid period."
    },
    {
      "h": "Disclaimer of warranties",
      "p": "To the fullest extent permitted by law, the Service is provided \"as is\" and \"as available\" without warranties of any kind, whether express or implied, including fitness for a particular purpose and non-infringement. We do not warrant that the Service or the underlying transit data will be uninterrupted, error-free, or accurate."
    },
    {
      "h": "Limitation of liability",
      "p": "To the extent permitted by law, neither party is liable for indirect, incidental, or consequential damages or lost profits. Our total aggregate liability arising from the Service is capped at the fees you paid in the twelve months before the event giving rise to the claim. Nothing limits liability that cannot be excluded by law, including for gross negligence, fraud, or death or personal injury."
    },
    {
      "h": "Indemnity",
      "p": "You will indemnify and hold TransitKit harmless from third-party claims arising out of your feeds, brand assets, or content, your breach of these Terms, or your unlawful use of the Service, including claims that your content or its use infringes the rights of others."
    },
    {
      "h": "Changes to these Terms",
      "p": "We may update these Terms from time to time. We will post the updated version with a new \"Last updated\" date and, for material changes, give reasonable notice. Continued use of the Service after changes take effect constitutes acceptance."
    },
    {
      "h": "Governing law and disputes",
      "p": "These Terms are governed by the laws of Italy. Disputes arising from these Terms or the Service are subject to the jurisdiction of the courts of Italy. This does not affect the separate buyer terms that Paddle applies to the payment transaction."
    }
  ],
  "contact": "Questions: "
}

const it = {
  "title": "Termini di servizio",
  "updated": "Ultimo aggiornamento: luglio 2026",
  "intro": "I presenti Termini di servizio disciplinano l'utilizzo di TransitKit, una piattaforma white-label che realizza e ospita app native iOS e Android brandizzate per il trasporto pubblico destinate agli operatori del trasporto, unitamente a pagine web delle fermate, notifiche push gestite dall'operatore e una console per l'operatore. Acquistando, accedendo o utilizzando il Servizio, l'utente accetta i presenti Termini. Chi accetta per conto di un'organizzazione dichiara di avere il potere di vincolarla.",
  "sections": [
    {
      "h": "Parti e accettazione",
      "p": "\"TransitKit\", \"noi\" o \"nostro\" indica il fornitore del Servizio; \"tu\" o \"Cliente\" indica l'operatore del trasporto che sottoscrive l'abbonamento. L'utilizzo o l'acquisto del Servizio costituisce accettazione dei presenti Termini. I presenti Termini intercorrono tra imprese; l'offerta è business-to-business e il Cliente è un operatore del trasporto, non un consumatore individuale."
    },
    {
      "h": "Il Servizio",
      "p": "TransitKit realizza e ospita app native iOS e Android brandizzate per il trasporto, pagine web delle fermate con codici QR, notifiche push gestite dall'operatore e una console per l'operatore all'indirizzo console.transitkit.app. Il Servizio si basa sui feed GTFS e GTFS-RT che l'operatore già pubblica. Potremo aggiungere, modificare o rimuovere funzionalità nel tempo per migliorare il Servizio."
    },
    {
      "h": "Account e idoneità",
      "p": "Il Cliente deve essere un operatore del trasporto, fornire informazioni di account e fatturazione accurate e mantenerle aggiornate. Il Cliente è responsabile della custodia delle credenziali di accesso e di tutte le attività svolte tramite il proprio account. Il Cliente deve informarci tempestivamente di qualsiasi utilizzo non autorizzato."
    },
    {
      "h": "Abbonamenti, rinnovo automatico e fatturazione",
      "p": "Il Servizio è un abbonamento ricorrente, senza alcun importo anticipato né costi di attivazione, fatturato mensilmente o annualmente. Il piano Standard è di 599 $/mese, oppure 499 $/mese con fatturazione annuale (l'annuale equivale a due mesi gratuiti); il piano Custom è quotato in base al perimetro. L'abbonamento si rinnova automaticamente a ogni periodo fino alla disdetta."
    },
    {
      "h": "Pagamenti tramite Paddle (Merchant of Record)",
      "p": "I pagamenti sono elaborati da Paddle.com in qualità di nostro rivenditore autorizzato e Merchant of Record. Paddle gestisce la transazione, la fatturazione e le imposte sulle vendite/IVA applicabili, e l'acquisto è altresì soggetto ai termini per l'acquirente di Paddle. TransitKit è il fornitore e licenziante del software; Paddle è il Merchant of Record (venditore ufficiale) per il pagamento."
    },
    {
      "h": "Modifiche dei prezzi",
      "p": "Potremo modificare i prezzi dell'abbonamento. Forniremo un ragionevole preavviso prima che la modifica abbia effetto e ogni nuovo prezzo si applicherà a partire dal periodo di rinnovo successivo. Se il Cliente non accetta una modifica di prezzo, può disdire prima che essa abbia effetto."
    },
    {
      "h": "Uso consentito e responsabilità del Cliente",
      "p": "Il Cliente deve fornire feed GTFS e GTFS-RT e asset di brand accurati e garantisce di detenere tutti i diritti su di essi e sul loro utilizzo nelle app brandizzate. Il Cliente deve utilizzare il Servizio in modo lecito e non può rivenderlo, sottoporlo a reverse engineering, effettuarne scraping né abusare delle sue API o dei suoi endpoint in tempo reale. Un abbonamento copre una sola organizzazione operatore."
    },
    {
      "h": "Contenuti del Cliente e concessione di licenza",
      "p": "Il Cliente conserva la titolarità dei propri feed, asset di brand e altri contenuti forniti. Il Cliente ci concede una licenza non esclusiva per ospitare, elaborare e visualizzare tali contenuti al solo fine di gestire ed erogare il Servizio, inclusa la pubblicazione delle app brandizzate e delle pagine web. Tale licenza cessa al momento della rimozione dei contenuti pertinenti o della cessazione dell'abbonamento."
    },
    {
      "h": "Proprietà intellettuale",
      "p": "TransitKit è titolare della piattaforma, del motore, del software e di tutta la proprietà intellettuale e del brand correlati. Concediamo al Cliente un diritto d'uso del Servizio non esclusivo e non trasferibile per la durata dell'abbonamento. Il Cliente conserva la titolarità dei propri dati, feed e brand; nulla nei presenti Termini trasferisce la proprietà intellettuale del Cliente a noi né la nostra al Cliente."
    },
    {
      "h": "Piattaforme e dati di terzi",
      "p": "Il Servizio dipende da terzi, tra cui Apple (App Store, push), Google (Google Play, Firebase Cloud Messaging) e i feed di dati di trasporto pubblicati dal Cliente e da altri. La distribuzione delle app brandizzate è soggetta alle regole di Apple e Google. Non garantiamo l'accuratezza né la disponibilità continua dei dati di trasporto a monte o delle piattaforme di terzi."
    },
    {
      "h": "Disponibilità e manutenzione",
      "p": "Ci adoperiamo per mantenere il Servizio disponibile ed eseguiamo manutenzione ordinaria e aggiornamenti nell'ambito dell'abbonamento. L'accesso può essere interrotto per manutenzione, aggiornamenti o fattori al di fuori del nostro controllo. Il Servizio è fornito senza uno specifico impegno di disponibilità."
    },
    {
      "h": "Sospensione",
      "p": "Possiamo sospendere in tutto o in parte il Servizio in caso di mancato pagamento, di violazione sostanziale dei presenti Termini o qualora l'utilizzo minacci la sicurezza, l'integrità o il regolare funzionamento del Servizio o delle piattaforme di terzi. Ove praticabile, forniremo un preavviso e la possibilità di rimediare."
    },
    {
      "h": "Durata, disdetta e risoluzione",
      "p": "L'abbonamento prosegue per ciascun periodo pagato e si rinnova automaticamente fino alla disdetta. Il Cliente può disdire in qualsiasi momento per interrompere i rinnovi futuri tramite il proprio account o Paddle; il Servizio resta attivo fino al termine del periodo già pagato, senza penali di disdetta. Possiamo risolvere il contratto in caso di violazione sostanziale non sanata dopo diffida. I rimborsi sono disciplinati dalla nostra Politica di rimborso."
    },
    {
      "h": "Effetti della risoluzione",
      "p": "Alla risoluzione, il diritto del Cliente di utilizzare il Servizio cessa. Interrompiamo la pubblicazione e l'hosting delle app brandizzate e delle pagine web e rimuoviamo il deployment; la rimozione da App Store e Google Play segue le relative procedure. Il Cliente resta responsabile degli importi maturati fino al termine del periodo pagato."
    },
    {
      "h": "Esclusione di garanzie",
      "p": "Nella misura massima consentita dalla legge, il Servizio è fornito «così com'è» e «ove disponibile», senza garanzie di alcun tipo, espresse o implicite, comprese l'idoneità a uno scopo particolare e l'assenza di violazioni. Non garantiamo che il Servizio o i dati di trasporto sottostanti siano ininterrotti, privi di errori o accurati."
    },
    {
      "h": "Limitazione di responsabilità",
      "p": "Nella misura consentita dalla legge, nessuna delle parti risponde di danni indiretti, incidentali o consequenziali né del lucro cessante. La nostra responsabilità complessiva derivante dal Servizio è limitata ai corrispettivi pagati dal Cliente nei dodici mesi precedenti l'evento che ha originato la pretesa. Nulla limita la responsabilità che non può essere esclusa per legge, inclusa quella per colpa grave, dolo o morte o lesioni personali."
    },
    {
      "h": "Manleva",
      "p": "Il Cliente terrà indenne e manlevato TransitKit da pretese di terzi derivanti dai propri feed, asset di brand o contenuti, dalla violazione dei presenti Termini o dall'uso illecito del Servizio, comprese le pretese secondo cui i contenuti del Cliente o il loro utilizzo violino diritti altrui."
    },
    {
      "h": "Modifiche ai presenti Termini",
      "p": "Potremo aggiornare periodicamente i presenti Termini. Pubblicheremo la versione aggiornata con una nuova data di \"Ultimo aggiornamento\" e, per modifiche sostanziali, forniremo un ragionevole preavviso. L'uso continuato del Servizio dopo l'entrata in vigore delle modifiche costituisce accettazione."
    },
    {
      "h": "Legge applicabile e controversie",
      "p": "I presenti Termini sono regolati dalla legge italiana. Le controversie derivanti dai presenti Termini o dal Servizio sono soggette alla giurisdizione dei tribunali italiani. Ciò non pregiudica i distinti termini per l'acquirente applicati da Paddle alla transazione di pagamento."
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
