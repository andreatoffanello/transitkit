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

  // Aria labels
  ariaLoading: string
  ariaLinesAtStop: string
  ariaDayGroupTabs: string
  ariaDirections: string
  ariaRealtimeData: string
  ariaGoBack: string

  // Component prefixes
  dockPrefix: string
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
  ariaLoading: 'Caricamento',
  ariaLinesAtStop: 'Linee che fermano qui',
  ariaDayGroupTabs: 'Gruppi orari',
  ariaDirections: 'Direzioni',
  ariaRealtimeData: 'Dati in tempo reale',
  ariaGoBack: 'Torna indietro',
  dockPrefix: 'Dock ',
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
  ariaLoading: 'Loading',
  ariaLinesAtStop: 'Lines stopping here',
  ariaDayGroupTabs: 'Schedule groups',
  ariaDirections: 'Directions',
  ariaRealtimeData: 'Real-time data',
  ariaGoBack: 'Go back',
  dockPrefix: 'Dock ',
}

const STRINGS: Record<Locale, AppStrings> = { it: IT, en: EN }

export function getStrings(locale: string | undefined): AppStrings {
  const lang = (locale ?? 'it').split('-')[0]?.toLowerCase() as Locale
  return STRINGS[lang] ?? IT
}
