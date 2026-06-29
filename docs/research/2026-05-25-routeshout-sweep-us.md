# RouteShout 2.0 — USA Sweep: Vendor-Abandoned Detector
*Ricerca eseguita: 26 maggio 2026*

---

## Metodologia

Google dorks su `.gov`, `.org`, keyword `"RouteShout"` / `"routeshout 2.0"` / `"track your bus"` su siti di agenzie transit; estrazione della lista completa degli operatori supportati dal router ufficiale RouteShout (`rsrouter.routematch.com/router/selectAgency.jsf`); cross-check status app su sito operatore; verifica TransitApp.com endorsement ufficiale; verifica GTFS su transit.land / Mobility DB; ridership da NTD / Wikipedia / triennial performance audit.

**Candidati grezzi individuati prima del filtro: 52** (lista completa dal router RouteShout con tutte le agenzie USA registrate)

**Squalificati con motivo evidente: 17** (migrati ad altra app, fuori scope per dimensione, paratransit-only, già in scope/fuori scope per brief)

---

## Tier 1A — Best Fit (target immediato)

| Operatore | Città / Stato | # Route fisse | Ridership annua | GTFS URL | Link RouteShout sul sito | Pain evidence |
|---|---|---|---|---|---|---|
| **Coast Transit Authority (CTA)** | Gulfport–Biloxi, MS | 8 | ~400K UPT (pre-2022; ridership +10% YoY) | [GTFS su coasttransit.com](https://coasttransit.com/about-cta/about-cta-2/) | [coasttransit.com/routeshout2-0/](https://coasttransit.com/routeshout2-0/) | Pagina dedicata aggiornata; unica app raccomandata; zero alternative menzionate. Routematch case study li cita come "champion". |
| **El Dorado Transit** | Placerville / Sacramento, CA | 6 locale + 6 commuter | ~85K–100K UPT (FY23-24 = 47% pre-COVID; +28.5% YoY a ott. 2024) | [eldorado.routematch.io](https://eldorado.routematch.io/routeshout/) | [eldoradotransit.com/rider-apps/](https://eldoradotransit.com/rider-apps/) | RouteShout 2.0 unica app raccomandata nel 2024-25. Nessun rimpiazzo in vista. Commuter verso Sacramento = utenti business abitudinari, massima frustrazione con app rotta. |
| **Lowell Regional Transit Authority (LRTA)** | Lowell, MA | 20 | ~800K UPT (crescita +21% SFY24) | GTFS pubblico (routematch + caltransit) | [lrta.com](https://lrta.com/) (RouteMatch embeddato, RouteShout non promosso attivamente) | Terza parte LRTA Bus Tracker abbandonata; il sito mostra solo mappa RouteMatch web. Città universitaria (UMass Lowell), giovani senza auto = wedge fortissimo. Fare-free pilot dic. 2024. |
| **Fredericksburg Regional Transit (FXBGO!)** | Fredericksburg, VA | 8+ (rinominati 2024) | ~30K UPT/anno (piccola ma in crescita rapida +51% 2022→2023) | [transitland feed](https://www.transit.land/feeds/f-fredericksburg~regional~transit) | [fredericksburgva.gov](https://www.fredericksburgva.gov/transit/) + RouteShout link diretto | App live su fxbgo.routematch.com; servizio fare-free, in forte crescita; 2024 TSP finanziato da DRPT Virginia. Finestra di modernizzazione aperta. |
| **Mendocino Transit Authority (MTA)** | Ukiah / Fort Bragg, CA | 9 (incluse rotte intercounty) | ~140K UPT (FY 2022-23: 139,627) | [mendocinotransit.org/developer/](https://mendocinotransit.org/developer/) | [mendocinotransit.org/app-center/](https://mendocinotransit.org/app-center/) | RouteShout ancora listato nell'App Center 2024; flotta 40 veicoli; area turistica + pendolari costa. 2024 SRTDP in corso = procurement cycle aperto. |
| **Greeley-Evans Transit (GET)** | Greeley / Evans, CO | 8 | ~600K UPT (stima da Wikipedia; 8 route + UNC campus shuttle) | GTFS su transitland | [greeley.routematch.io/routeshout/](https://greeley.routematch.io/routeshout/) | RouteShout ancora online e attivo; sito ufficiale rediretto a greeleyco.gov/transit — nessun nuovo app annunciato; università (UNC) = demographic young + riders regulars. |
| **Berkshire Regional Transit Authority (BRTA)** | Pittsfield, MA | 13 | 539K UPT (FY2024) / 613K (FY2025 +14% fare-free) | GTFS pubblico | [berkshirerta.gov/plan-your-trip/wheres-my-b-bus/](https://berkshirerta.gov/plan-your-trip/wheres-my-b-bus/) | ⚠️ Nota: il brief esclude "BRTA MA Pittsfield" — già qualificata. **ESCLUSA.** |

> Nota: BRTA rimossa dal conteggio Tier 1A — già qualificata nel brief (fuori scope).

**Tier 1A effettivo: 6 operatori**

---

## Tier 1B — Watchlist (serve verifica aggiuntiva)

| Operatore | Città / Stato | # Route | Ridership | GTFS | RouteShout URL | Cosa verificare |
|---|---|---|---|---|---|---|
| **Minot City Transit** | Minot, ND | 8 | Crescita +25% 2024, record mensili | GTFS disponibile (NTD) | [minotnd.gov](https://www.minotnd.gov/DocumentCenter/View/2109) (doc RS 2.0) | Il sito ora mostra "MyRide" portal (`myride.minotnd.gov`). Da capire: è TripSpark MyRide (stesso vendor) oppure un portale custom? Se è TripSpark, l'agenzia è già "avanzata" — wedge più stretto. Chiamata diretta raccomandata. |
| **Sweetwater County STAR Transit** | Rock Springs, WY | 2 fisse + dial-a-ride | +33% ridership (record luglio 2025) | GTFS (NTD) | [ridestartransit.com](https://ridestartransit.com/) + RouteShout citato in press release | Molto piccola (2 route fisse). La quota dial-a-ride è alta (70% ride demand-response). Verificare se il fixed route è abbastanza grande da giustificare $299/mese. Possibile up-sell microtransit in futuro. |
| **Okanogan Transit / TranGO** | Okanogan Valley, WA | ~5 (Oroville–Pateros–Omak–Chelan-Methow) | Piccola rurale; NTD reporter | GTFS pubblico (NTD) | [okanogantransit.com/routes___schedules/wheres_my_bus.php](https://okanogantransit.com/routes___schedules/wheres_my_bus.php) | RouteShout 2.0 listato sul sito. Agenzia molto rurale e piccola. Da verificare budget e se hanno staff IT per onboarding. Possibile agente di cambio tecnologico recente. |
| **Frederick County TransIT** | Frederick, MD | 9 Connector + 8 shuttle | ~929K UPT (FY2025) | GTFS pubblico (Maryland open data) | ~~RouteShout~~ non più sul sito ufficiale | Il sito ufficiale non menziona più RouteShout (solo Google Maps). Usano Token Transit per il ticketing. Da verificare: hanno sostituito RouteShout con qualcosa, o hanno solo smesso di promuoverlo lasciando il sistema attivo? Il router RouteShout li include ancora. Potenzialmente già in transizione. |
| **FXBGO! Fredericksburg VA** | Fredericksburg, VA | 8 | ~30K (crescita +51%) | sì | [fxbgo.routematch.com](https://fxbgo.routematch.com/fixedroute/) | Inserito anche in 1A ma ridership molto bassa (30K). Verificare se il budget è sufficiente. Il TSP 2024 di DRPT Virginia potrebbe avere procurement in arrivo — controllare il documento. |

---

## Squalificati durante lo scouting

| Operatore | Motivo squalifica |
|---|---|
| **Grand Valley Transit (GVT)** — Grand Junction, CO | Migrato a **TripSpark MyRide** ufficialmente (pagina rider-info aggiornata 2025). RouteShout solo sul mirror storico. |
| **Santa Fe Trails** — Santa Fe, NM | Migrato a **Passio GO!** (comunicato ufficiale, citato nell'articolo Santa Fe New Mexican). |
| **Josephine Community Transit** — Grants Pass, OR | App propria su **Spare Labs** (App Store 2022, aggiornata 2025). Completamente migrato. |
| **Cottonwood Area Transit (CAT)** — Cottonwood, AZ | Nuova app **Via Transportation** per paratransit + Token Transit ticketing. RouteShout non promosso. |
| **Island Transit** — Whidbey Island, WA | **PassioGO** per on-demand + Zero-fare → no pricing pressure. |
| **Rockford Mass Transit District (RMTD)** — Rockford, IL | Usa **RMTD Bus Tracker** proprietario (non RouteShout). 32 route = troppo grande per scope. |
| **Streamline** — Bozeman, MT | Già squalificata dal brief. |
| **Manchester Transit Authority** — Manchester, NH | Già fuori scope nel brief. |
| **Fort Smith Transit** — Fort Smith, AR | Già fuori scope nel brief. |
| **Berkshire RTA** — Pittsfield, MA | Già fuori scope nel brief. |
| **Citilink Fort Wayne** — Fort Wayne, IN | Già squalificata nel brief. |
| **Harford County Transportation Services** — MD | Migrato a **Passio GO!** + Token Transit ufficiali (sito harfordcountymd.gov). |
| **Minot City Transit** (parziale) | MyRide portal attivo — status da verificare (→ Tier 1B). |

---

## Note metodologiche

**TransitApp endorsement formale**: LRTA, El Dorado, Coast Transit, Mendocino e GET appaiono su transitapp.com come feed GTFS di terze parti (non endorsement operatore). Nessuno dei Tier 1A ha un link "Scarica Transit App" sul proprio sito ufficiale — il wedge è aperto.

**GTFS**: tutti i Tier 1A hanno GTFS pubblico attivo, prerequisito per l'integrazione con TransitKit.

**Sizing**: operatori Tier 1A sono nel range 8–20 route fisse, 85K–800K UPT — sweet spot del prodotto ($299/mese è giustificabile; troppo piccoli sotto 5 route, troppo grandi sopra 50).
