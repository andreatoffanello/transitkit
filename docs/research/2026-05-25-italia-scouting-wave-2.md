# Italia Scouting Wave 2 — TransitKit
**Data:** 2026-05-26  
**Metodologia:** Applicazione sistematica di 4 filtri (Cotrap-exclusion, myCicero bundled vs. solo-ticketing, vendor age >3 anni, community-built replacement) su ~35 operatori in 10 regioni italiane non coperte dallo scouting precedente. Fonte dati: App Store, Google Play, Transitland, siti ufficiali operatori, Moovit.

**Candidati raw esplorati:** 35 operatori/contesti.

---

## Tier 1A — 4/4 filtri superati

| Operatore | Area | # linee (est.) | GTFS | App esistente | Filtri | Pain evidence |
|-----------|------|----------------|------|---------------|--------|---------------|
| **STPS Sondrio** | Valtellina, Lombardia extraurbano | 35 extraurbane | Su Transitland tramite regione (non pubblicato autonomamente) | App "STPS SpA" — PPL Dijiti, rating 3.0/5 su 4 recensioni, iOS 9 target, UI fossile | ✅ Non Cotrap ✅ myCicero solo-ticketing (MooneyGo ticketing bolt-on) ✅ App pre-2022, vendor morto ✅ Community app Moovit unica alternativa | "Ticket purchasing is very complicated" — recensione utente; app rilasciata ~2022, nessun aggiornamento significativo da allora |
| **Autoservizi Silvestri Livigno** | Livigno (SO), Lombardia — turismo alpino | 4 linee urbane + connessioni | Sì — Transitland feed attivo, 1 feed (f-u0-switzerland associato per tratto CH) | Nessuna app branded. Moovit unica fonte strutturata. Dal 2026: MyLivignoPass card ma non app TPL dedicata | ✅ Non Cotrap ✅ Nessun myCicero ✅ Nessuna app branded esistente ✅ Community: solo Moovit, nessun indie | Nessuna app propria in un comune 100% turistico con >1M presenze/anno. Il feedback su Moovit è l'unica fonte di orari strutturata |
| **ATM Azienda Trasporti Molisana** | Campobasso + provincia, Molise | 36 urbane + regionale | Non confermato pubblicamente | App "ATM Molise" lanciata 2024 — ma solo ticketing/prenotazione, nessun trip planning nativo | ✅ Non Cotrap ✅ No myCicero bundled ✅ App nuova ma solo-booking, no trip planning ✅ Community: Moovit unica alternativa | Nuova app (2024) richiede nuova registrazione e ha reimpostato la base utenti. Funzionalità base rispetto ai competitor. 29 operatori privati nella regione senza aggregazione |
| **Civitavecchia Servizi Pubblici** | Civitavecchia, Lazio — porto principale | 15 linee | Non confermato pubblicamente | App "Civitavecchia online" — QR-code per bus + parcheggi, ticketing ma nessun trip planning, UI multouso non brandizzata come app TPL | ✅ Non Cotrap ✅ No myCicero bundled ✅ App non aggiornata significativamente, non ricercabile come "bus Civitavecchia" | ✅ Community: Moovit unica alternativa, nessun indie | Porto con ~7M passeggeri/anno (crocieristi). Operatore standalone, nessuna integrazione regionale Cotral |

---

## Tier 1B — 3/4 filtri superati

| Operatore | Area | # linee (est.) | GTFS | App esistente | Filtri | Filtro mancante / cosa cambierebbe |
|-----------|------|----------------|------|---------------|--------|------------------------------------|
| **ATAP Pordenone** | Pordenone, FVG | 8 urbane + 56 extraurbane | Non confermato autonomamente | App "ATAPP" — developer esterno (eu.proexe), dati scarsi su aggiornamento, fusa nel sistema regionale TPL FVG | ✅ Non Cotrap ✅ No myCicero bundled ✅ App del 2014 (prima versione) | ⚠️ Filtro 3 ambiguo: nel 2024 ATAP è integrata nel sistema TPL FVG che ha la sua app Pluservice aggiornata. Se TPL FVG copre il bacino, lo slot è occupato |
| **APM Macerata** | Macerata, Marche — urbano | ~10 linee urbane | Non pubblicato | App "ApMobilità" — iOS, focus parking + ZTL + abbonamenti. Nessun trip planning bus | ✅ Non Cotrap ✅ No myCicero trip planning bundled ✅ App non TPL-focused | ⚠️ Filtro 4: ~10 linee molto limitate, GTFS non confermato. Il bacino extraurbano è coperto da Contram (già Tier 1A in wave 1) |
| **TIEMME Caltanissetta** | Caltanissetta, Sicilia — urbano | ~8-12 linee | Presente via Moovle / Comune | App Tiemme Trasporti (KentKart, 2025) — trip planning + RT via Moovle | ✅ Non Cotrap ✅ No myCicero bundled | ⚠️ Filtro 3: app Tiemme lanciata 2025 (Kenya-based KentKart) — vendor attivo. ⚠️ Filtro 4: Moovle è la community app ma è diventata app ufficiale delegata. Slot occupato |
| **COTRAB Basilicata** | Potenza + Matera, Basilicata | Bacino regionale extraurbano | Non confermato | App offline per Potenza urbano — basica, nessun trip planning, nessun ticketing moderno | ✅ Non Cotrap (è COTRAB, consorzio Basilicata separato) ✅ No myCicero bundled ✅ App primitiva | ⚠️ Filtro 1: COTRAB è un *consorzio* analogo a Cotrap ma per la Basilicata. La pressione politica verso aggregazione regionale è alta (gara TPL 2026 in corso). Rischio: diventare irrilevante prima dell'onboarding |
| **SAIS Autolinee Sicilia** | Sicilia orientale/centrale (Siracusa, Enna, Ragusa) | ~30 interurbane | Non confermato | App SAIS Autolinee (Sitrap) — ticketing only, no trip planning, poche recensioni | ✅ Non Cotrap ✅ myCicero assente ✅ App fossile (poche recensioni, UX datata) | ⚠️ Filtro 1 (GTFS): nessun feed pubblico trovato. Senza GTFS non si può costruire l'app di routing. SAIS dovrebbe fornire i dati |

