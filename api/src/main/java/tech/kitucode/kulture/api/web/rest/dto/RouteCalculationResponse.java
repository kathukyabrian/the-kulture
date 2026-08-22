package tech.kitucode.kulture.api.web.rest.dto;

import tools.jackson.databind.JsonNode;

public record RouteCalculationResponse(JsonNode geometry, double distanceMeters, double durationSeconds) {}
