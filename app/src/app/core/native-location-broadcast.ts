import { registerPlugin } from '@capacitor/core';

interface NativeLocationBroadcastPlugin {
  start(options: { vehicleId: string; vehicleName: string; apiBaseUrl: string }): Promise<{ active: boolean }>;
  stop(): Promise<{ active: boolean }>;
  getStatus(): Promise<{ active: boolean; vehicleId?: string }>;
}

export const NativeLocationBroadcast = registerPlugin<NativeLocationBroadcastPlugin>('LocationBroadcast');
