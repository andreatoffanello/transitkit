<script setup lang="ts">
const { locale } = useI18n()
const { site } = useAppConfig()

const en = {
  "title": "Refund Policy",
  "updated": "Last updated: July 2026",
  "intro": "TransitKit is a recurring subscription for transit operators, with nothing due upfront and no setup fee. Payments, invoicing and any refunds are handled by our Merchant of Record, Paddle.com (Paddle.com Market Ltd), and your purchase is also subject to Paddle's buyer terms.",
  "sections": [
    {
      "h": "What this policy covers",
      "p": "This policy applies to the TransitKit subscription that operators purchase to run their branded apps and services. It is billed monthly or annually and renews automatically each period until you cancel. You pay for the period you are in; there is no upfront charge or setup fee."
    },
    {
      "h": "30-day money-back guarantee",
      "p": "Your initial subscription purchase comes with a 30-day money-back guarantee: you may request a full refund within 30 days of your first payment, for any reason. After this window, payments for the current period are non-refundable, but you can still cancel to prevent further charges."
    },
    {
      "h": "Cancellation vs. refund",
      "p": "You can cancel at any time. Cancelling stops future renewals and is not the same as a refund: your app and service keep running until the end of the period you have already paid for, and no cancellation fee applies. A refund, where available under this policy, returns money already charged."
    },
    {
      "h": "Monthly and annual plans",
      "p": "The 30-day money-back guarantee applies to the initial purchase of both monthly and annual plans. After the guarantee window, the current period — the current month, or the current year on an annual plan — is non-refundable, and cancelling takes effect at the end of that paid period. No pro-rata or partial-period refunds are given for the unused remainder of a period already paid, on either monthly or annual plans; the subscription simply runs to the end of that period. Renewal charges after the window are not refunded, but cancelling before renewal prevents them."
    },
    {
      "h": "Erroneous, duplicate or undelivered charges",
      "p": "If the service was charged in error, duplicated, or could not be delivered, contact us and we will make it right — including a full refund where appropriate, independently of the 30-day window. Refunds are always returned to your original payment method by Paddle, net of what was charged, with any applicable tax handled by Paddle as Merchant of Record."
    },
    {
      "h": "How to request a refund",
      "p": "Email team@transitkit.app with your account details, the transaction or order reference, and the reason for the request. We aim to respond within a few business days. Because Paddle processed your payment, refunds are issued by Paddle to your original payment method, and requests can also be handled directly through Paddle. Once approved, a refund is typically issued within 5–10 business days, though the exact time for the money to post depends on your bank or card provider."
    },
    {
      "h": "Statutory rights and the Terms of Service",
      "p": "The Service is sold business-to-business to transit operators. Statutory consumer rights, such as the EU 14-day right of withdrawal, apply only where the buyer qualifies as a consumer under applicable law. This policy does not limit any mandatory rights that apply to you under EU or Italian law where they do apply. It should be read together with our Terms of Service, which govern the subscription; this policy is governed by the laws of Italy."
    }
  ],
  "contact": "Questions: "
}

const it = {
  "title": "Politica di rimborso",
  "updated": "Ultimo aggiornamento: luglio 2026",
  "intro": "TransitKit è un abbonamento ricorrente per operatori di trasporto, senza nulla di anticipato e senza costi di attivazione. Pagamenti, fatturazione ed eventuali rimborsi sono gestiti dal nostro Merchant of Record, Paddle.com (Paddle.com Market Ltd); il tuo acquisto è inoltre soggetto ai termini per l'acquirente di Paddle.",
  "sections": [
    {
      "h": "Cosa copre questa policy",
      "p": "Questa policy si applica all'abbonamento TransitKit che gli operatori acquistano per gestire le proprie app e i propri servizi in white-label. È fatturato mensilmente o annualmente e si rinnova automaticamente a ogni periodo finché non disdici. Paghi per il periodo in corso: non ci sono addebiti anticipati né costi di attivazione."
    },
    {
      "h": "Garanzia soddisfatti o rimborsati di 30 giorni",
      "p": "Il tuo primo acquisto dell'abbonamento include una garanzia soddisfatti o rimborsati di 30 giorni: puoi richiedere il rimborso completo entro 30 giorni dal primo pagamento, per qualsiasi motivo. Trascorsa questa finestra, i pagamenti per il periodo in corso non sono rimborsabili, ma puoi comunque disdire per evitare ulteriori addebiti."
    },
    {
      "h": "Disdetta e rimborso",
      "p": "Puoi disdire in qualsiasi momento. La disdetta interrompe i rinnovi futuri e non equivale a un rimborso: la tua app e il servizio restano attivi fino al termine del periodo che hai già pagato, senza penali di disdetta. Il rimborso, dove previsto da questa policy, restituisce invece somme già addebitate."
    },
    {
      "h": "Piani mensili e annuali",
      "p": "La garanzia soddisfatti o rimborsati di 30 giorni si applica all'acquisto iniziale sia dei piani mensili sia di quelli annuali. Dopo la finestra di garanzia, il periodo in corso — il mese corrente, o l'anno corrente nel piano annuale — non è rimborsabile, e la disdetta ha effetto al termine di quel periodo già pagato. Non vengono concessi rimborsi pro rata o per periodi parziali sulla parte non utilizzata di un periodo già pagato, né sui piani mensili né su quelli annuali: l'abbonamento prosegue semplicemente fino al termine di quel periodo. Gli addebiti di rinnovo successivi alla finestra non vengono rimborsati, ma la disdetta prima del rinnovo li impedisce."
    },
    {
      "h": "Addebiti errati, duplicati o per servizi non erogati",
      "p": "Se il servizio è stato addebitato per errore, duplicato o non è stato erogato, contattaci e sistemiamo — rimborso completo incluso dove opportuno, indipendentemente dalla finestra di 30 giorni. I rimborsi vengono sempre restituiti da Paddle sul metodo di pagamento originale, al netto di quanto addebitato, con le eventuali imposte gestite da Paddle in qualità di Merchant of Record."
    },
    {
      "h": "Come richiedere un rimborso",
      "p": "Scrivi a team@transitkit.app indicando i dati del tuo account, il riferimento della transazione o dell'ordine e il motivo della richiesta. Puntiamo a rispondere entro pochi giorni lavorativi. Poiché Paddle ha elaborato il pagamento, i rimborsi vengono emessi da Paddle sul metodo di pagamento originale e le richieste possono essere gestite anche direttamente tramite Paddle. Una volta approvato, il rimborso viene di norma emesso entro 5–10 giorni lavorativi, anche se i tempi effettivi di accredito dipendono dalla tua banca o dall'emittente della carta."
    },
    {
      "h": "Diritti di legge e Condizioni di servizio",
      "p": "Il Servizio è venduto business-to-business a operatori di trasporto. I diritti inderogabili del consumatore, come il diritto di recesso di 14 giorni previsto dalla normativa dell'Unione Europea, si applicano solo quando l'acquirente si qualifica come consumatore ai sensi della legge applicabile. Questa policy non limita alcun diritto inderogabile a te spettante ai sensi della normativa dell'Unione Europea o italiana, laddove tali diritti trovino applicazione. Va letta insieme alle nostre Condizioni di servizio, che disciplinano l'abbonamento; questa policy è regolata dalla legge italiana."
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
