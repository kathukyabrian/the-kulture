import { CommonModule } from '@angular/common';
import { Component, Input, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { RouteResponse, TravellerContextResponse, VehicleSummaryResponse } from '../../core/api.models';
import { AuthService } from '../../core/auth.service';
import { ConfirmationService } from '../../core/confirmation.service';
import { KultureApiService } from '../../core/kulture-api.service';

@Component({ selector: 'app-traveller', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './traveller.component.html' })
export class TravellerComponent implements OnInit {
  @Input() tab: 'dashboard' | 'my-nganyas' | 'nganyas' = 'dashboard';
  context: TravellerContextResponse | null = null; routes: RouteResponse[] = []; vehicles: VehicleSummaryResponse[] = [];
  routeQuery = ''; selectedRouteId = ''; picker: 'default' | 'sampling' | null = null; loading = true; saving = false; error = '';
  constructor(private api: KultureApiService, private auth: AuthService, private router: Router, private confirmation: ConfirmationService) {}
  ngOnInit(): void { this.load(); this.api.getRoutes().subscribe({ next: routes => this.routes = routes, error: () => this.error = 'Could not load routes.' }); }
  get filteredRoutes(): RouteResponse[] { const q = this.routeQuery.trim().toLowerCase(); return q ? this.routes.filter(r => `${r.routeNumber} ${r.name} ${r.origin} ${r.destination}`.toLowerCase().includes(q)) : this.routes; }
  load(): void { this.loading = true; this.error = ''; this.api.getTravellerContext().subscribe({ next: context => { this.context = context; this.loading = false; this.loadVehicles(); }, error: response => { this.loading = false; this.error = response.status === 403 ? 'Your Kulture account is not active.' : 'Could not load your Kulture account.'; } }); }
  loadVehicles(): void { if (this.tab === 'dashboard') return; if (this.tab === 'my-nganyas' && !this.context?.activeRoute) { this.vehicles = []; return; } const request = this.tab === 'my-nganyas' ? this.api.getVehiclesByRoute(this.context!.activeRoute!.id) : this.api.getVehicles(); request.subscribe({ next: v => this.vehicles = v, error: () => this.error = 'Could not load nganyas.' }); }
  openPicker(type: 'default' | 'sampling'): void { this.picker = type; this.selectedRouteId = type === 'default' ? this.context?.defaultRoute?.id ?? '' : ''; this.routeQuery = ''; }
  saveRoute(): void { if (!this.picker || !this.selectedRouteId || this.saving) return; this.saving = true; const request = this.picker === 'default' ? this.api.setTravellerDefaultRoute(this.selectedRouteId) : this.api.startTravellerSampling(this.selectedRouteId); request.subscribe({ next: context => { this.context = context; this.saving = false; this.picker = null; this.loadVehicles(); }, error: () => { this.saving = false; this.error = 'Could not save that route.'; } }); }
  async stopSampling(): Promise<void> { if (this.saving || !(await this.confirmation.confirm({ title: 'Quit sampling mode?', message: 'My Nganyas will return to your default route.', confirmLabel: 'Quit sampling' }))) return; this.saving = true; this.api.stopTravellerSampling().subscribe({ next: context => { this.context = context; this.saving = false; this.loadVehicles(); }, error: () => { this.saving = false; this.error = 'Could not quit sampling mode.'; } }); }
  async logout(): Promise<void> { if (!(await this.confirmation.confirm({ title: 'Sign out?', message: 'You will need to sign in again.', confirmLabel: 'Sign out' }))) return; this.auth.logout(); this.router.navigateByUrl('/login'); }
}
