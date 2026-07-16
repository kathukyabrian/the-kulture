package tech.kitucode.kulture.api.web.rest.dto;

import java.util.UUID;

public record VehicleSummaryResponse(
	UUID id,
	String plateNumber,
	String name,
	String routeNumber,
	String routeName,
	String destination,
	String status,
	String occupancyStatus,
	boolean verified,
	int etaMinutes,
	LocationResponse latestLocation
) {
}
