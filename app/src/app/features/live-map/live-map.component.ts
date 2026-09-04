import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap, timer } from 'rxjs';
import { ArrivalEstimateResponse, RouteResponse, VehicleSummaryResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { KultureMapComponent } from '../../shared/kulture-map/kulture-map.component';
import { VehicleLocationEventsService } from '../../core/vehicle-location-events.service';

@Component({
  selector: 'app-live-map',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, KultureMapComponent],
  templateUrl: './live-map.component.html'
})
export class LiveMapComponent implements OnInit {
  query = '';
  vehicles: VehicleSummaryResponse[] = [];
  routes: RouteResponse[] = [];
  loading = true;
  error = '';
  menuOpen = false;
  readonly profileImageUrls: Record<string, string> = {};
  arrivalEstimates: Record<string, ArrivalEstimateResponse> = {};
  pickupPosition: [number, number] | null = null;
  locationMessage = 'Allow location access for real nearby arrival times';
  showingNearestRouteFallback = false;

  private readonly searchTerms = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);
  private readonly profileImagesRequested = new Set<string>();
  private passengerLocation: GeolocationPosition | null = null;
  private locationWatchId: number | null = null;
  private estimatesInFlight = false;
  private allVehicles: VehicleSummaryResponse[] = [];

  constructor(
    private readonly api: KultureApiService,
    readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService,
    private readonly locationEvents: VehicleLocationEventsService
  ) {}

  ngOnInit(): void {
    this.startPassengerLocation();
    timer(0, 30000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadVehicles(false));
    this.locationEvents.stream().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.allVehicles = this.allVehicles.map(vehicle => vehicle.id === event.vehicleId ? { ...vehicle, latestLocation: { latitude: event.latitude, longitude: event.longitude, speedKph: event.speedKph, headingDegrees: event.headingDegrees, recordedAt: event.recordedAt } } : vehicle);
      this.vehicles = this.vehicles.map(vehicle => vehicle.id === event.vehicleId ? { ...vehicle, latestLocation: { latitude: event.latitude, longitude: event.longitude, speedKph: event.speedKph, headingDegrees: event.headingDegrees, recordedAt: event.recordedAt } } : vehicle);
    });
    this.api.getRoutes().subscribe({ next: routes => { this.routes = routes; this.applyNearbyResults(); } });
    this.searchTerms
      .pipe(
        debounceTime(250),
        distinctUntilChanged(),
        tap(() => {
          this.loading = true;
          this.error = '';
        }),
        switchMap((query) => {
          const trimmed = query.trim();
          return (trimmed ? this.api.searchVehicles(trimmed) : this.api.getVehicles()).pipe(
            tap(() => (this.error = '')),
            catchError(() => {
              this.error = 'Could not reach the backend API.';
              return of([]);
            })
          );
        })
      )
      .subscribe((vehicles) => {
        this.allVehicles = vehicles;
        this.vehicles = [...vehicles];
        this.refreshNearbyEstimates();
        this.loadProfileImages(vehicles);
        this.loading = false;
      });
  }

  onSearch(query: string): void {
    this.searchTerms.next(query);
  }

  toggleMenu(): void {
    this.menuOpen = !this.menuOpen;
  }

  closeMenu(): void {
    this.menuOpen = false;
  }

  async logout(): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to enter your access details to return.', confirmLabel: 'Sign out' }))) return;
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  @HostListener('document:keydown.escape')
  closeMenuOnEscape(): void {
    this.closeMenu();
  }

  statusLabel(vehicle: VehicleSummaryResponse): string {
    if (vehicle.status === 'MAINTENANCE') {
      return 'Pit stop';
    }
    if (vehicle.status === 'ONLINE' && vehicle.latestLocation && Date.now() - new Date(vehicle.latestLocation.recordedAt).getTime() > 60000) {
      return 'Stale';
    }
    if (this.showingNearestRouteFallback) return 'Near route';
    const estimate = this.arrivalEstimates[vehicle.id];
    if (estimate?.status === 'AVAILABLE') return `${estimate.minimumMinutes}–${estimate.maximumMinutes} min`;
    if (estimate?.status === 'PASSED') return 'Passed';
    if (estimate?.status === 'NOT_APPROACHING') return 'Not approaching';
    if (estimate?.status === 'OFF_ROUTE') return 'Not near route';
    return vehicle.status === 'ONLINE' ? 'Live' : 'Offline';
  }

  get listHeading(): string { return this.showingNearestRouteFallback ? 'Vehicles on nearest route' : 'Nearby Nganyas'; }

  private loadVehicles(showLoading = true): void {
    if (showLoading) this.loading = true;
    const trimmed = this.query.trim();
    (trimmed ? this.api.searchVehicles(trimmed) : this.api.getVehicles())
      .pipe(
        tap(() => (this.error = '')),
        catchError(() => {
          this.error = 'Could not reach the backend API.';
          return of([]);
        })
      )
      .subscribe((vehicles) => {
        this.allVehicles = vehicles;
        this.vehicles = [...vehicles];
        this.refreshNearbyEstimates();
        this.loadProfileImages(vehicles);
        this.loading = false;
      });
  }

  private startPassengerLocation(): void {
    if (!navigator.geolocation) { this.locationMessage = 'Location is unavailable on this device'; return; }
    this.locationWatchId = navigator.geolocation.watchPosition(position => {
      const moved = !this.passengerLocation || this.distanceBetween(this.passengerLocation, position) >= 100;
      this.passengerLocation = position;
      this.pickupPosition = [position.coords.longitude, position.coords.latitude];
      this.locationMessage = '';
      if (moved || !Object.keys(this.arrivalEstimates).length) this.refreshNearbyEstimates();
    }, error => {
      this.locationMessage = error.code === error.PERMISSION_DENIED ? 'Allow location access for real nearby arrival times' : 'Could not determine your location';
    }, { enableHighAccuracy: true, maximumAge: 10000, timeout: 15000 });
    this.destroyRef.onDestroy(() => { if (this.locationWatchId !== null) navigator.geolocation.clearWatch(this.locationWatchId); });
  }

  private refreshNearbyEstimates(): void {
    if (!this.passengerLocation || this.estimatesInFlight || !this.vehicles.length) return;
    const position = this.passengerLocation; this.estimatesInFlight = true;
    this.api.estimateNearbyArrivals({ latitude: position.coords.latitude, longitude: position.coords.longitude, accuracyMeters: position.coords.accuracy, capturedAtEpochMillis: position.timestamp })
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: estimates => {
          this.arrivalEstimates = Object.fromEntries(estimates.map(estimate => [estimate.vehicleId, estimate]));
          this.applyNearbyResults();
          this.estimatesInFlight = false;
        },
        error: () => { this.locationMessage = 'Arrival times are temporarily unavailable'; this.estimatesInFlight = false; }
      });
  }

  private applyNearbyResults(): void {
    if (!this.allVehicles.length) return;
    const order = new Map(Object.values(this.arrivalEstimates).map((estimate, index) => [estimate.vehicleId, index]));
    const matching = this.allVehicles.filter(vehicle => !this.query.trim() || `${vehicle.name} ${vehicle.routeName} ${vehicle.destination} ${vehicle.plateNumber}`.toLowerCase().includes(this.query.trim().toLowerCase()));
    if (this.auth.signedIn() || this.query.trim() || !this.passengerLocation) {
      this.showingNearestRouteFallback = false;
      this.vehicles = [...matching].sort((a, b) => (order.get(a.id) ?? Number.MAX_SAFE_INTEGER) - (order.get(b.id) ?? Number.MAX_SAFE_INTEGER));
      return;
    }
    const approaching = matching.filter(vehicle => this.arrivalEstimates[vehicle.id]?.status === 'AVAILABLE');
    if (approaching.length) {
      this.showingNearestRouteFallback = false;
      this.vehicles = approaching.sort((a, b) => (order.get(a.id) ?? Number.MAX_SAFE_INTEGER) - (order.get(b.id) ?? Number.MAX_SAFE_INTEGER));
      return;
    }
    const nearestRoute = this.routes
      .filter(route => route.geometry?.coordinates.length && matching.some(vehicle => vehicle.routeNumber === route.routeNumber))
      .sort((a, b) => this.distanceToRoute(a) - this.distanceToRoute(b))[0];
    if (!nearestRoute) { this.showingNearestRouteFallback = false; this.vehicles = matching; return; }
    const routeVehicles = matching.filter(vehicle => vehicle.routeNumber === nearestRoute.routeNumber);
    const fresh = routeVehicles.filter(vehicle => vehicle.latestLocation && Date.now() - new Date(vehicle.latestLocation.recordedAt).getTime() <= 60000);
    this.showingNearestRouteFallback = true;
    this.vehicles = (fresh.length ? fresh : routeVehicles).slice(0, 5);
  }

  private distanceToRoute(route: RouteResponse): number {
    if (!this.passengerLocation || !route.geometry) return Number.MAX_SAFE_INTEGER;
    const latitude = this.passengerLocation.coords.latitude, longitude = this.passengerLocation.coords.longitude;
    return Math.min(...route.geometry.coordinates.map(([lng, lat]) => {
      const dy = (lat - latitude) * 111320;
      const dx = (lng - longitude) * 111320 * Math.cos(latitude * Math.PI / 180);
      return Math.hypot(dx, dy);
    }));
  }

  private distanceBetween(a: GeolocationPosition, b: GeolocationPosition): number { const dy=(b.coords.latitude-a.coords.latitude)*111320, dx=(b.coords.longitude-a.coords.longitude)*111320*Math.cos(a.coords.latitude*Math.PI/180); return Math.hypot(dx,dy); }

  private loadProfileImages(vehicles: VehicleSummaryResponse[]): void {
    for (const vehicle of vehicles) {
      if (this.profileImagesRequested.has(vehicle.id)) continue;
      this.profileImagesRequested.add(vehicle.id);
      this.api.getVehicleImages(vehicle.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: images => {
            if (!images.length) return;
            const randomImage = images[Math.floor(Math.random() * images.length)];
            this.preloadProfileImage(vehicle.id, randomImage.url);
          },
          error: () => this.profileImagesRequested.delete(vehicle.id)
        });
    }
  }

  private preloadProfileImage(vehicleId: string, url: string): void {
    const image = new Image();
    image.onload = () => this.profileImageUrls[vehicleId] = url;
    image.src = url;
  }
}
