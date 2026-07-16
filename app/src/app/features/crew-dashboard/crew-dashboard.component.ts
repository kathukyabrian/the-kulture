import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { VehicleDetailResponse } from '../../core/api.models';
import { KultureApiService } from '../../core/kulture-api.service';

@Component({
  selector: 'app-crew-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './crew-dashboard.component.html'
})
export class CrewDashboardComponent implements OnInit {
  vehicle: VehicleDetailResponse | null = null;
  loading = true;
  saving = false;
  error = '';

  constructor(private readonly api: KultureApiService) {}

  ngOnInit(): void {
    this.api
      .getVehicles()
      .pipe(
        switchMap((vehicles) => {
          const firstVehicle = vehicles[0];
          if (!firstVehicle) {
            this.error = 'No vehicles are available from the backend.';
            return of(null);
          }
          return this.api.getVehicle(firstVehicle.id);
        }),
        catchError(() => {
          this.error = 'Could not reach the backend API.';
          return of(null);
        })
      )
      .subscribe((vehicle) => {
        this.vehicle = vehicle;
        this.loading = false;
      });
  }

  toggleLive(): void {
    if (!this.vehicle) {
      return;
    }
    this.saving = true;
    const request = this.vehicle.status === 'ONLINE' ? this.api.goOffline(this.vehicle.id) : this.api.goLive(this.vehicle.id);
    request
      .pipe(
        catchError(() => {
          this.error = 'Could not update live status.';
          return of(this.vehicle);
        })
      )
      .subscribe((vehicle) => {
        this.vehicle = vehicle;
        this.saving = false;
      });
  }
}