---

## Squalificati durante scouting

| Operatore | Motivo |
|-----------|--------|
| **TUA Abruzzo** | App TUAbruzzo (Pluservice, 2024) con trip planning completo + ticketing, vendor attivo |
| **BusItalia Umbria** | App SALGO (Pluservice) con trip planning + ticketing full, aggiornata 2024 |
| **Cotral Lazio** | App propria (v4.9.4, 2024) con trip planning + RT + ticketing — vendor attivo, 3623 route |
| **TPL FVG** | App (Pluservice, aggiornata) copre Gorizia + Udine + Pordenone + Trieste — regionale unificata |
| **ARST Sardegna** | ARST Finder con trip planning + GTFS pubblico + RT — vendor attivo 2024 |
| **ATVO Venezia Orientale** | App "Venezia Veneto Turismo in Bus" aggiornata settembre 2025 — vendor attivo |
| **Busitalia Veneto** | App Pluservice + GTFS-RT pubblico su gtfs-biv.fsbusitalia.com — vendor attivo |
| **AST Sicilia** | App AST Ticketing (2024) — vendor attivo; AST è il regionale siciliano grande |
| **Etna Trasporti** | App dedicata + integrazione Moovle con RT — vendor attivo via Moovle/KentKart |
| **Autolinee Toscane** | App "at bus" (RATP Group, 2023, aggiornata) con trip planning completo — monopolio regionale |
| **Conerobus Ancona** | App ATMA (Pluservice, trip planning + ticketing) copre l'intera provincia Ancona — slot occupato |
| **ATP Esercizio Genova** | Filiale AMT Genova — troppo integrata nel sistema metropolitano genovese |
| **CUFFARO Autolinee** | Solo interurbano privato commerciale (Agrigento-Palermo), nessuna rete urbana/TPL strutturata |
| **Cotrap/Itineris** | App Itineris v2.1 (2025) con trip planning + RT + booking per tutti i 72 soci pugliesi |

---

## Sezione onesta: il mercato IT è davvero saturato?

**Risposta diretta: sì, per gli operatori medi. Rimane un nicchia reale ma piccola.**

Dopo due wave di scouting italiane (~50 operatori valutati in totale), il quadro è chiaro:

**Il layer superiore è chiuso.** Pluservice/myCicero e BusItalia/Ferrovie dello Stato dominano i bacini regionali medio-grandi con app aggiornate, trip planning integrato e rinnovi contrattuali attivi. Le regioni che non hanno ancora una soluzione unificata (es. Umbria con SALGO, FVG con TPLFVG) ce l'hanno già con i loro vendor storici.

**Il layer inferiore è fragile.** I candidati Tier 1A che restano sono operatori piccoli (4-35 linee) con vendite spesso fisiche e base digitale pressoché nulla. STPS Sondrio e Silvestri Livigno sono gli esempi canonici: GTFS presente ma app nulla o fossile. Tuttavia, il pricing €299/mese è già ai limiti per questi operatori: un extraurbano con 35 linee e 62 comuni serve probabilmente 500-1000 pendolari/die, il che rende l'argomentazione ROI non triviale (bisogna contare risparmio call-center + immagine istituzionale + accesso a fondi PNRR).

**Il GTFS è il collo di bottiglia reale.** In Italia, moltissimi piccoli operatori non pubblicano GTFS. Senza feed strutturato non c'è trip planning, e senza trip planning l'app è solo un PDF interattivo — poco differenziante rispetto a un sito mobile-responsive. Questo riduce ulteriormente il numero di candidati immediatamente attivabili.

**Stima realistica del mercato IT aggredibile oggi:** 8-12 operatori con tutti i requisiti allineati (slot aperto + GTFS presente o producibile + budget). Includendo i Tier 1B con potenziale a 18 mesi: forse 15-18 deal totali nel ciclo di vita del prodotto attuale.

**Raccomandazione strategica:** il mercato IT è insufficiente come bacino stand-alone per scalare il SaaS. Dopo aver chiuso i primi 4-6 deal italiani (orizonte 12-18 mesi), espansione EU è obbligatoria. Priority order suggerito:

1. **Portogallo** — piccoli operatori regionali (Rede Expresso franchisee, operatori insulari Açores/Madeira) con infrastruttura digitale bassa, ecosistema PT/ES condiviso, GTFS in crescita
2. **Spagna regionale** — fuori Cataluña/Madrid/Valencia (dove i vendor sono forti): Extremadura, Castilla-La Mancha, Murcia. Molti operatori privati concessori con app primitive o assenti
3. **UK regionali** — piccoli operatori bus fuori London (Bus Open Data Service li obbliga già al GTFS dal 2021, ma molti non hanno ancora una branded app decente)

Il UK è particolarmente interessante: il Bus Open Data mandate significa che il GTFS c'è già per tutti, abbassando drasticamente la barriera tecnica al lancio.
