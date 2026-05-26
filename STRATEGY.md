# TransitKit — Product & Business Strategy

> Documento vivo. Aggiornare quando cambiano decisioni architetturali, target, o modello di business.
> Ultimo aggiornamento: 2026-05-25 (v5 — deep verification pipeline: USA +3 net-new, Citilink declassato, STP Brindisi squalificato (era in Cotrap), ATAP urgente per gara AMP, BRTA urgente per Keolis takeover, AppalRider live su store ma nessun outreach)

---

## Cos'è TransitKit

White-label transit SaaS: codebase unico, config per operatore → app iOS + Android brandizzata + web stop pages con QR.

**Target:** piccoli operatori TPL con GTFS già pubblicato ma senza app propria.  
**Prezzo:** $299/mese, nessun setup fee, nessun tier.

---

## Prodotto — 3 componenti

| Componente | Stack | Stato |
|-----------|-------|-------|
| iOS app | SwiftUI | ✅ Live App Store — **AppalRider** (operatore AppalCART) |
| Android app | Jetpack Compose | ✅ Live Play Store — AppalRider |
| Web stop pages | Nuxt su Vercel | ✅ Live — privacy + QR landing |
| Pipeline GTFS→JSON | Python | ✅ Funzionante |
| Realtime proxy | Cloudflare/Hetzner | ✅ Live `rt.transitkit.app` |
| Push notifications | FCM + APNs | ✅ Wired (CMS separato) |

### iOS — 4 tab
1. **Orari** — linee → fermate → orari con countdown
2. **Mappa** — fermate, shapes linee, posizione utente
3. **Info** — tariffe, punti vendita, contatti
4. **Settings** — preferiti, notifiche, lingua

**Missing ancora:** Home tab (prossime partenze GPS), push notifications reali, CDN hosting.

### Web stop pages
- URL: `fermate.operatore.com/stop/[stopId]`
- QR code fisico alla fermata → mobile browser → tabellone partenze branded
- **Single Vercel project** per tutti gli operatori — host-based routing
- Ogni operatore aggiunge CNAME: `fermate.operatore.com` → deployment Vercel
- Middleware Nuxt legge `Host` header → carica config operatore giusto
- SSG + ISR (revalidate ogni ora), mobile-first

---

## Value proposition

In ordine di forza reale:

1. **Web stop pages + QR** — funziona senza download, immediato per turisti e occasionali
2. **App branded** — presenza su store con nome/colori operatore
3. **Push notifications** — service alerts diretti (non disponibili gratis su Transit App/Google Maps per piccoli)
4. **Zero gestione tecnica** — tutto incluso nel $299

### Cosa NON vendiamo
- **GTFS creation** — è commodity (esistono tool gratuiti), non scala, non è un moat. Se l'operatore non ha GTFS, non è il target adesso.
- GTFS hosting/manutenzione — inclusa ma non come argomento di vendita principale.

### Perché non basta essere su Transit App / Google Maps
- Transit App: notifiche branded non disponibili per piccoli operatori
- Google Maps: zero controllo sul brand, nessuna relazione diretta col rider
- Per molti target (STP Brindisi, AppalCART, ecc.): semplicemente non ci sono — il confronto è "app branded vs niente", non "app branded vs Transit App"

---

## Target market

### Criteri must-have
- GTFS pubblicato pubblicamente
- Nessuna app trip-planning propria (ticketing-only come myCicero non conta)
- Bacino interessante: turismo, università, o >500k rider/anno

### Criteri di conversione (alzano priorità)
- Già paga per tool digitali (Transit App, Token Transit, Masabi, ETA Spot) → sa che serve
- Domanda dimostrata (app non ufficiale fatta da studenti, lamentele social)
- Esperienza frammentata (2-3 tool diversi)
- App abbandonata o con recensioni pessime
- Operatore privato o semi-privato (decide in settimane, non mesi)
- 10-30 linee (sweet spot: giustifica $299, no IT interno)

### Red flag
- Ha appena firmato con Transit App / Pluservice / Moovit
- Ha lanciato app propria negli ultimi 12 mesi
- Troppo grande (>100 linee, metro di capitale)
- Ente pubblico puro con procurement rigido

