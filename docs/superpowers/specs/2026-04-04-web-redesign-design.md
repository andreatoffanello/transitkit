# TransitKit Web App — Redesign Design Spec

**Data:** 2026-04-04
**Stato:** Approvato
**Versione:** 2.0 — Refined Minimal
**Sostituisce:** `2026-04-03-web-app-design.md` (architettura valida, questa spec governa il design)

---

## 1. Direzione estetica

**Refined Minimal** — ispirazione Airbnb, Apple Maps, Apple Transit.

Principi guida:
- **Ariosità:** spazio bianco generoso, nessun elemento compresso
- **Content-first:** i dati (orari, linee, fermate) sono protagonisti, la UI è al servizio
- **Premium senza ornamenti:** nessuna decorazione gratuita — ogni elemento ha una funzione
- **Niente AI slop:** nessun gradiente arcobaleno, nessuna card con ombre enormi, nessun animation overflow

Il tono visivo è quello di un'app nativa iOS trasportata sul web con piena consapevolezza del medium.

---

## 2. Design system

### 2.1 Tipografia

Font: **Plus Jakarta Sans** — Google Fonts. Caricare solo i pesi necessari (400, 500, 600, 700) per minimizzare il peso.

```css
@import url('https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700&display=swap');
```

| Ruolo | Dimensione | Line-height | Peso | Uso |
|-------|-----------|-------------|------|-----|
| Display | 28px | 1.1 | 700 | Titolo hero home |
| Heading | 20px | 1.2 | 700 | Titolo pagina, nome fermata |
| Subheading | 15px | 1.3 | 600 | Titolo sezione, label intestazione |
| Body | 15px | 1.5 | 400 | Testo descrittivo, contenuto |
| Label | 13px | 1.4 | 500 | Metadati, orari secondari, info aggiuntive |
| Mono | 15px | 1.0 | 600 | Orari (`font-variant-numeric: tabular-nums`, `letter-spacing: -0.02em`) |
| Caption | 11px | 1.3 | 500 | Badge, tag, indicatori |

Regola: nessun testo sotto 11px. Nessun misto di più di 3 dimensioni per schermata.

---

### 2.2 Colori e CSS Variables

Le variabili `--color-primary`, `--color-accent`, `--color-text-on-primary` vengono iniettate dinamicamente da `config.json` dell'operatore al mount. Le variabili seguenti sono invarianti del design system.

#### Light mode

```css
:root {
  --bg-primary:    #FFFFFF;
  --bg-secondary:  #F5F5F7;
  --bg-elevated:   #FFFFFF;     /* + shadow */
  --text-primary:  #1A1A1A;
  --text-secondary:#6B6B6B;
  --text-tertiary: #ABABAB;
  --border:        rgba(0, 0, 0, 0.08);
  --operator-primary: var(--color-primary);
  --operator-tint:    color-mix(in srgb, var(--color-primary) 10%, transparent);
}
```

#### Dark mode

```css
@media (prefers-color-scheme: dark) {
  :root {
    --bg-primary:    #0A0A0F;
    --bg-secondary:  #131318;
    --bg-elevated:   #1C1C24;
    --text-primary:  #F2F2F7;
    --text-secondary:#8E8E9A;
    --text-tertiary: #48484E;
    --border:        rgba(255, 255, 255, 0.07);
  }
}
```

Il toggle manuale in Impostazioni sovrascrive `prefers-color-scheme` aggiungendo la classe `dark` o `light` su `<html>` e persistendo la scelta in `localStorage`.

---

### 2.3 Shadow system

```css
--shadow-sm: 0 1px 3px rgba(0,0,0,0.06), 0 1px 2px rgba(0,0,0,0.04);
--shadow-md: 0 4px 16px rgba(0,0,0,0.08), 0 1px 4px rgba(0,0,0,0.04);
--shadow-lg: 0 16px 48px rgba(0,0,0,0.12), 0 4px 16px rgba(0,0,0,0.06);
```

In dark mode le shadow si attenuano automaticamente perché il background è scuro — nessun override necessario.

---

### 2.4 Border radius

