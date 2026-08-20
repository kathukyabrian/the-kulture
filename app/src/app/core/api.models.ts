export type VehicleStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE';
export type OccupancyStatus = 'LOW' | 'MEDIUM' | 'FULL';
export type ListingState = 'ACTIVE' | 'HIDDEN' | 'SUSPENDED';
export interface RouteGeometry { type: 'LineString'; coordinates: [number, number][]; }

export interface RouteResponse {
  id: string;
  routeNumber: string;
  name: string;
  origin: string;
  destination: string;
  description: string;
  active: boolean;
  geometry: RouteGeometry | null;
}

export interface RouteAdminRequest {
  routeNumber: string;
  name: string;
  origin: string;
  destination: string;
  description: string;
  active: boolean;
  geometry: RouteGeometry | null;
}

export interface LocationResponse {
  latitude: number;
  longitude: number;
  speedKph: number;
  recordedAt: string;
}

export interface VehicleLocationEvent {
  sampleId: string;
  vehicleId: string;
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
  listingState: ListingState;
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
  listingState: ListingState;
  wifiAvailable: boolean;
  bassLevel: number;
  screenCount: number;
  soundSystem: string;
  customFeatures: string;
  watcherCount: number;
  fleetPosition: number;
  latestLocation: LocationResponse | null;
  crew: CrewMemberResponse[];
}

export interface FleetOverviewResponse {
  activeVehicles: number;
  pendingVerification: number;
  activeFleet: VehicleSummaryResponse[];
}

export interface VehicleAdminUpdateRequest {
  name: string;
  plateNumber: string;
  routeId: string;
  status: VehicleStatus;
  occupancyStatus: OccupancyStatus;
  listingState: ListingState;
  wifiAvailable: boolean;
  bassLevel: number;
  screenCount: number;
  soundSystem: string;
  customFeatures: string;
  crew: CrewAdminRequest[];
}

export interface CrewAdminRequest {
  userId: string;
  role: 'DRIVER' | 'CONDUCTOR';
}

export type AccountRole = 'ADMIN' | 'CREW' | 'TRAVELLER';
export type UserStatus = 'INVITED' | 'ACTIVE' | 'SUSPENDED';
export interface UserResponse { id: string; name: string; email: string; phoneNumber: string | null; role: AccountRole; status: UserStatus; assignmentId: string | null; vehicleId: string | null; vehicleName: string | null; assignmentRole: 'DRIVER' | 'CONDUCTOR' | null; createdAt: string; }
export interface AuthUserResponse { id: string; displayName: string; email: string; role: 'admin' | 'crew' | 'traveller'; }
export interface CrewContextResponse { user: AuthUserResponse; assignment: { id: string; role: 'DRIVER' | 'CONDUCTOR'; startedAt: string } | null; vehicle: VehicleDetailResponse | null; }
export interface TravellerContextResponse { user: AuthUserResponse; defaultRoute: RouteResponse | null; temporaryRoute: RouteResponse | null; activeRoute: RouteResponse | null; samplingEnabled: boolean; }

export interface PageResponse<T> {
  items: T[];
  totalItems: number;
  totalPages: number;
  page: number;
  size: number;
}

export interface MediaResponse {
  id: string;
  originalName: string;
  contentType: string;
  size: number;
  sortOrder: number;
  approved: boolean;
  url: string;
}
