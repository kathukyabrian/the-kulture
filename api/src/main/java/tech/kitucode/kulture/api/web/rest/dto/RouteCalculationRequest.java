package tech.kitucode.kulture.api.web.rest.dto;

import java.math.BigDecimal;
import java.util.List;

public record RouteCalculationRequest(List<Coordinate> waypoints) {
	public record Coordinate(BigDecimal longitude, BigDecimal latitude) {}
}
