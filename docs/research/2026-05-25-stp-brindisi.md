# STP Brindisi — Verifica Tier 1A
**Data:** 2026-05-25 | **Analista:** Claude Code (ricerca web live)

---

## TL;DR

**DECLASSATO a 2B.** STP Brindisi è formalmente consorziata Cotrap e coperta dall'ecosistema Itineris per biglietteria + prenotazione. Il gap reale (niente RT, niente trip planning degno, app on-demand ioki per un segmento di niche) esiste, ma il percorso decisionale per aggiungere un'app terza è bloccato dal consorzio. Operatore pubblico-controllato lento. Da monitorare se Itineris si dimostra fragile o se arriva gara regionale separata.

---

## 1. Copertura competitor

### Cotrap / Itineris
**SEGNALE CRITICO:** STP Brindisi è azienda consorziata Cotrap, elencata esplicitamente nella pagina ufficiale [S.T.P. Brindisi S.p.A. – Cotrap](https://www.cotrap.it/aziende_consorziate/s-t-p-brindisi-s-p-a/). Ha aderito al progetto Itineris il **1° febbraio 2021 in via sperimentale**, poi reso definitivo nel 2022.

> "Stp aderisce al progetto Itineris per la prenotazione online del viaggio tramite il sito www.cotrap.it, raggiungibile da tutti i dispositivi." — [Brindisi Report, 2021](https://www.brindisireport.it/attualita/stp-aderisce-progetto-itineris-prenotazione-online.html)

L'app Itineris Cotrap su Google Play ([id: itineris.puglia.cotrap.it](https://play.google.com/store/apps/details?id=itineris.puglia.cotrap.it)) include STP Brindisi e offre: prenotazione corsa, acquisto biglietti, QR di validazione, push notification su ritardi. **Non include trip planning multimodale né GTFS-RT tracking real-time del bus.** È un sistema di booking + biglietteria, non un'app passenger information completa.

Lo screen originale ("fuori dal consorzio Cotrap") era **errato**. STP Brindisi è dentro Cotrap.

### myCicero
Confermato solo ticketing. La [pagina dedicata](https://www.mycicero.it/stpBr/) e il sito STP descrivono myCicero come canale di acquisto e validazione biglietti per servizi urbani (Brindisi, Ostuni, Francavilla Fontana). Non ci sono funzioni di trip planning o RT tracking documentate per STP Brindisi.

> "L'app myCicero integra informazioni su orari, tempi di percorrenza e soluzioni su un'unica piattaforma" — la descrizione generica di myCicero include orari, ma per STP Brindisi non è verificabile una funzione di routing attiva.

### App branded operatore: Call Bus STP
Esiste un'app branded: [Call Bus STP](https://apps.apple.com/it/app/call-bus-stp/id6451437508) (App Store) / [Google Play](https://play.google.com/store/apps/details?id=com.ioki.brindisi) sviluppata da **ioki GmbH** (sussidiaria Deutsche Bahn per on-demand transit). Funzione: **esclusivamente servizio bus a chiamata** (on-demand), non orari fissi né trip planning. Ultimo update: maggio 2026 (v3.124.0). Rating: non sufficiente per essere mostrato (pochissime recensioni). Lanciata come "primo bus a chiamata in Puglia" da Francavilla Fontana.

ioki è vendor solido con contratto in corso — esclude TransitKit da questo segmento specifico.

### Moovit
**Copre STP Brindisi con orari statici** da GTFS. Ha pagine di linee aggiornate (es. Linea 1, 4, 8, OST-URBN, CIS-BRI). Non ha RT tracking per STP Brindisi (nessun feed GTFS-RT pubblicato — vedi §3). Utile per i turisti come strumento di planning, ma limitato alla consultazione.

### Google Maps Transit
**Copertura parziale e inaffidabile.** Una fonte editoriale ([Puglia Guys, 2025](https://www.pugliaguys.com/2025/07/25/public-transport-in-puglia/)) nota esplicitamente: "Google Maps has patchy coverage of interurban buses like STP" e "FSE routes and buses are not consistently shown" per destinazioni inland come Alberobello e Martina Franca. Il bus aeroporto-stazione risulta documentato in ritardo/anticipo senza aggiornamento RT.

### Sito operatore stpbrindisi.it
**Sito statico.** Nessun tracking real-time, nessun "dove è il mio bus", nessuna PWA, nessun tabellone dinamico. Orari in formato lista navigabile ma non interattiva. WhatsApp come unico canale "dinamico" per informazioni su orari.

### MaaS4Puglia / iniziativa regionale
La Regione Puglia ha lanciato **MaaS4Puglia** (fase 2 avviata novembre 2025) con sperimentazione nelle città inclusa Brindisi. Gli MaaS operator sono app di pagamento aggregato (MooneyGo, Urbi, Tabnet, WeTaxi) — non app passenger information branded. Nessuna app unica regionale che sostituirebbe un prodotto branded per operatore. La sperimentazione era in trial con crediti fino ad aprile 2026: stato post-trial non verificato.

### Open data Puglia — GTFS-RT
Nessun feed GTFS-RT trovato. Transitland riporta solo feed statico per STP Brindisi (vedi §3).

### Contratti vendor recenti
Nessuna evidenza di contratti con Pluservice, Wave Italia, Teseo o altri vendor app specializzati. L'unico vendor confermato per app branded è ioki GmbH (Call Bus STP, on-demand only).

---

## 2. Pain documentato

Le recensioni negative su TripAdvisor per STP Brindisi sono quasi tutte sul **servizio fisico**, non sull'esperienza digitale:

- ★1/5 — gennaio 2025: *"You keep delaying! You cannot rely on service. Bad almost all rude racist ignorant staff"*
- ★1/5 — marzo 2026: *"Absurd 25-minute delay, bad service"*
- ★1/5 — dicembre 2024: *"systematic delays and, in some cases, the complete absence of buses at scheduled stops"*
- ★2/5 — maggio 2024: *"last ride is set at 23:20, a time really too early for a tourist city"*

La Gazzetta del Mezzogiorno documenta un episodio di novembre 2024 in cui un gruppo di turisti ha rischiato di perdere l'aereo perché il bus STP è **partito in anticipo rispetto all'orario pubblicato sul sito e sulle app**.

> "Gli orari erano verificati sul sito della STP e sulle app disponibili in rete, ma il bus era arrivato e ripartito due minuti prima dell'orario previsto." — [Gazzetta del Mezzogiorno, 2024](https://www.lagazzettadelmezzogiorno.it/news/brindisi/1577188/brindisi-il-bus-stp-parte-in-anticipo-dalla-stazione-gruppo-di-turisti-rischia-di-perdere-laereo.html)

Questo conferma il gap RT: il problema non è che i dati digitali mancano del tutto, ma che sono statici e non riflettono la realtà operativa. Un'app con RT avrebbe potuto segnalare la partenza anticipata.

Il sito [oraristp.it](https://www.oraristp.it/) è un'iniziativa indipendente (team SVDEVTEAM, 3 persone) che ha creato una UI migliore sugli orari statici STP — segnale di domanda non soddisfatta, ma anche di low bar tecnica dell'operatore.

---

## 3. Segnali operativi

### GTFS
- **Feed statico attivo:** Transitland `f-s-societàtrasportipubblicibrindisi~ostuni`, **30 versioni archiviate**, ultima acquisita **1° maggio 2026** (copertura maggio-giugno 2026). Ultimo check di Transitland: 25 maggio 2026 (10 ore fa al momento della ricerca). Feed vivissimo.
- **GTFS-RT:** Non pubblicato. Nessun URL real-time trovato su Transitland, dati.puglia.it, o sito STP.

### Dimensioni
Linee urbane Brindisi: ~12 linee, 254 fermate. Linee extraurbane provinciali: ~30+ linee (Ostuni, Cisternino, Francavilla Fontana, Fasano, area Valle d'Itria). Ridership: non reperibile pubblicamente.

### Proprietà e velocità decisionale
Società per azioni a partecipazione pubblica prevalente: **Comune di Brindisi ~33%** + **Provincia di Brindisi** come soci principali. Regione Puglia e altri enti locali hanno facoltà di partecipazione dallo statuto. Non indipendente privato: processo decisionale tipicamente lento (CdA, delibere provinciali/comunali, atti amministrativi). Nessuna partecipazione SITA Sud confermata.

Il contratto con ioki GmbH per Call Bus STP implica che c'è capacità di firma con vendor privati, ma su perimetro on-demand specifico.

### Contratto di servizio regionale
Nessuna scadenza imminente trovata. STP Brindisi opera in regime di affidamento diretto (in-house) dalla Regione Puglia, tipico per operatori pubblici pugliesi.

---

## 4. Verdict

**DECLASSATO: Tier 2B** (da monitorare, non prioritario).

**Ragionamento:**
1. **Lo screen originale era errato su un punto chiave:** STP Brindisi è dentro Cotrap e ha Itineris per biglietteria + booking. Non è "fuori dal consorzio" — è consorziata dal 2021. Questo blocca l'angolo di attacco "l'unica app che copre l'operatore".
2. **Il gap digitale esiste, ma è parziale:** manca RT tracking, manca trip planning interattivo di qualità, manca app passenger information branded moderna. Itineris copre biglietteria+booking (decentemente), ioki copre on-demand. Il vuoto è nell'informazione passeggeri in tempo reale — ma colmarlo richiede prima che l'operatore pubblichi GTFS-RT, che non c'è.
3. **Velocità decisionale bassa:** ente pubblico con governance multi-stakeholder (Comune + Provincia), già vincolato a contratti attivi (ioki, Cotrap/Itineris). Ciclo di vendita lungo per un SaaS early-stage.

**Trigger per rientrare in Tier 1A:**
- Itineris/Cotrap perde il contratto o si dimostra tecnicamente fragile
- STP Brindisi avvia gara pubblica per app passenger information (monitorare ANAC/gare Puglia)
- STP Brindisi inizia a pubblicare GTFS-RT (renderebbe il wedge RT immediatamente attivabile)
- MaaS4Puglia fallisce o si ritira dalla provincia di Brindisi

**Se confermato, wedge angle:** turismo (Ostuni/Alberobello) + incidenti documentati (turisti che perdono l'aereo per bus in anticipo su orario). Ma prima serve RT — senza feed real-time, TransitKit non può differenziarsi da Moovit o Itineris su questo operatore.
