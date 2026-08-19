import { Component } from '@angular/core';
import { FleetOverviewComponent } from './fleet-overview.component';

@Component({ selector: 'app-admin-dashboard-route', standalone: true, imports: [FleetOverviewComponent], template: '<app-fleet-overview initialTab="dashboard" />' })
export class AdminDashboardRouteComponent {}

@Component({ selector: 'app-admin-nganyas-route', standalone: true, imports: [FleetOverviewComponent], template: '<app-fleet-overview initialTab="nganyas" />' })
export class AdminNganyasRouteComponent {}

@Component({ selector: 'app-admin-routes-route', standalone: true, imports: [FleetOverviewComponent], template: '<app-fleet-overview initialTab="routes" />' })
export class AdminRoutesRouteComponent {}
