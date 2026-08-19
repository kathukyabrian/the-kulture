package tech.kitucode.kulture.api.web.rest.dto;

import java.util.UUID;
import java.util.List;

public record VehicleAdminUpdateRequest(
	String name,
	String plateNumber,
	UUID routeId,
	String status,
	String occupancyStatus,
	String listingState,
	boolean wifiAvailable,
	int bassLevel,
	int screenCount,
	String soundSystem,
	String customFeatures,
	List<CrewAdminRequest> crew
) {
}
