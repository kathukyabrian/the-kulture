import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { catchError, forkJoin, of } from 'rxjs';
import { FleetOverviewResponse, RouteAdminRequest, RouteResponse, VehicleAdminUpdateRequest, VehicleDetailResponse, MediaResponse, VehicleSummaryResponse, UserResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { KultureApiService } from '../../core/kulture-api.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { AdminDashboardPageComponent } from './pages/admin-dashboard-page.component';
import { AdminNganyasPageComponent } from './pages/admin-nganyas-page.component';
import { AdminRoutesPageComponent } from './pages/admin-routes-page.component';
import { AdminUsersPageComponent } from './pages/admin-users-page.component';
import { KultureMapComponent } from '../../shared/kulture-map/kulture-map.component';

@Component({
  selector: 'app-fleet-overview',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink, AdminDashboardPageComponent, AdminNganyasPageComponent, AdminRoutesPageComponent, AdminUsersPageComponent, KultureMapComponent],
  templateUrl: './fleet-overview.component.html',
  styleUrl: './fleet-overview.component.scss'
})
export class FleetOverviewComponent implements OnInit {
  @Input() initialTab: 'dashboard' | 'nganyas' | 'routes' | 'users' = 'dashboard';
  adminTab: 'dashboard' | 'nganyas' | 'routes' | 'users' = 'dashboard';
  overview: FleetOverviewResponse | null = null;
  pendingVehicles: VehicleSummaryResponse[] = [];
  vehicles: VehicleSummaryResponse[] = [];
  routes: RouteResponse[] = [];
  managedRoutes: RouteResponse[] = [];
  routeForm: RouteAdminRequest | null = null;
  editingRouteId: string | null = null;
  routeSearchQuery = '';
  routePage = 1;
  routeTotal = 0;
  routePageCount = 1;
  readonly routesPerPage = 9;
  savingRoute = false;
  routingRoute = false;
  routeCalculationMessage = '';
  private routeCalculationSequence = 0;
  editing: VehicleDetailResponse | null = null;
  vehicleImages: MediaResponse[] = [];
  uploadingImage = false;
  editForm: VehicleAdminUpdateRequest | null = null;
  routePickerQuery = '';
  routePickerOpen = false;
  crewPhone = '';
  crewInvite = { name: '', email: '', phoneNumber: '' };
  crewRole: 'DRIVER' | 'CONDUCTOR' = 'DRIVER';
  foundCrewUser: UserResponse | null = null;
  showCrewInvite = false;
  pendingCrew: { user: UserResponse; role: 'DRIVER' | 'CONDUCTOR' }[] = [];
  searchQuery = '';
  vehiclePage = 1;
  vehicleTotal = 0;
  vehiclePageCount = 1;
  readonly vehiclesPerPage = 9;
  saving = false;
  loading = true;
  error = '';

  constructor(
    private readonly api: KultureApiService,
    private readonly auth: AuthService,
    private readonly router: Router,
    private readonly confirmation: ConfirmationService
  ) {}

  ngOnInit(): void {
    this.adminTab = this.initialTab;
    this.loadDashboard();
  }

