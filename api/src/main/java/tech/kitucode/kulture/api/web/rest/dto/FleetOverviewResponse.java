package tech.kitucode.kulture.api.web.rest.dto;

import java.util.List;

public record FleetOverviewResponse(
	long activeVehicles,
	long pendingVerification,
	List<VehicleSummaryResponse> activeFleet
) {
}
