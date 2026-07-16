# AppalRider — revisione listing store (2026-07-16)

## Stato pubblicazione (2026-07-16)

- **Google Play — PUBBLICATO.** Titolo, short description e full description
  aggiornati e verificati live via Play Developer API (edit committato).
- **App Store — PARZIALE.** Solo `promotionalText` è live: con 1.2.5 in
  READY_FOR_SALE, Apple rifiuta `description` e `keywords`
  (*"cannot be edited at this time"*). Servono una nuova versione + review.
  → in corso: **1.2.6 (16)**, che porta descrizione + keywords nuove.

## GOTCHA — `schedules.json` non basta cancellarlo

`ios/TransitKit/Sources/Resources/schedules.json` (5.9 MB) era **dead weight**:
`ScheduleLoader` fa memoria → cache su disco → CDN, senza fallback al bundle
(`Bundle.main` serve solo `config.json` + `GoogleService-Info.plist`). Ma
`project.yml` glob-a l'intera cartella `Resources`, e **`scripts/build-ios.sh` e
`scripts/upload-ios.sh` ce lo ri-copiavano dentro a ogni build** da
`output/{op}/schedules.json` — `upload-ios.sh` abortiva pure se non lo trovava.
Cancellarlo dal repo da solo non serve a niente: lo script di release lo rimette.

Rimosso lo staging da entrambi gli script + `rm -f` difensivo (il file è
*generato*, quindi una copia stale su una macchina qualunque rientrerebbe nel
bundle in silenzio). Effetto: **`.app` da ~11.7 MB → 5.8 MB**.


Contesto: pre-email a Craig Hughes (AppalCART). Hughes aprirà il listing e poi
l'app. Tutto ciò che è enumerato qui è stato verificato contro il feed live
(`schedules.json` refresh 2026-07-16T04:58Z) e contro il codice iOS/Android.

## Stato attuale

| Campo | iOS | Android |
|---|---|---|
| Nome | AppalRider | **Appal Rider** (con spazio) |
| Sottotitolo / short desc | Live AppalCART bus tracker | Real-time AppalCART tracking. Clean, fast, no clutter. |
| Descrizione | 14 rotte enumerate, calendario ASU, ~2.6k char | ~900 char, apertura polemica contro l'incumbent |
| Promo text | *vuoto* | — |

## Rotte reali oggi (9)

Express · Orange · Purple · Gray · Red · Green · POP105 · **Teal** · **Maroon**

La descrizione iOS ne elenca 14: 7 non esistono in questa stagione
(Wellness District, Blue, Gold, Silver, State Farm Shuttle, Pink), **"NC 105 Lot
Shuttle" non esiste in nessuno snapshot** (è un nome di *fermata*), e **Teal e
Maroon — live adesso — mancano**. L'alert di servizio che AppalCART pubblica in
questo momento nomina proprio Teal.

Decisione: **non enumerare mai le rotte**. Ad aprile lo snapshot ne aveva 25, oggi
9. Qualunque lista hardcoded è sbagliata per metà anno e richiede un review cycle
per correggerla. L'app è già la fonte di verità.

---

## App Store — descrizione nuova

Live AppalCART buses, in your pocket.

AppalRider is the fastest way to ride AppalCART in Boone — track every fare-free bus in real time, see when the next one is coming, and find your way across campus or across town in seconds.

Built for App State students, Boone locals, and anyone visiting the High Country.

LIVE BUS TRACKING
Watch buses move on the map in real time, on every route AppalCART is running today. Position, direction, and route colors update continuously from AppalCART's official GTFS-Realtime feeds.

LIVE ARRIVALS AT EVERY STOP
Tap any stop for the next departures, route colors, and minutes until your bus arrives. No more guessing at a faded timetable on a windy corner.

SERVICE ALERTS
Detours, closures, and service changes as AppalCART publishes them — with anything affecting your saved stops surfaced first.

