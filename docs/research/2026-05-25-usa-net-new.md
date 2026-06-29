# USA Net-New Operator Scouting — 2026-05-25

## Methodology

Hunted across three vectors: (1) college towns with standalone bus systems that lack a serious app, (2) resort/ski towns with GTFS but thin app coverage, (3) RouteShout 2.0 agencies (app last updated April 2019, rated 1.6★ on iOS / 248 ratings) — any agency still on RouteShout is by definition app-abandoned. Verified GTFS via Transitland, app ratings via direct App Store fetches, Transit App presence via transitapp.com region pages. Excluded the 5 already-verified candidates (AppalCART, Corvallis Transit, BRTA, Citilink, Pullman Transit).

---

## Tier 1A — Best Fits (act now)

| Operator | City | Routes | Ridership | GTFS URL | Existing App | Pain Evidence |
|---|---|---|---|---|---|---|
| **Manchester Transit Authority (MTA)** | Manchester, NH | 16 | 354k/yr (2024) | `http://rapid.nationalrtap.org/GTFSFileManagement/UserUploadFiles/7841/MTA_GTFS_October_2023.zip` | RouteShout 2.0 — iOS 1.6★ / 248 ratings / last update **Apr 2019** | App page: "really clumsy, overly complicated"; no Transit App endorsement; listed on transitapp.com passively (GTFS passthrough only) |
| **Mountain Line Transit Authority (MLTA)** | Morgantown, WV | 23 | 650k/yr (2025) | `http://site.busride.org/google/google_transit.zip` (⚠ 403 at last check — feed may have rotated) | Mountain Line Bus Finder — iOS **1.9★ / 55 ratings / last update Oct 2015** | App Store: "Little chance the correct bus will show up"; 11-year-old iOS app from Advanced Tracking Technologies; WVU with ~30k students on campus |
| **Fort Smith Transit** | Fort Smith, AR | 6 | ~205k/yr (2022) | via Transitland `f-9wvq-fortsmithtransit` (routematch hosted) | RouteShout 2.0 — iOS 1.6★ / 248 ratings / last update Apr 2019 | App crashes, ETAs 5 hours off (Play Store reviews); facing 2026 budget cuts ($150k reduction); no Transit App endorsement found |
| **Washington County Transit (WCT)** | Hagerstown, MD | 14 | ~516k/yr | via Transitland `f-dr09-washingtoncountycommuter` | Switched from RouteShout → Passio GO (June 2025) | Recently forced onto Passio GO after RouteShout era; Passio GO app is generic multi-agency; riders lost their familiar tool; wedge window open before they settle |

---

## Tier 1B — Watchlist (re-check in 90 days)

| Operator | City | Routes | Ridership | GTFS URL | Existing App | What Would Need to Change |
|---|---|---|---|---|---|---|
| **Valley Transit** | Walla Walla, WA | 11 | 502k/yr (2024) | Transitland `f-c23n-valleytransit` | Walla Walla Valley Transit iOS app — Connexionz / 4.2★ but only **5 ratings**; their DoubleMap app was killed July 2023 | Connexionz app is actively maintained (Dec 2025 update). Low rating count means satisfaction is unknown — if Connexionz drops support or ratings crater, immediate opportunity. No Transit App operator endorsement found. |
| **TranGO (Okanogan County Transit Authority)** | Okanogan / Twisp / Winthrop, WA | ~8 | ~60k/yr (small but growing 11.5% YoY) | Transitland `f-okanogan~county~transportation~nutrition` | RouteShout 2.0 (still listed on their site) | Ridership too small for $299/mo without a county or state grant component. Watch for ridership crossing 100k/yr threshold or a tourism partnership angle with Methow Valley. |
| **CyRide** | Ames, IA | 24 | ~7M/yr (ISU — large) | gtfs.cityofames.org | "Ames Ride" — student-developed app (Patrick Demers); newer than RouteShout era | Too large ($299 underprices it); student app is recent (2022) and well-reviewed. Revisit only if student dev abandons maintenance or ISU seeks official vendor. |
| **Eastern Sierra Transit Authority (ESTA)** | Mammoth Lakes, CA | 14 | unverified via NTD | Transitland `f-9qf-laketahoe~ca~us` | Transit App is recommended by ESTA (via their real-time page) | Transit App partnership is explicit — disqualified unless they drop the endorsement. Mark as disqualified (see below). |

---

## Disqualified During Scouting

| Operator | City | Reason |
|---|---|---|
| **Eastern Sierra Transit Authority (ESTA)** | Mammoth Lakes, CA | ESTA explicitly recommends Transit App on their website for real-time tracking — operator endorsement in place. |
| **Mountain Rides** | Sun Valley / Ketchum, ID | Uses GMV Syncromatics for GTFS and tracking; active branded app maintained by GMV. No wedge. |
| **Bloomington Transit** | Bloomington, IN | Switched to ETA SPOT (Jan 2024) then to Umo (Nov 2024) for fares — two active vendors, recent changes; not idle. |
| **Yakima Transit** | Yakima, WA | New Connexionz app launched Dec 2025 (v1.0.2); too fresh to have user pain built up yet. |
| **Cheyenne Transit Program** | Cheyenne, WY | Recently switched to Spare Labs (app rated 4.3★, updated Mar 2025); on-demand pivot; not a target. |

---

## Notes

- **RouteShout 2.0 is the best hunting ground**: the app has been frozen since April 2019 (iOS) and serves as a reliable "abandoned vendor" signal for any agency still using it. MTA Manchester and Fort Smith Transit are the two cleanest catches from this vector.
- **Mountain Line (Morgantown)** is the most compelling single candidate: 650k riders, 30k WVU students, an iOS app last updated in 2015 rated 1.9★, and GTFS confirmed on Transitland (403 on current URL warrants a live GTFS re-check before outreach).
- **Washington County Transit** is a recency play — just migrated off RouteShout in June 2025. They have no muscle memory for the new vendor yet and their ridership (516k) comfortably justifies the price.
