package tech.kitucode.kulture.api.web.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.FleetOverviewService;
import tech.kitucode.kulture.api.service.VehicleService;
import tech.kitucode.kulture.api.web.rest.dto.FleetAlertResponse;
import tech.kitucode.kulture.api.web.rest.dto.FleetOverviewResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleSummaryResponse;

@RestController
@RequestMapping("/api/admin")
public class AdminResource {

	private final FleetOverviewService fleetOverviewService;
	private final VehicleService vehicleService;

	public AdminResource(FleetOverviewService fleetOverviewService, VehicleService vehicleService) {
		this.fleetOverviewService = fleetOverviewService;
		this.vehicleService = vehicleService;
	}

	@GetMapping("/fleet/overview")
	public FleetOverviewResponse overview() {
		return fleetOverviewService.overview();
	}

	@GetMapping("/vehicles/pending-verification")
	public List<VehicleSummaryResponse> pendingVerification() {
		return vehicleService.pendingVerification();
	}

	@PostMapping("/vehicles/{vehicleId}/verify")
	public VehicleDetailResponse verify(@PathVariable UUID vehicleId) {
		return vehicleService.verify(vehicleId);
	}

	@GetMapping("/alerts")
	public List<FleetAlertResponse> alerts() {
		return fleetOverviewService.alerts();
	}

	@PatchMapping("/alerts/{alertId}/resolve")
	public FleetAlertResponse resolveAlert(@PathVariable UUID alertId) {
		return fleetOverviewService.resolve(alertId);
	}
}
