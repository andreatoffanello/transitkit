export type Locale = 'it' | 'en'

export interface AppStrings {
  // DepartureRow
  now: string
  minutes: string

  // Stop page
  noDepartures: string
  stopNotFound: string
  updatedRealtime: string
  openInGoogleMaps: string
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

  // Home page
  linesAndSchedules: string
  officialApp: string
  contacts: string
  officialWebsite: string
  privacy: string
  recentStops: string

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
  ariaDayGroupTabs: string
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
}

const IT: AppStrings = {
  now: 'Ora',
  minutes: 'min',
  noDepartures: 'Nessuna partenza nelle prossime 2 ore.',
  stopNotFound: 'Fermata non trovata',
  updatedRealtime: 'Aggiornato in tempo reale',
  openInGoogleMaps: 'Apri in Google Maps',
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
  linesAndSchedules: 'Orari e linee',
  officialApp: 'App ufficiale',
  contacts: 'Contatti',
  officialWebsite: 'Sito ufficiale',
  privacy: 'Privacy policy',
  recentStops: 'Fermate recenti',
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
  ariaDayGroupTabs: 'Gruppi orari',
  ariaDirections: 'Direzioni',
  ariaRealtimeData: 'Dati in tempo reale',
  ariaLinesOfTypePrefix: 'Linee',
  scheduledTime: 'orario prev.',
  dockPrefix: 'Dock ',
  lineLabel: 'Linea',
  stops: 'fermate',
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
}

const EN: AppStrings = {
  now: 'Now',
  minutes: 'min',
  noDepartures: 'No departures in the next 2 hours.',
  stopNotFound: 'Stop not found',
  updatedRealtime: 'Updated in real time',
  openInGoogleMaps: 'Open in Google Maps',
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
  linesAndSchedules: 'Schedules & Lines',
  officialApp: 'Official App',
  contacts: 'Contact',
  officialWebsite: 'Official Website',
  privacy: 'Privacy policy',
  recentStops: 'Recent stops',
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
  ariaDayGroupTabs: 'Schedule groups',
  ariaDirections: 'Directions',
  ariaRealtimeData: 'Real-time data',
  ariaLinesOfTypePrefix: 'Lines',
  scheduledTime: 'sched.',
  dockPrefix: 'Dock ',
  lineLabel: 'Line',
  stops: 'stops',
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
}

const STRINGS: Record<Locale, AppStrings> = { it: IT, en: EN }

export function getStrings(locale: string | undefined): AppStrings {
  const lang = (locale ?? 'it').split('-')[0]?.toLowerCase() as Locale
  return STRINGS[lang] ?? IT
}
