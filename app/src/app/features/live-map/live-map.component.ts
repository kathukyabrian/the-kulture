import { CommonModule } from '@angular/common';
import { Component, HostListener, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, distinctUntilChanged, of, switchMap, tap } from 'rxjs';
import { VehicleSummaryResponse } from '../../core/api.models';
import { KultureApiService } from '../../core/kulture-api.service';

@Component({
  selector: 'app-live-map',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './live-map.component.html'
})
export class LiveMapComponent implements OnInit {
  query = '';
  vehicles: VehicleSummaryResponse[] = [];
  loading = true;
  error = '';
  menuOpen = false;

  private readonly searchTerms = new Subject<string>();

  constructor(private readonly api: KultureApiService) {}

  ngOnInit(): void {
    this.loadVehicles();
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
