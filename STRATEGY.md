# TransitKit — Product & Business Strategy

> Documento vivo. Aggiornare quando cambiano decisioni architetturali, target, o modello di business.
> Ultimo aggiornamento: 2026-04-01 (v4 — deep research completata, RFTA disqualificata, nuovi target USA)

---

## Cos'è TransitKit

White-label transit SaaS: codebase unico, config per operatore → app iOS + Android brandizzata + web stop pages con QR.

**Target:** piccoli operatori TPL con GTFS già pubblicato ma senza app propria.  
**Prezzo:** $299/mese, nessun setup fee, nessun tier.

---

## Prodotto — 3 componenti

| Componente | Stack | Stato |
|-----------|-------|-------|
| iOS app | SwiftUI | ✅ Funzionante — RFTA, TCAT |
| Android app | Jetpack Compose | 🔲 Da costruire |
| Web stop pages | Nuxt su Vercel | 🔲 Da costruire |
| Pipeline GTFS→JSON | Python | ✅ Funzionante |

### iOS — 4 tab
1. **Orari** — linee → fermate → orari con countdown
2. **Mappa** — fermate, shapes linee, posizione utente
3. **Info** — tariffe, punti vendita, contatti
4. **Settings** — preferiti, notifiche, lingua

**Missing ancora:** Home tab (prossime partenze GPS), push notifications reali, CDN hosting.

### Web stop pages
- URL: `fermate.operatore.com/stop/[stopId]`
- QR code fisico alla fermata → mobile browser → tabellone partenze branded
- **Single Vercel project** per tutti gli operatori — host-based routing
- Ogni operatore aggiunge CNAME: `fermate.operatore.com` → deployment Vercel
- Middleware Nuxt legge `Host` header → carica config operatore giusto
- SSG + ISR (revalidate ogni ora), mobile-first

---

## Value proposition

In ordine di forza reale:

1. **Web stop pages + QR** — funziona senza download, immediato per turisti e occasionali
2. **App branded** — presenza su store con nome/colori operatore
3. **Push notifications** — service alerts diretti (non disponibili gratis su Transit App/Google Maps per piccoli)
4. **Zero gestione tecnica** — tutto incluso nel $299

### Cosa NON vendiamo
- **GTFS creation** — è commodity (esistono tool gratuiti), non scala, non è un moat. Se l'operatore non ha GTFS, non è il target adesso.
- GTFS hosting/manutenzione — inclusa ma non come argomento di vendita principale.

### Perché non basta essere su Transit App / Google Maps
- Transit App: notifiche branded non disponibili per piccoli operatori
- Google Maps: zero controllo sul brand, nessuna relazione diretta col rider
- Per molti target (STP Brindisi, AppalCART, ecc.): semplicemente non ci sono — il confronto è "app branded vs niente", non "app branded vs Transit App"

---

## Target market

### Criteri must-have
- GTFS pubblicato pubblicamente
- Nessuna app trip-planning propria (ticketing-only come myCicero non conta)
- Bacino interessante: turismo, università, o >500k rider/anno

### Criteri di conversione (alzano priorità)
- Già paga per tool digitali (Transit App, Token Transit, Masabi, ETA Spot) → sa che serve
- Domanda dimostrata (app non ufficiale fatta da studenti, lamentele social)
- Esperienza frammentata (2-3 tool diversi)
- App abbandonata o con recensioni pessime
- Operatore privato o semi-privato (decide in settimane, non mesi)
- 10-30 linee (sweet spot: giustifica $299, no IT interno)

### Red flag
- Ha appena firmato con Transit App / Pluservice / Moovit
- Ha lanciato app propria negli ultimi 12 mesi
- Troppo grande (>100 linee, metro di capitale)
- Ente pubblico puro con procurement rigido

