# TransitKit — Market Research

> Ultimo aggiornamento: 2026-04-01 (v4 — ricerca approfondita completata: App Store, Play Store, Reddit, GitHub, sito operatore, news 2024-2026 per ogni candidato)
> Obiettivo: identificare operatori con alta probabilità di conversione a $299/mese

---

## Criteri di selezione

### Must-have

1. **Nessuna app di trip planning** — ticketing-only (Token Transit, myCicero, MooneyGo) non conta come app
2. **GTFS pubblicato** — feed statico pubblico verificabile
3. **Bacino utenza interessante** — turismo significativo, città universitaria, o >500k rider/anno

### Segnali di conversione

Più segnali ha un operatore, più alta è la probabilità di chiudere.

**Peso alto (3 punti)**
- Già paga per tool digitali esistenti (Transit App, Token Transit, Masabi, ETA Spot, TransLoc...)
- Domanda dimostrata: app non ufficiale fatta da studenti, lamentele social documentate
- Esperienza frammentata: ticketing + tracker + maps su 3 strumenti separati
- App esistente con recensioni pessime (sotto 3 stelle) o abbandonata dallo store

**Peso medio (2 punti)**
- Funding recente (FTA grants USA, PNRR o fondi regionali in Italia)
- Operatore privato o semi-privato (decide in settimane, non mesi)
- 10-30 linee: abbastanza grande da giustificare $299/mese, abbastanza piccolo da non avere IT interno

