import { Component } from '@angular/core';
import { TravellerComponent } from './traveller.component';
@Component({ standalone: true, imports: [TravellerComponent], template: '<app-traveller tab="dashboard" />' }) export class TravellerDashboardPageComponent {}
@Component({ standalone: true, imports: [TravellerComponent], template: '<app-traveller tab="my-nganyas" />' }) export class TravellerMyNganyasPageComponent {}
@Component({ standalone: true, imports: [TravellerComponent], template: '<app-traveller tab="nganyas" />' }) export class TravellerNganyasPageComponent {}
