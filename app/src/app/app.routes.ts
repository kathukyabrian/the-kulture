import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { CrewDashboardComponent } from './features/crew-dashboard/crew-dashboard.component';
import { AdminDashboardRouteComponent, AdminNganyasRouteComponent, AdminRoutesRouteComponent } from './features/fleet-overview/admin-route-pages.component';
import { LiveMapComponent } from './features/live-map/live-map.component';
import { LoginComponent } from './features/login/login.component';
import { NganyaProfileComponent } from './features/nganya-profile/nganya-profile.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: '', component: LiveMapComponent, canActivate: [authGuard], data: { roles: ['nganya'] } },
  { path: 'nganyas/:id', component: NganyaProfileComponent, canActivate: [authGuard], data: { roles: ['nganya'] } },
  { path: 'fleet/nganyas/:id/preview', component: NganyaProfileComponent, canActivate: [authGuard], data: { roles: ['admin'], adminPreview: true } },
  { path: 'crew', component: CrewDashboardComponent, canActivate: [authGuard], data: { roles: ['crew'] } },
  { path: 'fleet', component: AdminDashboardRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: 'fleet/nganyas', component: AdminNganyasRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: 'fleet/routes', component: AdminRoutesRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: '**', redirectTo: '' }
];
