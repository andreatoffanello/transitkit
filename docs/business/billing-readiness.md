# Billing readiness — TransitKit

> Cosa serve operativamente per **prendere i soldi** dal primo cliente. Documento decisionale, non aspirazionale.
> Ultimo aggiornamento: 2026-05-25

## Stato attuale (lo stallo)

- **Nessun account merchant aperto** (Paddle/LemonSqueezy/Stripe)
- **Nessuna entità legale**: Andrea opera come persona fisica, no P.IVA, no Ditta Individuale, no Inc.
- **Nessun template contrattuale firmabile** (MSA, Order Form, Data Processing Agreement)
- **Nessun fattura di test mai emessa**
- **Pipeline commerciale potenziale**: 21 Tier 1A, di cui 3 con finestre urgenti (giugno-luglio 2026)

Conseguenza: anche se ATAP firmasse domani, non potremmo emettere fattura legale né incassare in modo conforme.

---

## Il vincolo nascosto: chi è il cliente?

La scelta dell'infrastruttura billing dipende dal mix di clienti dei **primi 2-3 deal**, non dal modello a regime.

| Cliente tipo | Pagamento accettabile | Vincolo legale lato nostro |
|--------------|----------------------|---------------------------|
| Operatore USA pubblico (es. AppalCART, BRTA, Pullman) | Wire transfer / ACH a banca italiana, oppure Paddle invoice | W-9 form opzionale per importi <$600/anno; per importi maggiori serve W-8BEN (foreign individual) |
| Operatore USA privato | Carta/wire/Paddle subscription | Stesso W-8BEN |
| Operatore IT privato (es. Riviera Trasporti) | Bonifico bancario + fattura | Serve P.IVA (forfettario o ordinario) per emettere fattura B2B regolare |
| PA italiana (es. ATAP, gara AMP) | Bonifico solo a P.IVA con fattura elettronica via SdI | **Obbligatoria** P.IVA + iscrizione MePA/Sintel per gare |
| Operatore EU/UK (futuro) | SEPA/Paddle | P.IVA se EU intra-community, Paddle MoR risolve VAT |

**Conclusione operativa**: se il primo deal è una PA italiana (ATAP — più probabile cronologicamente per via della scadenza 17 giu) servono **subito** P.IVA + fattura elettronica. Se il primo deal è USA, prestazione occasionale o Paddle bastano.

---

## Tre percorsi possibili (ordinati per ROI)

### Percorso A — Minimum viable (per primo deal USA isolato)
**Quando ha senso:** primo deal è AppalCART/BRTA/Pullman, importo annuo <€5,000, urgenza alta.

**Setup (2-3 giorni):**
1. Andrea emette **prestazione occasionale** (notula): documento privato senza P.IVA, con ritenuta d'acconto 20% se cliente è sostituto d'imposta. Per clienti USA non lo sono → fattura semplice senza ritenuta + dichiarazione redditi diversi a fine anno.
2. Cliente paga via **wire transfer** (SWIFT) a banca italiana di Andrea.
3. Limite: prestazione occasionale ≤ €5,000/anno per singolo committente *e* ≤ €5,000/anno totali senza obbligo gestione separata INPS. Sopra €5,000 totali scatta INPS gestione separata.

**Pro:** zero setup. **Contro:** non scala oltre 1-2 deal, problema con cliente periodico (subscription).

### Percorso B — Paddle MoR (per scalare 3-10 deal)
**Quando ha senso:** dopo il primo deal USA (validazione product-market fit), e/o se si vogliono evitare incombenze tax USA.

**Setup (1-2 settimane):**
1. **Aprire Ditta Individuale forfettario** (Italia) — ~€200 commercialista, INPS gestione separata, regime forfettario 5% (start-up).
2. **Iscriversi a Paddle** (paddle.com/signup) — richiede:
   - Business registration (Italian VAT number / P.IVA)
   - Bank account intestato all'attività
   - ID owner (carta identità)
   - Sito web pubblico con TOS/Privacy/Refund policy
   - Approvazione 3-10 giorni lavorativi
3. **Creare subscription product**:
   - Nome: "TransitKit White-Label Subscription"
   - Pricing: **$299/mese** (no setup fee, billing mensile)
   - Variants: monthly $299, annual $2990 (2 mesi gratis)
   - Trial: 30 giorni gratuiti
   - Currency: USD primario, EUR/GBP secondari (Paddle auto-converts)