| Elemento | Radius |
|----------|--------|
| Badge, tag, chip piccoli | 4px |
| Input, chip navigazione | 10px |
| Card standard | 16px |
| Card grande, tab bar, modal | 20px |
| Dot, avatar, indicator | 9999px (full) |

---

### 2.5 Iconografia — Lucide

**Nessuna emoji.** Ogni simbolo è un'icona Lucide con attributi coerenti.

| Contesto | Dimensione | Stroke width |
|----------|-----------|--------------|
| UI generale (bottoni, azioni, nav) | 20px | 1.75px |
| Inline nel testo | 16px | 1.75px |
| Tab bar / sidebar | 24px | 1.75px |

Colore default: `--text-secondary`. Colore stato attivo: `--operator-primary`.

**Mappa icone → emoji sostituite:**

| Icona Lucide | Sostituisce |
|--------------|-------------|
| `Bus` | emoji autobus |
| `TramFront` | emoji tram |
| `TrainFront` | emoji treno |
| `Ship` | emoji traghetto |
| `MapPin` | emoji spillo |
| `Heart` | emoji cuore |
| `Clock` | emoji orologio |
| `Search` | emoji lente |
| `ChevronLeft` | freccia indietro |
| `Share2` | emoji condividi |
| `SunMedium` | emoji sole |
| `Moon` | emoji luna |
| `Home` | emoji casa |
| `Route` | emoji linee |
| `Map` | emoji mappa |
| `Settings` | emoji ingranaggio |
| `Navigation` | emoji navigazione |
| `Star` | emoji stella |
| `Radio` | live indicator testuale |
| `AlertCircle` | emoji attenzione |
| `RefreshCw` | emoji aggiorna |

---

## 3. Navigazione

### 3.1 Bottom Tab Bar (mobile, < 1024px)

```
┌────────────────────────────────────────────────────────┐
│  [Home]     [Route]     [Map]*    [Settings]           │
│  Home       Linee       Mappa     Impostazioni         │
└────────────────────────────────────────────────────────┘
```

*La tab Mappa appare solo se `features.enableMap === true` in `config.json`.

**Specifiche:**
- Altezza: `64px + env(safe-area-inset-bottom)` — rispetta il notch iPhone
- Background: `backdrop-filter: blur(20px) saturate(180%)` con `background: rgba(var(--bg-primary-rgb), 0.85)`
- Border top: `1px solid var(--border)`
- Shadow: `--shadow-lg` invertita verso l'alto (`box-shadow: 0 -16px 48px ...`)
- Tab attiva: icona 24px + label Caption in `--operator-primary`, dot 4px × 4px sotto l'icona (color: `--operator-primary`, radius: full)
- Tab inattiva: icona + label in `--text-tertiary`
- Transizione: `color 200ms ease`
- Nessun effetto bounce o scala sull'icona — movimento pulito

**HTML / Vue struttura:**

```vue
<nav class="tab-bar">
  <NuxtLink to="/" class="tab-item" :class="{ active: route.path === '/' }">
    <Home :size="24" :stroke-width="1.75" />
    <span>Home</span>
  </NuxtLink>
  <!-- ... -->
</nav>
```

---

### 3.2 Sidebar desktop (≥ 1024px)

```
┌──────────────────────────────────────────────────────────────────┐
│ ┌────────────────┐ ┌──────────────────────────────────────────┐  │
│ │ Logo / Operatore│ │                                          │  │
│ │                 │ │      Contenuto principale                │  │
│ │ ─────────────   │ │                                          │  │
│ │ Home            │ │                                          │  │
│ │ Linee           │ │                                          │  │
│ │ Mappa*          │ │                                          │  │
│ │ Impostazioni    │ │                                          │  │
│ └────────────────┘ └──────────────────────────────────────────┘  │
└──────────────────────────────────────────────────────────────────┘
```

