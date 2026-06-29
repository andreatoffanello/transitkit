# Scouting Italia — candidati net-new TPL
**Data:** 2026-05-25  
**Scope:** operatori italiani, esclusi STP Brindisi, ATAP Biella, SAF Duemila/VCO (in verifica separata)

---

## Metodologia

Ricerca condotta su tre vettori paralleli: (1) app store (Google Play / App Store) per operatori con app Pluservice/myCicero o proprietaria con rating basso, (2) Transitland e portali open data regionali (Marche, Veneto, Lombardia) per verifica GTFS, (3) stampa locale e TripAdvisor/Trustpilot per segnali di pain documentato. La verifica consortium ha controllato Cotrap/Itineris, MooneyGo, Bip+, MuoversiPiemonte, 5T, MUVT, CTPI, e Arriva Italia su ogni candidato. Esclusi i capoluogo grandi (ATM, ATAC, GTT, ANM) e operatori interprovinciali puri di lungo corso.

---

## Tier 1A — Best fit (2 candidati)

| Operatore | Area | # linee stimate | Bacino | GTFS | Stato app | Evidenza pain |
|-----------|------|-----------------|--------|------|-----------|---------------|
| **APAM Mantova** | Mantova, Provincia di Mantova (Lombardia) | ~15 urbane + 30+ extraurbane | Pendolari + studenti, ~50k ab. | Non trovato su Transitland/MobilityDB; dati su Moovit via terze parti | App proprietaria APAM mobile+ — **rating 1.6★** (49 rating iOS) / **2.5★** (89 rating Android). Sviluppatore: Convergence1. Nessun aggiornamento major recente. | "Fa schifo" nei commenti App Store; utenti segnalano dati orari sbagliati, ticket che scompaiono. Nessun GTFS pubblico trovato → Google Maps non la copre con trip planning nativo. |
| **Contram Mobilità** | Macerata (Marche) | ~20 linee provinciali | Pendolari + studenti universitari (Camerino, Macerata) | Feed Transitland esiste (Marche unified, c. 2016) ma 3533 giorni senza aggiornamento — de facto morto. URL ufficiale: `trasporti.marche.it/Downloads/opendata/` (TLS invalido). | App proprietaria — **rating 2★** (iOS, App Store Italia). Recensioni: "inutile, aggiornano ogni settimana ma non cambia nulla"; RT live "Unknown" nel 90% dei casi. | TripAdvisor dicembre 2024: "l'app non funzionava per nessuno quel giorno"; utenti segnalano orari completamente errati rispetto al sito web. Bacino universitario (Camerino ateneo) = pain acuto per studenti fuori sede. |

**Nota APAM:** il fatto che non pubblichino GTFS aperto è un wedge commerciale, non un blocco. Moovit li ha comunque indicizzati tramite scraping, ma Google Maps transit planning non li copre. Chi si abbona a TransitKit ottiene GTFS publishing come parte del servizio — questo è un valore aggiunto immediato e misurabile.

---

## Tier 1B — Watchlist (4 candidati)

| Operatore | Area | # linee | Bacino | GTFS | Stato app | Cosa deve cambiare |
|-----------|------|---------|--------|------|-----------|-------------------|
| **Riviera Trasporti (RT)** | Imperia, Sanremo (Liguria) | ~25 linee provinciali | Turismo costiero + pendolari frontalieri FR | Sconosciuto; non su Transitland | App lanciata ottobre 2025 (v1.0.9, iOS "no ratings yet"). Vendor: myCicero, ma app ancora fresca. | App praticamente senza rating — finestra di 6-12 mesi per entrare prima che myCicero radichi. **Ma:** RT è in concordato preventivo omologato agosto 2023, piano al 31/12/2025. Valutare solvibilità prima di qualsiasi deal. Se il concordato chiude con successo → candidata forte nel 2026. |
| **ATC Capri** | Isola di Capri (Campania) | 4-5 linee isolane | Turismo di lusso internazionale | Non trovato pubblicamente | ATC GO app (Pluservice): **2.4★** con 15 recensioni. Aggiornamenti attivi. | Volume basso (isola) ma premium-brand fit altissimo. Il problema: Capri è piccola, $299/mese è giustificato solo se ATC paga in quanto brand-value (app come concierge del turista). Da valutare solo se si punta a un tier pricing premium per isole turistiche. |
| **Autolinee Varesine (CTPI)** | Varese (Lombardia) | ~25 linee (urbane + extraurbane) | Pendolari + confine CH | GTFS su Transitland (Autolinee Varesine extraurbane/urbane — feed attivo verificato) | App CTPI — funzionante ma segnalata mancanza real-time dinamico rispetto a Como. Nessun rating basso estremo trovato. | Consorzio CTPI (Autolinee Varesine + Castano) = potenzialmente due decisori. In bacino Agenzia TPL Lombardia. Da chiarire se l'agenzia obbliga a usare una specifica piattaforma app. Watchlist finché non si chiarisce la governance. |
| **Etna Trasporti / Interbus** | Catania, Messina, est-Sicilia | 30+ linee regionali | Studenti + turisti (Taormina, Etna) | Non trovato su Transitland; AMTS Catania pubblica GTFS ma è operatore separato | App proprietaria Etnatrasporti (Google Play ID: `it.etnatrasporti.mobile`). Rating non ottenuto con certezza, ma nessun rating stellare trovato; servizio offline-ticket problematico da review TripAdvisor 2024-2025. | Fa parte del gruppo Interbus (grande aggregatore siciliano). Il rischio è che il gruppo abbia IT interno o contratti pluriennali. Verificare se l'app è gestita in-house o da vendor. Se vendor = opportunità. |

