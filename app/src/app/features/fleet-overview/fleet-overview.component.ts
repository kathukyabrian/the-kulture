import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { alerts, vehicles } from '../../core/demo-data';

@Component({
  selector: 'app-fleet-overview',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './fleet-overview.component.html'
})
export class FleetOverviewComponent {
  readonly vehicles = vehicles;
  readonly alerts = alerts;

  get activeVehicles(): number {
    return this.vehicles.filter((vehicle) => vehicle.status === 'ONLINE').length;
  }

  get pendingVerification(): number {
    return this.vehicles.filter((vehicle) => !vehicle.verified).length;
  }

  get projectedRevenue(): number {
    return this.vehicles.reduce((sum, vehicle) => sum + vehicle.earningsToday, 0);
  }
}
