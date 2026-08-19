import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  FleetOverviewResponse,
  VehicleDetailResponse,
  VehicleStatus,
  OccupancyStatus,
  VehicleSummaryResponse
} from './api.models';
import { PageResponse, RouteAdminRequest, RouteResponse, VehicleAdminUpdateRequest, MediaResponse } from './api.models';

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

  getRoutes() {
    return this.http.get<RouteResponse[]>(`${this.baseUrl}/routes`);
  }

  getFleetOverview() {
    return this.http.get<FleetOverviewResponse>(`${this.baseUrl}/admin/fleet/overview`);
  }

  getAdminVehicles(query: string, page: number, size: number) {
    return this.http.get<PageResponse<VehicleSummaryResponse>>(`${this.baseUrl}/admin/vehicles`, {
      params: { q: query, page, size }
    });
  }

  getAdminVehicle(id: string) {
    return this.http.get<VehicleDetailResponse>(`${this.baseUrl}/admin/vehicles/${id}`);
  }

  getPendingVerification() {
    return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/admin/vehicles/pending-verification`);
  }

  verifyVehicle(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}/verify`, {});
  }

  updateVehicle(vehicleId: string, request: VehicleAdminUpdateRequest) {
    return this.http.put<VehicleDetailResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}`, request);
  }

  createVehicle(request: VehicleAdminUpdateRequest) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/admin/vehicles`, request);
  }

  getAdminRoutes(query: string, page: number, size: number) {
    return this.http.get<PageResponse<RouteResponse>>(`${this.baseUrl}/admin/routes`, { params: { q: query, page, size } });
  }

  createRoute(request: RouteAdminRequest) {
    return this.http.post<RouteResponse>(`${this.baseUrl}/admin/routes`, request);
  }

  updateRoute(routeId: string, request: RouteAdminRequest) {
    return this.http.put<RouteResponse>(`${this.baseUrl}/admin/routes/${routeId}`, request);
  }

  getVehicleImages(vehicleId: string) { return this.http.get<MediaResponse[]>(`${this.baseUrl}/vehicles/${vehicleId}/images`); }
  getAdminVehicleImages(vehicleId: string) { return this.http.get<MediaResponse[]>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images`); }
  uploadVehicleImage(vehicleId: string, file: File) { const body = new FormData(); body.append('file', file); return this.http.post<MediaResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images`, body); }
  deleteVehicleImage(vehicleId: string, imageId: string) { return this.http.delete<void>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images/${imageId}`); }

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
