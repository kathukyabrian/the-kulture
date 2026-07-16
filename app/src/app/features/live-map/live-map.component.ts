import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { vehicles, VehicleInfo } from '../../core/demo-data';

@Component({
  selector: 'app-live-map',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './live-map.component.html'
})
export class LiveMapComponent {
  query = '';
  readonly vehicles = vehicles;

  get filteredVehicles(): VehicleInfo[] {
    const query = this.query.trim().toLowerCase();
    if (!query) {
      return this.vehicles;
    }
    return this.vehicles.filter((vehicle) => {
      const target = [
        vehicle.name,
        vehicle.plateNumber,
        vehicle.route.routeNumber,
        vehicle.route.name,
        vehicle.route.destination
      ].join(' ').toLowerCase();
      return target.includes(query);
    });
  }

  statusLabel(vehicle: VehicleInfo): string {
    if (vehicle.status === 'MAINTENANCE') {
      return 'Pit stop';
    }
    return vehicle.status === 'ONLINE' ? `${vehicle.etaMinutes} min` : 'Offline';
  }
}
