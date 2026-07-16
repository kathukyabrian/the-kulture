package tech.kitucode.kulture.api.web.rest.dto;

import java.util.UUID;

public record RouteResponse(
	UUID id,
	String routeNumber,
	String name,
	String origin,
	String destination,
	String description,
	boolean active
) {
}