  async verify(vehicleId: string): Promise<void> {
    const vehicle = this.pendingVehicles.find((candidate) => candidate.id === vehicleId);
    if (!(await this.confirmation.confirm({ title: 'Verify nganya?', message: `${vehicle?.name ?? 'This nganya'} will become approved and visible as verified.`, confirmLabel: 'Verify' }))) return;
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

  searchVehicles(): void {
    this.vehiclePage = 1;
    this.loadVehiclePage();
  }

  changeVehiclePage(page: number): void {
    this.vehiclePage = Math.min(Math.max(page, 1), this.vehiclePageCount);
    this.loadVehiclePage();
  }

  searchRoutes(): void { this.routePage = 1; this.loadRoutePage(); }
  changeRoutePage(page: number): void { this.routePage = Math.min(Math.max(page, 1), this.routePageCount); this.loadRoutePage(); }
  createRoute(): void { this.editingRouteId = null; this.routeCalculationMessage = ''; this.routeForm = { routeNumber: '', name: '', origin: '', destination: '', description: '', active: true, geometry: null, waypoints: null }; }
  editRoute(route: RouteResponse): void { this.editingRouteId = route.id; this.routeCalculationMessage = ''; const coordinates = route.geometry?.coordinates ?? []; const legacyWaypoints = coordinates.length > 1 ? this.sampleWaypoints(coordinates) : null; this.routeForm = { routeNumber: route.routeNumber, name: route.name, origin: route.origin, destination: route.destination, description: route.description, active: route.active, geometry: route.geometry, waypoints: route.waypoints ?? legacyWaypoints }; }
  cancelRouteEdit(): void { this.routeCalculationSequence++; this.routingRoute = false; this.editingRouteId = null; this.routeForm = null; }

  routeWaypointsChanged(waypoints: [number, number][]): void {
    if (!this.routeForm) return;
    this.routeForm.waypoints = waypoints.length ? waypoints : null;
    this.routeForm.geometry = waypoints.length > 1 ? { type: 'LineString', coordinates: waypoints } : null;
    this.routeCalculationMessage = '';
    if (waypoints.length < 2) { this.routingRoute = false; return; }
    this.calculateRoadRoute(waypoints);
  }

  followRouteRoads(): void {
    if (!this.routeForm || this.routingRoute) return;
    const source = this.routeForm.waypoints?.length ? this.routeForm.waypoints : this.routeForm.geometry?.coordinates ?? [];
    const waypoints = this.sampleWaypoints(source);
    if (waypoints.length < 2) { this.routeCalculationMessage = 'Choose at least two points first.'; return; }
    this.routeForm.waypoints = waypoints;
    this.calculateRoadRoute(waypoints);
  }

  private calculateRoadRoute(waypoints: [number, number][]): void {
    const sequence = ++this.routeCalculationSequence;
    this.routingRoute = true;
    this.api.calculateRoute(waypoints).subscribe({
      next: result => {
        if (sequence !== this.routeCalculationSequence || !this.routeForm) return;
        this.routeForm.geometry = result.geometry;
        this.routingRoute = false;
        this.routeCalculationMessage = `${(result.distanceMeters / 1000).toFixed(1)} km road route`;
      },
      error: () => {
        if (sequence !== this.routeCalculationSequence) return;
        this.routingRoute = false;
        this.routeCalculationMessage = 'Could not follow roads. Check the points or routing configuration.';
      }
    });
  }

  private sampleWaypoints(coordinates: [number, number][]): [number, number][] {
    if (coordinates.length <= 25) return coordinates.map(point => [...point] as [number, number]);
    return Array.from({ length: 25 }, (_, index) => coordinates[Math.round(index * (coordinates.length - 1) / 24)]).map(point => [...point] as [number, number]);
  }

  async saveRoute(): Promise<void> {
    if (!this.routeForm || this.routingRoute) return;
    const creating = !this.editingRouteId;
    if (!(await this.confirmation.confirm({ title: creating ? 'Create route?' : 'Save route?', message: `${this.routeForm.routeNumber} · ${this.routeForm.name} will be ${creating ? 'created' : 'updated'}.`, confirmLabel: creating ? 'Create' : 'Save' }))) return;
    this.savingRoute = true;
    const request = this.editingRouteId ? this.api.updateRoute(this.editingRouteId, this.routeForm) : this.api.createRoute(this.routeForm);
    request.subscribe({ next: () => { this.savingRoute = false; this.cancelRouteEdit(); this.loadDashboard(); }, error: () => { this.savingRoute = false; this.error = 'Could not save this route.'; } });
  }

  editVehicle(vehicleId: string): void {
    this.error = '';
    this.pendingCrew = [];
    this.resetCrewPicker();
    this.api.getAdminVehicle(vehicleId).subscribe({
      next: (vehicle) => {
        this.editing = vehicle;
        this.editForm = {
          name: vehicle.name,
          plateNumber: vehicle.plateNumber,
          routeId: vehicle.route.id,
          status: vehicle.status,
          occupancyStatus: vehicle.occupancyStatus,
          listingState: vehicle.listingState,
          wifiAvailable: vehicle.wifiAvailable,
          bassLevel: vehicle.bassLevel,
          screenCount: vehicle.screenCount,
          soundSystem: vehicle.soundSystem,
          customFeatures: vehicle.customFeatures,
          crew: []
        };
        this.routePickerQuery = this.routeLabel(vehicle.route);
        this.routePickerOpen = false;
        this.loadVehicleImages(vehicle.id);
      },
      error: () => (this.error = 'Could not load this nganya.')
    });
  }

  createVehicle(): void {
    const firstRoute = this.routes[0];
    if (!firstRoute) {
      this.error = 'Add an active route before creating a nganya.';
      return;
    }
    this.editing = null;
    this.vehicleImages = [];
    this.pendingCrew = [];
    this.resetCrewPicker();
    this.editForm = {
      name: '',
      plateNumber: '',
      routeId: '',
      status: 'OFFLINE',
      occupancyStatus: 'LOW',
      listingState: 'ACTIVE',
      wifiAvailable: false,
      bassLevel: 50,
      screenCount: 0,
      soundSystem: '',
      customFeatures: '',
      crew: []
    };
    this.routePickerQuery = '';
    this.routePickerOpen = false;
  }

  get filteredVehicleRoutes(): RouteResponse[] {
    const query = this.routePickerQuery.trim().toLowerCase();
    if (!query) return this.routes;
    return this.routes.filter((route) =>
      [route.routeNumber, route.name, route.origin, route.destination].some((value) => value.toLowerCase().includes(query))
    );
  }

  searchVehicleRoutes(query: string): void {
    this.routePickerQuery = query;
    this.routePickerOpen = true;
    if (this.editForm) this.editForm.routeId = '';
  }

  selectVehicleRoute(route: RouteResponse): void {
    if (!this.editForm) return;
    this.editForm.routeId = route.id;
    this.routePickerQuery = this.routeLabel(route);
    this.routePickerOpen = false;
  }

  closeRoutePicker(): void {
    this.routePickerOpen = false;
  }

  findCrewUser(): void {
    if (!this.crewPhone.trim()) return;
    this.error = '';
    this.api.findUserByPhone(this.crewPhone).subscribe({
      next: (user) => { this.foundCrewUser = user; this.showCrewInvite = false; },
      error: (response) => { this.foundCrewUser = null; this.showCrewInvite = response.status === 404; this.crewInvite = { name: '', email: '', phoneNumber: this.crewPhone }; if (response.status !== 404) this.error = 'Use a valid Kenyan mobile number.'; }
    });
  }

  inviteAndAssignCrew(): void {
    this.api.inviteCrew(this.crewInvite).subscribe({ next: (user) => { this.foundCrewUser = user; this.showCrewInvite = false; this.assignCrewUser(); }, error: () => this.error = 'Could not create this crew account.' });
  }

  async assignCrewUser(): Promise<void> {
    const user = this.foundCrewUser; if (!user) return;
    let confirmMove = false;
    if (user.vehicleId && user.vehicleId !== this.editing?.id) confirmMove = await this.confirmation.confirm({ title: 'Move crew member?', message: `${user.name} is assigned to ${user.vehicleName}. Move them to this nganya?`, confirmLabel: 'Move' });
    if (user.vehicleId && user.vehicleId !== this.editing?.id && !confirmMove) return;
    if (!this.editing) { this.pendingCrew = [...this.pendingCrew.filter((item) => item.user.id !== user.id), { user, role: this.crewRole }]; this.resetCrewPicker(); return; }
    this.api.assignCrew(this.editing.id, { userId: user.id, role: this.crewRole, confirmMove }).subscribe({ next: () => { this.resetCrewPicker(); this.editVehicle(this.editing!.id); }, error: () => this.error = 'Could not assign this crew member.' });
  }

  async removeCrewMember(assignmentId: string, displayName: string): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Remove crew member?', message: `${displayName} will become unassigned but their account will remain active.`, confirmLabel: 'Remove' }))) return;
    this.api.endCrewAssignment(assignmentId).subscribe({ next: () => this.editVehicle(this.editing!.id), error: () => this.error = 'Could not remove this crew member.' });
  }

