# Pullman Transit (City of Pullman, WA) — Verification Report

**Date:** 2026-05-25  
**Status at entry:** Tier 1A (desk screen, 2026-04-01)  
**Verdict:** **Confirmed Tier 1A — stronger than original screen suggested**

---

## 1. Competitor Coverage

### Transit App
Transit App **does cover Pullman Transit** with real-time data, serving all routes across 133 stops. The page at [transitapp.com/en/region/pullman/pullman-transit](https://transitapp.com/en/region/pullman/pullman-transit) lists 8 bus lines (Airport, Blue, Community Service Express, Lentil, Loop, Paradise, Silver, Wheat) and promotes "active service changes." However, Transit App is **not an official agency partner** — Pullman Transit does not appear in the Transit App agency partner list at transitapp.com/agencies, which lists "180+ agency partners." This is a GTFS consumption integration, not an endorsed partnership. Transit App is a threat but not a killer: it does not carry the Pullman Transit brand, offers no operator-level customization, and does not send operator-controlled push alerts.

### Moovit
No Moovit coverage page for Pullman, WA found in indexed search results. The Moovit site-search returns only Washington D.C./Baltimore and Seattle-Tacoma metro pages for "Washington transit." **Unverified** — Moovit may ingest GTFS passively but no dedicated Pullman coverage found.

### Google Maps
Google Maps does carry Pullman Transit — confirmed by a [Daily Evergreen partnership article](https://dailyevergreen.com/18840/news/cc-pullman-transit-partners-with-google-maps/) and current search results showing bus routes with departure times. Integration is schedule + real-time (GTFS-RT is live, see §3). Google Maps is table stakes, not a differentiator.

### Branded Apps: PTBusBeacon (official) + third-party
- **PTBusBeacon** — official app by GMV Syncromatics (Syncromatics acquired by Spanish firm GMV in 2015). App Store: [id1329034348](https://apps.apple.com/us/app/ptbusbeacon/id1329034348). **Rating: 1.2★ / 90 ratings — confirmed current as of 2026-05-25.** Last updated September 13, 2025 (v4.14.0). The rating and count match the original desk screen; this is not a stale figure.

  > "This is the first review I've ever written because I am so sick of this app not functioning...Missed multiple busses because of this." — Lolmclolster, App Store, 08/27/2024 ★1

  > "Spoty. Sometimes you have to open it several times to actually get it to load. Time updates are inaccurate at times." — Keena84, App Store, 06/02/2025 ★2

  > "The recently updated app is better than nothing, but it takes a long time to load & doesn't offer a consistent or reliable view of updated times." — eanicol, App Store, 09/20/2021 ★2

- **Pullman Bus Tracker** — independent third-party app by individual developer Sheheryar Pirzada ([App Store id6751259403](https://apps.apple.com/us/app/pullman-bus-tracker/id6751259403)). Released 2024, v1.0.11. Rating: 5.0★ / 10 ratings (negligible sample). Not official, not city-endorsed. Signals that community frustration with PTBusBeacon is high enough to motivate an indie developer build.

- **CityTransit** — generic aggregator, low prominence, not agency-endorsed.

### WSU-Internal Shuttle Services
Distinct from Pullman Transit: WSU runs **CATS** (Cougar Accessible Transportation — paratransit), the **Lewiston Airport Shuttle** (separate booking), **Wheatland Express** (intercity to Spokane — unrelated), and an **Emergency Ride Home** program. The four on-campus **Express Routes** operated by Pullman Transit for WSU are city-operated, not WSU-internal — the city contracts these and they appear in PTBusBeacon and GTFS feeds. No competing WSU internal app found for these routes.

### Operator Web Tracker / PWA
[pullmanbusbeacon.com](https://pullmanbusbeacon.com) is a GMV Syncromatics-hosted web map — effectively a web version of PTBusBeacon. No independent city-built PWA or "Where's My Bus" tracker found. The city website (pullman-wa.gov) appears to link to the GMV tool and the app stores only.

### Recent Contracts / News
A search result (attributed to the Daily Evergreen article ["Pullman Transit assessing routes, looking to change app"](https://dailyevergreen.com/127014/news/pullman-transit-assessing-routes-looking-to-change-app/)) surfaced a key quote from Brad Rader, Pullman Transportation operations supervisor:

> "The mobile app is something that I'm going to address and perhaps use something different because it's just not working."

The article confirms Pullman Transit was in the final year of its Syncromatics contract at the time of writing. The PTBusBeacon update history (v4.14.0, September 2025) shows GMV still under contract through at least late 2025, but the expressed intent to replace the app vendor is on record. No announcement of a new vendor or RFP found for 2025–2026 — the situation appears unresolved.

---

## 2. Documented Pain

**PTBusBeacon rating: 1.2★ / 90 ratings — confirmed active (last review June 2025).**

Failure pattern is consistent across reviews: infinite load screens, inaccurate ETAs, missed buses. The 2024 indie-developer clone (Pullman Bus Tracker) is strong circumstantial evidence that the official app is perceived as beyond rescue by at least some in the community.

Reddit-specific threads were not surfaced by indexed search (r/Pullman, r/WSU searches returned no results). The Daily Evergreen paywalls its articles (HTTP 403), blocking direct quote extraction from 2024–2025 issues. However, the operational supervisor's own public admission that the app "is just not working" is stronger evidence than user complaints alone.

---

## 3. Operational Signals

**GTFS static feed:**
- URL: `https://pullmanbusbeacon.com/gtfs`
- Transitland ID: `f-pullman~transit~wa~us`
- Last fetched: **2026-05-25 (10 minutes before query)** — actively maintained and current.

**GTFS-RT (real-time):**
- Vehicle Positions: `https://pullmanbusbeacon.com/gtfs-rt/vehiclepositions`
- Trip Updates: `https://pullmanbusbeacon.com/gtfs-rt/tripupdates`
- Alerts: `https://pullmanbusbeacon.com/gtfs-rt/alerts`
- Last fetched: **2026-05-25 (1 minute before query)** — fully operational.

All three GTFS-RT endpoints are live and current. This is a clean technical integration target: no scraping required, no proxy negotiation needed beyond the standard RT proxy pipeline.

**Route count:** Fall 2025 service includes 4 Community Service fixed routes (Blue, Loop, Paradise, Silver) + Community Service Express + 4 WSU on-campus Express Routes + Airport Route = **~9–10 active routes** depending on semester. The original "10 routes" figure is confirmed.

**Ridership:** "Over 1.4 million rides annually" — sourced from [PacTrans UW research page](https://depts.washington.edu/pactrans/research/projects/pullman-transit-route-pilot-study-evaluating-transit-re-routing-on-ridership-and-pedestrian-flows/), describing Pullman Transit as "the leading rural transit system throughout Washington." NTD 2024 specific figures unverified (DOT data portal requires direct query), but the 1.4M figure is consistent across multiple independent sources.

**Fare:** Pullman Transit is fare-free system-wide (since 1980, one of the first in the US). Youth fare-free was extended October 2022. This removes any in-app ticketing complexity — rider value prop is pure information (real-time arrivals, alerts, trip planning).

**Ownership / decision chain:** City of Pullman, Department of Public Works. Brad Rader (Transportation Operations Supervisor) is the named decision-maker in the Syncromatics evaluation. City Council sets budget; tech contracts likely go through Public Works director. 2025–2026 Mayor's Proposed Budget PDF exists but exceeded fetch limits — specific line items unverified.

**Seasonality risk:** Ridership is highly WSU-dependent. Fall and Spring semesters drive peak load; summer and Community Service schedules are materially reduced. A TransitKit deal should be scoped around academic calendar terms, not flat annual SaaS, or at minimum the sales conversation must surface the seasonal pattern early.

**Fall 2025 service update:** Routes restructured August 17, 2025 — on-campus Express Routes rebalanced north/south campus coverage, extra evening hour added, airport route changed to demand-response on the inbound leg. The system is actively evolving, which means any app must handle service calendar changes gracefully.

---

## 4. Verdict

**Confirmed Tier 1A.** All original signals hold under current verification:

- 1.2★ / 90 App Store rating verified current (June 2025 review).
- GTFS + GTFS-RT feeds live and clean as of today.
- ~1.4M annual riders, 30k WSU students as the core cohort.
- Operations supervisor on record wanting to replace the app vendor.
- Transit App covers the agency passively via GTFS but is not an endorsed partner and carries none of the brand.
- No Moovit dedicated coverage found.
- Fare-free removes ticketing complexity from the product scope.

**Primary wedge angle:** WSU student pain — 30k students riding a 1.2★ app that the agency's own supervisor admits "is just not working." The existence of a community-built indie clone (Pullman Bus Tracker, 2024) confirms demand exists and the official app has lost rider trust. The contract with GMV Syncromatics appears nominally active but the replacement intent is public. A well-timed outreach timed to the contract renewal window (confirm via Public Works) is the entry point.

**One flag:** Transit App's passive GTFS integration means a competitor is already in the App Store for Pullman. It is not a killshot — it's not branded, not city-endorsed, and offers none of the operator-side tools TransitKit provides — but it will come up in any sales conversation and needs a prepared response.

**Next step:** Identify the exact Syncromatics/GMV contract end date via a public records request (City of Pullman Clerk) or a direct call to Brad Rader at Public Works (509-332-6535). If the contract renews in 2026, the window to intervene is now.
