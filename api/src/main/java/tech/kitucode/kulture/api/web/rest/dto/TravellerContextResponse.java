package tech.kitucode.kulture.api.web.rest.dto;

public record TravellerContextResponse(
	AuthUserResponse user,
	RouteResponse defaultRoute,
	RouteResponse temporaryRoute,
	RouteResponse activeRoute,
	boolean samplingEnabled
) {}
