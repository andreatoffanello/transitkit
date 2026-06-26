export type Locale = 'it' | 'en'

export interface AppStrings {
  // DepartureRow
  now: string
  minutes: string

  // Stop page
  noDepartures: string
  stopNotFound: string
  updatedRealtime: string
  backToHome: string

  // Lines list
  all: string
  linesPageTitle: string
  linesPageDescription: string

  // Line detail
  lineNotFound: string
  backToLines: string
  noStopsFound: string
  noStopsFoundHint: string

  // 404 catch-all
  pageNotFound: string
  pageNotFoundHint: string
  pageNotFoundTitle: string
  pageNotFoundBody: string
  pageNotFoundBack: string

  // Home page
  linesAndSchedules: string
  officialApp: string
  contacts: string
  officialWebsite: string
  privacy: string
  resources: string
  recentStops: string
  favoriteStops: string
  addToFavorites: string
  removeFromFavorites: string

  // Day groups
  weekdayLabels: {
    mon: string; tue: string; wed: string; thu: string
    fri: string; sat: string; sun: string
  }
  weekdayGroupNames: {
    weekdays: string
    everyday: string
    weekdaysSat: string
  }

  // Transit types
  transitTypes: {
    bus: string; tram: string; metro: string; rail: string
    ferry: string; cable_tram: string; gondola: string
    funicular: string; trolleybus: string; monorail: string
  }

  // Empty state
  noDeparturesToday: string
  noDeparturesHint: string
  nextServiceDay: string

  // Realtime status
  schedulesNotLive: string

  // Section headers – stop page
  upcomingDepartures: string
  todaySchedule: string

  // Aria labels
  ariaLoading: string
  ariaLinesAtStop: string
  ariaDirections: string
  ariaRealtimeData: string
  ariaLinesOfTypePrefix: string

  // Scheduled time label (shown when realtime delay exists)
  scheduledTime: string

  // Component prefixes
  dockPrefix: string

  // LineBadge
  lineLabel: string

  // Lines list — stop count
  stops: string

  // Home page — stop search
  searchStops: string

  // Lines list — search
  searchLines: string

  // Lines list — empty state
  noLinesFound: string
  clearFilters: string

  // Lines list — filtered results count
  linesFound: string

  // SEO meta description fragments
  schedulesAndDepartures: string
  stopsAndSchedules: string

  // Stop position in network
  stopInNetwork: string
  stopPosition: string
  stopPositionOf: string

  // Live document.title fragments
  nextDepartureIn: string
  minutesShort: string

  // Stop page — anchor to full schedule
  viewFullSchedule: string

  // Stop page — next departure hint in empty upcoming state
  nextDepartureToday: string

  // Home page — schedule freshness
  schedulesUpdated: string
  schedulesValidUntil: string

  // Home page — onboarding empty state
  onboardingHint: string

  // Line count words (singular/plural)
  lineSingular: string
  linePlural: string

  // Realtime last-updated timestamp
  updatedAt: string

  // Stop page — copy link fallback (desktop)
  copyLink: string
  copiedFeedback: string

  // Stop page — tab labels

  // Stop page — actions
  refresh: string
  share: string

  // Stop page — next departure badge
  nextDepartureLabel: string

  // Stop page — show more departures
  showMore: string

  // Accessibility
  skipToContent: string

  // Realtime delay aria-label
  minutesDelay: string

  // Home page — nearby stops
  nearbyStops: string
  locating: string
  distanceM: string
  distanceKm: string

  // App download banner
  downloadApp: string


  // Realtime degraded notice (used in UpcomingPanel when GTFS-RT is down)
  realtimeUnavailable: string

  // Bottom tab bar (mobile)
  tabHome: string
  tabLines: string

  // Home — operator info card (parity native)
  homeOperatorsTitle: string
  homeOperatorsAttribution: string // "Dati forniti da {operator}"
  homeOperatorRoutes: string       // "{n} linee"
  homeOperatorLiveCount: string    // "{n} mezzi live"
  homeFooterDisclaimer: string     // "App dei cittadini, non gestita da {operator}"

  // Home — search & onboarding
  startSearch: string
  clearSearch: string
  noStopsFoundForQuery: string    // "No stops found for "{query}""
  searchInLines: string
  featuredLines: string
  allLines: string
  schedulesExpiringSoon: string

  // Home — schedule validity
  validUntil: string              // "Valid until {date}"



  // 404 page





