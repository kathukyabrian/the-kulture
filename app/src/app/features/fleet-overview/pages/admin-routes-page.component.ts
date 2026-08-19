import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouteResponse } from '../../../core/api.models';

@Component({ selector: 'app-admin-routes-page', standalone: true, imports: [CommonModule, FormsModule], templateUrl: './admin-routes-page.component.html' })
export class AdminRoutesPageComponent {
  @Input() routes: RouteResponse[] = []; @Input() total = 0; @Input() page = 1; @Input() pageCount = 1; @Input() perPage = 6; @Input() query = '';
  @Output() queryChange = new EventEmitter<string>(); @Output() pageChange = new EventEmitter<number>(); @Output() createRoute = new EventEmitter<void>(); @Output() editRoute = new EventEmitter<RouteResponse>();
}