  removePendingCrew(userId: string): void { this.pendingCrew = this.pendingCrew.filter((item) => item.user.id !== userId); }
  private resetCrewPicker(): void { this.crewPhone = ''; this.foundCrewUser = null; this.showCrewInvite = false; this.crewInvite = { name: '', email: '', phoneNumber: '' }; }

  cancelEdit(): void {
    this.editing = null;
    this.editForm = null;
    this.routePickerQuery = '';
    this.routePickerOpen = false;
    this.pendingCrew = [];
    this.resetCrewPicker();
    this.vehicleImages = [];
  }

  uploadImage(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!this.editing || !file) return;
    this.uploadingImage = true;
    this.api.uploadVehicleImage(this.editing.id, file).subscribe({ next: () => { this.uploadingImage = false; input.value = ''; this.loadVehicleImages(this.editing!.id); }, error: () => { this.uploadingImage = false; this.error = 'Could not upload image. Use JPG, PNG or WebP up to 8 MB.'; } });
  }

  async deleteImage(image: MediaResponse): Promise<void> {
    if (!this.editing || !(await this.confirmation.confirm({ title: 'Delete image?', message: `${image.originalName} will be permanently removed from the public gallery.`, confirmLabel: 'Delete' }))) return;
    this.api.deleteVehicleImage(this.editing.id, image.id).subscribe({ next: () => this.loadVehicleImages(this.editing!.id), error: () => (this.error = 'Could not delete image.') });
  }

  async saveVehicle(): Promise<void> {
    if (!this.editForm) return;
    if (!this.editForm.routeId) {
      this.error = 'Select a route for this nganya.';
      this.routePickerOpen = true;
      return;
    }
    const isCreating = !this.editing;
    const confirmed = await this.confirmation.confirm({
      title: isCreating ? 'Create nganya?' : 'Save changes?',
      message: isCreating ? `${this.editForm.name || 'This nganya'} will be added for admin verification.` : `Your changes to ${this.editing?.name} will be saved.`,
      confirmLabel: isCreating ? 'Create' : 'Save'
    });
    if (!confirmed) return;
    this.saving = true;
    this.error = '';
    const request = this.editing
      ? this.api.updateVehicle(this.editing.id, this.editForm)
      : this.api.createVehicle(this.editForm);
    request.subscribe({
      next: (vehicle) => {
        this.saving = false;
        if (isCreating) {
          if (this.pendingCrew.length) {
            forkJoin(this.pendingCrew.map((item) => this.api.assignCrew(vehicle.id, { userId: item.user.id, role: item.role, confirmMove: true }))).subscribe({ next: () => window.location.reload(), error: () => { this.error = 'Nganya created, but one or more crew assignments failed.'; } });
          } else window.location.reload();
          return;
        }
        this.cancelEdit();
        this.loadDashboard();
      },
      error: () => {
        this.saving = false;
        this.error = 'Could not save this nganya.';
      }
    });
  }

  async logout(): Promise<void> {
    if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to enter your access details to return.', confirmLabel: 'Sign out' }))) return;
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }

  private loadDashboard(): void {
    this.loading = true;
    this.error = '';

    if (this.adminTab === 'dashboard') {
      forkJoin({ overview: this.api.getFleetOverview(), pending: this.api.getPendingVerification() }).subscribe({
        next: ({ overview, pending }) => { this.overview = overview; this.pendingVehicles = pending; this.loading = false; },
        error: () => { this.error = 'Could not load dashboard data.'; this.loading = false; }
      });
      return;
    }

    if (this.adminTab === 'nganyas') {
      forkJoin({ routes: this.api.getRoutes(), vehiclesPage: this.api.getAdminVehicles(this.searchQuery, this.vehiclePage - 1, this.vehiclesPerPage) }).subscribe({
        next: ({ routes, vehiclesPage }) => {
          this.routes = routes;
          this.applyVehiclePage(vehiclesPage);
          this.loading = false;
        },
        error: () => { this.error = 'Could not load nganyas.'; this.loading = false; }
      });
      return;
    }

    if (this.adminTab === 'routes') {
      this.loadRoutePage();
      return;
    }
    this.loading = false;
  }

  private loadVehiclePage(): void {
    this.error = '';
    this.api.getAdminVehicles(this.searchQuery, this.vehiclePage - 1, this.vehiclesPerPage).subscribe({
      next: (result) => {
        this.applyVehiclePage(result);
        this.loading = false;
      },
      error: () => { this.error = 'Could not load nganyas.'; this.loading = false; }
    });
  }

  private loadRoutePage(): void {
    this.error = '';
    this.api.getAdminRoutes(this.routeSearchQuery, this.routePage - 1, this.routesPerPage).subscribe({
      next: (result) => { this.managedRoutes = result.items; this.routeTotal = result.totalItems; this.routePageCount = Math.max(1, result.totalPages); this.routePage = result.page + 1; this.loading = false; },
      error: () => { this.error = 'Could not load routes.'; this.loading = false; }
    });
  }

  private applyVehiclePage(result: { items: VehicleSummaryResponse[]; totalItems: number; totalPages: number; page: number }): void {
    this.vehicles = result.items;
    this.vehicleTotal = result.totalItems;
    this.vehiclePageCount = Math.max(1, result.totalPages);
    this.vehiclePage = result.page + 1;
  }

  private routeLabel(route: RouteResponse): string {
    return `[${route.routeNumber}] ${route.name} · ${route.origin} → ${route.destination}`;
  }

  private loadVehicleImages(vehicleId: string): void { this.api.getAdminVehicleImages(vehicleId).subscribe({ next: (images) => (this.vehicleImages = images), error: () => (this.error = 'Could not load gallery images.') }); }
}
