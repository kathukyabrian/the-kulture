import { CommonModule } from '@angular/common';
import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { catchError, of, switchMap, tap, timer } from 'rxjs';
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
  private readonly destroyRef = inject(DestroyRef);
  private imageVehicleId: string | null = null;

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
        if (vehicle && this.imageVehicleId !== vehicle.id) {
          this.imageVehicleId = vehicle.id;
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
