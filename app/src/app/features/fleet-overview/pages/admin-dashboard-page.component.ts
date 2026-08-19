import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FleetOverviewResponse, VehicleSummaryResponse } from '../../../core/api.models';

@Component({ selector: 'app-admin-dashboard-page', standalone: true, imports: [CommonModule], templateUrl: './admin-dashboard-page.component.html' })
export class AdminDashboardPageComponent {
  @Input({ required: true }) overview!: FleetOverviewResponse;
  @Input() pendingVehicles: VehicleSummaryResponse[] = [];
  @Output() verifyVehicle = new EventEmitter<string>();
}
