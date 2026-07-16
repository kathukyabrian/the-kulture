export type VehicleStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE';
export type OccupancyStatus = 'LOW' | 'MEDIUM' | 'FULL';

export interface RouteResponse {
  id: string;
  routeNumber: string;
  name: string;
  origin: string;
  destination: string;
  description: string;
  active: boolean;
}

export interface LocationResponse {
  latitude: number;
  longitude: number;
  speedKph: number;
  recordedAt: string;
}

export interface CrewMemberResponse {
  id: string;
  displayName: string;
  role: 'DRIVER' | 'CONDUCTOR';
  rating: number;
}

export interface VehicleSummaryResponse {
  id: string;
  plateNumber: string;
  name: string;
  routeNumber: string;
  routeName: string;
  destination: string;
  status: VehicleStatus;
  occupancyStatus: OccupancyStatus;
  verified: boolean;
  etaMinutes: number;
  latestLocation: LocationResponse | null;
}

export interface VehicleDetailResponse {
  id: string;
  plateNumber: string;
  name: string;
  route: RouteResponse;
  status: VehicleStatus;
  occupancyStatus: OccupancyStatus;
  verified: boolean;
  wifiAvailable: boolean;
  bassLevel: number;
  screenCount: number;
  watcherCount: number;
  earningsToday: number;
  fleetPosition: number;
  latestLocation: LocationResponse | null;
  crew: CrewMemberResponse[];
}

export interface FleetAlertResponse {
  id: string;
  vehicleName: string;
  plateNumber: string;
  type: string;
  message: string;
  severity: string;
  resolved: boolean;
  createdAt: string;
}

export interface FleetOverviewResponse {
  activeVehicles: number;
  pendingVerification: number;
  liveAlerts: number;
  projectedRevenue: number;
  activeFleet: VehicleSummaryResponse[];
  alerts: FleetAlertResponse[];
}
