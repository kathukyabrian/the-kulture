import { Routes } from '@angular/router';
import { CrewDashboardComponent } from './features/crew-dashboard/crew-dashboard.component';
import { FleetOverviewComponent } from './features/fleet-overview/fleet-overview.component';
import { LiveMapComponent } from './features/live-map/live-map.component';
import { NganyaProfileComponent } from './features/nganya-profile/nganya-profile.component';

export const routes: Routes = [
  { path: '', component: LiveMapComponent },
  { path: 'nganyas/:id', component: NganyaProfileComponent },
  { path: 'crew', component: CrewDashboardComponent },
  { path: 'fleet', component: FleetOverviewComponent },
  { path: '**', redirectTo: '' }
];