---

## Squalificati durante scouting

| Operatore | Motivo squalifica |
|-----------|------------------|
| **Adriabus Pesaro (Marche)** | App myCicero attiva, rating 4.9★ con 1.084+ recensioni — soddisfazione alta, nessun need. |
| **Dolomiti Bus Belluno** | Nuova app MaaS Veneto lanciata dicembre 2024 (integrazione bus+treno, Satispay); 15.000 utenti attivi; GTFS su Google Transit. Finestra chiusa. |
| **Autolinee Toscane** | Monopolista regionale Toscana con app propria. Troppo grande, fuori scope. |
| **ATV Verona** | Pluservice/myCicero attivo e funzionante (Ticket Bus Verona + myCicero). RT attivo. |
| **Trentino Trasporti / SAD Bolzano** | App regionali proprie, integrate con OpenDataHub AltoAdige, GTFS attivo — mercato saturo istituzionalmente. |

---

## Nota onesta sul mercato italiano

Il mercato italiano è genuinamente difficile per una proposta a €299/mese, e i candidati Tier 1A si fermano a 2 — non 5, non 8.

**Perché così pochi?**

Il problema non è la mancanza di operatori piccoli — ce ne sono decine. Il problema è la stratificazione delle piattaforme: quasi tutti i piccoli operatori TPL sono già su **myCicero/Pluservice** o **Moovit** per la parte orari, e chi non ce l'ha è spesso in un consorzio regionale (Agenzia TPL Lombardia, Busitalia Veneto, Arriva Italia) che impone la piattaforma dall'alto. Il vendor lock-in non è scelto dall'operatore — è spesso condizione del contratto di servizio con la Regione.

L'altro freno è strutturale: molti operatori extraurbani italiani sono pubblici (SpA a capitale pubblico al 100%) o in regime di affidamento in house, con decisioni IT che passano da gara pubblica o delibera di CdA. Un SaaS a €299/mese che non è nel capitolato di gara non viene comprato nemmeno se l'app fa schifo — si aspetta la prossima gara. Questo è esattamente il caso di Contram (ScPA con soci pubblici).

**APAM è il candidato più interessante** perché è SpA con partecipazione mista e ha un'app proprietaria sviluppata da un vendor terzo (Convergence1) che sta chiaramente non mantenendo. Il pain è documentato, il GTFS mancante è un valore aggiunto immediato, e Mantova non ha il profilo "grande capoluogo con IT team interno".

**Contram** è interessante ma il modello SCpA (Società Consortile per Azioni con soci pubblici) allunga i tempi decisionali. Il bacino universitario di Camerino è un segnale forte di pain, ma ci vuole un campione interno che spinga.

**Il finding più sorprendente:** non è che il mercato sia saturo di buone app — è saturo di **contratti myCicero/Pluservice** con app mediocri che gli operatori non possono cambiare facilmente perché il vendor è integrato anche nel sistema di biglietteria. Chi rompe con myCicero deve riscrivere anche il flusso di acquisto ticket. Questo è sia il moat di Pluservice sia la nostra opportunità: chi ha già la biglietteria separata (o non ce l'ha affatto) è più libero di muoversi sull'app. APAM usa Convergence1 per l'app ma vende biglietti fisici → nessun lock-in biglietteria myCicero.

---

## Summary

- **Tier 1A:** 2 candidati (APAM Mantova, Contram Mobilità Macerata)
- **Tier 1B watchlist:** 4 candidati (Riviera Trasporti, ATC Capri, Autolinee Varesine CTPI, Etna Trasporti)
- **Squalificati:** 5 durante scouting

**Top 3 pitch:**
1. **APAM Mantova** — App proprietaria a 1.6★, nessun GTFS pubblico (= Google Maps non li copre), vendor Convergence1 che non aggiorna. Pain documentato. Nessun lock-in biglietteria myCicero.
2. **Contram Mobilità Macerata** — 2★, bacino universitario Camerino (studenti fuori sede = utenti dipendenti dall'app), RT live broken. Timeline lunga per natura consortile.
3. **Riviera Trasporti Imperia** — App appena nata (ottobre 2025, zero rating), operatore in turnaround finanziario post-concordato. Finestra di 6-12 mesi, ma verificare solvibilità prima.

**Finding sorprendente:** il lock-in reale nel TPL italiano non è la dimensione degli operatori o la concorrenza di altre app — è il bundling biglietteria+orari di myCicero/Pluservice, che rende il cambio app costoso anche per chi vuole farlo. I candidati più aggredibili sono quelli che hanno già separato le due funzioni, come APAM.
