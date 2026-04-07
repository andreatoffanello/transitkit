# Architettura del Data Layer — transit-engine

**Data:** 2026-04-07
**Stato:** In revisione

---

## Problema attuale

Il pipeline attuale converte i file GTFS statici in un unico `schedules.json` monolitico (~3MB). Ogni client (iOS e web) scarica l'intero file, costruisce le lookup table in memoria e gestisce tutta la logica di trasformazione lato client.

Questo approccio ha tre problemi concreti:

1. **Accoppiamento forte tra formato sorgente e client.** Ogni quirk del feed GTFS di un operatore (campi non standard, formati orari anomali, banchine codificate nel testo) deve essere gestito da ogni client. La logica si duplica.
2. **Nessun contratto di schema garantito.** Il client non sa a priori quali campi saranno presenti o assenti per un dato operatore. I null check sono sparsi nel codice.
3. **Scalabilità verso multi-operatore difficile.** Aggiungere un secondo operatore richiederebbe estendere il JSON monolitico o scaricare file separati senza un'API uniforme.

---

## Principio guida

Introdurre uno strato intermedio tra le sorgenti GTFS e i client. I client ricevono sempre dati puliti, pre-trasformati, in uno schema predicibile. Zero logica di trasformazione nel client.

---

## Componenti

### 1. Neon DB — un progetto per operatore

Ogni operatore ha un progetto Neon dedicato. L'isolamento è completo: nessuna tabella condivisa, nessun multi-tenant.

Lo schema è il contratto. Se un campo non può essere popolato per un dato operatore, il valore è `null` — ma la colonna esiste sempre e la sua nullabilità è dichiarata nella tabella `operators.features`. Il client non deve mai gestire l'assenza di una chiave JSON.

### 2. Pipeline — GTFS → Neon

Il pipeline non è un servizio sempre attivo. I feed GTFS statici cambiano ogni 3-6 mesi; il pipeline viene eseguito manualmente o in CI quando arriva un nuovo feed.

**Struttura:**

```
pipeline/
  base_transformer.py              # logica GTFS standard (parsing, normalizzazione)
  build.py                         # entrypoint CLI: --operator {id}
  operators/
    actv/
      normalizer.py                # patch specifiche ACTV
    atvo/
      normalizer.py                # patch specifiche ATVO
```

`base_transformer.py` gestisce la trasformazione GTFS standard: parse dei file CSV, calcolo dei giorni di servizio, risoluzione degli stop times, costruzione delle route_directions.

`operators/{id}/normalizer.py` applica le patch specifiche dell'operatore: campi non standard, formati orari anomali, estrazione della lettera di banchina dal testo del nome fermata, normalizzazione dei colori linea.

Il pipeline scrive direttamente su Neon via `DATABASE_URL`. Le variabili d'ambiente per operatore sono in `shared/operators/{id}/.env` (gitignored). Il file `shared/operators/{id}/.env.example` è committato e contiene le chiavi richieste senza valori.

**Esecuzione:**

```bash
python pipeline/build.py --operator actv
```

### 3. API — Vercel Functions, un deployment per operatore

Ogni operatore ha un progetto Vercel separato. L'unica variabile d'ambiente necessaria è `DATABASE_URL`, che punta al Neon project di quell'operatore.

**Endpoints:**

| Metodo | Path | Descrizione |
|--------|------|-------------|
| `GET` | `/schedule` | Bulk JSON per iOS — tutto l'orario in un singolo payload |
| `GET` | `/config` | Metadati operatore + feature flags |
| `GET` | `/stops/nearby` | `?lat=&lng=&radius=` — fermate nel raggio specificato (metri) |
| `GET` | `/stops/:id/departures` | `?date=&limit=` — prossime partenze da una fermata |
| `GET` | `/routes` | Tutte le linee |
| `GET` | `/routes/:id` | Singola linea con fermate ordinate per direzione |
| `GET` | `/trips/:id` | Viaggio completo con tutti gli stop times |

Il payload di `/schedule` usa lo stesso schema pulito degli endpoint granulari — nessun array compatto, nessun campo abbreviato. È lo stesso dato, serializzato in bulk.

L'URL dell'API per ogni operatore è registrato in `shared/operators/{id}/config.json` nel campo `apiUrl`.

### 4. Client iOS

`ScheduleStore` diventa un cache layer sull'API, non un parser.

**Ciclo di vita dei dati:**

- **Primo avvio:** download bulk da `GET /schedule` → salvataggio su disco (cache persistente).
- **Background App Refresh:** re-download bulk in background mentre l'app è chiusa, aggiornamento della cache su disco.
- **Apertura app:** controllo freshness — confronto `lastUpdated` ricevuto dall'API con il valore in cache. Re-download solo se stale.
- **Navigazione normale:** tutte le view leggono dalla cache su disco. Zero chiamate API durante l'uso normale.

`ScheduleStore` non contiene più logica di trasformazione. Costruisce solo le lookup table in memoria a partire dai dati già puliti della cache.

**GTFS-RT:** invariato. `VehicleStore` continua a fare polling diretto all'endpoint realtime dell'operatore. Nessuna modifica.

### 5. Client Web (Nuxt SSR)

Il web non scarica il bulk. Le server route chiamano gli endpoint granulari per pagina, rendendo ogni risposta specifica al contesto.

