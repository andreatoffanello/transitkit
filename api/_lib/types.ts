export interface Operator {
  id: string;
  name: string;
  url: string | null;
  timezone: string;
  features: Record<string, boolean>;
}

export interface Route {
  id: string;
  operatorId: string;
  name: string;
  longName: string | null;
  color: string | null;
  textColor: string | null;
  transitType: number;
}

export interface Stop {
  id: string;
  operatorId: string;
  name: string;
  lat: number;
  lng: number;
  platformCode: string | null;
  dockLetter: string | null;
}

export interface RouteDirection {
  routeId: string;
  directionId: number;
  headsign: string | null;
  stopIds: string[];
  shapePolyline: string | null;
}

export interface Trip {
  id: string;
  operatorId: string;
  routeId: string;
  directionId: number;
  headsign: string | null;
  serviceDays: string[];
}

export interface StopTime {
  tripId: string;
  stopId: string;
  arrivalTime: string;
  departureTime: string;
  stopSequence: number;
}

export interface Departure {
  tripId: string;
  routeId: string;
  routeName: string;
  routeColor: string | null;
  routeTextColor: string | null;
  headsign: string | null;
  departureTime: string;
  serviceDays: string[];
}

export interface ScheduleResponse {
  operator: Operator;
  routes: (Route & { directions: RouteDirection[] })[];
  stops: (Stop & { departures: Departure[] })[];
  lastUpdated: string;
}