### Pipeline USA — Tier 1A (verificati v5, 2026-05-25, evidenza puntuale per ognuno)
| # | Operatore | Città | Linee | Rider/anno | Urgenza | Segnale chiave |
|---|-----------|-------|-------|------------|---------|----------------|
| 1 | **ATAP** | Biella/Vercelli IT | ~30 | n/d | 🔥 **17 giu 2026** | Gara AMP Lotto 5 pubblicata 21/05 — app branded come argomento offerta tecnica |
| 2 | **BRTA** | Pittsfield MA | 13 | ~800k | 🔥 **1 lug 2026** | Director Lambert (dic 2025) caccia ITS rider app via grant; Keolis takeover 01/07 può bundlare |
| 3 | **Mendocino Transit Authority** | Ukiah/Fort Bragg CA | n/d | n/d | 🔥 2024 SRTDP aperto | RouteShout ancora linkato; SRTDP procurement cycle attivo, GTFS-RT live |
| 4 | **Pullman Transit** | Pullman WA | 10 | 1.4M | alta | Ops supervisor Brad Rader: "PTBusBeacon non funziona"; community dev replacement 5★ (10 ratings = sweet spot per co-opt) |
| 5 | **AppalCART** | Boone NC | 12 | 1.42M | media | App demo **AppalRider live su App Store + Play Store** (vedi sotto). ETA SPOT 1.5★/1000 |
| 6 | **Corvallis Transit** | Corvallis OR | 9-12 | ~2M | media | Connexionz 1.8★/9 confermato; OSU 35k, fare-free dal 2011 |
| 7 | **Mountain Line** | Morgantown WV | ~10 | 650k | media | WVU 30k; app ufficiale ferma a ottobre 2015 (1.9★/55, vendor scomparso) |
| 8 | **Manchester Transit Authority** | Manchester NH | 16 | 354k | media | RouteShout 2.0 ancora raccomandato sul sito ufficiale; SNHU pass |
| 9 | **Lowell Regional Transit Authority** | Lowell MA | 20 | ~800k | media | Fare-free dic 2024 + città universitaria + nessun replacement app visibile → high-impact wedge |
| 10 | **Coast Transit Authority** | Gulfport-Biloxi MS | 8 | n/d | media | RouteShout unica app raccomandata, pagina dedicata aggiornata, area turistica costiera |
| 11 | **El Dorado Transit** | Placerville/Sacramento CA | n/d | n/d | media | RouteShout ancora unica app 2025; commuter Sacramento = utenti business con aspettative alte; ridership +28.5% YoY |
| 12 | **Greeley-Evans Transit** | Greeley CO | n/d | ~600k | bassa | UNC campus, RouteShout attivo, nessuna migrazione annunciata |
| 13 | **Fort Smith Transit** | Fort Smith AR | 6 | 205k | bassa | RouteShout trap + budget cutting 2026 → ricettivi a savings pitch ($299 < vendor enterprise) |

> **Silent vendor lock-in (insight wave 2):** il router pubblico RouteShout (`rsrouter.routematch.com/router/selectAgency.jsf`) lista **52 agenzie USA ancora attive**. Molte non sanno di essere abbandonate perché "il sistema funziona" (bus circolano, GTFS arriva). **Shift di pitch:** non "la vostra app è rotta" ma *"i vostri rider non sanno che esiste un'alternativa — e cominciano a saperlo"*. Più efficace perché non costringe l'operatore ad ammettere fallimento.

> **Declassati a 1B:** **Citilink** (Fort Wayne IN) — Transit App ufficialmente endorsato da Citilink/NAVINEO a novembre 2025; finestra rientro = scoping "Transit Tomorrow" RFP se include canale rider mobile.

> **Squalificati pre-v5:** RFTA (Transit App formalmente endorsato + web BusTracker), TCAT (Navi 4.4★/1020), Advance Transit (raccomanda Transit App), Streamline Bus (Syncromatics gen 2026), Island Explorer (PWA NPS), Annapolis (microtransit shift).

