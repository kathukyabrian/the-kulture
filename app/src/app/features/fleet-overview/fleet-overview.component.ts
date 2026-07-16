import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { FleetOverviewResponse, VehicleSummaryResponse } from '../../core/api.models';
import { KultureApiService } from '../../core/kulture-api.service';

@Component({
  selector: 'app-fleet-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fleet-overview.component.html'
})
export class FleetOverviewComponent implements OnInit {
  overview: FleetOverviewResponse | null = null;
  pendingVehicles: VehicleSummaryResponse[] = [];
  loading = true;
  error = '';

  constructor(private readonly api: KultureApiService) {}

  ngOnInit(): void {
    this.loadDashboard();
  }

  verify(vehicleId: string): void {
    this.api
      .verifyVehicle(vehicleId)
      .pipe(
        catchError(() => {
          this.error = 'Could not verify this vehicle.';
          return of(null);
        })
      )
      .subscribe(() => this.loadDashboard());
  }

  private loadDashboard(): void {
    this.loading = true;
    forkJoin({
      overview: this.api.getFleetOverview(),
      pending: this.api.getPendingVerification()
    })
      .pipe(
        catchError(() => {
          this.error = 'Could not reach the backend API.';
          return of({ overview: null, pending: [] as VehicleSummaryResponse[] });
        })
      )
      .subscribe(({ overview, pending }) => {
        this.overview = overview;
        this.pendingVehicles = pending;
        this.loading = false;
      });
  }
}
