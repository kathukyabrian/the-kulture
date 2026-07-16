import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { vehicles } from '../../core/demo-data';

@Component({
  selector: 'app-crew-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink],
  templateUrl: './crew-dashboard.component.html'
})
export class CrewDashboardComponent {
  readonly vehicle = vehicles[0];
  live = this.vehicle.status === 'ONLINE';

  toggleLive(): void {
    this.live = !this.live;
  }
}