### Pipeline Italia — Tier 1A (verificati v5)
| # | Operatore | Area | Linee | GTFS | Urgenza | Segnale chiave |
|---|-----------|------|-------|------|---------|----------------|
| 1 | **ATAP** | Biella/Vercelli, Piemonte | ~30 | magellanoprogetti.it (102+ versioni, daily) | 🔥 **17 giu 2026** | Gara AMP Lotto 5 — vedi sopra USA tabella (è lo stesso operatore) |
| 2 | **APAM** | Mantova, Lombardia | n/d | nessun GTFS pubblico trovato | media | App 1.6★/2.5★, vendor Convergence1 morto, **biglietteria fisica separata** (no lock-in myCicero) |
| 3 | **Contram Mobilità** | Macerata, Marche | n/d | Transitland (fermo 2016, da rinegoziare) | media | App 2★, Camerino uni, RT broken 90% — freno: SCpA pubblica = decisione lenta |
| 4 | **STPS Sondrio** | Valtellina, Lombardia | 35 | presente | media | App fossile 3★/4 recensioni, vendor morto, area montana/turistica |
| 5 | **ATM Molise** | Campobasso, Molise | 36 | n/d | media | App nuova 2024 ma **solo booking, no trip planning** — gap UX aperto |
| 6 | **Civitavecchia Servizi Pubblici** | Civitavecchia, Lazio | 15 | n/d | media | Porto da 7M passeggeri/anno (crocieristi), app generica non brandizzata, zero Google Maps coverage |
| 7 | **Autoservizi Silvestri** | Livigno, Lombardia | 4 | Transitland | bassa | Operatore turistico, comune 1M+ presenze/anno, zero app — bacino piccolo ma high-margin |
| 8 | **Riviera Trasporti** | Imperia, Liguria | n/d | n/d | bassa | App nuova ottobre 2025 senza rating ancora; turnaround post-concordato — finestra 6-12 mesi, **verificare solvibilità prima** |

> **Condizionato:** SAIS Autolinee Sicilia (30+ interurbane, ticketing Sitrap fossile, no trip planning) — Tier 1A solo se GTFS confermato pubblico, altrimenti 1B.

> **Insight strategico wave 2:** Il mercato IT realmente aggredibile è **~10 operatori totali** (4 wave 1 + 4 wave 2 + 2 condizionati). Dopo i primi 4-6 deal IT serve **espansione EU**. Priorità:
> 1. **UK** — Bus Open Data Mandate ha forzato GTFS pubblico per *tutti* gli operatori dal 2021 → barriera tecnica zero, mercato 1000+ operatori frammentati
> 2. **Portogallo** — Carris/STCP grandi, decine di operatori regionali piccoli
> 3. **Spagna regionale** — ATM Sevilla / Granada / Mallorca / canarie

> **Declassati a 1B:** SAF Duemila + VCO Trasporti S.r.l. (split scoperto in v5: due operatori distinti VCO, entrambi senza app decente ma con asti acquisizione SAF↔Comazzi lug 2025 = priorità più bassa).