4. **Integrare checkout** nel sito (transitkit.app): un singolo `<script src="https://cdn.paddle.com/paddle/paddle.js">` + bottone "Subscribe" che apre Paddle Overlay.
5. **Webhook** per attivare provisioning automatico: endpoint `/api/paddle-webhook` su Vercel Functions verifica firma + crea record operatore + setup CDN dataset.

**Pro:**
- Gestisce US sales tax stato-per-stato (nexus issue risolto)
- Gestisce EU VAT MOSS automaticamente
- Gestisce chargebacks, refund, dunning
- Riceviamo bonifici netti dalla Paddle Italia (no SWIFT, no FX manuale)
- Fee: 5% + $0.50/transazione

**Contro:**
- Fee non trascurabile a regime (a 10 clienti × $299 = $2,990/mese, Paddle prende ~$150/mese)
- Setup richiede attesa approvazione
- PA italiane **non possono** pagare via Paddle (serve fattura elettronica SdI, non gestita da Paddle)

### Percorso C — Setup completo IT-first (per gare PA italiane)
**Quando ha senso:** primo deal è una PA italiana (ATAP via gara AMP — scadenza 17 giu).

**Setup (2-4 settimane):**
1. **Aprire Ditta Individuale forfettario** + P.IVA (immediato dopo apertura)
2. **Attivare PEC + fatturazione elettronica SdI** (Aruba/Register, ~€30/anno)
3. **Iscrizione MePA** (Acquistinretepa) per gare PA — obbligatoria se si partecipa a procedure pubbliche
4. **Iscrizione AVCPass / ANAC** per CIG/CUP
5. **DURC + visura camerale** per ogni gara
6. **Possibilmente iscrizione SOA** se gare specifiche lo richiedono (raro per SaaS)

Parallelamente: Paddle per clienti privati internazionali.

**Pro:** copre tutti i tipi di cliente. **Contro:** 2-4 settimane di setup amministrativo, ~€500-1000 di costi iniziali + commercialista ricorrente.

---

## Raccomandazione operativa

**Doppio binario, ordine consigliato:**

1. **Settimana 1 (entro 1 giu)**: aprire Ditta Individuale forfettario + PEC + fatturazione SdI. Costo €200-400, blocca contemporaneamente Percorso A (fattura PA italiana possibile) e abilita Percorso B (P.IVA per Paddle).
2. **Settimana 1-2 (entro 10 giu)**: aprire account Paddle, creare subscription product, integrare checkout su transitkit.app, webhook minimo.
3. **Settimana 2 (10-17 giu)**: outreach ATAP con offerta tecnica per gara AMP, già armati di P.IVA + capacità fatturazione elettronica.
4. **Settimana 3+**: outreach BRTA (Paddle subscription per fluidità) + Mendocino + Pullman.

**Non aspettare** il primo "sì" per aprire P.IVA — il commercialista impiega 5-10 giorni e bloccarsi a quello stadio brucia le finestre urgenti.

---

## Checklist immediata Andrea

- [ ] Contattare commercialista per Ditta Individuale forfettario (Aprile/Maggio = momento ottimo, non blocca dichiarazione 2026)
- [ ] Aprire conto corrente business separato (Fineco, Hype Business, o IBAN dedicato banca corrente)
- [ ] Acquistare PEC + fatturazione elettronica (Aruba: ~€25 PEC + ~€30 fatturazione/anno)
- [ ] Decidere nome ditta: "Andrea Toffanello" semplice, o "TransitKit di Andrea Toffanello" (consigliato per brand)
- [ ] Sottoscrivere Paddle (paddle.com/signup) — richiederà documenti del punto 1
- [ ] Iscriversi MePA (acquistinretepa.it) se si vuole partecipare a gare PA italiane oltre ad ATAP
- [ ] Pubblicare TOS + Privacy + Refund policy su transitkit.app (richiesto da Paddle)

## Stima costi setup totale

| Voce | Costo una tantum | Costo annuale |
|------|------------------|---------------|
| Ditta Individuale + P.IVA forfettario | €200 (commercialista) | €0 fisso + 5% imposta sostitutiva |
| INPS gestione separata | — | ~€4,000/anno fisso (min) |
| PEC | — | €25 |
| Fatturazione elettronica SdI | — | €30 |
| Conto business | €0-50 | €0-120 |
| Paddle subscription product | €0 | 5% + $0.50/tx sul fatturato |
| MePA iscrizione | €0 | €0 |
| **Totale anno 1** | **~€250** | **~€4,200 fisso + 5% utile** |

Break-even: 2 clienti × $299/mese × 12 = $7,176/anno ≈ €6,600 → copre i costi fissi con margine.
