import { CommonModule } from '@angular/common';
import { Component, effect, Input, OnDestroy, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CrewContextResponse, MediaResponse, OccupancyStatus, RouteResponse, VehicleDetailResponse, VehicleSummaryResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { KultureMapComponent } from '../../shared/kulture-map/kulture-map.component';
import { LocationBroadcastService } from '../../core/location-broadcast.service';
import { Capacitor } from '@capacitor/core';
import { NativeLocationBroadcast } from '../../core/native-location-broadcast';
import { environment } from '../../../environments/environment';

@Component({ selector: 'app-crew-dashboard', standalone: true, imports: [CommonModule, FormsModule, RouterLink, KultureMapComponent], templateUrl: './crew-dashboard.component.html', styleUrl: './crew-dashboard.component.css' })
export class CrewDashboardComponent implements OnInit, OnDestroy {
  @Input() initialTab: 'dashboard' | 'my-nganya' | 'nganyas' = 'dashboard';
  context: CrewContextResponse | null = null; images: MediaResponse[] = []; vehicles: VehicleSummaryResponse[] = []; query = '';
  mapRoutes: RouteResponse[] = []; mapVehicles: VehicleSummaryResponse[] = [];
  loading = true; saving = false; error = ''; sharingLocation = false; locationMessage = '';
  private refreshTimerId: number | null = null; private imageVehicleId: string | null = null;
  constructor(private readonly api: KultureApiService, private readonly auth: AuthService, private readonly router: Router, private readonly confirmation: ConfirmationService, private readonly locationBroadcast: LocationBroadcastService) {
    effect(() => {
      if (Capacitor.isNativePlatform()) return;
      this.sharingLocation = this.locationBroadcast.sharing();
      if (!this.sharingLocation) return;
      const queued = this.locationBroadcast.queueSize();
      this.locationMessage = ({ idle: 'Acquiring location...', queued: `Sending location${queued ? ` (${queued} queued)` : ''}...`, broadcasting: 'Location sharing is active.', offline: `Offline; ${queued} location update${queued === 1 ? '' : 's'} queued.`, unauthorized: 'Location sharing authorization was lost.', error: 'Location updates will retry automatically.' } as const)[this.locationBroadcast.state()];
    });
  }
  ngOnInit(): void { this.loadContext(); if (Capacitor.isNativePlatform()) void this.restoreNativeSharingState(); if (this.initialTab === 'my-nganya') this.refreshTimerId = window.setInterval(() => this.loadContext(false), 5000); if (this.initialTab === 'nganyas') this.loadVehicles(); }
  ngOnDestroy(): void { if (this.refreshTimerId !== null) window.clearInterval(this.refreshTimerId); }

  loadContext(showLoading = true): void { if (showLoading) this.loading = true; this.error = ''; this.api.getCrewContext().subscribe({ next: (context) => { const routeChanged = context.vehicle && this.context?.vehicle?.route.id !== context.vehicle.route.id; this.context = context; this.mapVehicles = context.vehicle ? [this.toMapVehicle(context.vehicle)] : []; if (context.vehicle && (routeChanged || !this.mapRoutes.length)) this.mapRoutes = [context.vehicle.route]; if (!context.vehicle) this.mapRoutes = []; if (context.vehicle && !Capacitor.isNativePlatform()) this.locationBroadcast.restore(context.vehicle.id); this.loading = false; if (this.initialTab === 'my-nganya' && context.vehicle && this.imageVehicleId !== context.vehicle.id) { this.imageVehicleId = context.vehicle.id; this.api.getVehicleImages(context.vehicle.id).subscribe({ next: (images) => this.images = images, error: () => this.images = [] }); } }, error: (response) => { this.loading = false; this.error = response.status === 403 ? 'Your crew account is not active.' : 'Could not load your crew account.'; } }); }
  loadVehicles(): void { const request = this.query.trim() ? this.api.searchVehicles(this.query.trim()) : this.api.getVehicles(); request.subscribe({ next: (vehicles) => this.vehicles = vehicles, error: () => this.error = 'Could not load nganyas.' }); }

  async setOnline(online: boolean): Promise<void> { const vehicle = this.context?.vehicle; if (!vehicle || this.saving) return; const confirmed = await this.confirmation.confirm({ title: online ? 'Go online?' : 'Go offline?', message: online ? `${vehicle.name} will become visible for live tracking.` : `${vehicle.name} will stop live tracking.`, confirmLabel: online ? 'Go online' : 'Go offline' }); if (!confirmed) return; this.saving = true; this.error = ''; const request = online ? this.api.goLive(vehicle.id) : this.api.goOffline(vehicle.id); request.subscribe({ next: () => { this.saving = false; if (!online) this.stopLocationSharing(); this.loadContext(); }, error: (response) => { this.saving = false; this.error = response.status === 403 ? 'You are no longer assigned to this nganya.' : 'Could not update the nganya status.'; this.loadContext(); } }); }
  async enterMaintenance(): Promise<void> { const vehicle = this.context?.vehicle; if (!vehicle || this.saving || vehicle.status === 'MAINTENANCE') return; const confirmed = await this.confirmation.confirm({ title: 'Enter maintenance mode?', message: `${vehicle.name} will be marked as under maintenance and removed from live tracking.`, confirmLabel: 'Enter maintenance' }); if (!confirmed) return; this.saving = true; this.error = ''; this.api.updateVehicleStatus(vehicle.id, 'MAINTENANCE', vehicle.occupancyStatus).subscribe({ next: () => { this.saving = false; this.stopLocationSharing(); this.loadContext(); }, error: (response) => { this.saving = false; this.error = response.status === 403 ? 'You are no longer assigned to this nganya.' : 'Could not enter maintenance mode.'; this.loadContext(); } }); }
  setOccupancy(occupancy: OccupancyStatus): void { const vehicle = this.context?.vehicle; if (!vehicle || this.saving || vehicle.occupancyStatus === occupancy) return; this.saving = true; this.api.updateOccupancy(vehicle.id, occupancy).subscribe({ next: () => { this.saving = false; this.loadContext(); }, error: () => { this.saving = false; this.error = 'Could not update occupancy.'; } }); }

  async startLocationSharing(): Promise<void> { const vehicle = this.context?.vehicle; if (!vehicle || this.sharingLocation) return; if (!navigator.geolocation && !Capacitor.isNativePlatform()) { this.locationMessage = 'Location sharing is not supported by this browser.'; return; } const confirmed = await this.confirmation.confirm({ title: 'Share live location?', message: 'The Kulture will send this device location for your assigned nganya until you stop sharing, go offline, or sign out.', confirmLabel: 'Allow location' }); if (!confirmed) return; this.locationMessage = 'Waiting for location permission...'; if (Capacitor.isNativePlatform()) { try { await NativeLocationBroadcast.start({ vehicleId: vehicle.id, vehicleName: vehicle.name, apiBaseUrl: environment.apiBaseUrl }); this.sharingLocation = true; this.locationMessage = 'Background location sharing is active.'; } catch { this.locationMessage = 'Location permission was denied or unavailable.'; } return; } if (!this.locationBroadcast.start(vehicle.id)) this.locationMessage = 'Location sharing is not supported by this browser.'; }
  stopLocationSharing(): void { if (Capacitor.isNativePlatform()) void NativeLocationBroadcast.stop(); else this.locationBroadcast.stop(); if (this.sharingLocation) this.locationMessage = 'Location sharing stopped.'; this.sharingLocation = false; }
  isLocationStale(): boolean { const recordedAt = this.context?.vehicle?.latestLocation?.recordedAt; return !!recordedAt && Date.now() - new Date(recordedAt).getTime() > 5 * 60 * 1000; }
  private toMapVehicle(vehicle: VehicleDetailResponse): VehicleSummaryResponse { return { id: vehicle.id, plateNumber: vehicle.plateNumber, name: vehicle.name, routeNumber: vehicle.route.routeNumber, routeName: vehicle.route.name, destination: vehicle.route.destination, status: vehicle.status, occupancyStatus: vehicle.occupancyStatus, verified: vehicle.verified, listingState: vehicle.listingState, etaMinutes: 0, latestLocation: vehicle.latestLocation }; }
  private async restoreNativeSharingState(): Promise<void> { try { const status = await NativeLocationBroadcast.getStatus(); this.sharingLocation = status.active; if (status.active) this.locationMessage = 'Background location sharing is active.'; } catch { this.sharingLocation = false; } }
  async logout(): Promise<void> { if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'Location sharing will stop and you will need to sign in again.', confirmLabel: 'Sign out' }))) return; this.stopLocationSharing(); this.auth.logout(); this.router.navigateByUrl('/login'); }
}
