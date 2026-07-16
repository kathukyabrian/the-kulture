import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { vehicles, VehicleInfo } from '../../core/demo-data';

@Component({
  selector: 'app-nganya-profile',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './nganya-profile.component.html'
})
export class NganyaProfileComponent {
  readonly vehicle: VehicleInfo;

  constructor(route: ActivatedRoute) {
    const id = route.snapshot.paramMap.get('id');
    this.vehicle = vehicles.find((vehicle) => vehicle.id === id) ?? vehicles[0];
  }

  get rating(): number {
    const total = this.vehicle.crew.reduce((sum, crew) => sum + crew.rating, 0);
    return Math.round((total / this.vehicle.crew.length) * 10) / 10;
  }
}
