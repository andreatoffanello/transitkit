// ---- Operator Config ----
export interface OperatorConfig {
  id: string
  name: string
  fullName: string
  url: string
  region: string
  country: string
  timezone: string
  locale: string[]
  theme: ThemeConfig
  store: StoreConfig
  map: MapConfig
  features: FeaturesConfig
  contact?: ContactConfig
  fares?: FareInfo
  pointsOfSale?: PointOfSale[]
  privacyUrl?: string
  gtfsRt?: GtfsRtConfig
  headsignMap?: Record<string, string>
}

export interface ThemeConfig {
  primaryColor: string
  accentColor: string
  textOnPrimary: string
  secondaryColor?: string
}

export interface StoreConfig {
  title: string
  subtitle: string
  keywords: string
}

export interface MapConfig {
  centerLat: number
  centerLng: number
  defaultZoom: number
}

export interface FeaturesConfig {
  enableMap: boolean
  enableGeolocation: boolean
  enableFavorites: boolean
  enableNotifications: boolean
}

export interface ContactConfig {
  phone?: string
  email?: string
}

export interface GtfsRtConfig {
  vehicle_positions?: string
  trip_updates?: string
  service_alerts?: string
}

export interface FareInfo {
  types: FareType[]
  purchaseUrl?: string
  notes?: string
}

export interface FareType {
  name: string
  price: string
  notes?: string
}

export interface PointOfSale {
  name: string
  address?: string
  hours?: string
}

// ---- Schedule Data ----
export type TransitType =
  | 'bus' | 'tram' | 'metro' | 'rail' | 'ferry'
  | 'cable_tram' | 'gondola' | 'funicular' | 'trolleybus' | 'monorail'

export interface ScheduleData {
  operator: OperatorMeta
  lastUpdated: string
  validUntil: string
  headsigns: string[]
  lineNames: string[]
  routeIds: string[]
  tripIds: string[]
  routes: Route[]
  stops: ScheduleStop[]
  stopPatterns?: string[][]
}

export interface OperatorMeta {
  id: string
  name: string
  url: string
}

export interface Route {
  id: string
  name: string
  longName: string
  color: string
  textColor: string
  transitType: TransitType
  directions: RouteDirection[]
  route_url?: string
}

export interface RouteDirection {
  id: number
  headsign: string
  stopIds: string[]
  shape: [number, number][]
}

export interface ScheduleStop {
  id: string
  name: string
  lat: number
  lng: number
  lines: string[]
  departures: Record<string, (string | number)[][]>
  docks?: Dock[]
}

export interface Dock {
  letter: string
  lat: number
  lng: number
  lines: string[]
}

// ---- Resolved types ----
export interface Departure {
  id: string
  time: string
  lineName: string
  routeId: string
  headsign: string
  color: string
  textColor: string
  transitType: TransitType
  dock: string
  tripId?: string
  minutesFromMidnight: number
  realtimeDelay?: number
  isRealtime?: boolean
}

export interface DayGroup {
  id: string
  days: string[]
  displayLabel: string
}
