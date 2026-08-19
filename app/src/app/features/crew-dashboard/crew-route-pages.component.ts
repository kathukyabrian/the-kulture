import { Component } from '@angular/core';
import { CrewDashboardComponent } from './crew-dashboard.component';

@Component({ selector: 'app-crew-dashboard-route', standalone: true, imports: [CrewDashboardComponent], template: '<app-crew-dashboard initialTab="dashboard" />' })
export class CrewDashboardRouteComponent {}
@Component({ selector: 'app-crew-my-nganya-route', standalone: true, imports: [CrewDashboardComponent], template: '<app-crew-dashboard initialTab="my-nganya" />' })
export class CrewMyNganyaRouteComponent {}
@Component({ selector: 'app-crew-nganyas-route', standalone: true, imports: [CrewDashboardComponent], template: '<app-crew-dashboard initialTab="nganyas" />' })
export class CrewNganyasRouteComponent {}
