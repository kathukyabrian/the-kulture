import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { VehicleDetailResponse } from '../../core/api.models';
import { KultureApiService } from '../../core/kulture-api.service';

@Component({
  selector: 'app-nganya-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './nganya-profile.component.html'
})
export class NganyaProfileComponent implements OnInit {
  vehicle: VehicleDetailResponse | null = null;
  loading = true;
  error = '';

  constructor(
    private readonly route: ActivatedRoute,
    private readonly api: KultureApiService
  ) {}

  ngOnInit(): void {
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
          return this.api.getVehicle(id).pipe(
            catchError(() => {
              this.error = 'Could not load this nganya from the backend.';
              return of(null);
            })
          );
        })
      )
      .subscribe((vehicle) => {
        this.vehicle = vehicle;
        this.loading = false;
      });
  }

  rating(vehicle: VehicleDetailResponse): number {
    if (!vehicle.crew.length) {
      return 0;
    }
    const total = vehicle.crew.reduce((sum, crew) => sum + Number(crew.rating), 0);
    return Math.round((total / vehicle.crew.length) * 10) / 10;
  }
}
