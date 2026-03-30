# TransitKit — Market Research

## Criteri di selezione operatore

Un operatore è un buon target se soddisfa TUTTI questi criteri:

### Must-have
- [ ] **No app branded** — niente app propria per trip planning/tracking (ticketing-only non conta)
- [ ] **GTFS pubblicato** — feed statico accessibile pubblicamente (o orari strutturati sul sito da cui creare GTFS)
- [ ] **Bacino utenza interessante** — almeno uno tra: turismo significativo, città universitaria, >500k rider/anno

### Nice-to-have (alzano la priorità)
- [ ] **GTFS-RT** — feed real-time (Trip Updates, Vehicle Positions, Service Alerts)
- [ ] **10-50 linee** — non troppo piccolo (3 linee = poco valore), non troppo grande (100+ = complessità)
- [ ] **Operatore privato o semi-privato** — decisioni più veloci di un ente pubblico puro
- [ ] **Budget digitale esistente** — già pagano Transit App / Moovit / altro = sanno che serve
- [ ] **Esperienza digitale frammentata** — usano 2-3 tool diversi (ticketing + tracker + maps) = pain chiaro
- [ ] **Fare-free** — semplifica l'app (niente sezione ticketing)
- [ ] **Stagionalità turistica** — picchi di download organici in alta stagione

### Red flags (scartare)
- ❌ Ha appena firmato/rinnovato contratto con Transit App, Pluservice, Moovit
- ❌ Ha lanciato una nuova app negli ultimi 12 mesi
- ❌ Troppo grande (metro di una capitale) — non ci guardano nemmeno
- ❌ Zero presenza digitale E zero GTFS E orari solo cartacei — troppo effort iniziale

---

## USA — Candidati verificati (2026-03-30)

### Tier 1 — Da contattare per primi