**Specifiche:**
- Width sidebar: `240px`, fissa (`position: sticky`, altezza `100vh`)
- Background: `--bg-elevated` con `box-shadow: var(--shadow-sm)`
- Border right: `1px solid var(--border)`
- Logo/nome operatore in cima: padding `24px 20px 20px`, immagine 32px × 32px, nome Subheading
- Link di navigazione: padding `10px 16px`, radius `10px`, icona 20px + label Body
- Link attivo: background `--operator-tint`, testo + icona `--operator-primary`
- Link hover: background `--bg-secondary`, transizione `200ms ease`
- Il contenuto principale ha `margin-left: 240px` e `max-width: calc(100% - 240px)`
- Nessuna bottom tab bar su desktop

---

### 3.3 Page Header

**Variante A — Large (sezioni principali: Home, Linee, Impostazioni)**

```
padding: 24px 20px 12px
titolo: Heading (20px, 700)
sottotitolo opzionale: Label (13px, 500) in --text-secondary
nessun border bottom
nessuna shadow
```

**Variante B — Navigation (pagine dettaglio: Fermata, Linea)**

```
height: 52px
position: sticky, top: 0
backdrop-filter: blur(20px)
background: rgba(--bg-primary, 0.9)
border-bottom: 1px solid var(--border) — appare solo dopo scroll (via IntersectionObserver)

[ChevronLeft 20px] [Indietro / Label]     [Titolo centrato, truncato]     [Azione destra opzionale]
```

Il titolo centrato è troncato con `text-overflow: ellipsis` se supera lo spazio disponibile. Il pulsante "Indietro" usa `ChevronLeft` (20px, stroke 1.75px) seguito dal testo "Indietro" in Label weight 500.

---

## 4. Pagine

### 4.1 Home (`/`)

**Layout verticale, sezioni in ordine:**

#### Hero
- Background: `--operator-tint` come sfondo della sezione (sottile tint del colore operatore)
- Nome operatore: Display (28px, 700) in `--text-primary`
- Frase contestuale basata sull'ora del giorno:
  - 5–11: "Buongiorno. Dove vai stamattina?"
  - 12–17: "Buon pomeriggio. Prossime partenze?"
  - 18–22: "Buona sera. Orari serali disponibili."
  - 23–4: "Servizio notturno attivo."
- Frase: Body in `--text-secondary`
- Padding: `32px 20px 24px`

#### Search bar
- Input prominente: height `52px`, radius `10px`, background `--bg-secondary`
- Icona `Search` (20px) a sinistra, colore `--text-tertiary`
- Placeholder: "Cerca fermata..." in `--text-tertiary`
- Focus: border `1px solid --operator-primary`, shadow `0 0 0 3px --operator-tint`
- Transizione: `200ms ease`

#### Sezione "Nelle vicinanze" (solo se geolocation abilitata)
- Titolo sezione: Subheading con icona `Navigation` (16px) inline a sinistra
- Lista fermate: fino a 5 risultati, ordinate per distanza
- Ogni riga fermata (vedi struttura comune sotto)
- Empty state se geolocation negata: icona `Navigation` (32px) + "Abilita la posizione per vedere le fermate vicine" in Body `--text-secondary` + bottone "Abilita" in `--operator-primary`

#### Sezione "Preferiti"
- Titolo: Subheading con icona `Star` (16px) inline
- Lista fermate preferite (max 5 in home, link "Vedi tutti" se > 5)
- Empty state: icona `Star` (32px) + "Le fermate preferite appariranno qui" in Body `--text-secondary`

#### Sezione "Recenti"
- Titolo: Subheading con icona `Clock` (16px) inline
- Lista ultime 5 fermate visitate
- Empty state: nessuno — la sezione non appare se vuota

#### Struttura riga fermata (comune a tutte le sezioni)
```
┌──────────────────────────────────────────────────────────┐
│  [dot colore linea]  Nome Fermata          [badge linea] │
│                      14:32  ·  Linea 42   →              │
└──────────────────────────────────────────────────────────┘
```
- Padding: `12px 0`
- Border bottom: `1px solid var(--border)` tranne l'ultimo elemento
- Nome fermata: Body (15px, 400) in `--text-primary`
- Orario prossima partenza: Mono (15px, 600) in `--operator-primary`
- Badge linea: Caption (11px, 500), background colore linea, testo `--color-text-on-primary`, radius 4px, padding `2px 6px`
- Tap → naviga a `/stop/:stopId`
- Nessun chevron a destra — l'intera riga è tappable

