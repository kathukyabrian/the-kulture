package tech.kitucode.kulture.api.web.rest.dto;
import tools.jackson.databind.JsonNode;

public record RouteAdminRequest(
	String routeNumber,
	String name,
	String origin,
	String destination,
	String description,
	boolean active,
	JsonNode geometry,
	JsonNode waypoints
) {
}
