# Outreach — Mendocino Transit Authority (CA)

> Target: **General Manager** (nome da identificare via mendocinotransit.org/about)
> Urgenza: ALTA — 2024 SRTDP (Short Range Transit Development Plan) in corso = procurement window aperta
> Razionale wedge: app-center page raccomanda ancora RouteShout; SRTDP è il momento istituzionale per includere un'app rider

---

## Research

- 9 routes incluse rotte intercounty (Ukiah ↔ Fort Bragg, Mendocino coast)
- ~140k UPT (FY 2022-23: 139,627)
- 40 veicoli
- Area: turismo + pendolari costa Mendocino, copre anche tratte rurali
- GTFS publisher: https://mendocinotransit.org/developer/
- App Center page: https://mendocinotransit.org/app-center/ — RouteShout ancora listato (2024)
- GTFS-RT live e attivo
- 2024 SRTDP = ciclo procurement aperto
- Decision chain: General Manager → MTA Board of Directors → CalTrans (per fondi state)

## Identificazione contatto

1. Sito: https://mendocinotransit.org/about/ — sezione "Staff" / "Administration"
2. Phone main: cercare su footer sito
3. Pattern email probabile: `gm@mendocinotransit.org` o `[firstname]@mendocinotransit.org`
4. LinkedIn: ricerca "Mendocino Transit Authority General Manager"

## Email v1

**Subject:** Mendocino SRTDP — branded rider app for coastal + commuter routes

**Body:**

Hi [NAME],

I noticed two things while researching MTA: (1) your App Center page still recommends RouteShout 2.0, which Apple hasn't seen an update from since April 2019 and now sits at 1.6★ on the App Store, and (2) the 2024 SRTDP is open, which is the natural moment to lock in a rider-facing mobile decision.

I'm Andrea, founder of TransitKit. We ship branded iOS + Android apps for transit operators on top of existing GTFS feeds — no operations stack changes, no rip-and-replace. Live example for AppalCART NC: [AppalRider iOS](https://apps.apple.com/app/appalrider/idXXXX) and [Google Play](https://play.google.com/store/apps/details?id=com.transitkit.appalcart).

For MTA specifically: your GTFS-RT is already live and clean (great — most operators we approach aren't there yet), and the Ukiah ↔ Fort Bragg corridor plus the tourist-coast routes are exactly the high-information-asymmetry context where a branded app earns its keep.

Pricing: $0 setup, $299/month per deployment, 30-day trial. Below most direct-procurement thresholds.

Worth a 20-minute call to map this against your SRTDP timeline? [calendly.com/transitkit/30min](https://calendly.com/transitkit/30min)

Andrea Toffanello  
Founder, TransitKit  
transitkit.app · +39 [PHONE]

## Follow-ups

**v2 (dopo 7 giorni):**

Subject: Re: MTA SRTDP — quick note

Hi [NAME] — following up. One thing worth flagging: the SRTDP public-comment period typically locks vendor decisions for 3-5 years downstream. If a mobile rider experience isn't formally scoped now, it tends to slip to the next cycle. Happy to be ignored if timing is wrong, but a 15-minute call costs you very little to confirm fit/no-fit.

[calendly.com/transitkit/30min](https://calendly.com/transitkit/30min)

Andrea

**v3 (dopo altri 10 giorni):**

Subject: Closing the loop

Hi [NAME] — last note. If SRTDP doesn't include scope for a rider app this cycle, I'll re-reach out when MTA next publishes a transit RFP. Otherwise, happy to be a resource at any point. Best of luck with the planning process.

Andrea

## Domande probabili

- "Where do you fit in our SRTDP language?" → "Likely under 'Customer Information Systems' or 'Passenger Amenities' — happy to draft suggested SRTDP language to make it easy."
- "We have very rural ridership without smartphones — is this worth it?" → "App + web stop pages with QR. Riders without smartphones use the web pages from any browser. Same backend."
- "What about Spanish localization?" → "Built-in. iOS and Android both ship multilingual; web stop pages auto-detect browser locale."
- "Can you integrate with our existing fare system?" → "We don't replace fare collection. We integrate with existing ticketing vendors (Token Transit, Masabi, etc.) via deep-link, or we leave fare entirely out if you collect on-board."
- "What about the intercounty MTA route to BART?" → "Real-time will continue working as long as your GTFS-RT feed includes the route. We display it natively."

## Discovery call goals

1. Confirm SRTDP timeline + when vendor scope locks
2. Identify CalTrans procurement constraints (state funding rules)
3. Confirm GM authority vs Board approval thresholds
4. Tourist vs commuter ridership split — affects feature priority (multilingual, attractions integration)