---

### 4.2 Linee (`/lines`)

**Header Variante A Large:**
- Titolo: "Linee"
- Nessun sottotitolo

**Search bar:**
- Identica a Home, placeholder "Cerca linea..."

**Chip filtro tipo (scrollabili orizzontalmente):**
- Chip: height `36px`, padding `0 14px`, radius `10px`, background `--bg-secondary`
- Chip attivo: background `--operator-tint`, testo `--operator-primary`, border `1px solid --operator-primary` a 30% opacity
- Icone Lucide a sinistra del label: `Bus`, `TramFront`, `TrainFront`, `Ship` — dimensione 16px
- Label: Label (13px, 500)
- Nessuna emoji. Nessun chip "Tutti" con emoji
- Scroll orizzontale senza scrollbar visibile (`scrollbar-width: none`)
- Transizione chip: `background 150ms ease, color 150ms ease`

**Lista linee:**
- Divider testuale tra tipi se raggruppato: Subheading in `--text-tertiary`, `padding: 20px 20px 8px`
- Ogni riga linea:
  ```
  ┌──────────────────────────────────────────────────────────┐
  │  [Badge]  Nome Linea                Destinazione →       │
  └──────────────────────────────────────────────────────────┘
  ```
  - Badge linea: come sopra, 32px × 32px min-width, centrato, radius 8px
  - Nome linea: Body (15px, 400) in `--text-primary`
  - Destinazione: Label (13px, 500) in `--text-secondary`
  - Padding: `14px 20px`
  - Border bottom `1px solid var(--border)`
  - Tap → `/lines/:lineId`

---

### 4.3 Dettaglio Linea (`/lines/[lineId]`)

**Header Variante B Navigation:**
- Titolo: nome della linea (es. "Linea 42")
- Azione destra: nessuna (la condivisione non è necessaria qui)

**LineBadge grande:**
- 48px × 48px, radius 12px, numero linea Heading (20px, 700), colore da `config.json`
- Sotto il badge: nome completo della linea Body + tipo (Bus/Tram/...) con icona Lucide 16px

**Direction switcher:**
- Due pill in toggle group, non dropdown, non segmented control
- Struttura:
  ```
  ┌────────────────────────────────────────┐
  │  [pill A: direzione andata]            │
  │  [pill B: direzione ritorno]           │
  └────────────────────────────────────────┘
  ```
- Pill attivo: background `--operator-primary`, testo `--color-text-on-primary`
- Pill inattivo: background `--bg-secondary`, testo `--text-secondary`
- Height: `40px`, radius `10px`, transizione `200ms ease`
- Le label delle pill sono le destinazioni finali (es. "Verso Centro" / "Verso Aeroporto")

**Stop list — timeline:**
- Lista verticale con connettore linea sinistra
- Connettore: linea verticale `2px solid --operator-primary` a 30% opacity, con dot `8px × 8px` `--operator-primary` a ogni fermata
- Prima e ultima fermata: dot filled `--operator-primary`
- Fermate intermedie: dot outlined (border `2px solid --operator-primary` a 50%, background trasparente)
- Nome fermata: Body in `--text-primary`
- Padding per fermata: `12px 0 12px 20px` (20px per il connettore)
- Fermata corrente (real-time): dot verde `#34C759` animato pulse, nome in `--operator-primary`, sfondo riga `--operator-tint`
- Tap su fermata → naviga a `/stop/:stopId`

---

### 4.4 Dettaglio Fermata (`/stop/[stopId]`) — pagina principale

Questa è la pagina più importante del prodotto. È il target dei QR code fisici. Deve essere funzionale in 2 secondi, senza scroll, su qualsiasi telefono.

**Header Variante B Navigation:**
- Titolo: nome fermata (truncato con ellipsis)
- Azione destra: icona `Share2` (20px) — `navigator.share()` con fallback copia link

**Above the fold: sezione "Prossime partenze"**

Questa sezione occupa l'intero viewport sopra il fold. È il contenuto primario.

