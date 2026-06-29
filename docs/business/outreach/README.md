# USA Outreach — sequencing guide

> Ordine consigliato di esecuzione, con razionali. Aggiornare con risposte effettive.
> Stato: 2026-05-26 — bozze pronte, **nessuna ancora mandata**.

## Sequenza consigliata (3-4 settimane)

### Settimana 0 (= prima di mandare qualsiasi email)
- [ ] **Verificare** che `transitkit.app` abbia una landing pubblica decente con: link store AppalRider iOS+Android, prezzo, contatti, Privacy + TOS minimi
- [ ] **Registrare** un Calendly link `cal.com/transitkit/30min` per discovery call
- [ ] **Comprare** dominio email professionale: `andrea@transitkit.app` (non Gmail) — vendor: Fastmail o Google Workspace
- [ ] **Preparare** one-pager PDF (vedi `one-pager.md`) — ospitato su `transitkit.app/one-pager.pdf`
- [ ] **Screen recording 60s** di AppalRider iOS (Loom o file mp4) — ospitato su `transitkit.app/demo` o YouTube unlisted

Senza questi 5 prerequisiti, le email vanno mandate ma rendono meno: il destinatario clicca il link Gmail signature → trova un sito vuoto → archive.

### Settimana 1 (priorità urgenza)
- [ ] **Pullman dev outreach** — `pullman-transit.md` step 1 — Sheheryar Pirzada (community dev)
  - Razionale: applichiamo il pattern community-replacement (<500 download = vantaggio + obbligo co-opt prima dell'operatore)
  - Outcome atteso: sì collaborazione → diventa local champion / no → procediamo direttamente all'operatore con consapevolezza del suo lavoro

### Settimana 2 (urgenza alta, finestra chiudibile)
- [ ] **BRTA outreach** — `brta-pittsfield.md` — Kathleen Lambert (administrator)
  - Razionale: lei ha pubblicamente dichiarato di voler perseguire ITS grant per rider app + Keolis prende ops 1 luglio
  - Mandare entro **20 giugno** per dare 10 giorni di window prima del takeover
- [ ] **Mendocino outreach** — `mendocino-transit.md` — GM (nome da identificare via sito MTA)
  - Razionale: 2024 SRTDP è in corso = procurement window aperta, app-center page raccomanda ancora RouteShout

### Settimana 3 (urgenza media, ma forte segnale)
- [ ] **Pullman operatore outreach** — `pullman-transit.md` step 2 — Brad Rader (Ops Supervisor) + Public Works director
  - Solo dopo aver chiuso (positivamente o negativamente) il dev outreach in settimana 1
  - Razionale: lui ha già detto pubblicamente che PTBusBeacon non funziona — il pitch parte già accettato

### Settimana 3-4 (motion separato)
- [ ] **AppalCART outreach** — `appalcart.md` — exec director + ASU Parking & Transportation
  - Razionale: motion diverso perché l'app esiste già. Serve **prima** seeding minimo per avere metriche download da citare ("X download organici in N settimane")
  - Prerequisito: seeding r/AppState + QR fermate campus per 2 settimane

## Hot list contatti urgenti

| Contatto | Org | Titolo | Telefono/Email | Note |
|----------|-----|--------|----------------|------|
| Kathleen Lambert | BRTA Pittsfield MA | Administrator | berkshirerta.gov contact form + ricerca LinkedIn | Director da dic 2025, background ARPA grants |
| Brad Rader | City of Pullman Public Works | Transportation Ops Supervisor | 509-332-6535 | Citato pubblicamente: "PTBusBeacon non funziona" |
| Sheheryar Pirzada | indep. iOS dev | author "Pullman Bus Tracker" | da cercare via LinkedIn/App Store dev page | Community dev — contattare PRIMA dell'operatore |
| MTA GM | Mendocino Transit Authority | General Manager | mendocinotransit.org/about (da verificare) | 2024 SRTDP cycle aperto |
| AppalCART ED | AppalCART Boone NC | Executive Director | appalcart.com (verificare nome) | + co-contact ASU Parking & Transportation |

## Metriche da tracciare per ogni outreach

In foglio separato (Google Sheet / Airtable):

| Operatore | Data invio | Canale | Aperta? | Risposta? | Esito | Note |
|-----------|-----------|--------|---------|-----------|-------|------|

Stack consigliato: Mailtracker per delivery + Hunter.io per email verification + Calendly per booking. Tutto free tier.

## Cosa NON mandare nelle email

- Allegati pesanti (PDF >2MB) — usare link
- Frasi tipo "rivoluzionare", "best-in-class", "leverage"
- Più di 1 link nel corpo (oltre alla signature)
- Lunghezze >180 parole
- Header HTML elaborati (segnale spam)
- "Hope this finds you well" — pleonastico
- "Quick question" come subject — bruciato

## Cosa funziona (testato in altri contesti SaaS B2B)

- Subject: specifico e con numero ("ATAP Lotto 5: l'app branded come componente offerta tecnica")
- Apertura: cita evidenza pubblica concreta che dimostra che hai fatto i compiti
- Corpo: 3 frasi su pain → 2 frasi su soluzione → 1 frase su prossimo passo (call 30 min)
- Chiusura: link Calendly + link store AppalRider come proof
- Signature: nome + ruolo ("Founder, TransitKit") + sito + telefono
