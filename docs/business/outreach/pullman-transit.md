# Outreach — Pullman Transit (WA)

> Due step **in sequenza obbligatoria**:
> 1. **Sheheryar Pirzada** (community dev) — settimana 1
> 2. **Brad Rader / Public Works** (operatore) — settimana 3, solo dopo aver chiuso step 1
>
> Razionale: pattern community-replacement (<500 download). Approcciare l'operatore senza prima allearsi con il dev rischia drama Reddit "startup vuole sostituire il mio lavoro" che brucia il brand.

---

## STEP 1 — Sheheryar Pirzada (community dev)

### Research

- Autore "Pullman Bus Tracker" iOS app (id6751259403)
- Released 2024, v1.0.11, rating 5.0★ / 10 ratings
- Indipendente, no city endorsement
- Segnale: ha investito tempo gratis = frustrato + capace + ha know-how locale

### Identificazione canale

1. App Store dev page → cerca link sito personale / contact
2. LinkedIn: ricerca "Sheheryar Pirzada Pullman" (probabilmente studente WSU o ex)
3. GitHub: cercare repo "pullman-bus-tracker" o simili
4. Twitter/X: stessa ricerca

### Email v1

**Subject:** Your Pullman Bus Tracker app — let's compare notes

**Body:**

Sheheryar,

I came across your Pullman Bus Tracker on the App Store while researching transit operators with poor official apps. You shipped a 5★ app where PTBusBeacon sits at 1.2★ with 90 ratings — that's a quiet but real proof of how broken the official tool is.

Short context on me: I run TransitKit, a small studio shipping branded transit apps for operators that ignore their rider experience. We built one for AppalCART NC last month (live on App Store + Play Store as AppalRider). Pullman Transit is on our shortlist.

I don't want to land on Brad Rader's desk and have him be reminded of your work in a weird way. Two questions:

1. Are you still actively maintaining Pullman Bus Tracker, or was it a one-off frustration project?
2. If we do approach the city about a paid official app, would you be interested in some form of collaboration — co-credit, paid advisory, or just a heads-up out of respect?

Open to a 20-min call. If you'd rather just say "leave it to me" or "leave Pullman alone," I'll respect either.

Andrea Toffanello  
Founder, TransitKit  
transitkit.app

### Outcome handling

**Caso A — risponde positivamente (collaborazione):**
- Offrire: $500-1500 una tantum per advisory + co-credit "originally inspired by Sheheryar Pirzada's community app" nelle app description
- Eventualmente: nostra app sostituisce sua + lui riceve credit + redirect dell'esistente
- Allinearsi su narrativa pubblica condivisa prima di approcciare il city

**Caso B — risponde positivamente ma "io vorrei portare avanti la mia":**
- Rispetto. Chiediamo: "Would you be open to having us as a contractor under your name, or as a co-author? Or should we just stay out of Pullman?"
- Se "stay out" → onorare. Pullman esce da pipeline. Annotare in STRATEGY.md.

**Caso C — non risponde in 10 giorni:**
- Mandare follow-up breve: "Quick last note — I'll proceed approaching the city next week unless you'd prefer otherwise. Last chance to chime in. No hard feelings either way."
- Se ancora silenzio → procedere step 2 ma menzionare apertamente l'esistenza della sua app nel pitch al city (NON minimizzare)

**Caso D — risponde negativamente / arrabbiato:**
- Apologize, ask what would feel fair. Forse compenso più alto + escludere wedge dal pitch (focus su altri aspetti, non sull'esistenza della sua app)
- Se proprio non c'è accordo → Pullman esce da pipeline, NON vale l'eventuale drama

---

## STEP 2 — Brad Rader / Public Works (city)

### Research

- Brad Rader, Transportation Operations Supervisor, City of Pullman Public Works
- Telefono: 509-332-6535
- Quote pubblica (Daily Evergreen, "Pullman Transit assessing routes, looking to change app"): da citare nell'email
- 10 routes, 1.4M UPT/anno, WSU 30k students
- Fare-free dal 1980 (uno dei primi USA)
- Vendor attuale: GMV Syncromatics (contract end date NON verificato)
- Decision chain: Public Works → City Council per budget

### Identificazione canale email

1. Sito City of Pullman: pullman-wa.gov/government/departments/public_works
2. Pattern email probabile: `brader@pullman-wa.gov` (verificare con Hunter.io)
3. Telefono diretto: 509-332-6535
4. Backup: contattare Public Works director (nome da sito)

### Email v1 (mandare DOPO chiusura step 1)

**Subject:** PTBusBeacon replacement — branded app live in 4 weeks

**Body:**

Brad,

You were quoted in the Daily Evergreen saying the current app "is just not working." App Store agrees: PTBusBeacon sits at 1.2★ with 90 ratings, and a frustrated WSU student shipped their own replacement last year that's already outscoring it.

I'm Andrea, founder of TransitKit. We ship branded iOS + Android apps for transit operators on top of existing GTFS feeds — your real-time stays on Syncromatics, your operations stack stays exactly as it is. We just replace the broken rider-facing app with one Pullman Transit owns and controls. Live example for AppalCART NC: [AppalRider iOS](https://apps.apple.com/app/appalrider/idXXXX) and [Play Store](https://play.google.com/store/apps/details?id=com.transitkit.appalcart).

Pricing: $0 setup, $299/month, 30-day trial, deployable in 4 weeks. Below your competitive bid threshold for direct services procurement.

[For transparency, I've already coordinated with Sheheryar Pirzada, the dev behind the community Pullman Bus Tracker. He's [supportive / agreed to advisory / requested we focus on co-existence] — happy to share that context on a call.]

Can I send a 60-second demo and grab 20 minutes? [calendly.com/transitkit/30min](https://calendly.com/transitkit/30min)

Andrea Toffanello  
Founder, TransitKit  
transitkit.app · +39 [PHONE]

**Nota redazionale:** la frase in [parentesi quadre] va personalizzata in base all'esito step 1. Se Sheheryar non risponde, ometterla del tutto. Se ha collaborato, citarlo positivamente. **MAI** mentire sul suo coinvolgimento.

### Follow-ups (v2, v3)

Stesso pattern di BRTA: v2 dopo 7 giorni, v3 dopo altri 10 giorni con tono "closing the loop". Adattare i contenuti specifici al feedback ricevuto.

### Domande probabili (preparare risposte)

- "What about the Syncromatics contract?" → "We co-exist. Syncromatics keeps powering vehicle tracking backend; we replace only the rider-facing app. Zero conflict, no procurement RFP needed."
- "How is this different from Transit App?" → Transit App aggregates many cities, we give you a branded operator-owned channel with push alerts you control. Better for WSU semester messaging.
- "What about the Pullman Bus Tracker that already exists?" → [dipende da esito step 1]
- "How much disruption to riders?" → "Day-1 of launch: branded app appears in store, old one still works. Riders migrate organically as they find the new one. You can announce it via WSU channels."
- "Does it work on iPhone SE / old Android?" → "iOS 16+ and Android 8+. Covers 95%+ of active devices on WSU campus."

### Discovery call goals

1. Confirm Syncromatics contract end date / renewal terms
2. Confirm tech budget owner (Public Works director vs City Manager vs Council)
3. Identify ASU equivalent — chi è la controparte WSU per partnership? Office of Student Life? Parking & Transportation Services?
4. Confirm if fare-free creates any specific app requirements (no ticketing → simpler scope, cheaper for us, easier to sell)