TRIP PLANNER
Tell AppalRider where you're starting and where you're going. It plans the trip on current schedules, walks you through any transfers, and draws the whole route on the map.

NEARBY STOPS & FAVORITES
The closest stops appear when you open the app, sorted by distance. Save the ones you use every day and see their next departures right on the home screen.

FULL TIMETABLES
Every route, every stop, grouped by weekday, Saturday, and Sunday.

DESIGNED FOR BOONE
• Free to use, no account, no ads
• Service across the Town of Boone, Appalachian State campus, downtown, shopping districts, and major apartment complexes
• Every AppalCART bus is wheelchair-accessible — accessibility details included
• Paratransit booking windows and Rural Services pickup days, with one-tap dial to Dispatch

ABOUT APPALCART
AppalCART is the fare-free bus system serving the Town of Boone, Appalachian State University, and the surrounding community, courtesy of the Town of Boone, Watauga County, and Appalachian State University.

AppalRider is an independent app built on AppalCART's public GTFS and real-time feeds. It is not an official AppalCART product and is not affiliated with AppalCART. For Paratransit reservations, Rural Services, and official information, call AppalCART Dispatch at 828-297-1300 or visit appalcart.com.

---

## Google Play — descrizione nuova

Live AppalCART buses, in your pocket.

AppalRider is the fastest way to ride AppalCART in Boone — track every fare-free bus in real time, see when the next one is coming, and find your way across campus or across town in seconds.

Built for App State students, Boone locals, and anyone visiting the High Country.

LIVE BUS TRACKING
Watch buses move on the map in real time, on every route AppalCART is running today. Position, direction, and route colors update continuously from AppalCART's official GTFS-Realtime feeds.

LIVE ARRIVALS AT EVERY STOP
Tap any stop for the next departures, route colors, and minutes until your bus arrives. No more guessing at a faded timetable on a windy corner.

SERVICE ALERTS
Detours, closures, and service changes as AppalCART publishes them — with anything affecting your saved stops surfaced first.

TRIP PLANNER
Tell AppalRider where you're starting and where you're going. It plans the trip on current schedules, walks you through any transfers, and draws the whole route on the map.

NEARBY STOPS & FAVORITES
The closest stops appear when you open the app, sorted by distance. Save the ones you use every day and see their next departures right on the home screen.

FULL TIMETABLES
Every route, every stop, grouped by weekday, Saturday, and Sunday.

DESIGNED FOR BOONE
• Free to use, no account, no ads
• Service across the Town of Boone, Appalachian State campus, downtown, shopping districts, and major apartment complexes
• Every AppalCART bus is wheelchair-accessible — accessibility details included
• Paratransit booking windows and Rural Services pickup days, with one-tap dial to Dispatch

ABOUT APPALCART
AppalCART is the fare-free bus system serving the Town of Boone, Appalachian State University, and the surrounding community, courtesy of the Town of Boone, Watauga County, and Appalachian State University.

AppalRider is an independent app built on AppalCART's public GTFS and real-time feeds. It is not an official AppalCART product and is not affiliated with AppalCART. For Paratransit reservations, Rural Services, and official information, call AppalCART Dispatch at 828-297-1300 or visit appalcart.com.

---

## Campi corti

- **Play — nome:** `Appal Rider` → `AppalRider` (allinea a iOS, bundle, landing)
- **Play — short description:** `Live AppalCART bus times, map, and alerts. Free, no account.`
- **iOS — sottotitolo:** invariato — `Live AppalCART bus tracker`
- **iOS — promo text** (oggi vuoto, editabile *senza* review): usarlo per lo
  stagionale, es. `Summer service is running. Full Fall routes return in August — the app updates automatically.`
- **iOS — keywords:** Apple indicizza già nome+sottotitolo, quindi `appalcart`,
  `bus`, `tracker` nei keyword sono char sprecati. Proposta (99/100):
  `boone,asu,app state,mountaineer,watauga,nc,shuttle,bus times,schedule,routes,campus,student,realtime`
