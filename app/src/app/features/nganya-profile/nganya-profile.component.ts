import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap } from 'rxjs';
import { MediaResponse, RouteResponse, VehicleDetailResponse, VehicleSummaryResponse } from '../../core/api.models';
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
  mapRoutes: RouteResponse[] = [];
  mapVehicles: VehicleSummaryResponse[] = [];
  readonly adminPreview: boolean;
  readonly crewPreview: boolean;
  readonly travellerPreview: boolean;
  readonly backPath: string;

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
          const request = this.adminPreview ? this.api.getAdminVehicle(id) : this.api.getVehicle(id);
          return request.pipe(
            catchError(() => {
              this.error = 'Could not load this nganya from the backend.';
              return of(null);
            })
          );
        })
      )
      .subscribe((vehicle) => {
        this.vehicle = vehicle;
        this.mapRoutes = vehicle ? [vehicle.route] : [];
        this.mapVehicles = vehicle ? [this.toMapVehicle(vehicle)] : [];
        this.loading = false;
        if (vehicle) {
          const imagesRequest = this.adminPreview ? this.api.getAdminVehicleImages(vehicle.id) : this.api.getVehicleImages(vehicle.id);
          imagesRequest.subscribe({ next: (images) => (this.images = images), error: () => (this.images = []) });
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

  rating(vehicle: VehicleDetailResponse): number {
    if (!vehicle.crew.length) {
      return 0;
    }
    const total = vehicle.crew.reduce((sum, crew) => sum + Number(crew.rating), 0);
    return Math.round((total / vehicle.crew.length) * 10) / 10;
  }

  async logout(): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to enter your access details to return.', confirmLabel: 'Sign out' }))) return;
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
