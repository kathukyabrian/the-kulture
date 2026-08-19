import { Routes } from '@angular/router';
import { authGuard, guestGuard } from './core/auth.guard';
import { CrewDashboardRouteComponent, CrewMyNganyaRouteComponent, CrewNganyasRouteComponent } from './features/crew-dashboard/crew-route-pages.component';
import { AdminDashboardRouteComponent, AdminNganyasRouteComponent, AdminRoutesRouteComponent, AdminUsersRouteComponent } from './features/fleet-overview/admin-route-pages.component';
import { LiveMapComponent } from './features/live-map/live-map.component';
import { LoginComponent } from './features/login/login.component';
import { RegisterComponent } from './features/login/register.component';
import { SetupPasswordComponent } from './features/login/setup-password.component';
import { NganyaProfileComponent } from './features/nganya-profile/nganya-profile.component';
import { TravellerDashboardPageComponent, TravellerMyNganyasPageComponent, TravellerNganyasPageComponent } from './features/traveller/traveller-route-pages.component';

export const routes: Routes = [
  { path: 'login', component: LoginComponent, canActivate: [guestGuard] },
  { path: 'register', component: RegisterComponent, canActivate: [guestGuard] },
  { path: 'setup-password', component: SetupPasswordComponent, canActivate: [guestGuard] },
  { path: '', component: LiveMapComponent },
  { path: 'nganyas/:id', component: NganyaProfileComponent },
  { path: 'traveller', component: TravellerDashboardPageComponent, canActivate: [authGuard], data: { roles: ['traveller'] } },
  { path: 'traveller/my-nganyas', component: TravellerMyNganyasPageComponent, canActivate: [authGuard], data: { roles: ['traveller'] } },
  { path: 'traveller/nganyas', component: TravellerNganyasPageComponent, canActivate: [authGuard], data: { roles: ['traveller'] } },
  { path: 'traveller/nganyas/:id', component: NganyaProfileComponent, canActivate: [authGuard], data: { roles: ['traveller'], travellerPreview: true } },
  { path: 'fleet/nganyas/:id/preview', component: NganyaProfileComponent, canActivate: [authGuard], data: { roles: ['admin'], adminPreview: true } },
  { path: 'crew', component: CrewDashboardRouteComponent, canActivate: [authGuard], data: { roles: ['crew'] } },
  { path: 'crew/my-nganya', component: CrewMyNganyaRouteComponent, canActivate: [authGuard], data: { roles: ['crew'] } },
  { path: 'crew/nganyas', component: CrewNganyasRouteComponent, canActivate: [authGuard], data: { roles: ['crew'] } },
  { path: 'crew/nganyas/:id', component: NganyaProfileComponent, canActivate: [authGuard], data: { roles: ['crew'], crewPreview: true } },
  { path: 'fleet', component: AdminDashboardRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: 'fleet/nganyas', component: AdminNganyasRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: 'fleet/routes', component: AdminRoutesRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: 'fleet/users', component: AdminUsersRouteComponent, canActivate: [authGuard], data: { roles: ['admin'] } },
  { path: '**', redirectTo: '' }
];