### Pipeline USA — Tier 1A (gol a porta vuota verificati v4)
| # | Operatore | Città | Linee | Rider/anno | Segnale chiave |
|---|-----------|-------|-------|------------|----------------|
| 1 | **AppalCART** | Boone NC | 12 | 1.5M | ETA SPOT 1.5★/994 rating, App State 20k studenti, nessun Transit App, GTFS+RT |
| 2 | **Corvallis Transit** | Corvallis OR | 9-12 | ~2M | OSU 35k studenti, Connexionz 1.8★/9 rating (morta), GTFS confermato |
| 3 | **BRTA** | Pittsfield MA | 13 | ~800k | RouteShout abbandonato 2019 (1.6★/246 rating), GTFS confermato |
| 4 | **Citilink** | Fort Wayne IN | 14 | 1.6M | Token Transit + tracking separati, nessuna app branded fixed-route, GTFS+RT |
| 5 | **Pullman Transit** | Pullman WA | 10 | 1.4M | PTBusBeacon ufficiale 1.2★/90 rating (broken), WSU 30k studenti, GTFS+RT |

> **Rimossi dalla lista attiva**: RFTA (Transit App formalmente endorsato + web BusTracker), TCAT (Navi 4.4★/1020 rating),
> Advance Transit (raccomanda Transit App per nome sul sito), Streamline Bus (app Syncromatics ufficiale gen 2026),
> Island Explorer (PWA ufficiale NPS), Annapolis Transit (shifting to microtransit).

### Pipeline Italia — Tier 1A (verificati v4, GTFS + no app trip-planning)
| # | Operatore | Area | Linee | GTFS | Segnale chiave |
|---|-----------|------|-------|------|----------------|
| 1 | STP Brindisi | Puglia | ~30 | Transitland + open data (29 versioni, CC-BY-4.0) | myCicero ticketing only, fuori Cotrap, privato, Ostuni/Alberobello |
| 2 | ATAP | Biella/Vercelli, Piemonte | ~30 | magellanoprogetti.it (100+ versioni, attivissimo) | myCicero+MooneyGo ticketing only, MuoversiPiemonte non è un'app |
| 3 | SAF Duemila/VCO | VCO, Piemonte | ~18 | Feed Regione Piemonte (verificare linee provinciali incluse) | Lago Maggiore, privato, Alibus VCO è solo shuttle |

> **Nota:** Il mercato italiano è genuinamente saturo (Pluservice/Cotrap/Teseo/AroundSardinia coprono la maggior parte). Tier 1A certo = 3 operatori. Da re-verificare: Riviera Trasporti (stato app incerto), Autoservizi Preite (Calabria, GTFS regionale da confermare).

> **Rimossa:** STP Lecce — Cotrap/Itineris app copre tutti i 60 operatori del consorzio Puglia, aggiornata 01/04/2026.

### Pipeline Italia — Ferry (separata, dopo i primi clienti TPL)
Alilaguna (Venezia), Travelmar (Costiera Amalfitana), Alilauro (Golfo Napoli).

---

## Acquisizione organica

### Strategia principale: demo app before contract
1. Costruire app per RFTA o TCAT con GTFS già scaricato
2. Pubblicare su App Store come "[Città] Bus — Community App"
3. Raccogliere download organici
4. Outreach: *"X persone hanno scaricato l'app del vostro servizio. Volete renderla ufficiale?"*

Il cold outreach diventa inbound con prova di domanda esistente.

### Altri canali
- **SEO**: "come mettere transit su Google Maps", "GTFS piccolo operatore"
- **Rider communities**: Reddit (r/Ithaca, r/transit), gruppi Facebook/Telegram locali
- **Associazioni di categoria**: CTAA (USA, operatori rurali), ANAV (Italia, autobus privati)
- **Email personalizzata** con demo live dell'operatore specifico

---

## Architettura dati

```
GTFS feed pubblico operatore
    ↓ pipeline/build.py
schedules.json (~3MB per operatore)
    ↓ CDN
    ├── iOS: ScheduleLoader con SHA256 manifest sync (da DoVe)
    ├── Android: (futuro)
    └── Web: Nuxt SSG/ISR — fetch al build + revalidate ogni ora
```

**CDN**: GitHub Pages (ora, costo zero). Vercel Blob se serve più controllo.

