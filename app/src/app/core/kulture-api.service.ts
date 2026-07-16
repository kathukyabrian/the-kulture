import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  FleetAlertResponse,
  FleetOverviewResponse,
  VehicleDetailResponse,
  VehicleStatus,
  OccupancyStatus,
  VehicleSummaryResponse
} from './api.models';

@Injectable({ providedIn: 'root' })
export class KultureApiService {
  private readonly baseUrl = '/api';

  constructor(private readonly http: HttpClient) {}

  getVehicles() {
    return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/vehicles`);
  }

  searchVehicles(query: string) {
    return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/vehicles/search`, {
      params: { q: query }
    });
  }

  getVehicle(id: string) {
    return this.http.get<VehicleDetailResponse>(`${this.baseUrl}/vehicles/${id}`);
  }

  getFleetOverview() {
    return this.http.get<FleetOverviewResponse>(`${this.baseUrl}/admin/fleet/overview`);
  }

  getPendingVerification() {
    return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/admin/vehicles/pending-verification`);
  }

  getAlerts() {
    return this.http.get<FleetAlertResponse[]>(`${this.baseUrl}/admin/alerts`);
  }

  verifyVehicle(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}/verify`, {});
  }

  goLive(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/go-live`, {});
  }

  goOffline(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/go-offline`, {});
  }

  updateVehicleStatus(vehicleId: string, status: VehicleStatus, occupancyStatus: OccupancyStatus) {
    return this.http.patch<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/status`, {
      status,
      occupancyStatus
    });
  }
}
