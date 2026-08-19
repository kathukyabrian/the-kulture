import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { RouteResponse, VehicleSummaryResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { KultureMapComponent } from '../../shared/kulture-map/kulture-map.component';

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

  constructor(
    private readonly api: KultureApiService,
    readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.loadVehicles();
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
    return vehicle.status === 'ONLINE' ? `${vehicle.etaMinutes} min` : 'Offline';
  }

  private loadVehicles(): void {
    this.api
      .getVehicles()
      .pipe(
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
