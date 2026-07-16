export type VehicleStatus = 'ONLINE' | 'OFFLINE' | 'MAINTENANCE';
export type OccupancyStatus = 'LOW' | 'MEDIUM' | 'FULL';

export interface RouteInfo {
  id: string;
  routeNumber: string;
  name: string;
  origin: string;
  destination: string;
  description: string;
}

export interface VehicleInfo {
  id: string;
  plateNumber: string;
  name: string;
  route: RouteInfo;
  status: VehicleStatus;
  occupancyStatus: OccupancyStatus;
  verified: boolean;
  wifiAvailable: boolean;
  bassLevel: number;
  screenCount: number;
  watcherCount: number;
  earningsToday: number;
  fleetPosition: number;
  etaMinutes: number;
  latitude: number;
  longitude: number;
  crew: Array<{ name: string; role: 'DRIVER' | 'CONDUCTOR'; rating: number }>;
}

export const routes: RouteInfo[] = [
  {
    id: '11111111-1111-1111-1111-111111111111',
    routeNumber: '111',
    name: 'Ngong Road Express',
    origin: 'Railways',
    destination: 'Ngong',
    description: 'High-energy run from Nairobi CBD to Ngong Road.'
  },
  {
    id: '22222222-2222-2222-2222-222222222222',
    routeNumber: '46',
    name: 'Kawangware Circuit',
    origin: 'Kencom',
    destination: 'Kawangware',
    description: 'CBD to Kawangware via Valley Arcade.'
  },
  {
    id: '33333333-3333-3333-3333-333333333333',
    routeNumber: '125',
    name: 'Rongai Night Run',
    origin: 'Railways',
    destination: 'Rongai',
    description: 'CBD to Rongai with late-night commuter demand.'
  }
];

export const vehicles: VehicleInfo[] = [
  {
    id: 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa',
    plateNumber: 'KDJ 842X',
    name: 'The Matrix',
    route: routes[0],
    status: 'ONLINE',
    occupancyStatus: 'MEDIUM',
    verified: true,
    wifiAvailable: true,
    bassLevel: 92,
    screenCount: 8,
    watcherCount: 128,
    earningsToday: 8200,
    fleetPosition: 4,
    etaMinutes: 7,
    latitude: -1.2921,
    longitude: 36.8219,
    crew: [
      { name: 'Kim', role: 'DRIVER', rating: 4.8 },
      { name: 'Jojo', role: 'CONDUCTOR', rating: 4.7 }
    ]
  },
  {
    id: 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb',
    plateNumber: 'KCT 111A',
    name: 'Cyberpunk',
    route: routes[1],
    status: 'ONLINE',
    occupancyStatus: 'LOW',
    verified: false,
    wifiAvailable: true,
    bassLevel: 88,
    screenCount: 6,
    watcherCount: 94,
    earningsToday: 7600,
    fleetPosition: 7,
    etaMinutes: 10,
    latitude: -1.2833,
    longitude: 36.8167,
    crew: [
      { name: 'Mwas', role: 'DRIVER', rating: 4.6 },
      { name: 'Oti', role: 'CONDUCTOR', rating: 4.5 }
    ]
  },
  {
    id: 'cccccccc-cccc-cccc-cccc-cccccccccccc',
    plateNumber: 'KBB 999Z',
    name: 'Soul Train',
    route: routes[2],
    status: 'MAINTENANCE',
    occupancyStatus: 'FULL',
    verified: false,
    wifiAvailable: false,
    bassLevel: 95,
    screenCount: 10,
    watcherCount: 42,
    earningsToday: 5100,
    fleetPosition: 12,
    etaMinutes: 45,
    latitude: -1.302,
    longitude: 36.77,
    crew: [
      { name: 'Amani', role: 'DRIVER', rating: 4.4 },
      { name: 'Biko', role: 'CONDUCTOR', rating: 4.3 }
    ]
  }
];

export const alerts = [
  {
    id: 'ffffffff-ffff-ffff-ffff-ffffffffffff',
    vehicleName: 'Soul Train',
    plateNumber: 'KBB 999Z',
    type: 'Verification',
    message: 'New gallery and plate details need admin review.',
    severity: 'WARNING'
  }
];
