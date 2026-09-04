package tech.kitucode.kulture.api.web.rest.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ArrivalEstimateResponse(
	UUID vehicleId,
	String status,
	Integer minimumMinutes,
	Integer maximumMinutes,
	Integer remainingDistanceMeters,
	OffsetDateTime vehicleLocationTimestamp,
	OffsetDateTime estimatedAt,
	String confidence,
	String reason,
	SnappedPoint passengerPosition,
	SnappedPoint vehiclePosition
) {
	public record SnappedPoint(double latitude, double longitude) {}
}
