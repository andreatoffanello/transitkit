# TransitKit — Analisi pricing & packaging (decision memo)

> Autore: analisi pricing/packaging B2B verticale · Data: 2026-06-15
> Scopo: validare o smentire i placeholder **$499 / $799** rispetto al tier storico **$299**, e
> raccomandare numeri difendibili. Non aspirazionale: i contro prima dei pro.
> Fonti interne: [`one-pager.md`](outreach/one-pager.md), [`billing-readiness.md`](billing-readiness.md),
> [`STRATEGY.md`](../../STRATEGY.md), [`MARKET_RESEARCH.md`](../../MARKET_RESEARCH.md).
> Fonti esterne citate inline con URL. Ogni numero non pubblicato è marcato **[STIMA]** o **[ASSUNZIONE]**.

---

> ⚠️ **AGGIORNAMENTO post-controvalutazione (§8).** Ricerca empirica su funding e procurement (FTA grant
> 80/20, soglia micro-purchase $15k, D.Lgs 36/2023) **ribalta in parte** la raccomandazione US qui sotto:
> gli operatori US comprano con grant e sono **price-inelastici** sotto soglia → $399 unico era troppo
> conservativo, importava il vincolo IT sugli US. Posizione rivista: **disaccoppiare US (~$499) e IT
> (≤€5k/anno)**. Leggere §8 prima di decidere.

## TL;DR — la raccomandazione in una riga (versione 1, pre-controvalutazione)

**Tier unico core a `$399/mo` (≈ €390/mo per la PA IT), non $499. Niente tier "fare" flat a $799: il modulo
pagamenti va o come add-on di integrazione (+$99–149/mo, deep-link a Token Transit/Masabi) o, se un giorno
gestito in proprio, in revenue-share — mai flat.** Il "secondo tier" più difendibile oggi non è la fare,
è un piano **Network/multi-operatore** per consorzi.

Motivo sintetico: i $499 "per pareggiare l'incumbent" si appoggiano a un'ancora che **non esiste nei dati**
(nessun incumbent ha un prezzo pubblico noto a ~$499 per il solo layer rider), e a un **segmento che la tua
stessa metodologia di scouting squalifica** (chi paga oggi un vendor vivo = red flag "ha appena firmato").
La pipeline reale è "app branded vs niente": lì ogni dollaro è una nuova voce di budget e $499 alza l'attrito
senza una controparte di valore percepito. $399 cattura più valore di $299 senza rompere nulla.

---

## 1. Benchmark competitivo (reale, con fonti)

### 1.1 Il fatto scomodo: **quasi nessuno pubblica i prezzi**

Tutti i vendor rilevanti (RouteShout/GMV Sync, Passio, TransLoc/Modaxo, Avail, Ride Systems, Moovit,
myCicero/Pluservice) sono **quote-only**. Non esiste un listino pubblico contro cui dire "siamo più
economici". L'unico dato hard ottenuto da documento di gara è:

