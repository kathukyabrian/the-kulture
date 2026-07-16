package tech.kitucode.kulture.api.web.rest.dto;

import java.time.Instant;
import java.util.UUID;

public record FleetAlertResponse(
	UUID id,
	String vehicleName,
	String plateNumber,
	String type,
	String message,
	String severity,
	boolean resolved,
	Instant createdAt
) {
}
