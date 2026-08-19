import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { VehicleDetailResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';

@Component({
  selector: 'app-crew-dashboard',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './crew-dashboard.component.html'
})
export class CrewDashboardComponent implements OnInit {
  vehicle: VehicleDetailResponse | null = null;
  loading = true;
  saving = false;
  error = '';

  constructor(
    private readonly api: KultureApiService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService
  ) {}

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

  async toggleLive(): Promise<void> {
    if (!this.vehicle) {
      return;
    }
    const action = this.vehicle.status === 'ONLINE' ? 'take this nganya offline' : 'make this nganya live';
    if (!(await this.confirmation.confirm({ title: 'Change live status?', message: `This will ${action}.`, confirmLabel: 'Continue' }))) return;
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

  async logout(): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to enter your access details to return.', confirmLabel: 'Sign out' }))) return;
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
