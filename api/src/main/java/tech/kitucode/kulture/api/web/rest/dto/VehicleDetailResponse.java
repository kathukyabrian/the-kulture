package tech.kitucode.kulture.api.web.rest.dto;

import java.util.List;
import java.util.UUID;

public record VehicleDetailResponse(
	UUID id,
	String plateNumber,
	String name,
	RouteResponse route,
	String status,
	String occupancyStatus,
	boolean verified,
	String listingState,
	boolean wifiAvailable,
	int bassLevel,
	int screenCount,
	String soundSystem,
	String customFeatures,
	int watcherCount,
	int fleetPosition,
	LocationResponse latestLocation,
	List<CrewMemberResponse> crew
) {
}