  // PageHeader
  back: string
  backDefault: string


  // AppDownloadBanner
  closeBanner: string

  // AppSidebar / AppTabBar aria
  mainNavAriaLabel: string

  // OG / manifest
  ogTagline: string
  manifestDescription: string
  manifestShortcutLines: string
  manifestShortcutLinesSub: string
  manifestShortcutFavorites: string
  manifestShortcutFavoritesSub: string
  manifestFallbackDescription: string

  // Stop page — inline departures section header
  today: string

  // Trip detail (svolgimento corsa)
  tripNoData: string
  tripTitlePrefix: string // "Linea " → "Linea 12"

  // app.vue error boundary
  errorPageNotFound: string
  errorServiceUnavailable: string
  errorLoadSchedules: string
  errorUnexpected: string
  errorRetry: string
  errorSkipToContent: string
}

const IT: AppStrings = {
  now: 'Ora',
  minutes: 'min',
  noDepartures: 'Nessuna partenza nelle prossime 2 ore.',
  stopNotFound: 'Fermata non trovata',
  updatedRealtime: 'Aggiornato in tempo reale',
  backToHome: 'Torna alla home',
  all: 'Tutti',
  linesPageTitle: 'Linee',
  linesPageDescription: 'Tutte le linee di',
  lineNotFound: 'Linea non trovata',
  backToLines: 'Linee',
  noStopsFound: 'Nessuna fermata trovata',
  noStopsFoundHint: 'Non ci sono fermate disponibili per questa direzione.',
  pageNotFound: 'Pagina non trovata',
  pageNotFoundHint: 'La pagina che cerchi non esiste. Prova a cercare una linea.',
  pageNotFoundTitle: 'Pagina non trovata',
  pageNotFoundBody: 'La pagina che stai cercando non esiste o è stata spostata.',
  pageNotFoundBack: 'Torna alla home',
  linesAndSchedules: 'Orari e linee',
  officialApp: 'App ufficiale',
  contacts: 'Contatti',
  officialWebsite: 'Sito ufficiale',
  privacy: 'Privacy policy',
  resources: 'Link utili',
  recentStops: 'Fermate recenti',
  favoriteStops: 'Preferiti',
  addToFavorites: 'Aggiungi ai preferiti',
  removeFromFavorites: 'Rimuovi dai preferiti',
  weekdayLabels: {
    mon: 'Lun', tue: 'Mar', wed: 'Mer', thu: 'Gio',
    fri: 'Ven', sat: 'Sab', sun: 'Dom',
  },
  weekdayGroupNames: {
    weekdays: 'Lun-Ven',
    everyday: 'Ogni giorno',
    weekdaysSat: 'Lun-Sab',
  },
  transitTypes: {
    bus: 'Bus', tram: 'Tram', metro: 'Metro', rail: 'Treno',
    ferry: 'Ferry', cable_tram: 'Funivia', gondola: 'Gondola',
    funicular: 'Funicolare', trolleybus: 'Filobus', monorail: 'Monorotaia',
  },
  noDeparturesToday: 'Nessuna partenza oggi',
  noDeparturesHint: 'Non ci sono corse programmate per oggi su questa fermata.',
  nextServiceDay: 'Prossima corsa',
  schedulesNotLive: 'Orari non in tempo reale',
  upcomingDepartures: 'Prossime partenze',
  todaySchedule: 'Orari di oggi',
  ariaLoading: 'Caricamento',
  ariaLinesAtStop: 'Linee che fermano qui',
  ariaDirections: 'Direzioni',
  ariaRealtimeData: 'Dati in tempo reale',
  ariaLinesOfTypePrefix: 'Linee',
  scheduledTime: 'orario prev.',
  dockPrefix: 'Dock ',
  lineLabel: 'Linea',
  stops: 'fermate',
  searchStops: 'Cerca fermata...',
  searchLines: 'Cerca linea…',
  noLinesFound: 'Nessuna linea trovata',
  clearFilters: 'Cancella filtri',
  linesFound: 'linee trovate',
  schedulesAndDepartures: 'Orari e partenze',
  stopsAndSchedules: 'Fermate e orari',
  stopInNetwork: 'Questa fermata nella rete',
  stopPosition: 'fermata',
  stopPositionOf: 'di',
  nextDepartureIn: 'in',
  minutesShort: 'min',
  viewFullSchedule: 'Vedi orari completi',
  nextDepartureToday: 'Prossima oggi',
  schedulesUpdated: 'Orari aggiornati al',
  schedulesValidUntil: 'Validi fino al',
  onboardingHint: 'Cerca le linee per trovare e salvare le tue fermate preferite',
  lineSingular: 'linea',
  linePlural: 'linee',
  updatedAt: 'Aggiornato alle',
  copyLink: 'Copia link',
  copiedFeedback: 'Copiato!',
  skipToContent: 'Vai al contenuto principale',
  minutesDelay: 'minuti di ritardo',
  nearbyStops: 'Fermate vicine',
  locating: 'Localizzazione in corso...',
  distanceM: 'm',
  distanceKm: 'km',
  refresh: 'Aggiorna',
  share: 'Condividi',
  nextDepartureLabel: 'prossima',
  showMore: 'Mostra altri',
  downloadApp: 'Scarica app',
  realtimeUnavailable: 'Orari non in tempo reale',
  tabHome: 'Home',
  tabLines: 'Linee',
  homeOperatorsTitle: 'Chi muove la città',
  homeOperatorsAttribution: 'Orari e mezzi in tempo reale di {operator}',
  homeOperatorRoutes: '{n} linee',
  homeOperatorLiveCount: '{n} mezzi live',
  homeFooterDisclaimer: 'App dei cittadini. Non gestita da {operator}.',

  startSearch: 'Inizia la tua ricerca',
  clearSearch: 'Cancella ricerca',
  noStopsFoundForQuery: 'Nessuna fermata trovata',
  searchInLines: 'Cerca nelle linee',
  featuredLines: 'Linee consigliate',
  allLines: 'Tutte le linee',
  schedulesExpiringSoon: 'In scadenza',

  validUntil: 'Valido fino al {date}',








  back: 'Indietro',
  backDefault: 'Indietro',


  closeBanner: 'Chiudi',

  mainNavAriaLabel: 'Navigazione principale',

  ogTagline: 'Orari e linee',
  manifestDescription: 'Orari e fermate in tempo reale',
  manifestShortcutLines: 'Linee',
  manifestShortcutLinesSub: 'Vedi tutte le linee',
  manifestShortcutFavorites: 'Preferiti',
  manifestShortcutFavoritesSub: 'Fermate preferite',
  manifestFallbackDescription: 'Orari e fermate in tempo reale',

  today: 'Oggi',
  tripNoData: 'Dettaglio corsa non disponibile',
  tripTitlePrefix: 'Linea ',

  errorPageNotFound: 'Pagina non trovata',
  errorServiceUnavailable: 'Servizio temporaneamente non disponibile',
  errorLoadSchedules: 'Impossibile caricare gli orari. Riprova tra qualche minuto.',
  errorUnexpected: 'Si è verificato un errore imprevisto.',
  errorRetry: 'Riprova',
  errorSkipToContent: 'Vai al contenuto principale',
}