| Operatore | Città | Stato | Linee | Rider/anno | GTFS | GTFS-RT | App branded | Note | Status |
|-----------|-------|-------|-------|------------|------|---------|-------------|------|--------|
| **RFTA** | Aspen/Glenwood Springs | CO | 18 | 4M+ | ✅ [URL](https://www.rfta.com/RFTAGTFSExport.zip) | Parziale (web) | Solo ticketing (Masabi) | Esperienza frammentata in 3 tool. Più grande operatore rurale USA. Mercato sci/lusso premium. | 🔬 GTFS scaricato, pilota |
| **TCAT** | Ithaca | NY | 25 | 2.85M | ✅ [URL](https://s3.amazonaws.com/tcat-gtfs/tcat-ny-us.zip) | ✅ (Availtec) | Solo ticketing (TFare/Genfare) | Studenti Cornell hanno costruito app non ufficiale = domanda dimostrata. | 🔬 GTFS scaricato |
| **Coast RTA** | Myrtle Beach | SC | 10 | ~700k | ❓ (Ride Systems, non standard?) | ❌ | App abbandonata (tolta da Play Store ott 2025) | 20M turisti/anno. Shuttle gratis stagionale. Finanziamenti RIDE 4 in arrivo. | 📋 Da verificare GTFS |

### Tier 2 — Ottimi candidati

| Operatore | Città | Stato | Linee | Rider/anno | GTFS | GTFS-RT | App branded | Note | Status |
|-----------|-------|-------|-------|------------|------|---------|-------------|------|--------|
| **Citilink** | Fort Wayne | IN | 14 | 2M | ✅ (fwcitilink.com/gtfs/) | ✅ (Systrans) | Solo paratransit (Via) | Dichiarano apertamente di voler unificare ticketing+tracking. | 📋 Da approfondire |
| **CAT** | Charlottesville | VA | 12 | 1.35M | ✅ (apps.charlottesville.gov) | ✅ (ETA Spot) | No (solo ETA Spot generico) | UVA 25k studenti. Fare-free. Monticello UNESCO. | 📋 Da approfondire |
| **CET** | Bend | OR | 21 | 660k ↑ | ✅ (Passio) | ✅ (Passio) | No (Transit App) | Crescita rapida outdoor/turismo. Sta reintroducendo biglietti. | 📋 Da approfondire |
| **Santa Cruz METRO** | Santa Cruz | CA | 24 | 5.3M | ✅ (developer.scmetro.org) | ✅ (BusTime) | Solo ticketing (Masabi Splash Pass) | Il più grande per volume. UCSC 19k studenti. Rischio: investimento forte in Masabi. | 📋 Da approfondire |

### Tier 3 — Buoni ma rischio Transit App lock-in

| Operatore | Città | Stato | Linee | Rider/anno | Note | Status |
|-----------|-------|-------|-------|------------|------|--------|
| **ART** | Asheville | NC | 18 | 1.58M | 12M turisti/anno ma investimento Swiftly recente | 📋 |
| **WATA** | Williamsburg | VA | 12 | 1.5M | Colonial Williamsburg ma Masabi appena integrato (gen 2025) | 📋 |
| **MST** | Monterey | CA | 30 | 3M | Partnership profonda Transit App + Royale gratuito | 📋 |
| **Park City** | Park City | UT | 9 | 2.3M rider | Appena consolidato su Transit App (nov 2025) — timing pessimo | ❌ |
| **COAST NH** | Portsmouth | NH | ~12 | ~700k | Usa Passio GO generico. Zona costiera + UNH. | 📋 |

### Bocciati

| Operatore | Motivo |
|-----------|--------|
| Mountain Line (Flagstaff) | Ha app branded FLGRide + Mountain Line Go! |
| Wave Transit (Wilmington) | Ha appena lanciato app Connexionz |
| Park City Transit | Ha appena consolidato su Transit App (nov 2025) |

---

## ITALIA — Candidati (2026-03-30)

### Tier 1 — Traghetti/Navigazione (filone d'oro — zero presenza Pluservice)

| Operatore | Tratta | Turisti | App branded | GTFS | Note | Status |
|-----------|--------|---------|-------------|------|------|--------|
| **Alilauro** | Napoli→Sorrento→Capri→Ischia | Milioni | ❌ No | ❌ No | Traghetti veloci. Serve creare GTFS da orari sito. | 📋 Da approfondire |
| **Caremar** | Napoli→Ischia→Procida→Capri | Milioni | Solo sito mobile | ❌ No | Traghetti lenti. | 📋 |
| **SNAV** | Golfo di Napoli + Adriatico | Alto | ❌ No | ❌ No | Aliscafi. | 📋 |
| **Travelmar** | Costiera Amalfitana | Altissimo | ❌ No | ❌ No | Stagionale. Tra i corridoi più turistici al mondo. | 📋 Da approfondire |
| **Medmar** | Napoli→Ischia→Procida | Alto | ❌ No | ❌ No | | 📋 |
| **NLG** | Napoli→Capri | Alto | ❌ No | ❌ No | Navigazione Libera del Golfo. | 📋 |
| **Navigazione Laghi** | Garda + Como + Maggiore | 30M+/anno | "DreamLake" (guida turistica, NON transit) | ❌ No | Gestione governativa. App è guida, non orari real-time. | 📋 Da approfondire |

### Tier 2 — Città turistiche del Sud senza app

| Operatore | Città | Pop. | App branded | GTFS | Turismo | Status |
|-----------|-------|------|-------------|------|---------|--------|
| **AMAT** | Palermo | 650k | ❌ No | Expired | Altissimo | 📋 |
| **CASAM** | Matera | 60k | ❌ No | ❌ No | UNESCO, Capitale Cultura 2019 | 📋 |
| **SGM** | Lecce | 95k | ❌ No | ❌ No | Salento, turismo esploso | 📋 |
| **ATM** | Messina | 230k | ❌ No | Expired | Gateway Isole Eolie | 📋 |
| **ATC** | La Spezia | 95k | ❌ No | ❌ No | Gateway Cinque Terre! | 📋 Da approfondire |

### Tier 3 — Hanno GTFS ma no app branded

| Operatore | Area | Linee | GTFS | Note | Status |
|-----------|------|-------|------|------|--------|
| **Ferrovie della Calabria** | Tutta Calabria | 944 | ✅ Sì | Solo myCicero ticketing. Feed enorme. | 📋 |
| **ARST** | Tutta Sardegna | 319 | ✅ Sì | App esistenti sono non ufficiali/third-party | 📋 |
| **Riviera Trasporti** | Imperia/Liguria | ~30 | ✅ Sì | Costa turistica. Nessuna app. | 📋 Da approfondire |

### Competitor Italia: Pluservice
Pluservice (gruppo Mooney) domina il mid-market italiano. Hanno: MOM Treviso, Busitalia Veneto, SVT Vicenza, DolomitiBus, Roger (Emilia-Romagna), TPL FVG, SALGO Umbria, ATMA Ancona. **Non entrare nel loro territorio.**

---

## Legenda Status

- 🔬 In lavorazione (GTFS scaricato, analisi in corso)
- 📋 Da approfondire (nella lista, non ancora verificato a fondo)
- ✅ Verificato e confermato come buon target
- ❌ Scartato (con motivo)
- 🤝 Contattato
- 💰 Cliente

---

## Come aggiungere un nuovo operatore

1. Verifica i criteri must-have (no app, GTFS o orari strutturati, bacino utenza)
2. Cerca su App Store + Play Store: "[nome operatore] transit" e "[città] bus/ferry"
3. Cerca GTFS su: Transitland, Mobility Database, sito operatore, portali open data regionali
4. Compila una riga nella tabella del tier appropriato
5. Segna lo status

## Fonti per trovare nuovi operatori

### USA
- [Mobility Database](https://database.mobilitydata.org/) — 2000+ feed GTFS
- [Transitland](https://www.transit.land/) — aggregatore con API
- [FTA National Transit Database](https://www.transit.dot.gov/ntd) — tutti gli operatori USA
- [Transit App partner list](https://transitapp.com/partners) — chi usa Transit App (= no app propria)
- [Token Transit agencies](https://tokentransit.com/agency) — chi ha solo ticketing

### Italia
- [busmaps.com Italy](https://busmaps.com/en/italy/feedlist) — 145 feed italiani catalogati
- [Transitland Italia](https://www.transit.land/operators?country=IT)
- [dati.gov.it tag GTFS](https://www.dati.gov.it/node/192?tags=gtfs)
- [Wikipedia TPL Italia](https://it.wikipedia.org/wiki/Aziende_di_trasporto_pubblico_italiane) — lista completa operatori

### Europa
- [EU GTFS feeds](https://eu.data.europa.eu/) — portale open data EU
- Transitland per paese
- Mobility Database filtro per paese