**Peso basso (1 punto)**
- Turismo stagionale forte (download organici garantiti in alta stagione)
- Fare-free (semplifica l'app, niente logica ticketing in Phase 1)
- GTFS-RT disponibile (real-time rende l'app molto più utile, senza effort aggiuntivo)

**Soglie**: 8+ punti = Tier 1. 5-7 = Tier 2. Sotto 5 = non prioritario.

### Motivi di scarto

- Ha lanciato una nuova app negli ultimi 12 mesi — anche con solo 2 rating, se è un vendor serio (Syncromatics, Connexionz) il contratto è già firmato
- Raccomanda Transit App nominalmente sul proprio sito come strumento ufficiale per i rider — hanno già delegato il problema, non pagheranno per un secondo tool
- Ente pubblico puro con procurement rigido e contratti sopra 5k€ (gara pubblica obbligatoria)
- Troppo grande: capitale o >100 linee senza segmentazione
- Coperto da Pluservice/Cotrap/Teseo in Italia — non competere nel loro territorio

---

## USA

### Tier 1 — Candidati confermati

Per ogni operatore: verificati App Store iOS, Google Play, Reddit locale, GitHub, sito ufficiale, news 2024-2026.
Un candidato passa solo se tutti e sei i canali sono negativi.

| Operatore | Città, Stato | Linee | Rider/anno | GTFS | Score |
|-----------|-------------|-------|-----------|------|-------|
| AppalCART | Boone, NC | 12 | 1.5M | Transitland + RT | 10 |
| Corvallis Transit (CTS) | Corvallis, OR | 12 | ~2M | URL diretto + RT | 10 |
| BRTA | Pittsfield, MA | 13 | ~800k | Trillium | 8 |
| Citilink | Fort Wayne, IN | 14 | 1.6M | Sito ufficiale + RT | 8 |
| Pullman Transit | Pullman, WA | 10 | 1.4M | Transitland + RT | 9 |

---

**AppalCART** — Boone, NC

L'unico tool disponibile ai rider è ETA SPOT: un'app generica (non branded AppalCART) con 1.5 stelle e 994 rating. L'operatore non endorsa Transit App, non esiste app non ufficiale nonostante 20.000 studenti di Appalachian State University. La frustrazione è documentata pubblicamente su Reddit e Twitter. Fare-free. GTFS con real-time su Transitland.

Target principale per la demo app.

---

**Corvallis Transit (CTS)** — Corvallis, OR

App Connexionz presente su iOS e Android ma con solo 9 rating totali — gli utenti hanno smesso di usarla e di recensirla. Ultimo aggiornamento funzionale del 2019; la release di dicembre 2025 ha cambiato solo le impostazioni età. Nessuna app non ufficiale nonostante 35.000 studenti di Oregon State University. Nessuna integrazione Transit App trovata. GTFS con URL diretto confermato, real-time incluso. Fare-free.

Dimensioni paragonabili ad AppalCART con una situazione app ancora più degradata.

---

**BRTA (Berkshire Regional Transit Authority)** — Pittsfield, MA

RouteShout 2.0 è l'unico tool: 1.6 stelle con 246 rating, abbandonato dal 2019. Il sito del vendor RouteShout è un 404 da anni. L'operatore usa anche "Where's My B-Bus" (web-only, RouteMatch) ma nessuna app funzionante. Nessuna integrazione Transit App. 13 linee nel sweet spot dimensionale. Area turistica forte: Tanglewood, MASS MoCA, Berkshire hills.

---

**Citilink** — Fort Wayne, IN

Citilink Access esiste ma è solo per il servizio paratransit (prenotazione per rider con disabilità, via Via Transportation). Per i rider normali: Token Transit per pagare, Transit App per tracciare — due strumenti separati senza integrazione, e nessuno è endorsato formalmente dall'operatore come soluzione unica. Nessuna app branded per le linee fisse. 14 linee, real-time via Systrans.

---

**Pullman Transit** — Pullman, WA

Situazione app caotica: PTBusBeacon (Syncromatics/GMV) è l'app ufficiale ma ha 1.2 stelle su iOS (90 rating) e 2.0 stelle su Android (111 rating) — GPS inaffidabile, crash frequenti, loop infiniti. WSU la linka come app ufficiale pur essendo notoriamente rotta. Esiste anche un'app indie alternativa (Pullman Bus Tracker) ma è solo iOS, solo dev, 10 rating. 30.000 studenti WSU senza una soluzione che funzioni. Fare-free. GTFS con real-time.

---

### Tier 2 — Da approfondire

| Operatore | Città, Stato | Linee | Rider | GTFS | Situazione attuale | Note |
|-----------|-------------|-------|-------|------|--------------------|------|
| Mountain Rides | Ketchum/Sun Valley, ID | 6-10 | ~400k | Da confermare URL | TransLoc generico, 2.6 stelle — operatore già spende $10k/anno e si lamenta pubblicamente dell'affidabilità | GTFS implicato da RT ma URL diretto non trovato. Budget digitale provato: pitch si scrive da solo |
| CAT | Charlottesville, VA | 12 | 1.35M | Transitland + RT | App ufficiale 1 stella / 7 rating (abbandonata 2019). Ma la city page linka Transit App come strumento per i rider | Borderline: verificare se Transit App è promosso attivamente o è solo un link passivo nella pagina |
| Coast RTA | Myrtle Beach, SC | 10 | ~700k | Da confermare | App rimossa dallo store ottobre 2025. 20M turisti/anno. RIDE 4 grant recente | GTFS non confermato — verificare prima di procedere |
| CET | Bend, OR | 21 | 660k | Passio + RT | Solo Transit App di terzi, nessuna app branded. Crescita forte, outdoor tourism | Candidato solido se GTFS confermato come operabile |
| Crested Butte Mountain Express | Crested Butte, CO | 4-5 | 659k | Da confermare | Solo app on-demand (FirstTracks), nessuna per linee fisse. Fare-free, sci, forte turismo | Piccolo per numero linee ma rider/anno notevoli — GTFS da verificare |
| CyRide | Ames, IA | 13 | 4.7M | Confermato | No app dedicata. App studenti "Ames Ride" non ufficiale ma attiva | Iowa State 36k studenti, #1 in Iowa. Verificare se il budget attuale giustifica $299 |
| Martha's Vineyard VTA | Martha's Vineyard, MA | ~12 | ~1M | Confermato | VTA Pay (ticketing) + tracker web separati. Nessuna app unificata | Isola resort, stagionale, frammentato |
| Verde Shuttle | Sedona, AZ | 2 | ~150k | Trillium | No app branded | Solo 2 linee ma Sedona ha 3M visitatori/anno. Piccolo operatore, facile da convertire |
| Ozark Regional Transit | Fayetteville, AR | ~12 | 315k | Confermato | App solo on-demand. Linee fisse senza app | Università Arkansas fare-free, crescita +15.8% |

---

### Scartati

| Operatore | Motivo |
|-----------|--------|
| RFTA (Aspen CO) | Transit App integrazione formale (17 linee, real-time) + web BusTracker con trip planning — entrambi endorsati sul sito rfta.com |
| TCAT (Ithaca NY) | Navi (Cornell AppDev) 4.4 stelle / 1.020 rating / 5K MAU attiva dal 2018 |
| Advance Transit (Hanover NH) | Operatore raccomanda Transit App per nome sul proprio sito come strumento ufficiale per i rider |
| Streamline Bus (Bozeman MT) | App Syncromatics/GMV ufficiale lanciata gennaio 2026 su iOS e Android — vendor serio con contratto |
| Island Explorer (Bar Harbor ME) | PWA ufficiale NPS funzionante. Turismo stagionale, bassa fidelizzazione app |
| Annapolis Transit (MD) | Shifting verso microtransit nel budget 2025-26. Nessun driver universitario |
| Mountain Line (Flagstaff AZ) | App branded FLGRide + Mountain Line Go! |
| Wave Transit (Wilmington NC) | App Connexionz appena lanciata |
| Park City Transit (UT) | Consolidato su Transit App ufficiale (novembre 2025) |
| Steamboat Springs Transit (CO) | App branded "Go Steamboat" con real-time |
| Jackson Hole START Bus (WY) | Partnership profonda Transit App |
| Gatlinburg Trolley (TN) | App branded (2023) |
| Breckenridge Free Ride (CO) | App branded "My Free Ride" (GMV) |
| Telluride SMART (CO) | Transit App + Token Transit ufficiali |
| Key West Transit (FL) | Passio GO! + altri |
| Durango Transit (CO) | App Ride Systems + TransLoc |
| Missoula Mountain Line (MT) | Transit App ufficiale |
| Green Mountain Transit (Burlington VT) | App branded "Ride Ready by GMT" |
| Bloomington Transit (IN) | Umo Mobility + Token Transit |
| Cape Cod RTA (MA) | App branded CCRTA GoApp |
| BATA (Traverse City MI) | Transit App + BATA Bus Tracker |
| Vail Town Transit (CO) | App branded |
| Moab Area Transit (UT) | App branded |
| Blacksburg Transit (VA) | App branded iOS + Android |
| Link Transit (Wenatchee WA) | App branded recente |
| CARTA (Chattanooga TN) | CARTA GO + Bus Tracker |
| MST (Monterey CA) | Partnership profonda Transit App |
| Nantucket NRTA (MA) | TransLoc/Avail branded |
| CVTD (Logan UT) | App Syncromatics ufficiale su iOS e Android |

---

## Italia — TPL (bus)

### Mappa competitor

Prima di contattare qualsiasi operatore italiano, verificare la copertura:

- **Pluservice/Mooney**: copre Abruzzo (TUA), Friuli-VG (TPLFVG), Umbria (SALGO), Calabria (FdC + Lirosi), Toscana nord-ovest (Teseo parziale). Stronghold consolidato.
- **Cotrap/Itineris**: copre tutto il consorzio Puglia — 60 operatori incluse le linee STP Lecce. App aggiornata 01/04/2026.
- **AroundSardinia**: copre ARST, ATP Sassari, ATP Nuoro, CTM, ASPO — quasi tutta la Sardegna.
- **Südtirolmobil**: Alto Adige/Südtirol completamente coperto (200.000 install).
- **MuoversiPiemonte**: solo alert di disruzione, NON è un'app trip-planning — non blocca il pitch.
- **MooneyGo/myCicero**: solo ticketing, NON è trip-planning — non blocca il pitch.

---

### Tier 1A — GTFS pubblicato, nessuna app trip-planning

| Operatore | Area | Linee | GTFS | Status |
|-----------|------|-------|------|--------|
| STP Brindisi | Prov. Brindisi, Puglia | ~30 | Transitland + sito (29 versioni, CC-BY-4.0, aggiornato 01/04/2026) | Confermato |
| ATAP | Biella + Vercelli, Piemonte | ~30 | magellanoprogetti.it (100+ versioni, aggiornato 01/04/2026) | Confermato |
| SAF Duemila/VCO | VCO, Piemonte | ~18 | Feed Regione Piemonte aggregato | Confermato — verificare che le linee provinciali SAF siano incluse nel feed, non solo il shuttle aeroportuale |
| Riviera Trasporti | Imperia, Liguria | ~30 | GTFS stale su Transitland (1849 giorni), Moovit ha RT quindi esiste altrove | Da re-verificare — ricerca precedente trovava un'app iOS (id6753180867, ottobre 2025), ricerca v4 non la trova. Cercare direttamente su App Store prima di qualsiasi contatto. |
| Autoservizi Preite | Cosenza, Calabria | ~20 | Probabilmente nel feed CORe Regione Calabria (da confermare) | Da verificare — nessuna app iOS/Android trovata, operatore privato con cultura digitale (GPS dal 2005, ISO certificato) |

---

**STP Brindisi** — Puglia

MooneyGo e myCicero per il ticketing, "Call Bus STP" (ioki GmbH) per il servizio on-demand — nessuno dei tre è un'app trip-planning per le linee fisse. STP Brindisi è fuori dal consorzio Cotrap quindi non è coperta da Cotrap/Itineris. Operatore privato (S.p.A.). Area turistica forte: Ostuni, Alberobello (UNESCO), Salento.

---

**ATAP** — Biella e Vercelli, Piemonte

MyCicero e MooneyGo per il ticketing, trip planner solo via web sul sito. MuoversiPiemonte invia alert di disruzione ma non è un'app consumer branded per ATAP. GTFS eccellente: 100+ versioni archiviate, aggiornato quotidianamente. Operatore pubblico (SpA municipale) — ciclo di decisione più lento ma non impossibile.

---

**SAF Duemila/VCO** — VCO, Piemonte

"Alibus VCO" esiste ma è solo per la prenotazione dello shuttle aeroportuale Brig-Malpensa — non è un'app per le linee provinciali. Nessuna altra app trovata. Operatore privato (S.p.A., ha acquisito Comazzi nel luglio 2025). Area turistica: Lago Maggiore, Val d'Ossola, Domodossola. Prima di contattare: confermare che il feed GTFS regionale Piemonte includa le linee provinciali SAF e non solo lo shuttle.

---

### Tier 1B — Nessun GTFS (da creare come parte del pacchetto)

| Operatore | Area | Linee | Perché interessante |
|-----------|------|-------|---------------------|
| STPS Sondrio | Valtellina, Lombardia | ~35 | MooneyGo ticketing only. Olimpiadi Invernali 2026 (Livigno/Bormio) = urgenza e visibilità. Sci + Stelvio. |
| AMC Catanzaro | Catanzaro, Calabria | ~30 | MyCicero ticketing only. Include una funicolare urbana (unica in Calabria). Capoluogo regionale. |
| CLP Caserta | Caserta, Campania | ~20 | Nessuna app. Privato (consorzio). Reggia di Caserta UNESCO. |
| AIR Campania | Avellino, Campania | ~30 | Nessuna app. Privato. Gateway Napoli-Calabria via Irpinia. |
| COTRAB | Potenza + Matera, Basilicata | ~50 | App offline base (orari scaricabili, no real-time). Matera UNESCO. 50 linee. |

---

### Scartati

| Operatore | Motivo |
|-----------|--------|
| STP Lecce | Cotrap/Itineris copre tutti i 60 operatori del consorzio Puglia inclusa STP Lecce — aggiornata 01/04/2026 |
| Lirosi Autoservizi | Lirosi Linee + Lirosi Move entrambe attive (Pluservice) |
| Ferrovie della Calabria | myFdC lanciata gennaio 2026 (Pluservice) |
| AMTS Catania | CT Mover + Pluservice |
| KYMA Taranto | App con trip planner attiva |
| ATAM Reggio Calabria | App branded attiva |
| SGM Lecce | App propria + Moovit |
| ATM Messina | ATM MovUp con trip planner + ticketing |
| AMAT Palermo | PalerMobilita + Bus Palermo |
| Linee Lecco | Transita (Agenzia TPL Como/Lecco/Varese) |
| Start Romagna (Rimini) | Roger (Pluservice/Mooney) |
| SETA Modena/RE/PC | App SETA branded |
| TEP Parma | Teseo TEP |
| Autolinee Toscane | "at bus" domina tutta la Toscana |
| ATB Bergamo | ATB Mobile |
| ASF Como | ASF For You + Arriva MyPay |
| Brescia Trasporti | Bresciapp! |
| APAM Mantova | APAM mobile+ |
| CTM Cagliari | BusFinder |
| ATP Sassari | App branded |
| ASPO Olbia | Pluservice |
| ATP Nuoro | Teseo Nuoro |
| AMTAB Bari | Bari Smart + MUVT |
| Trentino Trasporti | Muoversi in Trentino (regionale) |
| SASA Bolzano | Südtirolmobil (regionale, 200k install) |
| TPL FVG | Pluservice (4 operatori provinciali) |
| Busitalia Umbria | SALGO (Pluservice) |
| TUA Abruzzo | Pluservice (tutta la regione) |
| CTT Nord / COPIT / CAP | Teseo (Pisa/Livorno/Pistoia/Prato) |
| SAIS Autolinee Sicilia | App iOS + Android attiva (Sitrap, gennaio 2026) |
| ATV Verona | Pluservice |
| ATVO Veneto Orientale | App turismo branded |
| Conerobus/ATMA Ancona | Pluservice |
| SVAP Aosta | SVAP Bus app |
| TPL Linea Savona | App + myCicero |
| EAV Napoli | GO Eav — troppo grande |
| Cotral Lazio | App branded — troppo grande |
| Interbus Sicilia | App branded |
| Ferrovie del Gargano | App branded |
| FCE Catania | App branded |
| ATAF Foggia | App ATAF Foggia branded |
| ATM Trapani | ATM Trapani Bus branded |
| Autolinee Varesine | CTPI Varesine (iOS/Android) |
| Grandabus Cuneo | Moeves Bus (Pluservice) |
| ARFEA Alessandria/Asti | ARFEApp + Moeves |
| SUN Novara | Noe app (Pluservice) |
| Ferrotramviaria | App propria (Bari Nord) |
| Riviera Trasporti | Stato incerto — verificare prima di qualsiasi azione |
| ATP Esercizio Genova | Fusa in AMT Genova (gen 2021) — non esiste più |
| Francigena Viterbo | Non è un operatore autonomo — è un servizio Cotral |
| Miccolis | My Bus Matera + Mobility Ticket Potenza |
| AroundSardinia (ATP+ARST+CTM+ASPO) | Sistema regionale integrato — Sardegna praticamente coperta |

---

## Italia — Ferry (pipeline separata, dopo i primi clienti TPL)

> Mercato diverso: cliente tipo, stagionalità e ciclo di vendita differenti dal TPL bus.
> Tutti questi operatori richiedono creazione GTFS come parte del pacchetto.

### Golfo di Napoli — priorità alta (privati, zero Pluservice, turismo elevatissimo)

| Operatore | Tratte | App | Note |
|-----------|--------|-----|------|
| Alilauro | Napoli → Sorrento, Capri, Ischia | No | Privato, milioni di turisti |
| Travelmar | Costiera Amalfitana | No | Privato, tra i corridoi più turistici al mondo |
| Caremar | Napoli → Ischia, Procida, Capri | Solo sito mobile | Semi-privato |
| SNAV | Golfo di Napoli + Adriatico | No | Privato (MSC group) |
| Medmar | Napoli → Ischia, Procida | No | Privato |
| NLG | Napoli → Capri | No | Privato, tratta premium |

### Altre acque — priorità media

| Operatore | Tratte | GTFS | App | Note |
|-----------|--------|------|-----|------|
| Alilaguna | Laguna di Venezia (aeroporto ↔ città ↔ isole) | Feed ACTV | No (MyPass terzi) | Venezia 12M+ turisti/anno. Privato. |
| Nav. Lago d'Iseo | Lago d'Iseo, Lombardia | Parziale | No (myCicero ticketing) | Monte Isola + Franciacorta |
| Toremar | Elba, Capraia, Giglio | Regione Toscana | No (booking terzi) | Gruppo Moby — decisioni lente |
| Liberty Lines | Eolie, Egadi, Pelagie, Ustica | Da creare | App booking (no tracking) | Grande e complesso |
| Laziomar | Ponza, Ventotene | Da creare | App booking base | Ventotene UNESCO, stagionale |
