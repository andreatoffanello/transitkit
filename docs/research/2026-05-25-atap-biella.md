# ATAP S.p.A. — Verifica Tier 1A
**Data:** 2026-05-25 | **Stato:** Confermato Tier 1A con segnale d'urgenza aggiuntivo

---

## 1. Copertura competitor

### App branded operatore
Nessuna app branded ATAP su App Store o Play Store. Nessun risultato cercando "ATAP Biella", "ATAP Vercelli", "Trasporti Biella" sui due store. Il sito [atapspa.it](https://www.atapspa.it) rimanda esclusivamente a **MooneyGo** e **MyCicero** per l'acquisto biglietti — entrambe ticketing-only, nessun trip planning, nessun RT.

### MuoversiPiemonte
Portale web puro, nessuna app mobile companion. Integra ATAP nella sezione avvisi variazioni servizio (testo, non dati strutturati). Non offre trip planning strutturato né RT per le province di Biella/Vercelli. Fonte: [muoversinpiemonte.it](https://www.muoversinpiemonte.it).

### 5T / BIP app
L'app [BIP Piemonte](https://play.google.com/store/apps/details?id=com.fivet.bip) (sviluppatore: 5T srl) è limitata alla **ricarica della tessera**. Rating 3.0/5 su 180 recensioni. Nessuna funzionalità di trip planning o orari integrati. 5T copre Torino e Piemonte come infrastruttura MaaS, non come app consumer per Biella/Vercelli.

### MaaS Piemonte
Il progetto "MaaS for Piemonte" (PNRR, fase sperimentale 2025) integra **ATAP Biella** tra i sei operatori TPL coinvolti, ma tramite le super-app MooneyGo, Urbi, Wetaxi, ACI-Sara Go — orientate al multimodale/ticketing, non a orari+RT branded per l'operatore. Non è un competitor diretto per un'app branded.

> "Il bus service è attualmente incentivizzato per: Arriva, Bus Company, ATAP Biella, SUN Novara" — [fitconsulting.it su MaaS Piemonte](https://www.fitconsulting.it/maas-la-regione-piemonte-avvia-la-sperimentazione-di-maas-for-piemonte/)

### Moovit
Copre linee ATAP Biella (300, 340, 350, 360 e altre) con orari statici da GTFS. La funzione "Live Location" di Moovit dipende dalla disponibilità di GTFS-RT da parte dell'operatore — **ATAP non pubblica GTFS-RT** (confermato da Transitland: solo Static GTFS). Moovit copre ma con dati statici, nessun real-time.

### Google Maps Transit
Le linee ATAP compaiono su Google Maps Transit (i dati GTFS vengono ingestiti via Transitland/partner). Confermato da risultati di ricerca e pagine Moovit. Tuttavia: solo orari statici, nessun RT, nessuna app branded.

### Trip planner su atapspa.it
Il sito ha un journey planner embedded, ma con **disclaimer esplicito di anomalie tecniche** e risultati non ottimali per percorsi che coinvolgono operatori non-ATAP. Usabilità bassa: orari frammentati tra tre sezioni (urbano Biella, urbano Vercelli, extraurbano), formato PDF scaricabile.

### Magellano Progetti
Il vendor `magellanoprogetti.it` che pubblica il GTFS (`http://www.magellanoprogetti.it/atap/google_transit.zip`) è specializzato in **cartografia territoriale**, non in app di trasporto. Non ha prodotti consumer. Ruolo: solo backend di pubblicazione dati.

### Vendor recenti (Pluservice, Wave, Teseo)
Nessuna evidenza di contratti recenti con vendor di app TPL (Pluservice, Wave Italia, Teseo) per ATAP. L'unica novità digitale recente trovata risale al 2016 (AEP Ticketing per BIP).

---

## 2. Pain documentato

### Trustpilot + Google Maps
Trustpilot: **solo 2 recensioni** su atapspa.it — una critica esplicita a frequenze insufficienti e sito internet non accessibile. Google Maps sulla sede ATAP: **145 recensioni, media 3/5** — segnale di insoddisfazione diffusa ma non esplosiva.

### Lamentele documentate (stampa locale)
- **Navette Piazzo e Città Studi** (Newsbiella, 16 aprile 2026): il Comune convoca ATAP perché i veicoli in servizio sono inadeguati per le strade strette del centro storico. Discussione focalizzata su acquisizione mezzi, nessun cenno a strumenti digitali.

  > "Il Piazzo shuttle non riesce a raggiungere Piazza Cisterna e si ferma a Piazza Cucco." — [newsbiella.it, 16/04/2026](https://www.newsbiella.it/2026/04/16/leggi-notizia/argomenti/biella/articolo/navette-piazzo-e-citta-studi-comune-e-atap-a-confronto-sul-servizio.html)

- **Allarme truffe** (Newsbiella, giugno 2025): ATAP avverte di una falsa pagina Facebook che vende abbonamenti fasulli. Segnale che il brand ATAP è abbastanza riconoscibile da essere impersonato — e che l'utenza interagisce via social, non via app ufficiale.

### UPO Vercelli
Gli studenti UPO under 26 beneficiano di trasporto gratuito urbano tramite il progetto **Piemove** (Regione Piemonte, dal 2025). Nessuna lamentela documentata specifica su strumenti digitali ATAP in ambito universitario — ma il programma Piemove implica un'utenza studentesca attiva sui mezzi ATAP.

### Reddit / Facebook
Nessun thread Reddit rilevante trovato. Non sono stati verificati direttamente i gruppi Facebook locali (Sei di Biella se, Pendolari Biella), ma l'assenza di risultati indicizzati suggerisce che le lamentele digitali siano disperse e non virali.

---

## 3. Segnali operativi

### GTFS: feed attivo, 102+ versioni, aggiornato oggi
Il feed `f-u0j-atap` su [Transitland](https://www.transit.land/feeds/f-u0j-atap) ha **oltre 102 versioni archiviate**. L'ultima versione è stata fetchata il **2026-05-25** (oggi, meno di 12 ore fa). URL sorgente: `http://www.magellanoprogetti.it/atap/google_transit.zip`. Il vendor aggiorna con cadenza regolare.

> "The most recent version was fetched on 2026-05-25 (12 hours ago)." — Transitland, feed details ATAP

### GTFS-RT: assente
**Nessun feed real-time pubblicato.** Confermato da Transitland (solo Static GTFS) e dall'assenza di qualsiasi menzione su atapspa.it. Moovit mostra la funzione "Live Location" generica ma senza dati RT da ATAP.

### Dimensioni operative
- **Dipendenti:** 206-207 (fonte: Wikipedia 2024, ufficiocamerale.it 2026)
- **Fatturato:** €15,9M (2024)
- **Province:** Biella, Vercelli, + tratte extraurbane in provincia di Torino
- **Linee:** urbane Biella (linee 300-360+), urbane Vercelli, extraurbane (inclusa linea 500 Biella-Carisio-Milano)

### Proprietà
Società per azioni a capitale interamente pubblico: **Provincia di Biella** (azionista principale), Provincia di Vercelli, 116 Comuni, 5 Comunità Montane. Decisione del CdA tramite assemblea soci. Velocità decisionale tipica della PA, ma presenza di Consorzio stabile pubblico — nessun private equity che complica i deal.

> "La Provincia di Biella deve individuare candidati per il CdA della partecipata ATAP S.p.A." — [provincia.biella.it, avviso CdA](https://www.provincia.biella.it/notizie/avviso-cda-atap)

### Gara TPL Piemonte — segnale d'urgenza critico
Il **21 maggio 2026** l'Agenzia Mobilità Piemontese ha pubblicato in GUUE l'avviso di preinformazione per **8 lotti** TPL regionali. Il **Lotto 5** (Bacino Nord-Est) copre Biella e Vercelli — circa 3,4M vett-km annui, compensazione ~€6,7M/anno. Scadenza presentazione manifestazioni di interesse: **17 giugno 2026**.

Questo significa che il contratto di servizio in essere con ATAP è **in scadenza** e il servizio andrà a gara. L'operatore vincitore del Lotto 5 potrebbe essere ATAP stessa (gestore uscente) o un operatore concorrente (Arriva, Busitalia, ecc.). Implicazione per TransitKit: ATAP ha interesse a presentarsi alla gara con strumenti digitali competitivi, inclusa un'app — **la pressione concorrenziale è un acceleratore di decisione**.

> "Lotto 5, Bacino Nord Est: ~3,4 milioni di vett-km annui, compensazione ~€6,7M/anno, province Biella e Vercelli." — [autobusweb.com, 2026-05-21](https://www.autobusweb.com/gara-servizio-tpl-piemonte-8-lotti/) e [federmobilita.it](https://federmobilita.it/2026/05/21/tpl-piemonte-al-via-la-stagione-delle-gare-pubblicato-avviso-unitario-per-8-lotti/)

---

## 4. Verdetto

**Confermato Tier 1A — con upgrade a priorità immediata.**

ATAP Biella/Vercelli ha tutti i segnali originari confermati: GTFS aggiornato quotidianamente (102+ versioni, feed vivo oggi), nessuna app branded, competitor limitati a ticketing o portali web, 206 dipendenti, €15,9M fatturato, bacino studenti UPO + pendolari interprovinciali.

Il segnale nuovo e critico è la **gara TPL Piemonte pubblicata il 21/05/2026**: ATAP deve dimostrare di essere un operatore moderno per difendere il Lotto 5. L'assenza totale di GTFS-RT e di app propria è una debolezza oggettiva nell'offerta tecnica. La finestra di contatto è adesso — entro giugno 2026 stanno lavorando sulla proposta.

**Wedge primario:** pendolari Biella-Vercelli e studenti UPO Vercelli (segmento captive, abbonamento mensile, utilizzo quotidiano). Secondario: turismo lago Viverone e accesso Oasi Zegna dalla rete extraurbana.

**Trigger che lo farebbe squalificare:** aggiudicazione Lotto 5 a operatore large-cap (Arriva, Busitalia) che porta già la propria piattaforma app in-house.

**Prossimo passo:** contatto diretto con direzione ATAP entro la prima settimana di giugno, prima che la presentazione per la gara assorba tutta la bandwidth. Interlocutore: Direttore Generale o responsabile innovazione/IT. Leva: "la gara vi chiede modernizzazione digitale — TransitKit è la risposta in 4 settimane, non in 18 mesi di sviluppo custom."
