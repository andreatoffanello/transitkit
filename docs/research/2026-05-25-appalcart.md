# AppalCART Due Diligence — 2026-05-25

**Tier classification under review:** 1A (first go-to-market target)
**Analyst note:** All claims sourced below. "Unverified" used where evidence is absent.

---

## 1. Competitor Coverage

### Transit App
No evidence of AppalCART coverage. [Transit App's agency page](https://transitapp.com/agencies) lists 1000+ cities by example but not by full roster. Direct search returned zero hits for "AppalCART" or "Boone" on transitapp.com. AppalCART's own website, route page, and Live Transit page contain zero mention of Transit App. **Assessment: Transit App does not cover Boone NC.** (Unverified with certainty — app-level confirmation would require direct install; no web evidence of coverage found.)

### Moovit
No evidence of AppalCART coverage found. Moovit.com returns no results for AppalCART or Boone NC. AppalCART does not mention Moovit anywhere on its website. **Assessment: Moovit does not appear to cover Boone NC.** (Same caveat — unverified at app level.)

### Google Maps Transit
Google Maps does return AppalCART schedule-based transit directions for Boone — the GTFS static feed is live and appears to be ingested. However, [AppalCART's own route page](https://www.appalcart.com/route-maps-and-schedules) notes that "times shown in Google Maps may not reflect current bus status" and directs users to ETA SPOT for real-time data. GTFS-RT feeds exist but it is **unverified** whether Google Maps ingests them for live departures in Boone.

### Branded Operator App: ETA SPOT
This is the live competitor. AppalCART contracts with ETA Transit Systems' SPOT platform since August 2023.

- **App Store (iOS):** [ETA SPOT](https://apps.apple.com/us/app/eta-spot/id1021211544) — **1.5★ / 1,000 ratings** as of May 2026. Last updated November 5, 2025 (v2.2.26).
- **Google Play (Android):** [ETA SPOT](https://play.google.com/store/apps/details?id=com.etatransit) — 100K+ downloads; reported 3.0★ (lower confidence — Play scraping was blocked).
- The web tracker at [appalcart.etaspot.net](https://appalcart.etaspot.net/) is functional but is a thin map widget, not a PWA.

**No new contract or RFP for a replacement app was found for 2025–2026.** ETA SPOT remains the sole endorsed tool.

### Recent Contract Signal
No evidence of a new app contract, transit software RFP, or signed deal with any third-party (Swiftly, Remix, Citymapper, etc.) in 2025–2026. **Clean runway.** (Source: news search across Watauga Democrat, The Appalachian, High Country Press — no relevant hits.)

---

## 2. Documented Pain

### App Store Reviews (iOS ETA SPOT — verified 2025)

> **1★ — Sept 18, 2025 (hxkeivnsoxhs):** "BY FAR the worst one [transit app] compared to previous university apps. Poor UI design, confusing navigation, mandatory app restart to exit settings, no route saving, hidden notification features."

> **1★ — Mar 9, 2025 (Nitika998):** "The app is unusable — the format doesn't fit iPhone properly. Arrival times are 5+ minutes inaccurate. I have to use alternative apps and websites."

> **1.5★ — Jan 19, 2025 (tomessssss):** "Bus routes disappear and I can't view buses when selecting routes. No route saving, no reminders, timing is inaccurate."

The 1.5★ / 1,000-rating figure from the original 2026-04-01 screen is **confirmed current** (fetched May 25, 2026). The most recent reviews are from 2025, showing the situation has not improved post the November 2025 update.

### Reddit
Zero indexed Reddit threads found for "AppalCART" or "ETA SPOT" on r/AppState or r/Boone. **Unverified — subreddit may have private or unindexed posts.** Direct subreddit visit was not performed.

### The Appalachian (App State student newspaper)
The paper has covered AppalCART actively in 2024–2025: [double-decker bus](https://theappalachianonline.com/appalcart-adds-double-decker-bus-to-its-fleet/), [route changes](https://theappalachianonline.com/adjustments-made-to-appalcart-routes/), [Orange Night Owl route](https://theappalachianonline.com/appalcart-board-votes-to-adopt-orange-night-owl-route/). No recent app-focused criticism articles were found (direct page fetches returned 403). Coverage confirms the rider base is active and engaged.

### Watauga Democrat
Two recent council-report articles confirm operational health:
- [March 2026 annual report](https://www.wataugademocrat.com/news/boone-town-council-hears-annual-appalcart-update/article_ea290966-db69-11ef-9198-b76ca0fe0bf0.html): 1.42M trips in 2025 (down from 1.51M due to Hurricane Helene), 483,918 fixed-route trips YTD (up 114K from prior year).
- [Ridership milestone](https://www.wataugademocrat.com/news/local/appalcart-reports-nearly-1-5-million-trips-over-the-past-year/article_72e15c6c-0810-40be-9371-c4bbfa1d53a8.html): ridership tripled from 412K (2021) to 1.51M (2024).

---

## 3. Operational Signals

### GTFS Feeds (verified live — May 25, 2026)
- **Static GTFS:** `https://s3.amazonaws.com/etatransit.gtfs/appalcart.etaspot.net/gtfs.zip` — fetched by Transitland 7 minutes before query on 2026-05-25. **Live.**
- **Vehicle Positions RT:** `https://s3.amazonaws.com/etatransit.gtfs/appalcart.etaspot.net/position_updates.pb` — listed on [Transitland RT feed](https://www.transit.land/feeds/f-dnmy-appalcart~rt), last fetched 2026-05-25.
- **Trip Updates RT:** `...trip_updates.pb` — same source.
- **Service Alerts RT:** `...alerts.pb` — same source.
- All three RT feeds are on S3 (ETA Transit's infrastructure), not the realtime proxy pattern needed for TransitKit. Migration path: straightforward.

### Route Count
The route page lists 17 named fixed routes (Blue, Express, Gold, Gray, Green, Teal, Maroon, Orange, Pink, Pop 105, Purple, Red, Silver, State Farm Lot Shuttle, Wellness District + Break + Night Owl variants). Wikipedia cites 12–13 core fixed routes; the live page shows more with seasonal variants. **Use 13–17 as working range; exact count from GTFS.**

### Ridership
- 2024: 1.51M trips (tripled from 412K in 2021)
- 2025: 1.42M (Helene impact, now recovering — +114K YTD vs prior year)
- Source: Boone Town Council presentations, Watauga Democrat March 2026.

### Ownership & Funding
AppalCART is Watauga County's Public Transportation Authority (independent authority). Funding split (2024): State 44.9% · Appalachian State University 35.6% · Federal 16.1% · Town of Boone 2.0% · Watauga County 1.5%. ASU's 35.6% share is the key procurement lever — the university is operationally co-dependent and has formal board representation via ASU's Governance Committee.

### Decision Speed Risk
Multi-stakeholder funding (state + county + university) means procurement goes through a public authority board. Not a single-signature deal. Budget cycle visible: board votes reported publicly (Watauga Democrat, The Appalachian). **Expect 3–9 month procurement cycle** if they are interested.

### Strategic Context
Fleet electrification is the declared tech priority for 2025–2037 ($13.89M gap to full EV). Ridership growth (double-decker added August 2025, Night Owl route approved for fall 2025) signals operational confidence but absorbs leadership attention. No tech RFP or "passenger app" initiative was found in any 2024–2026 source. **Unverified whether app improvement is on the board's radar.**

---

## 4. Verdict

**Confirmed Tier 1A.**

Three signals converge: (1) the incumbent app — ETA SPOT — is actively broken at scale (1.5★ / 1,000 iOS ratings, reviews from as recently as March 2025 describing it as "unusable"), (2) there is zero coverage from Transit App or Moovit to fill the gap, and (3) GTFS + GTFS-RT are live and verified today, meaning technical onboarding is a known path with no unknowns. The rider base is large (1.4M+ annual trips), captive (university students with no car alternative), and demonstrably frustrated.

**#1 wedge for outreach:** the App Store reviews. Lead with "your riders gave ETA SPOT 1.5 stars and 1,000 people bothered to write about it — here's what they said" in the cold outreach email. The executive director can verify this in 30 seconds.

**Trigger to revisit:** if a signed ETA Transit contract renewal or a new transit software RFP appears in Watauga Democrat or on the AppalCART board agenda before the first outreach meeting.
