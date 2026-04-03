# TransitKit Web — Task Backlog

Ultimo aggiornamento: 2026-04-03

## Stato progetto
- 295 test unitari ✅
- CI/CD GitHub Actions → Vercel ✅
- Produzione: https://transitkit.vercel.app
- CDN: https://andreatoffanello.github.io/transitkit-data/
- Dev locale: `npm run dev` → porta 3000, `.env.development` con `CDN_BASE=http://localhost:3000/mock`

---

## 🔴 Critico — Dati reali

### T381: Script GTFS parser
Legge un feed GTFS (zip o URL), genera `config.json` + `schedules.json` nel formato atteso dal CDN.
- Input: URL feed GTFS pubblico (es. `https://operator.com/gtfs.zip`)
- Output: `appalcart/config.json` + `appalcart/schedules.json`
- File: `scripts/parse-gtfs.ts`
- Blocca: qualsiasi test con dati reali

### T382: CLI `generate-operator.ts`
Wrapper CLI per T381: `npx ts-node scripts/generate-operator.ts --gtfs <url> --id <operatorId>`
Produce i file pronti per il deploy in `transitkit-data`.

---

## 🟡 UX

### T383: Home — skeleton per sezione "Fermate vicine"
Durante la geolocalizzazione (`nearbyState === 'locating'`), mostrare 3 righe skeleton
animate invece del testo "Localizzazione in corso...". Pattern identico agli altri skeleton loader.

### T384: Stop page — Apple Maps deeplink su iOS
Il link "Apri in Maps" usa `https://maps.google.com`. Su iOS Safari, aggiungere anche un link
`maps://?q=<nome>&ll=<lat>,<lng>` che apre Apple Maps nativo. Mostrare entrambi o rilevare
la piattaforma con `navigator.platform`.

### T380: Home — countdown a `schedules.validUntil`
Se `schedules.validUntil` è entro 7 giorni, mostrare un banner "Orari validi fino al DD/MM —
aggiornamento in arrivo" per avvisare l'utente.

---

## 🟢 Test / qualità

### T385: `schedule.test.ts` — test `getNextServiceDayGroupKey`
La funzione `getNextServiceDayGroupKey` in `utils/schedule.ts` è implementata ma non ha
test dedicati. Aggiungere: trova il prossimo giorno con servizio, gestisce wrap domenica→lunedì,
restituisce null se nessun giorno ha servizio.

### T386: E2E — test geolocalizzazione "Fermate vicine"
Playwright: mockare `navigator.geolocation` con coordinate Boone NC, verificare che appaiano
le 3 fermate più vicine ordinate per distanza.

### T387: `strings.test.ts` — verifica searchStops IT/EN
Spot-check della chiave `searchStops` aggiunta in T372 (potrebbe mancare nel test).

---

## 🔵 Infrastruttura / SEO

### T388: `transitkit-data` — struttura multi-operatore
Documentare la struttura del repo CDN: `<operatorId>/config.json`, `<operatorId>/schedules.json`.
Aggiungere un `index.json` con elenco operatori disponibili.

### T389: Vercel — dominio custom `appalcart.transitkit.app`
Configurare il dominio custom su Vercel e aggiungere la mapping in `utils/operators.ts`.
Requires: DNS access.

### T390: `nuxt.config.ts` — `payloadExtraction` audit
Verificare se rimuovere `payloadExtraction: false` migliora il Lighthouse score senza
reintrodurre hydration warnings (l'impostazione fu rimossa in T356).
