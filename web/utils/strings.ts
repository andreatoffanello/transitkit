export type Locale = 'it' | 'en'

export interface AppStrings {
  // DepartureRow
  now: string
  minutes: string

  // Stop page
  sectionNow: string
  sectionSchedule: string
  noDepartures: string
  noSchedule: string
  stopNotFound: string
  updatedRealtime: string
  openInGoogleMaps: string
  backToHome: string

  // Lines list
  noLines: string
  linesPageTitle: string

  // Line detail
  lineNotFound: string
  backToLines: string
  noStops: string
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

  // Day groups
  weekdayLabels: {
    mon: string; tue: string; wed: string; thu: string
    fri: string; sat: string; sun: string
  }
  weekdayGroupNames: {
    weekdays: string
    saturday: string
    sunday: string
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

  // Realtime status
  schedulesNotLive: string

  // Aria labels
  ariaLoading: string
  ariaLinesAtStop: string
  ariaDayGroupTabs: string
  ariaDirections: string
  ariaRealtimeData: string
  ariaGoBack: string

  // Component prefixes
  dockPrefix: string

  // LineBadge
  lineLabel: string
}

const IT: AppStrings = {
  now: 'Ora',
  minutes: 'min',
  sectionNow: 'Adesso',
  sectionSchedule: 'Orari',
  noDepartures: 'Nessuna partenza nelle prossime 2 ore.',
  noSchedule: 'Nessun orario disponibile.',
  stopNotFound: 'Fermata non trovata',
  updatedRealtime: 'Aggiornato in tempo reale',
  openInGoogleMaps: 'Apri in Google Maps',
  backToHome: 'Torna alla home',
  noLines: 'Nessuna linea disponibile.',
  linesPageTitle: 'Linee',
  lineNotFound: 'Linea non trovata',
  backToLines: 'Linee',
  noStops: 'Nessuna fermata disponibile per questa direzione.',
  noStopsFound: 'Nessuna fermata trovata',
  noStopsFoundHint: 'Non ci sono fermate disponibili per questa direzione.',
  pageNotFound: 'Pagina non trovata',
  pageNotFoundHint: 'La pagina che cerchi non esiste. Prova a cercare una linea.',
  linesAndSchedules: 'Orari e linee',
  officialApp: 'App ufficiale',
  contacts: 'Contatti',
  officialWebsite: 'Sito ufficiale',
  privacy: 'Privacy policy',
  weekdayLabels: {
    mon: 'Lun', tue: 'Mar', wed: 'Mer', thu: 'Gio',
    fri: 'Ven', sat: 'Sab', sun: 'Dom',
  },
  weekdayGroupNames: {
    weekdays: 'Lun-Ven',
    saturday: 'Sabato',
    sunday: 'Domenica',
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
  schedulesNotLive: 'Orari non in tempo reale',
  ariaLoading: 'Caricamento',
  ariaLinesAtStop: 'Linee che fermano qui',
  ariaDayGroupTabs: 'Gruppi orari',
  ariaDirections: 'Direzioni',
  ariaRealtimeData: 'Dati in tempo reale',
  ariaGoBack: 'Torna indietro',
  dockPrefix: 'Dock ',
  lineLabel: 'Linea',
}

const EN: AppStrings = {
  now: 'Now',
  minutes: 'min',
  sectionNow: 'Now',
  sectionSchedule: 'Schedule',
  noDepartures: 'No departures in the next 2 hours.',
  noSchedule: 'No schedule available.',
  stopNotFound: 'Stop not found',
  updatedRealtime: 'Updated in real time',
  openInGoogleMaps: 'Open in Google Maps',
  backToHome: 'Back to home',
  noLines: 'No lines available.',
  linesPageTitle: 'Lines',
  lineNotFound: 'Line not found',
  backToLines: 'Lines',
  noStops: 'No stops available for this direction.',
  noStopsFound: 'No stops found',
  noStopsFoundHint: 'There are no stops available for this direction.',
  pageNotFound: 'Page not found',
  pageNotFoundHint: 'The page you\'re looking for doesn\'t exist. Try browsing the lines.',
  linesAndSchedules: 'Schedules & Lines',
  officialApp: 'Official App',
  contacts: 'Contact',
  officialWebsite: 'Official Website',
  privacy: 'Privacy policy',
  weekdayLabels: {
    mon: 'Mon', tue: 'Tue', wed: 'Wed', thu: 'Thu',
    fri: 'Fri', sat: 'Sat', sun: 'Sun',
  },
  weekdayGroupNames: {
    weekdays: 'Mon-Fri',
    saturday: 'Saturday',
    sunday: 'Sunday',
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
  schedulesNotLive: 'Schedules may not be live',
  ariaLoading: 'Loading',
  ariaLinesAtStop: 'Lines stopping here',
  ariaDayGroupTabs: 'Schedule groups',
  ariaDirections: 'Directions',
  ariaRealtimeData: 'Real-time data',
  ariaGoBack: 'Go back',
  dockPrefix: 'Dock ',
  lineLabel: 'Line',
}

const STRINGS: Record<Locale, AppStrings> = { it: IT, en: EN }

export function getStrings(locale: string | undefined): AppStrings {
  const lang = (locale ?? 'it').split('-')[0]?.toLowerCase() as Locale
  return STRINGS[lang] ?? IT
}
