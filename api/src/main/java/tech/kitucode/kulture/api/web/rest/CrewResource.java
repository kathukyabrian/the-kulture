package tech.kitucode.kulture.api.web.rest;

import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.VehicleService;
import tech.kitucode.kulture.api.web.rest.dto.LocationUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleStatusUpdateRequest;

@RestController
@RequestMapping("/api/crew/vehicles")
public class CrewResource {

	private final VehicleService vehicleService;

	public CrewResource(VehicleService vehicleService) {
		this.vehicleService = vehicleService;
	}

	@PostMapping("/{vehicleId}/go-live")
	public VehicleDetailResponse goLive(@PathVariable UUID vehicleId) {
		return vehicleService.goLive(vehicleId);
	}

	@PostMapping("/{vehicleId}/go-offline")
	public VehicleDetailResponse goOffline(@PathVariable UUID vehicleId) {
		return vehicleService.goOffline(vehicleId);
	}

	@PostMapping("/{vehicleId}/location")
	public VehicleDetailResponse updateLocation(@PathVariable UUID vehicleId, @Valid @RequestBody LocationUpdateRequest request) {
		return vehicleService.updateLocation(vehicleId, request);
	}

	@PatchMapping("/{vehicleId}/status")
	public VehicleDetailResponse updateStatus(@PathVariable UUID vehicleId, @Valid @RequestBody VehicleStatusUpdateRequest request) {
		return vehicleService.updateStatus(vehicleId, request);
	}
}
