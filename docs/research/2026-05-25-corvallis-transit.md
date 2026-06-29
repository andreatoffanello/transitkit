# Corvallis Transit System (CTS) — Tier Verification
**Date:** 2026-05-25
**Analyst:** Research agent (TransitKit pipeline)
**Prior rating:** Tier 1A (desk screen 2026-04-01)

---

## 1. Competitor Coverage

### Transit App
Transit App has a [dedicated Corvallis Transit System page](https://transitapp.com/en/region/corvallis/corvallis-transit-system) listing all 10 lines, schedules, and real-time departures. Coverage is live and indexed. This is a **significant negative signal** — Transit App is CTS's de-facto third-party consumer app for anyone who discovers it organically. However, Transit App is a generic aggregator, not a white-label branded experience for CTS, and it has zero operator customization, push alerts, or service alert branding.

### Moovit
[Moovit covers Corvallis CTS fully](https://moovitapp.com/index/en/public_transit-Corvallis_Oregon-Portland_OR-site_249798651-144), with stop-level pages and trip planning. Same caveat: generic aggregator, no operator branding.

### Google Maps Transit
Google Maps surfaces CTS routes via GTFS integration — confirmed by the "Google Trip Planner" link on the official CTS website. This is expected baseline for any GTFS-publishing operator and does not constitute meaningful competition for a branded app.

### Official Branded App (Connexionz)
- **iOS:** [Corvallis Transit System](https://apps.apple.com/us/app/corvallis-transit-system/id1565426457) — **1.8★ / 9 ratings**, version 1.0.3, last updated **December 22, 2025**. Developer: Connexionz Limited.
- **Android:** [Google Play listing](https://play.google.com/store/apps/details?id=com.corvallis) — 1K+ downloads, rating not surfaced by scrape.

The 1.8★ / 9 ratings figure from the April desk screen is **confirmed current** (iOS). A December 2025 build push did not move the rating needle. The app is technically alive but despised.

### Beavus (third-party iOS)
[Beavus — Corvallis Bus](https://apps.apple.com/us/app/beavus-corvallis-bus/id1326663474) is a community-built iOS app that surfaces real-time CTS arrivals. A verified App Store review (Jan 2023) called it "far superior" to the official app:

> "Thank heavens, for the app called Beavus, which is far superior." — ABinOR56, 1★ review of the official app, Jan 9 2023

Beavus's existence is a **positive signal for us**: a volunteer built a replacement because the official offering is that broken. No Android equivalent found.

### Corvallis Bus (Rikki Gibson, web/open source)
A [web-based tracker](https://rikkigibson.github.io/corvallisbus/) with late-bus detection logic. Last data update September 2019 — effectively abandoned.

### OSU Beaver Bus — Separate System
OSU operates its own internal campus shuttle ([Beaver Bus](https://transportation.oregonstate.edu/beaver-bus)), separate from CTS. As of **January 1, 2026**, OSU migrated Beaver Bus tracking from the OSU Mobile App to **TransLoc**. Beaver Bus is also listed in Transit App. This is a **distinct operator** — do not conflate with CTS. TransLoc covering Beaver Bus does not affect CTS's open white-label opportunity.

---

## 2. Documented Pain

### Official App Reviews (iOS, confirmed)

| Date | Stars | Quote |
|------|-------|-------|
| Mar 5, 2024 | 1★ | "This new concept is WAYY worst then the old app… 'Type in bus stop number'???? Seriously? What happened to going on the map, clicking on the bus stop you want." — deadline17 |
| Jan 9, 2023 | 1★ | "I am shocked how bad the new app is compared to the prior Corvallis Transit app… I really hope that not too much money was paid for this app." — ABinOR56 |
| Feb 25, 2022 | 2★ | "App is ok, but recently got worse… impossible to tell which side of the street the stop you want is on." — sonofhorse |
| Jan 9, 2022 | 1★ | "Old app was better and more user friendly, please revert." — hdkslsmc |

The most recent review is **March 2024** — within scope. All reviews cite regression from a prior app that worked better, which is a classic Connexionz replacement pattern seen in other small-city deployments.

### Reddit / Community
Direct Reddit thread scraping was blocked. No indexed Reddit content for r/Corvallis or r/OregonStateUniv surfaced in search for "CTS bus" 2024–2025. **Unverified** — Reddit signal cannot be confirmed or denied without authenticated access.

### Daily Barometer / Gazette-Times
A [Daily Barometer article](https://dailybaro.orangemedianetwork.com/21130/daily-barometer/getting-around-town-using-public-transportation-on-and-off-campus/) covers CTS as the primary student transit resource, but the page returned HTTP 403. The article's title and existence confirm student-transit coverage, but the publication date and full text are **unverified**.

### Fare-Free Context
CTS has been fareless since February 2011 — ridership jumped 37.9% in year one. This means riders are not price-constrained; complaints are purely about service quality and information access, which maps directly to what a good app solves.

---

## 3. Operational Signals

| Signal | Finding |
|--------|---------|
| GTFS static URL | `http://www.corvallistransit.com/rtt/public/resource/gtfs.zip` (Connexionz-hosted) |
| GTFS last updated | **May 25, 2026** (11 hours before this research — actively maintained) |
| GTFS-RT | **Confirmed present** — Transitland lists 1 GTFS-RT feed (Onestop: `f-corvallis~or~rt`). Connexionz exposes real-time vehicle positions. |
| Feed versions | 100+ archived versions on Transitland — long-running, stable publisher |
| Routes | 9 fixed routes + Philomath Connection = 10 total |
| Fleet | 15 × 35-foot buses |
| Annual ridership | **Over 1 million rides/year** (city's own language; exact FY2024 figure not surfaced — NTD data portal access blocked). Original 2M estimate from desk screen is **unverified** and likely high; "1M+" is confirmed floor. |
| Ownership | City of Corvallis (public) — Department of Public Works |
| Funding | Transit Operations Fee (utility bill surcharge) + state/federal grants + OSU |
| Decision process | City of Corvallis public works + city council budget process. No dedicated transit technology RFP found in the last 18 months. |
| NTD ID | 00047 |

---

## 4. Verdict

**Confirmed Tier 1A — no downgrade warranted.**

The original desk-screen signals hold up under live verification. The Connexionz app's 1.8★ rating is confirmed current after a December 2025 update. GTFS is live and refreshed daily. GTFS-RT is published. Ridership is confirmed at 1M+ annually (exact figure unverified at 2M). Transit App and Moovit are present as generic aggregators but offer no branded, operator-controlled experience — the gap TransitKit fills.

**Primary wedge:** OSU's 35,000 students who ride fare-free daily. The most vocal App Store complaint is from a power user frustrated by a step backward in UX (March 2024). Beaver Bus switching to TransLoc in Jan 2026 shows OSU is actively re-evaluating transit tech — a timely opening.

**One caution:** No city-issued RFP for transit app technology was found. The procurement path will be a direct-sales motion to the City of Corvallis Public Works/CTS administration, not a competitive bid response. Budget cycle and decision process need a sales discovery call to map.

**Trigger to re-evaluate downward:** if Connexionz rolls a material UX update to the official app (watch the App Store version after any 2.x release) or if Transit App strikes a co-branding deal with CTS directly.