```
┌────────────────────────────────────────────────────────────────┐
│  PROSSIME PARTENZE                              [RefreshCw]    │
│                                                                │
│  ┌──────────────────────────────────────────────────────────┐  │
│  │  [Badge 42]  Verso Centro           3 min  [● live]     │  │
│  │  [Badge 7]   Verso Stazione        14:35               │  │
│  │  [Badge 42]  Verso Centro           7 min  [● live]     │  │
│  │  [Badge 15]  Verso Aeroporto       14:52               │  │
│  └──────────────────────────────────────────────────────────┘  │
│                                                                │
│  [AlertCircle]  Nessun ritardo segnalato                       │
└────────────────────────────────────────────────────────────────┘
```

**Specifiche DepartureRow:**
- Card: background `--bg-elevated`, shadow `--shadow-md`, radius `16px`, padding `16px 20px`
- Ogni riga partenza: `padding: 12px 0`, border bottom `1px solid var(--border)` (escluso ultimo)
- **Badge linea:** a sinistra, `36px × 36px`, radius `8px`
- **Headsign (destinazione):** Body (15px, 400) in `--text-primary`, flex-grow
- **Countdown o orario assoluto:** Mono (15px, 600) in `--operator-primary`, `min-width: 48px`, text-align right
  - Se < 1 minuto: "Ora" in `--operator-primary`
  - Se 1–59 minuti: "X min" dove X è il numero intero
  - Se >= 60 minuti: orario assoluto "HH:MM"
- **Indicatore real-time:**
  - Dot verde `#34C759`, 6px × 6px, radius full, animation `pulse` (opacity 1 → 0.4 → 1, 1.5s infinite)
  - Appare a destra dell'orario, solo se `isLive === true`
- **Ritardo:**
  - Orario originale barrato in `--text-tertiary` con `text-decoration: line-through`
  - Nuovo orario in arancione `#FF9500`
  - I due elementi sono inline, separati da spazio
- **Refresh button:** icona `RefreshCw` (20px) in alto a destra nella sezione. Quando in loading: `animation: spin 1s linear infinite`. Transizione al completamento: spin si ferma in 300ms.
- **Empty state prossime partenze:**
  - `AlertCircle` (32px) in `--text-tertiary`
  - "Nessuna partenza nelle prossime ore" in Body `--text-secondary`
  - Nessuna emoji, nessun testo drammatico

**Sezione "Orario" (below the fold)**

```
┌────────────────────────────────────────────────────────────────┐
│  ORARIO                                                        │
│                                                                │
│  [Lun–Ven]  [Sabato]  [Domenica/Festivi]                       │
│                                                                │
│  06:00  07:30  09:15  11:00  13:30                             │
│  15:00  17:30  19:15  21:00                                    │
└────────────────────────────────────────────────────────────────┘
```

**DayGroupTabs ridisegnati:**
- Container scroll orizzontale, nessuna scrollbar visibile
- Ogni tab è una pill: height `32px`, padding `0 14px`, radius `9999px`
- Tab attiva: background `--operator-primary`, testo `--color-text-on-primary` (Caption, 11px, 500)
- Tab inattiva: background `--bg-secondary`, testo `--text-secondary`
- Transizione `150ms ease`
- Nessuna tab "Tutti" — solo i day group presenti nei dati

**Lista orari:**
- Layout a griglia fluida: `display: flex; flex-wrap: wrap; gap: 8px`
- Ogni orario: Label (13px, 500) in `--text-primary`, padding `4px 8px`, radius `6px`, background `--bg-secondary`
- Orario corrente (quello più vicino): background `--operator-tint`, testo `--operator-primary`
- Orari passati (nel giorno): `--text-tertiary`

---

## 5. Responsive

### Mobile (< 1024px)

- Bottom tab bar attiva (vedi sezione 3.1)
- Layout a singola colonna
- `max-width: 512px` centrato con `margin: 0 auto`
- Padding orizzontale: `20px`
- Nessuna sidebar
- Il contenuto va in `padding-bottom: calc(64px + env(safe-area-inset-bottom) + 16px)` per non essere coperto dalla tab bar

