import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap, timer } from 'rxjs';
import { ArrivalEstimateResponse, MediaResponse, RouteResponse, VehicleDetailResponse, VehicleSummaryResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { KultureMapComponent } from '../../shared/kulture-map/kulture-map.component';

@Component({
  selector: 'app-nganya-profile',
  standalone: true,
  imports: [CommonModule, RouterLink, KultureMapComponent],
  templateUrl: './nganya-profile.component.html'
})
export class NganyaProfileComponent implements OnInit {
  vehicle: VehicleDetailResponse | null = null;
  loading = true;
  error = '';
  images: MediaResponse[] = [];
  currentCoverIndex = 0;
  mapRoutes: RouteResponse[] = [];
  mapVehicles: VehicleSummaryResponse[] = [];
  arrivalEstimate: ArrivalEstimateResponse | null = null;
  arrivalState: 'idle' | 'locating' | 'estimating' | 'denied' | 'error' = 'idle';
  pickupPosition: [number, number] | null = null;
  readonly adminPreview: boolean;
  readonly crewPreview: boolean;
  readonly travellerPreview: boolean;
  readonly backPath: string;
  private readonly destroyRef = inject(DestroyRef);
  private imageVehicleId: string | null = null;
  locationWatchId: number | null = null;
  private etaRefreshId: ReturnType<typeof setInterval> | null = null;
  private passengerLocation: GeolocationPosition | null = null;
  private estimateInFlight = false;

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: KultureApiService,
    readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService
  ) {
    this.adminPreview = this.route.snapshot.data['adminPreview'] === true;
    this.crewPreview = this.route.snapshot.data['crewPreview'] === true;
    this.travellerPreview = this.route.snapshot.data['travellerPreview'] === true;
    this.backPath = this.adminPreview ? '/fleet' : this.crewPreview ? '/crew/nganyas' : this.travellerPreview ? '/traveller/nganyas' : '/';
  }

  get passengerEtaEnabled(): boolean { return !this.adminPreview && !this.crewPreview; }

  enableArrivalEstimate(): void {
    if (!this.passengerEtaEnabled || this.locationWatchId !== null) return;
    if (!navigator.geolocation) { this.arrivalState = 'error'; return; }
    this.arrivalState = 'locating';
    this.locationWatchId = navigator.geolocation.watchPosition(position => {
      const moved = !this.passengerLocation || this.distanceBetween(this.passengerLocation, position) >= 75;
      this.passengerLocation = position;
      this.pickupPosition = [position.coords.longitude, position.coords.latitude];
      if (moved || !this.arrivalEstimate) this.refreshArrivalEstimate();
    }, error => {
      this.arrivalState = error.code === error.PERMISSION_DENIED ? 'denied' : 'error';
      this.stopArrivalEstimate();
    }, { enableHighAccuracy: true, maximumAge: 10000, timeout: 15000 });
    this.etaRefreshId = setInterval(() => this.refreshArrivalEstimate(), 20000);
    this.destroyRef.onDestroy(() => this.stopArrivalEstimate());
  }

  refreshArrivalEstimate(): void {
    if (!this.vehicle || !this.passengerLocation || this.estimateInFlight) return;
    this.estimateInFlight = true; this.arrivalState = 'estimating';
    const position = this.passengerLocation;
    this.api.estimateArrival(this.vehicle.id, { latitude: position.coords.latitude, longitude: position.coords.longitude, accuracyMeters: position.coords.accuracy, capturedAtEpochMillis: position.timestamp })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: estimate => { this.arrivalEstimate = estimate; this.arrivalState = 'idle'; this.estimateInFlight = false; if (estimate.passengerPosition) this.pickupPosition = [estimate.passengerPosition.longitude, estimate.passengerPosition.latitude]; },
        error: () => { this.arrivalState = 'error'; this.estimateInFlight = false; }
      });
  }

  arrivalMessage(): string {
    if (this.arrivalState === 'denied') return 'Allow location access to estimate arrival';
    if (this.arrivalState === 'error') return 'Could not calculate an estimate right now';
    if (this.arrivalState === 'locating') return 'Allow location access to estimate arrival';
    if (this.arrivalState === 'estimating' && !this.arrivalEstimate) return 'Estimating arrival…';
    const messages: Record<string, string> = { OFF_ROUTE: 'Move closer to this route to get an estimate', PASSED: 'This nganya has passed your position', NOT_APPROACHING: 'Not currently approaching you', STALE_LOCATION: 'Live location is temporarily unavailable', UNAVAILABLE: 'Arrival estimate unavailable', ROUTE_UNAVAILABLE: 'Could not calculate an estimate right now', PASSENGER_LOCATION_UNRELIABLE: 'Move to an open area for a more accurate location' };
    return this.arrivalEstimate ? messages[this.arrivalEstimate.status] ?? 'Could not calculate an estimate right now' : 'Estimate arrival to me';
  }

  distanceLabel(meters: number | null): string { return meters == null ? '' : meters >= 1000 ? `${(meters / 1000).toFixed(1)} km` : `${meters} m`; }
  freshnessLabel(timestamp: string): string { const seconds = Math.max(0, Math.floor((Date.now() - new Date(timestamp).getTime()) / 1000)); return seconds < 60 ? `${seconds} sec ago` : `${Math.floor(seconds / 60)} min ago`; }
  private stopArrivalEstimate(): void { if (this.locationWatchId !== null) navigator.geolocation.clearWatch(this.locationWatchId); if (this.etaRefreshId !== null) clearInterval(this.etaRefreshId); this.locationWatchId = null; this.etaRefreshId = null; }
  private distanceBetween(a: GeolocationPosition, b: GeolocationPosition): number { const dy=(b.coords.latitude-a.coords.latitude)*111320, dx=(b.coords.longitude-a.coords.longitude)*111320*Math.cos(a.coords.latitude*Math.PI/180); return Math.hypot(dx,dy); }

  ngOnInit(): void {
    timer(12000, 12000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => {
      if (this.images.length > 1) this.currentCoverIndex = (this.currentCoverIndex + 1) % this.images.length;
    });
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          this.loading = true;
          this.error = '';
          const id = params.get('id');
          if (!id) {
            this.error = 'Missing vehicle id.';
            return of(null);
          }
          return timer(0, 5000).pipe(switchMap(() => {
            const request = this.adminPreview ? this.api.getAdminVehicle(id) : this.api.getVehicle(id);
            return request.pipe(
              tap(() => (this.error = '')),
              catchError(() => {
                this.error = 'Could not refresh this nganya from the backend.';
                return of(this.vehicle);
              })
            );
          }));
        }),
        takeUntilDestroyed(this.destroyRef)
      )
      .subscribe((vehicle) => {
        const routeChanged = vehicle && this.vehicle?.route.id !== vehicle.route.id;
        this.vehicle = vehicle;
        if (vehicle && (routeChanged || !this.mapRoutes.length)) this.mapRoutes = [vehicle.route];
        if (!vehicle) this.mapRoutes = [];
        this.mapVehicles = vehicle ? [this.toMapVehicle(vehicle)] : [];
        this.loading = false;
        if (vehicle && this.passengerEtaEnabled && vehicle.status === 'ONLINE' && this.locationWatchId === null && this.arrivalState === 'idle' && !this.arrivalEstimate) {
          this.enableArrivalEstimate();
        }
        if (vehicle && this.imageVehicleId !== vehicle.id) {
          this.imageVehicleId = vehicle.id;
          const imagesRequest = this.adminPreview ? this.api.getAdminVehicleImages(vehicle.id) : this.api.getVehicleImages(vehicle.id);
          imagesRequest.subscribe({ next: (images) => { this.images = images; this.currentCoverIndex = 0; }, error: () => { this.images = []; this.currentCoverIndex = 0; } });
        }
      });
  }

  private toMapVehicle(vehicle: VehicleDetailResponse): VehicleSummaryResponse {
    return {
      id: vehicle.id,
      plateNumber: vehicle.plateNumber,
      name: vehicle.name,
      routeNumber: vehicle.route.routeNumber,
      routeName: vehicle.route.name,
      destination: vehicle.route.destination,
      status: vehicle.status,
      occupancyStatus: vehicle.occupancyStatus,
      verified: vehicle.verified,
      listingState: vehicle.listingState,
      etaMinutes: 0,
      latestLocation: vehicle.latestLocation
    };
  }

  async logout(): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to enter your access details to return.', confirmLabel: 'Sign out' }))) return;
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
