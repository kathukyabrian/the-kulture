import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import {
  FleetOverviewResponse,
  VehicleDetailResponse,
  VehicleStatus,
  OccupancyStatus,
  VehicleSummaryResponse
} from './api.models';
import { PageResponse, RouteAdminRequest, RouteCalculationResponse, RouteResponse, VehicleAdminUpdateRequest, MediaResponse, UserResponse, AccountRole, UserStatus, CrewContextResponse, TravellerContextResponse } from './api.models';
import { environment } from '../../environments/environment';
import { map } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class KultureApiService {
  private readonly baseUrl = environment.apiBaseUrl;

  constructor(private readonly http: HttpClient) {}

  getVehicles() {
    return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/vehicles`);
  }

  getVehiclesByRoute(routeId: string) { return this.http.get<VehicleSummaryResponse[]>(`${this.baseUrl}/vehicles`, { params: { routeId } }); }

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

  calculateRoute(waypoints: [number, number][]) {
    return this.http.post<RouteCalculationResponse>(`${this.baseUrl}/admin/routes/calculate`, {
      waypoints: waypoints.map(([longitude, latitude]) => ({ longitude, latitude }))
    });
  }

  getAdminUsers(query: string, role: AccountRole | '', status: UserStatus | '', assignmentRole: string, page: number, size: number) { return this.http.get<PageResponse<UserResponse>>(`${this.baseUrl}/admin/users`, { params: { q: query, role, status, assignmentRole, page, size } }); }
  findUserByPhone(phone: string) { return this.http.get<UserResponse>(`${this.baseUrl}/admin/users/by-phone`, { params: { phone } }); }
  inviteCrew(request: { name: string; email: string; phoneNumber: string }) { return this.http.post<UserResponse>(`${this.baseUrl}/admin/users/invite-crew`, request); }
  assignCrew(vehicleId: string, request: { userId: string; role: 'DRIVER' | 'CONDUCTOR'; confirmMove: boolean }) { return this.http.post<UserResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}/crew`, request); }
  endCrewAssignment(assignmentId: string) { return this.http.delete<void>(`${this.baseUrl}/admin/crew-assignments/${assignmentId}`); }
  resendInvitation(userId: string) { return this.http.post<UserResponse>(`${this.baseUrl}/admin/users/${userId}/resend-invitation`, {}); }
  updateUserStatus(userId: string, status: UserStatus) { return this.http.patch<UserResponse>(`${this.baseUrl}/admin/users/${userId}/status`, { status }); }

  getVehicleImages(vehicleId: string) { return this.http.get<MediaResponse[]>(`${this.baseUrl}/vehicles/${vehicleId}/images`).pipe(map(images => images.map(image => this.withAbsoluteMediaUrl(image)))); }
  getAdminVehicleImages(vehicleId: string) { return this.http.get<MediaResponse[]>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images`).pipe(map(images => images.map(image => this.withAbsoluteMediaUrl(image)))); }
  uploadVehicleImage(vehicleId: string, file: File) { const body = new FormData(); body.append('file', file); return this.http.post<MediaResponse>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images`, body).pipe(map(image => this.withAbsoluteMediaUrl(image))); }
  deleteVehicleImage(vehicleId: string, imageId: string) { return this.http.delete<void>(`${this.baseUrl}/admin/vehicles/${vehicleId}/images/${imageId}`); }

  goLive(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/go-live`, {});
  }

  getAssignedCrewVehicle() { return this.http.get<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles`); }
  getCrewContext() { return this.http.get<CrewContextResponse>(`${this.baseUrl}/crew/me`); }
  getTravellerContext() { return this.http.get<TravellerContextResponse>(`${this.baseUrl}/traveller/me`); }
  setTravellerDefaultRoute(routeId: string) { return this.http.put<TravellerContextResponse>(`${this.baseUrl}/traveller/default-route`, { routeId }); }
  startTravellerSampling(routeId: string) { return this.http.post<TravellerContextResponse>(`${this.baseUrl}/traveller/sampling`, { routeId }); }
  stopTravellerSampling() { return this.http.delete<TravellerContextResponse>(`${this.baseUrl}/traveller/sampling`); }
  updateOccupancy(vehicleId: string, occupancyStatus: OccupancyStatus) { return this.http.patch<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/occupancy`, { occupancyStatus }); }
  updateLocation(vehicleId: string, latitude: number, longitude: number, speedKph: number) { return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/location`, { latitude, longitude, speedKph }); }

  goOffline(vehicleId: string) {
    return this.http.post<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/go-offline`, {});
  }

  updateVehicleStatus(vehicleId: string, status: VehicleStatus, occupancyStatus: OccupancyStatus) {
    return this.http.patch<VehicleDetailResponse>(`${this.baseUrl}/crew/vehicles/${vehicleId}/status`, {
      status,
      occupancyStatus
    });
  }

  private withAbsoluteMediaUrl(image: MediaResponse): MediaResponse {
    if (/^https?:\/\//i.test(image.url)) return image;
    const apiOrigin = new URL(this.baseUrl, window.location.origin).origin;
    return { ...image, url: new URL(image.url, apiOrigin).toString() };
  }
}