### Desktop (≥ 1024px)

- Sidebar sinistra fissa 240px (vedi sezione 3.2)
- Nessuna bottom tab bar
- Contenuto principale: `margin-left: 240px`, `max-width: 768px` all'interno, `padding: 40px`
- Cards e liste si espandono fino a `max-width: 768px` e poi si fermano
- Le pagine dettaglio (Fermata, Linea) usano il Page Header Variante B senza sticky-on-scroll (già nella sidebar il contesto di navigazione è sempre visibile)

---

## 6. Dark Mode

### Gestione

Il toggle si trova in Impostazioni, primo elemento della pagina, implementato con un `<Switch>`.

```vue
<!-- Struttura semantica del toggle -->
<div class="setting-row">
  <div class="setting-label">
    <component :is="isDark ? Moon : SunMedium" :size="20" />
    <span>Tema scuro</span>
  </div>
  <Switch v-model="isDark" />
</div>
```

**Logica:**
1. Al mount: leggere `localStorage.getItem('theme')`. Se presente, applicare la classe `dark` o `light` su `<html>`.
2. Se assente: rispettare `prefers-color-scheme`.
3. Al toggle: aggiornare la classe su `<html>` + `localStorage.setItem('theme', value)`.
4. Il composable `useTheme()` espone `isDark: Ref<boolean>` e `toggle()`.

### Comportamento visivo

- Tutte le CSS variables si adattano automaticamente tramite il selettore `.dark` su `<html>`
- Nessuna immagine statica che non supporti dark mode — usare solo icone Lucide + colori CSS
- Shadow in dark mode: le shadow standard si riducono in visibilità automaticamente per il contrasto ridotto — questo è il comportamento corretto, non sovrascrivere

---

## 7. Animazioni e transizioni

**Principio:** il movimento supporta la comprensione, non intrattiene.

| Elemento | Transizione |
|----------|-------------|
| Tab/link hover | `color 200ms ease, background 200ms ease` |
| Chip filtro attivo | `background 150ms ease, color 150ms ease` |
| Page header border (scroll) | `opacity 300ms ease` |
| RefreshCw loading | `spin 1s linear infinite` |
| Live dot pulse | `opacity 1→0.4→1, 1.5s ease-in-out infinite` |
| DayGroupTab attiva | `background 150ms ease` |
| Orario corrente highlight | nessuna animazione — statico |
| Page transition (Nuxt) | `fade: opacity 150ms ease` |

**No:** bounce, scala, slide dal basso per elementi UI normali. Queste animazioni si riservano alle interazioni gestuali (bottom sheet, drag), non implementate in questa fase.

---

## 8. Empty states e error states

**Regola generale:** ogni empty state ha icona Lucide (32px, `--text-tertiary`), titolo Body in `--text-secondary`, e zero emoji.

| Stato | Icona | Testo |
|-------|-------|-------|
| Nessuna partenza | `AlertCircle` | "Nessuna partenza nelle prossime ore" |
| Nessun preferito | `Star` | "Le fermate preferite appariranno qui" |
| Nessuna fermata recente | — | (sezione non appare) |
| Geolocation negata | `Navigation` | "Abilita la posizione per vedere le fermate vicine" |
| Ricerca senza risultati | `Search` | "Nessuna fermata trovata per "{query}"" |
| Errore di rete (GTFS-RT) | — | Silenzioso — si mostra solo `isLive = false`, nessun errore visibile |
| Errore caricamento dati | `AlertCircle` | "Impossibile caricare gli orari. Riprova." + bottone `RefreshCw` |

---

## 9. Componenti Vue — API pubblica

### `LineBadge.vue`

```vue
<LineBadge :line="line" size="sm" />
<!-- size: 'sm' (24px) | 'md' (36px) | 'lg' (48px) -->
```

### `DepartureRow.vue`

```vue
<DepartureRow :departure="dep" :is-live="isLive" />
```

### `DayGroupTabs.vue`

```vue
<DayGroupTabs v-model="selectedGroup" :groups="dayGroups" />
```

### `StopRow.vue` (nuovo componente)

