import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { VehicleSummaryResponse } from '../../../core/api.models';

@Component({ selector: 'app-admin-nganyas-page', standalone: true, imports: [CommonModule, FormsModule, RouterLink], templateUrl: './admin-nganyas-page.component.html' })
export class AdminNganyasPageComponent {
  @Input() vehicles: VehicleSummaryResponse[] = []; @Input() total = 0; @Input() page = 1; @Input() pageCount = 1; @Input() perPage = 6; @Input() query = '';
  @Output() queryChange = new EventEmitter<string>(); @Output() pageChange = new EventEmitter<number>(); @Output() createVehicle = new EventEmitter<void>(); @Output() editVehicle = new EventEmitter<string>();
}
