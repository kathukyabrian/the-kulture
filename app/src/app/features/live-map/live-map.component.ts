import { CommonModule } from '@angular/common';
import { Component, DestroyRef, HostListener, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap, timer } from 'rxjs';
import { RouteResponse, VehicleSummaryResponse } from '../../core/api.models';
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

  private readonly searchTerms = new Subject<string>();
  private readonly destroyRef = inject(DestroyRef);

  constructor(
    private readonly api: KultureApiService,
    readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService,
    private readonly locationEvents: VehicleLocationEventsService
  ) {}

  ngOnInit(): void {
    timer(0, 30000).pipe(takeUntilDestroyed(this.destroyRef)).subscribe(() => this.loadVehicles(false));
    this.locationEvents.stream().pipe(takeUntilDestroyed(this.destroyRef)).subscribe(event => {
      this.vehicles = this.vehicles.map(vehicle => vehicle.id === event.vehicleId ? { ...vehicle, latestLocation: { latitude: event.latitude, longitude: event.longitude, speedKph: event.speedKph, recordedAt: event.recordedAt } } : vehicle);
    });
    this.api.getRoutes().subscribe({ next: routes => this.routes = routes });
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
        this.vehicles = vehicles;
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
    return vehicle.status === 'ONLINE' ? `${vehicle.etaMinutes} min` : 'Offline';
  }

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
        this.vehicles = vehicles;
        this.loading = false;
      });
  }
}