---

## Fasi di sviluppo

**Phase 1 — Demo pubblicabile (ora)**
- [ ] Home tab iOS con prossime partenze GPS
- [ ] CDN hosting schedules.json (GitHub Pages)
- [ ] Pubblicare demo app **AppalCART** su App Store (non ufficiale) — "Boone Bus — Community App"

**Phase 2 — Primo cliente**
- [ ] Outreach ai Tier 1A con demo live
- [ ] Convertire primo operatore
- [ ] Infrastruttura per onboarding (CDN per-operatore, custom domain setup)

**Phase 3 — Web + Android**
- [ ] Web stop pages Nuxt (single Vercel project, host-based routing)
- [ ] Android app (Jetpack Compose)
- [ ] Push notifications reali (OneSignal free tier)

**Phase 4 — Scale**
- [ ] Operator panel (alerts, news, aggiornamenti orari)
- [ ] Pipeline GTFS auto-update
- [ ] Altri operatori

---

## Fiscal strategy

- **Phase 0-1**: Nessun revenue → nessuna P.IVA necessaria
- **First client**: Paddle/LemonSqueezy (estero) o prestazione occasionale (Italia)
- **2+ clienti** (~7k€/anno): P.IVA forfettaria (5% tasse per 5 anni)
- **PA italiana**: contratti < 5k€/anno = affidamento diretto, no gara pubblica

---

## Dashboard

### Dashboard interna (per noi)
Monitoring aggregato su tutti gli operatori: stato GTFS, CDN health, app store versions, subscription status, download/active users. Necessaria da Phase 3 in poi (3+ operatori).

### Dashboard operatore (per loro)
**Critica prima del primo cliente** — senza di essa ogni alert passa da noi manualmente (non sostenibile con fuso orario USA).

Due funzioni:
1. **Messaggi fissi in-app** — banner/alert mostrati nell'app (es. "Linea 5 sospesa"). Scrivono su `alerts.json` sul CDN. L'app legge ad ogni avvio.
2. **Push notifications** — messaggio one-shot a tutti gli utenti dell'app dell'operatore.

Auth: un login per operatore (Clerk o JWT semplice).

---

## Push notifications

**Scelta: APNs + FCM nativi, no OneSignal.**

```
Operator invia dal dashboard
    ↓ API
    ├── legge device_tokens per operator_id dal DB
    ├── APNs HTTP/2 → iOS
    └── FCM HTTP v1 → Android
```

- **Costo: zero** — APNs e FCM sono gratuiti illimitati
- **Storage tokens:** tabella `device_tokens(operator_id, token, platform)` su Neon/Supabase
- **Credenziali:** chiave APNs `.p8` + service account FCM per operatore, in env vars o Vercel Blob cifrato
- **Complessità:** ~200 righe di codice backend, scritto una volta

**Perché non OneSignal:** $9/mese per app dopo il free tier. A 10 operatori = $90/mese di costo puro evitabile. Per il caso d'uso ("manda alert a tutti gli utenti dell'app X") non serve niente di più complesso.

---

## Decisioni architetturali prese

| Decisione | Scelta | Motivo |
|-----------|--------|--------|
| Web framework | Nuxt (non Next.js) | Andrea conosce Vue/Nuxt |
| Web deployment | Single Vercel project, host-based routing | Non scalare con progetti separati per operatore |
| GTFS creation | NON nel pacchetto standard | Tool gratuiti esistono, non scala, non è moat |
| CDN dati | GitHub Pages → Vercel Blob | Costo zero per iniziare |
| Ferry pipeline | Separata dal TPL | Clienti diversi, ciclo vendita diverso |
| Prima demo | AppalCART | ETA SPOT 1.5★/994 rating = dolore documentato, 20k studenti, GTFS+RT confermato, nessun Transit App |
| Push notifications | APNs + FCM nativi, no OneSignal | Zero costo, ~200 righe backend, niente dipendenze esterne |
| Dashboard operatore | Phase 2 (non Phase 3) | Bloccante per clienti USA (fuso orario) |
