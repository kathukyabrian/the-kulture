package tech.kitucode.kulture.api.web.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.FleetOverviewService;
import tech.kitucode.kulture.api.service.VehicleService;
import tech.kitucode.kulture.api.service.RouteService;
import tech.kitucode.kulture.api.service.OpenRouteService;
import tech.kitucode.kulture.api.service.MediaService;
import tech.kitucode.kulture.api.service.UserService;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import tech.kitucode.kulture.api.web.rest.dto.FleetOverviewResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleAdminUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleSummaryResponse;
import tech.kitucode.kulture.api.web.rest.dto.PageResponse;
import tech.kitucode.kulture.api.web.rest.dto.RouteResponse;
import tech.kitucode.kulture.api.web.rest.dto.RouteAdminRequest;
import tech.kitucode.kulture.api.web.rest.dto.RouteCalculationRequest;
import tech.kitucode.kulture.api.web.rest.dto.RouteCalculationResponse;
import tech.kitucode.kulture.api.web.rest.dto.MediaResponse;
import tech.kitucode.kulture.api.web.rest.dto.UserResponse;
import tech.kitucode.kulture.api.web.rest.dto.InviteCrewRequest;
import tech.kitucode.kulture.api.web.rest.dto.AssignCrewRequest;
import tech.kitucode.kulture.api.web.rest.dto.UpdateUserStatusRequest;
import tech.kitucode.kulture.api.web.rest.dto.UpdateUserRoleRequest;

@RestController
@RequestMapping("/api/admin")
public class AdminResource {

	private final FleetOverviewService fleetOverviewService;
	private final VehicleService vehicleService;
	private final RouteService routeService;
	private final OpenRouteService routingService;
	private final MediaService mediaService;
	private final UserService userService;

	public AdminResource(FleetOverviewService fleetOverviewService, VehicleService vehicleService, RouteService routeService, MediaService mediaService, UserService userService, OpenRouteService routingService) {
		this.fleetOverviewService = fleetOverviewService;
		this.vehicleService = vehicleService;
		this.routeService = routeService;
		this.routingService = routingService;
		this.mediaService = mediaService;
		this.userService = userService;
	}

	@PostMapping("/routes/calculate")
	public RouteCalculationResponse calculateRoute(@RequestBody RouteCalculationRequest request) {
		return routingService.calculate(request);
	}

	@GetMapping("/users")
	public PageResponse<UserResponse> users(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "") String role, @RequestParam(defaultValue = "") String status, @RequestParam(defaultValue = "") String assignmentRole, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) { return userService.list(q, role, status, assignmentRole, page, size); }

	@GetMapping("/users/{userId}") public UserResponse user(@PathVariable UUID userId) { return userService.get(userId); }
	@GetMapping("/users/by-phone") public UserResponse userByPhone(@RequestParam String phone) { return userService.byPhone(phone); }
	@PostMapping("/users/invite-crew") public UserResponse inviteCrew(@RequestBody InviteCrewRequest request) { return userService.invite(request); }
	@PostMapping("/users/{userId}/resend-invitation") public UserResponse resendInvitation(@PathVariable UUID userId) { return userService.resendInvitation(userId); }
	@PatchMapping("/users/{userId}/status") public UserResponse updateUserStatus(@PathVariable UUID userId, @RequestBody UpdateUserStatusRequest request) { return userService.updateStatus(userId, request); }
	@PatchMapping("/users/{userId}/role") public UserResponse updateUserRole(@PathVariable UUID userId, @RequestBody UpdateUserRoleRequest request) { return userService.updateRole(userId, request); }
	@PostMapping("/vehicles/{vehicleId}/crew") public UserResponse assignCrew(@PathVariable UUID vehicleId, @RequestBody AssignCrewRequest request) { return userService.assign(vehicleId, request); }
	@DeleteMapping("/crew-assignments/{assignmentId}") public void endCrewAssignment(@PathVariable UUID assignmentId) { userService.endAssignment(assignmentId); }

	@GetMapping("/vehicles/{vehicleId}/images")
	public List<MediaResponse> vehicleImages(@PathVariable UUID vehicleId) { return mediaService.adminImages(vehicleId); }

	@PostMapping(value = "/vehicles/{vehicleId}/images", consumes = "multipart/form-data")
	public MediaResponse uploadVehicleImage(@PathVariable UUID vehicleId, @RequestPart("file") MultipartFile file) { return mediaService.upload(vehicleId, file); }

	@DeleteMapping("/vehicles/{vehicleId}/images/{imageId}")
	public void deleteVehicleImage(@PathVariable UUID vehicleId, @PathVariable UUID imageId) { mediaService.delete(vehicleId, imageId); }

	@GetMapping("/routes")
	public PageResponse<RouteResponse> routes(@RequestParam(defaultValue = "") String q, @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "6") int size) {
		return routeService.adminList(q, page, size);
	}

	@PostMapping("/routes")
	public RouteResponse createRoute(@RequestBody RouteAdminRequest request) {
		return routeService.create(request);
	}

	@PutMapping("/routes/{routeId}")
	public RouteResponse updateRoute(@PathVariable UUID routeId, @RequestBody RouteAdminRequest request) {
		return routeService.update(routeId, request);
	}

	@GetMapping("/fleet/overview")
	public FleetOverviewResponse overview() {
		return fleetOverviewService.overview();
	}

	@GetMapping("/vehicles/pending-verification")
	public List<VehicleSummaryResponse> pendingVerification() {
		return vehicleService.pendingVerification();
	}

	@GetMapping("/vehicles")
	public PageResponse<VehicleSummaryResponse> vehicles(
		@RequestParam(defaultValue = "") String q,
		@RequestParam(defaultValue = "0") int page,
		@RequestParam(defaultValue = "6") int size
	) {
		return vehicleService.adminList(q, page, size);
	}

	@GetMapping("/vehicles/{vehicleId}")
	public VehicleDetailResponse vehicle(@PathVariable UUID vehicleId) {
		return vehicleService.getForAdmin(vehicleId);
	}

	@PostMapping("/vehicles")
	public VehicleDetailResponse createVehicle(@RequestBody VehicleAdminUpdateRequest request) {
		return vehicleService.createByAdmin(request);
	}

	@PostMapping("/vehicles/{vehicleId}/verify")
	public VehicleDetailResponse verify(@PathVariable UUID vehicleId) {
		return vehicleService.verify(vehicleId);
	}

	@PutMapping("/vehicles/{vehicleId}")
	public VehicleDetailResponse updateVehicle(
		@PathVariable UUID vehicleId,
		@RequestBody VehicleAdminUpdateRequest request
	) {
		return vehicleService.updateByAdmin(vehicleId, request);
	}

}