| Pagina | Endpoint |
|--------|----------|
| Fermata | `GET /stops/:id/departures` |
| Dettaglio linea | `GET /routes/:id` |
| Lista linee / Home | `GET /routes` |
| Mappa | `GET /stops/nearby` |

**GTFS-RT:** invariato. `useRealtime` continua a fare polling diretto. Nessuna modifica.

### 6. App multi-operatore

Quando un'app serve più operatori (es. area metropolitana con più aziende):

- `config.json` contiene un array di operatori invece di un singolo operatore.
- Il client chiama l'API di ogni operatore in parallelo.
- Le fermate vicine: una chiamata a `/stops/nearby` per operatore, risultati uniti lato client.
- Non esiste un'API aggregatore. Il merge avviene solo nel client.

---

## Schema DB

```sql
-- Metadati operatore e feature flags
CREATE TABLE operators (
    id          TEXT PRIMARY KEY,
    name        TEXT NOT NULL,
    url         TEXT,
    timezone    TEXT NOT NULL,
    features    JSONB NOT NULL DEFAULT '{}'
);

-- Linee
CREATE TABLE routes (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id),
    name          TEXT NOT NULL,        -- es. "1", "B"
    long_name     TEXT,                 -- es. "Ferrovia - Mestre"
    color         TEXT,                 -- hex senza #
    text_color    TEXT,                 -- hex senza #
    transit_type  INTEGER NOT NULL      -- GTFS route_type
);

-- Fermate
CREATE TABLE stops (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id),
    name          TEXT NOT NULL,
    lat           DOUBLE PRECISION NOT NULL,
    lng           DOUBLE PRECISION NOT NULL,
    platform_code TEXT,                 -- nullable
    dock_letter   TEXT                  -- nullable — estratto dal normalizer
);

-- Viaggi
CREATE TABLE trips (
    id            TEXT PRIMARY KEY,
    operator_id   TEXT NOT NULL REFERENCES operators(id),
    route_id      TEXT NOT NULL REFERENCES routes(id),
    direction_id  INTEGER NOT NULL,     -- 0 o 1
    headsign      TEXT,
    service_days  TEXT[] NOT NULL       -- es. ["monday","tuesday",...]
);

-- Stop times
CREATE TABLE stop_times (
    trip_id         TEXT NOT NULL REFERENCES trips(id),
    stop_id         TEXT NOT NULL REFERENCES stops(id),
    arrival_time    TEXT NOT NULL,      -- HH:MM:SS, può superare 24:00
    departure_time  TEXT NOT NULL,
    stop_sequence   INTEGER NOT NULL,
    PRIMARY KEY (trip_id, stop_sequence)
);

-- Direzioni per linea (sequenza fermate + shape)
CREATE TABLE route_directions (
    route_id        TEXT NOT NULL REFERENCES routes(id),
    direction_id    INTEGER NOT NULL,
    headsign        TEXT,
    stop_ids        TEXT[] NOT NULL,    -- fermate ordinate
    shape_polyline  TEXT,               -- encoded polyline, nullable
    PRIMARY KEY (route_id, direction_id)
);
```

### Feature flags (`operators.features`)

```json
{
  "docks": true,
  "realtime": true,
  "alerts": false,
  "bikes": false,
  "shapes": true
}
```

Ogni flag dichiara se quella funzionalità è disponibile per l'operatore. Il client legge `features` da `GET /config` al primo avvio e non tenta mai di chiamare endpoint non supportati.

---

## Struttura repository

```
transit-engine/
  pipeline/
    base_transformer.py
    build.py
    operators/
      {id}/
        normalizer.py
  api/
    routes/
      schedule.ts
      config.ts
      stops/
        nearby.ts
        [id]/
          departures.ts
      routes/
        index.ts
        [id].ts
      trips/
        [id].ts
  shared/
    operators/
      {id}/
        config.json          # committato
        .env.example         # committato — chiavi senza valori
        .env                 # gitignored — DATABASE_URL e segreti
  ios/
    TransitKit/
      Sources/
        Stores/
          ScheduleStore.swift
```

---

## Aggiungere un nuovo operatore

La sequenza è lineare e non richiede modifiche al codice del framework.

1. **Neon:** creare un nuovo progetto Neon → ottenere `DATABASE_URL`.
2. **Config:** creare `shared/operators/{id}/config.json` con `apiUrl`, tema colori e centro mappa.
3. **Env:** creare `shared/operators/{id}/.env` con `DATABASE_URL`. Copiare `.env.example` e aggiornare i valori.
4. **Normalizer:** scrivere `pipeline/operators/{id}/normalizer.py` per i quirk specifici dell'operatore.
5. **Pipeline:** eseguire `python pipeline/build.py --operator {id}`.
6. **Vercel:** creare un nuovo progetto Vercel per l'API, impostare la variabile d'ambiente `DATABASE_URL`.
7. **Deploy:** `vercel --prod` dalla cartella `api/` con il progetto collegato.

---

## Cosa non cambia

- Polling GTFS-RT lato client (sia iOS che web).
- Struttura di `shared/operators/{id}/config.json` — si aggiunge solo il campo `apiUrl`.
- Struttura frontend Nuxt (pagine, componenti, routing).
- UI e navigazione dell'app iOS.