const EN: AppStrings = {
  now: 'Now',
  minutes: 'min',
  noDepartures: 'No departures in the next 2 hours.',
  stopNotFound: 'Stop not found',
  updatedRealtime: 'Updated in real time',
  backToHome: 'Back to home',
  all: 'All',
  linesPageTitle: 'Lines',
  linesPageDescription: 'All lines of',
  lineNotFound: 'Line not found',
  backToLines: 'Lines',
  noStopsFound: 'No stops found',
  noStopsFoundHint: 'There are no stops available for this direction.',
  pageNotFound: 'Page not found',
  pageNotFoundHint: 'The page you\'re looking for doesn\'t exist. Try browsing the lines.',
  pageNotFoundTitle: 'Page not found',
  pageNotFoundBody: 'The page you\'re looking for doesn\'t exist or has been moved.',
  pageNotFoundBack: 'Back to home',
  linesAndSchedules: 'Schedules & Lines',
  officialApp: 'Official App',
  contacts: 'Contact',
  officialWebsite: 'Official Website',
  privacy: 'Privacy policy',
  resources: 'Resources',
  recentStops: 'Recent stops',
  favoriteStops: 'Favorites',
  addToFavorites: 'Add to favorites',
  removeFromFavorites: 'Remove from favorites',
  weekdayLabels: {
    mon: 'Mon', tue: 'Tue', wed: 'Wed', thu: 'Thu',
    fri: 'Fri', sat: 'Sat', sun: 'Sun',
  },
  weekdayGroupNames: {
    weekdays: 'Mon-Fri',
    everyday: 'Every day',
    weekdaysSat: 'Mon-Sat',
  },
  transitTypes: {
    bus: 'Bus', tram: 'Tram', metro: 'Metro', rail: 'Rail',
    ferry: 'Ferry', cable_tram: 'Cable Car', gondola: 'Gondola',
    funicular: 'Funicular', trolleybus: 'Trolleybus', monorail: 'Monorail',
  },
  noDeparturesToday: 'No departures today',
  noDeparturesHint: 'There are no scheduled trips for today at this stop.',
  nextServiceDay: 'Next service',
  schedulesNotLive: 'Schedules may not be live',
  upcomingDepartures: 'Upcoming departures',
  todaySchedule: "Today's schedule",
  ariaLoading: 'Loading',
  ariaLinesAtStop: 'Lines stopping here',
  ariaDirections: 'Directions',
  ariaRealtimeData: 'Real-time data',
  ariaLinesOfTypePrefix: 'Lines',
  scheduledTime: 'sched.',
  dockPrefix: 'Dock ',
  lineLabel: 'Line',
  stops: 'stops',
  searchStops: 'Search stop...',
  searchLines: 'Search line…',
  noLinesFound: 'No lines found',
  clearFilters: 'Clear filters',
  linesFound: 'lines found',
  schedulesAndDepartures: 'Schedules and departures',
  stopsAndSchedules: 'Stops and schedules',
  stopInNetwork: 'This stop in the network',
  stopPosition: 'stop',
  stopPositionOf: 'of',
  nextDepartureIn: 'in',
  minutesShort: 'min',
  viewFullSchedule: 'View full schedule',
  nextDepartureToday: 'Next today',
  schedulesUpdated: 'Schedules updated',
  schedulesValidUntil: 'Valid until',
  onboardingHint: 'Browse lines to find and save your favorite stops',
  lineSingular: 'line',
  linePlural: 'lines',
  updatedAt: 'Updated at',
  copyLink: 'Copy link',
  copiedFeedback: 'Copied!',
  skipToContent: 'Skip to main content',
  minutesDelay: 'minutes late',
  nearbyStops: 'Nearby stops',
  locating: 'Locating...',
  distanceM: 'm',
  distanceKm: 'km',
  refresh: 'Refresh',
  share: 'Share',
  nextDepartureLabel: 'next',
  showMore: 'Show more',
  downloadApp: 'Get the app',
  realtimeUnavailable: 'Live updates unavailable',
  tabHome: 'Home',
  tabLines: 'Lines',
  homeOperatorsTitle: 'Who moves the city',
  homeOperatorsAttribution: 'Schedules and live vehicles from {operator}',
  homeOperatorRoutes: '{n} routes',
  homeOperatorLiveCount: '{n} vehicles live',
  homeFooterDisclaimer: 'A community app. Not affiliated with {operator}.',

  startSearch: 'Start your search',
  clearSearch: 'Clear search',
  noStopsFoundForQuery: 'No stops found',
  searchInLines: 'Search in lines',
  featuredLines: 'Featured lines',
  allLines: 'All lines',
  schedulesExpiringSoon: 'Expiring soon',

  validUntil: 'Valid until {date}',








  back: 'Back',
  backDefault: 'Back',


  closeBanner: 'Close',

  mainNavAriaLabel: 'Main navigation',

  ogTagline: 'Schedules & Lines',
  manifestDescription: 'Real-time schedules and stops',
  manifestShortcutLines: 'Lines',
  manifestShortcutLinesSub: 'View all lines',
  manifestShortcutFavorites: 'Favorites',
  manifestShortcutFavoritesSub: 'Favorite stops',
  manifestFallbackDescription: 'Real-time schedules and stops',

  today: 'Today',
  tripNoData: 'Trip details unavailable',
  tripTitlePrefix: 'Line ',

  errorPageNotFound: 'Page not found',
  errorServiceUnavailable: 'Service temporarily unavailable',
  errorLoadSchedules: 'Could not load schedules. Please try again in a moment.',
  errorUnexpected: 'An unexpected error occurred.',
  errorRetry: 'Retry',
  errorSkipToContent: 'Skip to main content',
}

const STRINGS: Record<Locale, AppStrings> = { it: IT, en: EN }

export function getStrings(locale: string | undefined): AppStrings {
  const lang = (locale ?? 'it').split('-')[0]?.toLowerCase() as Locale
  return STRINGS[lang] ?? IT
}