```vue
<StopRow :stop="stop" :next-departure="departure" @click="navigate" />
```

### `PageHeaderLarge.vue`

```vue
<PageHeaderLarge title="Linee" subtitle="12 linee disponibili" />
```

### `PageHeaderNav.vue`

```vue
<PageHeaderNav title="Fermata Roma" @back="router.back()">
  <template #action>
    <button @click="share"><Share2 :size="20" /></button>
  </template>
</PageHeaderNav>
```

### `TabBar.vue`

Componente globale in layout, visibile solo su mobile. Gestisce la logica active-state tramite `useRoute()`.

### `Sidebar.vue`

Componente globale in layout, visibile solo su desktop (≥ 1024px). Stessa logica di TabBar per lo stato attivo.

---

## 10. SEO e accessibilità

### SEO (invariato da spec precedente)

- `<title>`: `{stopName} — {operatorName}`
- `<meta description>`: `Orari e prossime partenze dalla fermata {stopName}. Linee: {lines}.`
- `og:title`, `og:description`, `og:image` (screenshot generato della fermata)

### Accessibilità

- Tutti i link e bottoni interattivi: `aria-label` descrittivo quando il contenuto visivo non è sufficiente
- Tab bar: `role="navigation"`, ogni link ha `aria-current="page"` quando attivo
- Live indicator: `aria-live="polite"` sul container delle prossime partenze
- Focus outline: `2px solid --operator-primary` con `outline-offset: 2px` — mai rimosso
- Contrasto testi: ratio minimo 4.5:1 per body text, 3:1 per testi grandi (Heading/Display)
- `prefers-reduced-motion`: se attivo, disabilitare pulse animation e page transition

---

## 11. Struttura file aggiornata

```
web/
├── nuxt.config.ts
├── app.config.ts
├── middleware/
│   └── operator.global.ts
├── composables/
│   ├── useOperator.ts
│   ├── useRealtime.ts
│   └── useTheme.ts            ← nuovo: dark mode toggle + localStorage
├── utils/
│   ├── operators.ts
│   ├── schedule.ts
│   └── time.ts                ← nuovo: formatCountdown, formatTime helpers
├── assets/
│   └── css/
│       └── design-system.css  ← CSS variables, tipografia, shadow system
├── components/
│   ├── LineBadge.vue
│   ├── DepartureRow.vue
│   ├── DayGroupTabs.vue
│   ├── StopRow.vue            ← nuovo
│   ├── PageHeaderLarge.vue    ← nuovo
│   ├── PageHeaderNav.vue      ← nuovo
│   ├── TabBar.vue             ← nuovo
│   └── Sidebar.vue            ← nuovo
└── pages/
    ├── index.vue
    ├── lines/
    │   ├── index.vue
    │   └── [lineId].vue
    ├── stop/
    │   └── [stopId].vue
    └── settings.vue           ← nuovo: dark mode toggle
```

---

## 12. Checklist di qualità pre-consegna

Prima di considerare l'implementazione completa, verificare ogni punto:

- [ ] Nessuna emoji in tutta l'interfaccia — solo Lucide
- [ ] Plus Jakarta Sans caricata correttamente, fallback `sans-serif`
- [ ] Tutti gli orari usano il font Mono (`tabular-nums`)
- [ ] Dark mode funzionante con persistenza localStorage
- [ ] Bottom tab bar corretta su iOS Safari (safe-area-inset)
- [ ] Sidebar visibile solo su desktop (≥ 1024px)
- [ ] Sezione "Prossime partenze" above the fold su iPhone SE (375px)
- [ ] Empty states presenti per tutti gli stati vuoti
- [ ] `aria-label` su tutti i bottoni icon-only
- [ ] `prefers-reduced-motion` rispettato per le animazioni
- [ ] Contrasto testi verificato in light e dark mode
- [ ] Chip filtro tipo senza emoji — solo Lucide
- [ ] Direction switcher pill, non dropdown
- [ ] RefreshCw spin durante il refresh real-time
- [ ] Live dot pulse solo quando `isLive === true`
- [ ] Border bottom del Page Header appare solo dopo scroll (IntersectionObserver)
