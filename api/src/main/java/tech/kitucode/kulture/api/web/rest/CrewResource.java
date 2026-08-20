package tech.kitucode.kulture.api.web.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.VehicleService;
import tech.kitucode.kulture.api.service.UserService;
import tech.kitucode.kulture.api.service.LocationBroadcastService;
import tech.kitucode.kulture.api.web.rest.dto.LocationBatchRequest;
import tech.kitucode.kulture.api.web.rest.dto.LocationBatchResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleStatusUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.OccupancyUpdateRequest;

@RestController
@RequestMapping("/api/crew/vehicles")
public class CrewResource {

	private final VehicleService vehicleService;
	private final UserService userService;
	private final LocationBroadcastService locations;

	public CrewResource(VehicleService vehicleService, UserService userService, LocationBroadcastService locations) {
		this.vehicleService = vehicleService;
		this.userService = userService;
		this.locations = locations;
	}

	@PostMapping("/{vehicleId}/locations")
	public LocationBatchResponse updateLocations(@PathVariable UUID vehicleId, @Valid @RequestBody LocationBatchRequest request, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return locations.ingest(vehicleId, request);
	}

	@GetMapping
	public VehicleDetailResponse assignedVehicle(org.springframework.security.core.Authentication auth) { return vehicleService.getForAdmin(userService.assignedVehicleId(auth.getName())); }

	@PostMapping("/{vehicleId}/go-live")
	public VehicleDetailResponse goLive(@PathVariable UUID vehicleId, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return vehicleService.goLive(vehicleId);
	}

	@PostMapping("/{vehicleId}/go-offline")
	public VehicleDetailResponse goOffline(@PathVariable UUID vehicleId, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return vehicleService.goOffline(vehicleId);
	}

	@PostMapping("/{vehicleId}/location")
	public VehicleDetailResponse updateLocation(@PathVariable UUID vehicleId, @Valid @RequestBody LocationUpdateRequest request, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return vehicleService.updateLocation(vehicleId, request);
	}

	@PatchMapping("/{vehicleId}/status")
	public VehicleDetailResponse updateStatus(@PathVariable UUID vehicleId, @Valid @RequestBody VehicleStatusUpdateRequest request, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return vehicleService.updateStatus(vehicleId, request);
	}

	@PatchMapping("/{vehicleId}/occupancy")
	public VehicleDetailResponse updateOccupancy(@PathVariable UUID vehicleId, @Valid @RequestBody OccupancyUpdateRequest request, org.springframework.security.core.Authentication auth) {
		userService.requireAssignedVehicle(auth.getName(), vehicleId);
		return vehicleService.updateOccupancy(vehicleId, request.occupancyStatus());
	}
}