> **Squalificati v5:** **STP Brindisi** — è dentro Cotrap dal 2021 (screen v4 sbagliato, l'evidenza era pubblica sul sito Cotrap). App Itineris la copre per biglietteria; vendor ioki/Call Bus STP attivo per on-demand. Trigger rientro: gara ANAC Puglia per "sistema informativo passeggeri STP".

> **Squalificati pre-v5:** STP Lecce (Cotrap/Itineris 60 operatori, agg. 01/04/2026).

### Pattern di scouting emersi (filtri da applicare prima dello screen)

1. **RouteShout 2.0 detector (USA):** se il sito operatore raccomanda ancora RouteShout 2.0 (app ferma aprile 2019, 1.6★) è candidato automatico — funziona come rilevatore passivo di vendor abbandonato. Già applicato a BRTA, Manchester, Fort Smith.
2. **Cotrap check (IT Puglia/Sud):** verificare **sempre** l'elenco soci https://cotrap.it/ prima di mettere un operatore pugliese in pipeline. L'app Itineris copre i ~60 operatori soci → squalifica automatica.
3. **myCicero/Pluservice bundle (IT):** squalifica non per qualità app, ma perché ticketing+orari bundlati nella stessa app → sostituirla rompe anche il flusso d'acquisto. **Aggredibili solo operatori che tengono ticketing separato** (es. APAM con biglietti fisici).
4. **Transit App endorsement check:** controllare https://transitapp.com/agencies + comunicati stampa operatore degli ultimi 18 mesi. Operatori che hanno *endorsato* Transit App (non solo "supportato dall'aggregator") sono fuori scope. Errore v4 su Citilink originato qui.
5. **Procurement window:** gare/RFP e cambi gestore aprono finestre di settimane, non mesi. Monitorare periodicamente: ANAC IT, RFP portal MA/CT/OR/WA US, AMP Piemonte, GUUE. Esempi attivi: ATAP (17 giu), BRTA (1 lug).
6. **Community-built replacement detector:** se un rider ha pubblicato indipendentemente un'app per quell'operatore, è proof of pain incontestabile + permission marketing già fatto. Soglia chiave per leggere il segnale:
   - **<500 download** = vantaggio puro (caso Pullman, 10 ratings) — pain dimostrato, soluzione non scalata
   - **500-5000** = vantaggio con strategia di co-opt obbligatoria
   - **>5000** = svantaggio reale (concorrenza gratis effettiva), valutare se rinunciare al lead

   **Mossa obbligatoria prima dell'outreach operatore:** contattare il dev della community app. Offrire co-credit / compenso una tantum / local champion role. Trasforma rivale in canale di distribuzione + insider. **Mai** approcciare l'operatore prima — rischia drama pubblico ("startup vuole sostituire il mio lavoro") che brucia il brand.
7. **Vendor age >3 anni:** app store listing con ultimo update pre-2023 = vendor effettivamente abbandonato. Pullman (PTBusBeacon), Mountain Line (2015), Manchester (RouteShout 2019), AppalCART (ETA SPOT) hanno tutti questo segnale. Filtro deterministico facile da automatizzare.
8. **New executive director window (12 mesi):** nuove leadership cercano win visibili veloci → ricettività massima a pitch chiavi-in-mano. BRTA (Lambert, dic 2025) è il caso. Cercabile su LinkedIn + press release operatori.
9. **Fare-free college town:** Corvallis + Pullman + (parzialmente) AppalCART sono fare-free. Pattern correlato a: high ridership + rider attachment forte + IT budget basso + decisione veloce. Lista pubblica APTA / Eno disponibile.

### Pipeline Italia — Ferry (separata, dopo i primi clienti TPL)
Alilaguna (Venezia), Travelmar (Costiera Amalfitana), Alilauro (Golfo Napoli).

---

## Acquisizione organica

### Strategia principale: demo app before contract
1. Costruire app per operatore Tier 1A con GTFS già scaricato
2. Pubblicare su App Store + Play Store come app community brandizzata
3. Raccogliere download organici
4. Outreach: *"X persone hanno scaricato l'app del vostro servizio. Volete renderla ufficiale?"*

Il cold outreach diventa inbound con prova di domanda esistente.

**Stato esecuzione (2026-05-25):**
- ✅ **AppalRider** (operatore AppalCART, Boone NC) → **live su App Store + Play Store**.
- ⚠️ **Nessuna azione di distribuzione fatta**: zero seeding studenti App State, zero contenuti SEO/social, nessuna candidatura su subreddit r/AppState, nessun QR alle fermate. Download organici attesi = ~zero senza distribuzione attiva.
- ⚠️ **Nessuna azione commerciale fatta verso AppalCART**: zero outreach all'exec director, zero contatto Office of Sustainability/Parking & Transportation, nessuna scheda commerciale, nessun materiale di pitch. L'app sullo store da sola non è una proposta — è uno store listing.
- ⚠️ **Pricing & billing non implementato**: $299/mese è dichiarato in strategia ma manca tutta l'infrastruttura — Paddle vs LemonSqueezy vs Stripe non scelto, account merchant non aperto, subscription product non creato, contratto/MSA non redatto, fattura test mai emessa. Non possiamo prendere soldi anche se AppalCART firmasse domani.
- **Bloccanti reali in ordine di precedenza**:
  1. Decidere stack billing (Paddle/LemonSqueezy/Stripe) e aprire account merchant
  2. Definire forma legale del primo contratto (prestazione occasionale IT vs ditta US? scope of work? SLA?)
  3. Solo allora il seeding + outreach hanno un endpoint commerciale credibile

**Prossime mosse pianificabili (non ancora fatte):**

*Distribuzione AppalRider:*
- Seeding r/AppState con post onesto "ho costruito una versione community dell'app bus, feedback?" (≤200 parole, screenshot, no link spammoso)
- Volantinaggio QR alle fermate principali campus App State
- Outreach Office of Sustainability + Parking & Transportation App State (co-finanziatore AppalCART al 35.6%)

*Sales verso AppalCART:*
- Pacchetto pitch (one-pager + demo video screen recording 60s + link app store)
- Outreach exec director AppalCART con metriche download dopo 2-4 settimane di seeding
- Proposta commerciale formale: scope, SLA, prezzo, durata, billing

*Infrastruttura billing (PRIMA del primo "yes"):*
- POC Paddle Merchant of Record (preferito: gestisce tasse US sales tax + EU VAT, no apertura entità US)
- vs Stripe + manual tax management (più controllo, più overhead)
- vs LemonSqueezy (MoR alternativo, fee maggiore ma onboarding rapido)
- Setup subscription product $299/mese, trial 30 giorni gratuito, billing annuale opzionale
- Template MSA + Order Form (Cluely / Common Paper)

### Altri canali
- **SEO**: "come mettere transit su Google Maps", "GTFS piccolo operatore"
- **Rider communities**: Reddit (r/Ithaca, r/transit), gruppi Facebook/Telegram locali
- **Associazioni di categoria**: CTAA (USA, operatori rurali), ANAV (Italia, autobus privati)
- **Email personalizzata** con demo live dell'operatore specifico

---

## Architettura dati

```
GTFS feed pubblico operatore
    ↓ pipeline/build.py
schedules.json (~3MB per operatore)
    ↓ CDN
    ├── iOS: ScheduleLoader con SHA256 manifest sync (da DoVe)
    ├── Android: (futuro)
    └── Web: Nuxt SSG/ISR — fetch al build + revalidate ogni ora
```

**CDN**: GitHub Pages (ora, costo zero). Vercel Blob se serve più controllo.

---

## Fasi di sviluppo

**Phase 1 — Demo pubblicabile** ✅ COMPLETATA
- [x] Home tab iOS con prossime partenze GPS
- [x] CDN hosting schedules.json (GitHub Pages → static-cdn migration completata)
- [x] Pubblicare demo app **AppalCART** → AppalRider live su App Store + Play Store
- [x] Android app (Jetpack Compose) — paritaria con iOS
- [x] Web stop pages Nuxt (Nuxt 4, privacy + QR landing)
- [x] Realtime proxy (rt.transitkit.app)
- [x] Push notifications (APNs + FCM nativi, CMS separato `transitkit-console`)

**Phase 2 — Primo cliente** 🟡 IN CORSO (sbloccare distribuzione + outreach urgent windows)
- [ ] **Seeding AppalRider**: r/AppState, QR fermate campus, contatto Office of Sustainability
- [ ] **Outreach urgent (giugno 2026)**: ATAP entro 10 giu, BRTA entro 20 giu
- [ ] **Outreach Tier 1A non-urgent**: Pullman, Corvallis, Mountain Line, Manchester, Fort Smith
- [ ] Convertire primo operatore
- [ ] Operator panel (alerts/news/push) — già parzialmente esistente in transitkit-console

**Phase 3 — Scale**
- [ ] Pipeline GTFS auto-update (cron daily, diff alerting)
- [ ] Onboarding self-service operatore (CDN dataset, custom domain DNS setup)
- [ ] Altri operatori in pipeline

---

## Fiscal & billing strategy

**Modello a regime:**
- **Phase 0-1**: Nessun revenue → nessuna P.IVA necessaria
- **First client**: Paddle/LemonSqueezy (estero) o prestazione occasionale (Italia)
- **2+ clienti** (~7k€/anno): P.IVA forfettaria (5% tasse per 5 anni)
- **PA italiana**: contratti < 5k€/anno = affidamento diretto, no gara pubblica

**Stato 2026-05-25:** nulla di tutto questo è ancora attivato.
- Nessun account Paddle/LemonSqueezy/Stripe aperto
- Nessun subscription product creato
- Nessun template contrattuale (MSA / Order Form / scope of work)
- Nessuna fattura test emessa
- Nessuna scelta tra Paddle MoR vs Stripe + tax manuale vs LemonSqueezy

**Decisione da prendere prima del primo outreach commerciale serio:**

| Stack | Pro | Contro |
|-------|-----|--------|
| **Paddle (MoR)** | Gestisce US sales tax + EU VAT, no apertura entità US, no nexus tracking | Fee 5% + $0.50/tx, dashboard meno raffinata di Stripe |
| **Stripe + tax manuale** | Fee minore, dashboard top, multi-currency nativo | Devo gestire io sales tax USA stato-per-stato (incubo per AppalCART NC), serve commercialista internazionale |
| **LemonSqueezy** | MoR come Paddle, onboarding rapidissimo (giorni) | Fee 5% + $0.50, ora di proprietà Stripe (futuro incerto su pricing) |

Raccomandazione preliminare: **Paddle** per il primo cliente (massima copertura tax/VAT, minimo overhead amministrativo per developer-founder), riconsiderare Stripe solo dopo 5+ clienti o se Paddle dà problemi reali.

---

## Dashboard

### Dashboard interna (per noi)
Monitoring aggregato su tutti gli operatori: stato GTFS, CDN health, app store versions, subscription status, download/active users. Necessaria da Phase 3 in poi (3+ operatori).

### Dashboard operatore (per loro)
**Critica prima del primo cliente** — senza di essa ogni alert passa da noi manualmente (non sostenibile con fuso orario USA).

Due funzioni:
1. **Messaggi fissi in-app** — banner/alert mostrati nell'app (es. "Linea 5 sospesa"). Scrivono su `alerts.json` sul CDN. L'app legge ad ogni avvio.
2. **Push notifications** — messaggio one-shot a tutti gli utenti dell'app dell'operatore.

Auth: un login per operatore (Clerk o JWT semplice).

---

## Push notifications

**Scelta: APNs + FCM nativi, no OneSignal.**

```
Operator invia dal dashboard
    ↓ API
    ├── legge device_tokens per operator_id dal DB
    ├── APNs HTTP/2 → iOS
    └── FCM HTTP v1 → Android
```

- **Costo: zero** — APNs e FCM sono gratuiti illimitati
- **Storage tokens:** tabella `device_tokens(operator_id, token, platform)` su Neon/Supabase
- **Credenziali:** chiave APNs `.p8` + service account FCM per operatore, in env vars o Vercel Blob cifrato
- **Complessità:** ~200 righe di codice backend, scritto una volta

**Perché non OneSignal:** $9/mese per app dopo il free tier. A 10 operatori = $90/mese di costo puro evitabile. Per il caso d'uso ("manda alert a tutti gli utenti dell'app X") non serve niente di più complesso.

---

## Decisioni architetturali prese

| Decisione | Scelta | Motivo |
|-----------|--------|--------|
| Web framework | Nuxt (non Next.js) | Andrea conosce Vue/Nuxt |
| Web deployment | Single Vercel project, host-based routing | Non scalare con progetti separati per operatore |
| GTFS creation | NON nel pacchetto standard | Tool gratuiti esistono, non scala, non è moat |
| CDN dati | GitHub Pages → Vercel Blob | Costo zero per iniziare |
| Ferry pipeline | Separata dal TPL | Clienti diversi, ciclo vendita diverso |
| Prima demo | AppalCART (AppalRider) | ETA SPOT 1.5★/1000 rating = dolore documentato, App State 20k studenti, GTFS+RT confermato, nessun Transit App. App live store ma seeding ancora da fare |
| Push notifications | APNs + FCM nativi, no OneSignal | Zero costo, ~200 righe backend, niente dipendenze esterne |
| Dashboard operatore | Phase 2 (non Phase 3) | Bloccante per clienti USA (fuso orario) |