| Vendor | Cosa fa | Prezzo | Fonte | Stato |
|--------|---------|--------|-------|-------|
| **Swiftly** | Solo analytics/feed dati (NON app rider) | **$1.200/veicolo/anno** → 11 bus = $13.200/anno (~$1.100/mo) | [banningca.gov, gara Banning Pass Transit](https://banningca.gov/DocumentCenter/View/7351/Att-1-Trillium-Swiftly-Proposal) | **PUBBLICATO** (doc gov) |
| **Transit App** | App consumer (non white-label) | Deal no-cost con LA Metro, che risparmia ~$240k/anno di manutenzione app custom | [Smart Cities Dive](https://www.smartcitiesdive.com/news/Transit-app-LA-Metro-partnership/578250/) | PUBBLICATO |
| **Transit App Royale** | Tier consumer | $24,99/anno o $4,99/mo (al rider, non all'agenzia) | [help.transitapp.com](https://help.transitapp.com/article/362-royale-faq) | PUBBLICATO |
| **Masabi (Justride)** | Solo fare collection | Modello **revenue-share + subscription** (% non pubblicata) | [masabi.com](https://www.masabi.com/2023/01/12/how-to-reduce-the-cost-of-fare-collection-and-increase-revenues/) | PUBBLICATO (solo modello) |
| **myCicero/MooneyGo** (Pluservice) | Info + ticketing IT | B2B operatore quote-only; fee al rider €0,08/transazione (min €0,20) | [mycicero.it/costi](https://www.mycicero.it/costi/) | PUBBLICATO (solo fee consumer) |
| **Token Transit** | Solo fare collection | Fee all'agenzia non pubblicata (no surcharge al rider) | [tokentransit.com](https://tokentransit.com/) | NON pubblicato |

> ⚠️ Consolidamento da sapere per il pitch: **DoubleMap, Ride Systems, RouteShout 2.0, Syncromatics, TransLoc
> sono tutti finiti dentro Modaxo** (TransLoc venduta da Ford a Modaxo nel 2022; RouteShout → GMV Sync). Il
> "vendor che è stato venduto due volte" del one-pager è letteralmente vero e documentabile
> ([GlobeNewswire 2022](https://www.globenewswire.com/news-release/2022/03/01/2394748/0/en/Modaxo-Welcomes-TransLoc.html)).

### 1.2 Cosa significa per l'ancora di prezzo

Il prezzo del layer rider per un piccolo operatore, nella realtà, è **bimodale**:

- **Modo A — $0**: il vendor è morto o l'app è bundlata in un contratto ITS vecchio. È il caso del 90%
  della tua pipeline (RouteShout abbandonato, ETA SPOT 1.5★, PTBusBeacon rotto). Qui non c'è budget da
  spostare: ogni prezzo è una **nuova voce**.
- **Modo B — sepolto in un contratto ITS da $30–120k/anno** [STIMA, nessuna fonte pubblica isola la quota
  "app rider"]. Qui il layer rider non si compra separatamente; è incluso in CAD/AVL + hardware.

**Conseguenza diretta: i "$499 dell'incumbent" sono un placeholder non verificabile.** Non c'è evidenza che
un incumbent vivo costi ~$499/mo per il solo rider-facing. O costa $0 (abbandonato) o costa molto di più
(bundle ITS). Quindi a $499 **non stai "pareggiando l'incumbent"** — stai semplicemente alzando il tuo
prezzo del 67% su un segmento che paga zero. Il claim "stessa qualità del big vendor ma più economico"
**non regge perché manca il termine di paragone pubblico**.

Costo di un'app custom one-off (l'ancora vera): **$75–150k upfront + $20–50k/anno manutenzione** per una
piccola agenzia [STIMA, estrapolata da dev-firm ranges + il dato LA Metro $240k/anno]. Questo è il numero
contro cui ancorare: TransitKit a $399/mo = **$4.788/anno**, cioè **~3% del costo di costruirsela**.

---

## 2. Value metric & anchoring

### 2.1 Metrica di valore: **flat per-operatore (deployment)** — confermato

| Metrica | Verdetto | Perché |
|---------|----------|--------|
| **Flat per-operatore** | ✅ **Tienila** | Il costo marginale TransitKit per operatore è ~flat (un config, un dataset CDN, una submission). Zero IT interno lato operatore da modellare. Coerente con "zero upfront, stesso prodotto per tutti". Prevedibile per un budget pubblico. |
| Per-veicolo (modello Swiftly) | ❌ | Punisce la crescita, aggiunge attrito di conteggio, e ti mette in competizione diretta su una metrica dove l'incumbent ti batte sul percepito ($1.200/bus suona "tassa"). |
| Per-rider | ❌ | Incompatibile con la tua privacy stance (favoriti locali, nessun tracking rider). Non puoi né vuoi misurarli. |
| % sul transato | ⚠️ Solo per la fare (§3) | Ha senso *esclusivamente* per i pagamenti, dove il valore scala col GMV. |

### 2.2 Contro cosa ancorare (in ordine di forza)

1. **Costo di un build custom** ($75–150k + manutenzione) → TransitKit è ~3% del TCO triennale. Ancora fortissima, vera, citabile.
2. **Ownership + canale diretto**: push, brand, relazione col rider che Transit App/Google Maps non danno ai piccoli ([STRATEGY §value-prop](../../STRATEGY.md)). Ancora qualitativa ma reale.
3. ~~Costo annuo incumbent~~ → **da NON usare**: non hai il numero. Se lo usi, il prospect chiede "più economico di cosa?" e non hai risposta.

**Riposizionamento del messaggio**: da *"stessa qualità del big vendor ma più economico"* (indifendibile)
a **"il tuo canale, non quello di un aggregatore — a una frazione di quanto costerebbe costruirtelo, zero
upfront, live in 3 settimane"**. Questo è vero a $299, a $399 e anche a $499.

---

## 3. Tier design

### 3.1 Quanti tier: **1 core + 1 add-on**, non 2-3 tier paralleli

Il "same product for everyone" del one-pager è un *asset di vendita* (semplicità, zero negoziazione, sotto
soglia procurement). Spaccarlo in 3 tier enterprise-style contraddice il posizionamento. Ma un singolo
add-on opzionale non rompe la narrazione "un prodotto".

### 3.2 Il tier "fare" a $799: **i contro prima dei pro**

Il placeholder $799 = "$299 info + modulo fare" ha tre problemi strutturali:

1. **Flat mis-prezza la fare.** Un operatore con $1M/anno di incassi e uno con $50k non possono pagare lo
   stesso flat: per il primo $799 è regalato, per il secondo è fuori mercato. La fare scala col GMV, non
   con un canone fisso. È esattamente perché Masabi e Token Transit usano **revenue-share**
   ([Masabi](https://www.masabi.com/justride/)).
2. **Se gestisci la fare in proprio**, erediti PCI-DSS, settlement, chargeback, e processing fee
   (1,5–3,5% Visa/MC [PUBBLICATO, dato settore pagamenti]). Per un founder solo è un cambio di categoria di
   rischio, non un "modulo".
3. **Se NON la gestisci** (deep-link a Token Transit/Masabi, come già dice il one-pager FAQ), allora $799
   è "ti faccio pagare $500 in più per un deep-link" — debole.

**Raccomandazione fare:**
- **Ora**: NON costruire fare. Offrila come **add-on di integrazione a +$99–149/mo** (deep-link branded ai
  vendor esistenti, surfacing del flusso d'acquisto nell'app). Costo marginale tuo ~nullo, valore reale per
  l'operatore (esperienza unificata).
- **Post-PMF, se gestita in proprio**: **revenue-share** (es. 2–4% del transato in-app, o passthrough
  processor + piccola platform fee), con un floor mensile. Mai flat $799.

### 3.3 Il secondo tier che ha davvero senso: **Network / multi-operatore**

In Italia esistono consorzi e agenzie regionali reali (Cotrap = 60 operatori, agenzie TPL provinciali). Un
piano **Network** — N app/deployment sotto un'unica agenzia con dominio custom, console multi-operatore,
SLA prioritario — è un upsell molto più difendibile della fare: stessa meccanica di prezzo (flat
per-deployment con sconto volume), zero rischio nuovo, e tocca un compratore (agenzia) con budget più alto.
Da tenere come **tier 2 vero**, quando arriva il primo consorzio.

---

## 4. US vs IT — willingness-to-pay e vincoli

### 4.1 Il vincolo che decide il ceiling IT: la soglia procurement

Da [`billing-readiness.md`](billing-readiness.md) e [`STRATEGY.md`](../../STRATEGY.md): la PA italiana paga
**solo via bonifico a P.IVA con fattura elettronica SdI** — **Paddle non può gestirla**. Quindi i deal con
PA IT (es. ATAP, #1 urgente) sono su un binario di fatturazione diverso, in EUR.

Sulla soglia "€5.000/anno = affidamento diretto" citata internamente, **una precisazione onesta**: con il
D.Lgs 36/2023 l'affidamento diretto di servizi è legittimo fino a **€140.000**, non €5.000. La soglia di
~€5.000 NON è un muro legale — è la soglia sotto cui sei **esente dall'obbligo MePA/mercato elettronico** e
da gran parte della documentazione (CIG semplificato, ecc.). Quindi:

- **$499/mo annuale = ~$5.988 ≈ €5.500** → sopra la zona "frictionless", entri in overhead amministrativo
  MePA/CIG per la PA. Non illegale, ma più attrito proprio dove il ciclo è già lento.
- **$399/mo annuale = ~$4.788 ≈ €4.400** → **sotto** la zona frictionless. Affidamento diretto semplice.
- Per la PA IT quota in **EUR** (es. €390/mo / €3.900/anno) per evitare ambiguità FX in un contratto pubblico.

### 4.2 US

| | |
|---|---|
| Soglia bid USA | $399/mo annuale = $4.788 → ben sotto la tipica linea $25k di gara competitiva ([one-pager FAQ](outreach/one-pager.md)). Margine anche a $499. |
| Rail di pagamento | Paddle MoR (gestisce US sales tax stato-per-stato + W-8BEN). Fee 5% + $0,50/tx. |
| WTP | Pipeline = "anchor contro zero". Alta elasticità: il prezzo è una *nuova* voce di budget, non una sostituzione. Qui $399 vs $299 cambia poco la difficoltà del "sì"; $499 inizia a richiedere giustificazione. |

### 4.3 Sintesi US vs IT

- **US**: più operatori (21 Tier 1A), ma quasi tutti modo-A (vendor abbandonato). Paddle fluido. Ceiling pratico alto ($25k bid line), ma il vantaggio impulse-buy si perde sopra ~$500/mo.
- **IT**: mercato sottile (~10 operatori reali), molti pubblici → SdI obbligatorio, ciclo lento, e la zona frictionless ~€5k consiglia di tenere il core **sotto €5k/anno**. Questo è il vero tetto che fissa il numero.

**La convergenza dei vincoli (US bid line + IT zona €5k + ancora-contro-zero) punta tutta a un core
≤ ~€5k/anno → $399/mo è il massimo "frictionless".**

---

## 5. Annual & discount

| Leva | Raccomandazione | Note |
|------|-----------------|------|
| Mensile vs annuale | Spingi **annuale** | Per un founder solo, l'incasso anticipato è ossigeno di cassa e abbatte churn/dunning. Per la PA IT, una fattura/anno è anche più semplice (resta sotto soglia, un solo CIG). |
| Sconto annuale | **"2 mesi gratis" (10×) — tienilo** | 16,7% è all'estremo alto del range SaaS (10–20%) ma giustificato: pull-forward cassa + riduzione overhead amministrativo per deal. A $399 → **$3.990/anno**. |
| Trial | **30 giorni "full" → riframalo come pilot** | Con go-live di 3 settimane il "trial" è di fatto un pilot. Il vero de-risker non è il trial ma **zero upfront + uscita a 60 giorni + source escrow** (già nel one-pager): vendili più forte del trial. |

---

## 6. Sensitivity — dove il business diventa interessante

Assunzioni dai costi reali in [`billing-readiness.md`](billing-readiness.md): **fissi ~€4.200/anno**
(INPS gestione separata ~€4.000 + PEC/SdI ~€55) + Paddle 5% + forfettario 5%. Netto per operatore annuale ≈
prezzo × ~0,90.

| Scenario | Break-even (coprire i fissi) | Operatori per ~€70k netto/anno |
|----------|------------------------------|-------------------------------|
| **$299/mo** ($3.588/anno, netto ~€2.950) | ~**2 operatori** (come da repo) | ~**24** |
| **$399/mo** ($4.788/anno, netto ~€3.950) | ~**1,2 operatori** | ~**18** |
| **$499/mo** ($5.988/anno, netto ~€4.950) | ~**1 operatore** | ~**14** |

**Lettura non ovvia:** il vincolo che morde per primo **non è il prezzo, è la capacità operativa del founder
solo.** Manutenzione "inclusa" + supporto su fuso USA + gestione push/alert per-operatore non scala oltre
~15–20 operatori per una persona [ASSUNZIONE]. Quindi:

- **Sotto-prezzare è attivamente dannoso**: a $299 servono ~24 operatori per l'obiettivo, ma a 24 operatori
  sei già oltre la capacità di supporto sostenibile. A $399 ne bastano ~18 — più vicino al tetto operativo.
- **Floor** (sotto cui non vale la manutenzione + supporto del tuo tempo): **~$250–300/mo**. Sotto, il tempo
  founder per deal non è ripagato.
- **Ceiling "competitivo/frictionless"**: **~$415/mo** (≈ €5k/anno, la zona IT senza MePA). Sopra, in US
  reggi fino a ~$2.000/mo di pura soglia-bid, ma perdi l'impulse-buy e l'ancora-contro-zero si rompe.

---

## 7. Raccomandazione finale

### 7.1 Numeri

| Voce | Raccomandato | vs placeholder | Razionale |
|------|--------------|----------------|-----------|
| **Core "Rider"** (tutto l'attuale prodotto) | **$399/mo** · annuale **$3.990** (2 mesi gratis) · **€390/mo / €3.900/anno per PA IT** | **$499 → ABBASSARE a $399** | Cattura più valore di $299 e toglie il segnale "hobby", ma resta sotto US bid line e sotto la zona IT €5k. $499 rompe la zona IT e si ancora a un incumbent fantasma. |
| **Add-on Fare (integrazione)** | **+$99–149/mo**, deep-link Token Transit/Masabi | **$799 tier → ELIMINARE come tier flat** | Flat mis-prezza la fare e (se in proprio) carica PCI/settlement su un founder solo. |
| **Fare gestita in proprio** (futuro) | **Revenue-share 2–4% del transato + floor** | — | Unico modello sano per la fare. Solo post-PMF. |
| **Tier 2 "Network"** (consorzi/agenzie) | Flat per-deployment con sconto volume + SLA + dominio custom | (nuovo) | Secondo tier difendibile davvero, quando arriva il primo consorzio (es. tipo Cotrap). |
| Setup fee | **$0** (invariato) | — | Asset di posizionamento. |
| Trial | 30 gg, riframato come **pilot**; leva vera = zero upfront + uscita 60gg + escrow | — | — |

**Verdetto sui placeholder:**
- **$499 → ABBASSARE a $399.** (Non confermare.)
- **$799 → NON spedire come tier flat.** Ristrutturare in add-on (+$99–149) o revenue-share.

### 7.2 I 3 rischi principali della proposta

1. **Mismatch prezzo↔segmento (il più serio).** La storia "replace the incumbent a $499/$799" punta a
   operatori che pagano un vendor vivo — ma la tua metodologia di scouting li **squalifica** ("ha appena
   firmato / endorsa Transit App" = red flag). Non puoi al tempo stesso (a) cacciare solo operatori
   abbandonati e (b) prezzare per sostituire la spesa di un incumbent vivo. $399 risolve riconoscendo che il
   segmento reale è anchor-contro-zero.
2. **La fare è una trappola di scope.** Trasformare "+ modulo fare" in un tier flat sembra facile e invece
   apre PCI, settlement, processing fee e supporto pagamenti — categoria di rischio che un founder solo non
   dovrebbe assorbire pre-PMF. Tienila add-on/rev-share.
3. **Il binding constraint è la capacità di supporto, non il prezzo.** A qualunque prezzo, oltre ~15–20
   operatori il modello "tutto incluso, founder solo, fuso USA" si rompe. Il pricing più alto ($399 vs $299)
   è in parte una *difesa operativa*: meno operatori per lo stesso reddito = meno carico di supporto. Da
   monitorare prima ancora del prezzo.

### 7.3 Confidenza

- **Alta** su: metrica flat per-operatore; eliminare il $799 flat; tenere il core sotto €5k/anno per la IT;
  ancorare al build custom e non all'incumbent.
- **Media** su: il numero esatto $399 vs $349. Entrambi stanno sotto le soglie; $399 se credi al segnale di
  qualità, $349 se vuoi massimo margine sotto €5k con headroom. **$299 resta valido come sconto documentato**
  per gli operatori più price-sensitive (rurali/pubblici IT), come deroga — non come listino.
- **Da validare sul campo**: la WTP reale si misura solo dai primi 2–3 "sì". Parti a $399, e se i primi due
  deal chiudono senza frizione di prezzo, è segnale che il floor era troppo basso — non che $399 era giusto.

---

## 8. Controvalutazione (red-team del memo qui sopra)

> Due ricerche empiriche mirate ai due anelli più deboli della v1: (a) *esiste* davvero un competitor
> pubblico nella fascia $300–800? (b) la premessa "anchor-contro-zero = alta elasticità" è vera, o gli
> operatori comprano con grant e sono inelastici? I risultati **smentiscono in parte la v1**.

### 8.1 Finding A — nessun competitor pubblico nella fascia, conferma forte

Ricerca sui *nuovi entranti* SaaS (Peak Transit, Modeshift, TransitFare, Spare, Via, ETA, Connexionz,
Bytemark, ecc.): **tutti quote-only**. Gli unici prezzi pubblici trovati sono adiacenti, non comparabili:

- **AddTransit** $15/route/mo + $15/vehicle/mo (pubblicato, [addtransit.com/pricing](https://addtransit.com/pricing.php)) → 10 route + 10 veicoli = $450/mo. Ma è un tool dati GTFS, **non** un'app brandizzata (i rider usano l'app condivisa AddTransit).
- **Moovs** $149–$999/mo (pubblicato, [moovsapp.com/pricing](https://www.moovsapp.com/pricing)) → ma è per limo/charter/navette private, zero GTFS, zero TPL fisso.
- Capterra mostra EZTransport e Passio Navigator a "$100/feature/mo" — prezzo unitario di partenza, non flat full-solution.

**Implicazione (rafforza la v1 su un punto, la ribalta su un altro):**
- ✅ Conferma: l'ancora "incumbent a $499" **non esiste pubblicamente**. La v1 aveva ragione a dire "non puoi vantarti di essere più economico di un numero che nessuno pubblica".
- ❌ Ma la conclusione che ne traevo (→ prezzo basso) è **non-sequitur**: assenza di prezzo pubblico = assenza di *pressione competitiva visibile*, non obbligo di stare bassi. Il rischio reale non è essere battuti da un competitor visibile, è essere **misurati contro la quote enterprise invisibile** ($1.000–5.000+/mo per ITS comparabile). Contro quell'ancora mentale, **$499 è conservativo, non aggressivo**.

### 8.2 Finding B — il colpo che ribalta la v1: gli operatori US sono **price-INelastici**

La v1 assume "anchor-contro-zero → nuova voce di budget → alta elasticità → tieni basso". La ricerca su
funding e procurement dice il contrario per gli **operatori pubblici US** (cioè quasi tutta la pipeline):

1. **Si compra con grant FTA, non con cassa operativa.** Software/ITS/passenger-info è capital-eligible su
   §5307, §5311, §5339 e §5312 con match **80% federale / 20% locale**; come operating, 50/50.
   ([FTA 5339](https://www.transit.dot.gov/funding/grants/section-5339-bus-and-bus-facilities-program-bil),
   [guida Via](https://ridewithvia.com/resources/creative-ways-to-fund-on-demand-public-transportation-and-microtransit-in-2026),
   [FTA EMI/rider-experience](https://www.smartcitiesdive.com/news/fta-grants-rider-experience-public-transit-enhancing-mobility-innovation/720489/)).
2. **Felt cost reale**: un'app da $5.988/anno ($499/mo) costa all'agenzia **~$960–2.400/anno di tasca
   propria** (80/20 capital o 50/50 operating). Il delta $399↔$499 = $1.200/anno nominali → **$240–600/anno
   di felt cost**: sotto la soglia di rilevanza per chiunque firmi.
3. **La soglia micro-purchase è $15.000** (2 CFR §200.320, alzata da $10k il 1° ott 2025;
   [MRSC](https://mrsc.org/stay-informed/mrsc-insight/november-2025/federal-thresholds),
   [National RTAP](https://www.nationalrtap.org/Toolkits/Transit-Managers-Toolkit/Compliance/procurement-101)).
   **$399 annuale ($4.788) e $499 annuale ($5.988) sono ENTRAMBI nella zona micro-purchase** → nessun RFP,
   nessuna gara, stesso carico di compliance. Il delta di prezzo **non cambia l'attrito procurement**.
4. **Il grant funding *riduce attivamente* la price-sensitivity**: l'AEI documenta agenzie che pagano 2× lo
   stesso bus perché "i grant federali riducono la sensibilità al prezzo"
   ([AEI](https://www.aei.org/research-products/report/paying-less-for-public-transit-buses/)).

**Conseguenza:** la premessa portante della v1 ("alta elasticità → tieni basso") è **falsa per il segmento
US pubblico**. Il binding constraint non è il prezzo, è **(a) grant-eligibility** ("qualifica come ITS?") e
**(b) restare sotto soglia**. Entrambi i prezzi li soddisfano. A $399 sto **lasciando soldi sul tavolo**.

### 8.3 La diagnosi sbagliata della v1: il friction non è il prezzo, è la fiducia

Il punto più profondo. La v1 ottimizza il prezzo verso il basso "per facilitare il sì". Ma per un founder
**solo, italiano, senza entità US, senza referenze, con una sola demo (AppalRider nemmeno seeded)**, il
friction del primo deal è **credibilità e prova**, non i $100/mo. Nella zona grant-funded sotto-soglia il
prezzo è quasi irrilevante; quello che blocca l'agenzia è "è reale? ci sarà tra un anno? qualifica per il
nostro grant? chi altro lo usa?". $399 vs $499 **non tocca nessuna di queste**. Abbassare il prezzo risolve
il problema sbagliato — e per giunta un prezzo troppo basso può *sottrarre* credibilità a un compratore
istituzionale abituato a quote enterprise.

### 8.4 L'errore strutturale della v1: ho fatto decidere il prezzo globale al segmento più sottile

La v1 fissa **$399 unico** perché importa il vincolo IT (zona €5k senza MePA) sugli USA. Ma:
- **USA**: nessun vincolo €5k. Micro-purchase $15k + grant-inelasticità → headroom enorme.
- **IT**: il vincolo €5k è **reale** ma morbido. D.Lgs 36/2023 + L. Bilancio 2019: **<€5k** = affidamento
  diretto, **esente da MePA**, verifica congruità minima (il percorso più leggero). **€5k–140k** = resta
  affidamento diretto legittimo (NON gara), ma scatta l'**obbligo MePA/mercato elettronico** + il RUP deve
  documentare la congruità del prezzo. (I "5 preventivi" appartengono alla *procedura negoziata*, non
  all'affidamento diretto — correzione: era impreciso in una bozza.) $499/mo = ~€5.500/anno → *sopra* →
  attrito MePA proprio dove il ciclo è già lento. $399 = ~€4.788/anno (€3.990 annuale) → **sotto, con buffer**.

La pipeline IT è ~10 operatori; quella US ~21. **Far decidere il prezzo USA al vincolo di 10 operatori IT è
la coda che muove il cane.** Vanno **disaccoppiati**.

### 8.5 Dove la v1 REGGE alla controvalutazione

- ✅ **Metrica flat per-operatore**: confermata (AddTransit mostra l'attrito del per-unit; per-rider resta incompatibile con la privacy stance).
- ✅ **Uccidere il tier fare flat a $799**: rafforzato — *nessun* vendor fare (Masabi, Token, Bytemark) usa flat; tutti rev-share / "no upfront cost". Flat resta mis-pricing + rischio PCI su founder solo.
- ✅ **Ancorare al build custom ($75–150k), non all'incumbent fantasma**: rafforzato dall'assenza totale di prezzi pubblici comparabili.
- ⚠️ **Trial/annual**: la matematica multi-anno aggiunge un argomento nuovo (sotto).

### 8.6 Argomento nuovo emerso: struttura annuale vs multi-anno (procurement)

A $499/mo un contratto **triennale** = $17.964 → **supera** la micro-purchase $15k → può richiedere preventivi
multipli. A $399/mo triennale = $14.364 → resta sotto. **Implicazione**: indipendentemente dal prezzo,
**fatturare annuale (non multi-anno)** mantiene ogni impegno sotto soglia ed elimina l'attrito. Questo
**de-risca il prezzo più alto**: a $499 annuale resti comunque sotto i $15k. Quindi annuale > multi-anno è la
leva, non il prezzo basso.

### 8.7 Raccomandazione rivista (post-controvalutazione)

| | v1 (memo) | **v2 (post red-team)** | Perché è cambiato |
|---|---|---|---|
| **Core US** | $399/mo unico | **$499/mo** (placeholder confermato; valutare $599 sui segnali) | Grant-inelasticità + micro-purchase $15k + zero competitor visibile → $399 lasciava soldi sul tavolo |
| **Core IT** | $399/mo | **€399/mo · €3.990/anno** (sotto €5k, mono-tier) | €399 è il **tetto** frictionless: sotto €5k = affidamento diretto esente MePA. Sopra (es. €499) scatta obbligo MePA + congruità. Ticketing tier ~morto in IT (muro myCicero/Pluservice) |
| **Fatturazione** | annuale preferita | **annuale, mai multi-anno** | Tiene $499 sotto i $15k micro-purchase |
| **Fare** | no tier flat, add-on/rev-share | **invariato** (rafforzato) | Nessun vendor fare usa flat |
| **Metrica** | flat per-operatore | **invariato** | Confermato |
| **Messaggio** | ownership + frazione del custom | **invariato + leva "grant-eligible, sotto soglia, zero RFP"** | Per un buyer pubblico US questo conta più del prezzo |

**Verdetto rivisto sui placeholder:**
- **$499 (US) → CONFERMARE**, non abbassare. Probabilmente è anche il floor: testare $599 se i primi deal non oppongono frizione di prezzo.
- **$399/€390 (IT) → tenere sotto €5k/anno** per l'affidamento diretto senza 5 preventivi.
- **$799 fare flat → ELIMINARE** (invariato dalla v1).

### 8.8 Cosa NON ho potuto risolvere (limiti onesti della controvalutazione)

- **Nessuno studio TCRP/GAO** misura direttamente l'elasticità del *software* grant-funded (l'evidenza AEI è sui bus; estensione INFERITA, anche se strutturalmente solida).
- **La classificazione capital vs operating del SaaS ricorrente è ambigua** lato FTA: il felt cost reale ($960 vs $2.400) dipende da come l'agenzia la contabilizza. Non cambia la conclusione (inelastico in entrambi i casi) ma è un'incertezza.
- **WTP residua**: anche con l'inelasticità, il ceiling vero resta non misurato finché non chiudi 2–3 deal. La controvalutazione alza la stima del ceiling US, non la prova.
- **IT PNRR**: i fondi MaaS for Italy (€56,9M) finanziano l'*interoperabilità* (RAP regionali), non app standalone; un operatore piccolo può agganciarsi solo posizionando l'app come "piattaforma passenger-info MaaS-compatibile" su un avviso regionale ([innovazione.gov.it](https://innovazione.gov.it/progetti/mobility-as-a-service-for-italy/)). Canale possibile, non automatico — non sposta il pricing core.
