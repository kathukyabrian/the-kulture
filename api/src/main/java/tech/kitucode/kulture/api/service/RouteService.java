package tech.kitucode.kulture.api.service;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tech.kitucode.kulture.api.domain.Route;
import tech.kitucode.kulture.api.repository.RouteRepository;
import tech.kitucode.kulture.api.web.rest.dto.RouteResponse;
import tech.kitucode.kulture.api.web.rest.dto.RouteAdminRequest;
import tech.kitucode.kulture.api.web.rest.dto.PageResponse;

@Service
@Transactional(readOnly = true)
public class RouteService {

	private final RouteRepository routeRepository;
	private final ObjectMapper objectMapper;

	public RouteService(RouteRepository routeRepository, ObjectMapper objectMapper) {
		this.routeRepository = routeRepository;
		this.objectMapper = objectMapper;
	}

	public List<RouteResponse> list() {
		return routeRepository.findByActiveTrueOrderByRouteNumberAsc().stream()
			.map(this::toResponse)
			.toList();
	}

	public RouteResponse get(UUID id) {
		return toResponse(findById(id));
	}

	public PageResponse<RouteResponse> adminList(String query, int page, int size) {
		return PageResponse.from(routeRepository.searchAdmin(query == null ? "" : query.trim(), PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50))).map(this::toResponse));
	}

	@Transactional
	public RouteResponse create(RouteAdminRequest request) {
		validate(request, null);
		Route route = new Route();
		route.setId(UUID.randomUUID());
		route.setCreatedAt(Instant.now());
		apply(route, request);
		return toResponse(routeRepository.save(route));
	}

	@Transactional
	public RouteResponse update(UUID id, RouteAdminRequest request) {
		validate(request, id);
		Route route = findById(id);
		apply(route, request);
		return toResponse(route);
	}

	private void validate(RouteAdminRequest request, UUID id) {
		if (request.routeNumber() == null || request.routeNumber().isBlank() || request.name() == null || request.name().isBlank() || request.origin() == null || request.origin().isBlank() || request.destination() == null || request.destination().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route number, name, origin and destination are required");
		}
		boolean exists = id == null ? routeRepository.existsByRouteNumberIgnoreCase(request.routeNumber().trim()) : routeRepository.existsByRouteNumberIgnoreCaseAndIdNot(request.routeNumber().trim(), id);
		if (exists) throw new ResponseStatusException(HttpStatus.CONFLICT, "Route number is already in use");
		validateGeometry(request.geometry());
	}

	private void validateGeometry(JsonNode geometry) {
		if (geometry == null || geometry.isNull()) return;
		if (!"LineString".equals(geometry.path("type").asText()) || !geometry.path("coordinates").isArray() || geometry.path("coordinates").size() < 2) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Geometry must be a GeoJSON LineString with at least two coordinates");
		JsonNode previous = null;
		boolean distinct = false;
		for (JsonNode coordinate : geometry.path("coordinates")) {
			if (!coordinate.isArray() || coordinate.size() != 2 || !coordinate.get(0).isNumber() || !coordinate.get(1).isNumber()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Each coordinate must contain longitude and latitude");
			double longitude = coordinate.get(0).asDouble(), latitude = coordinate.get(1).asDouble();
			if (longitude < -180 || longitude > 180 || latitude < -90 || latitude > 90) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route coordinates are outside WGS84 bounds");
			if (previous != null && !previous.equals(coordinate)) distinct = true;
			previous = coordinate;
		}
		if (!distinct) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route geometry requires at least two distinct coordinates");
	}

	private void apply(Route route, RouteAdminRequest request) {
		route.setRouteNumber(request.routeNumber().trim());
		route.setName(request.name().trim());
		route.setOrigin(request.origin().trim());
		route.setDestination(request.destination().trim());
		route.setDescription(request.description() == null ? "" : request.description().trim());
		route.setActive(request.active());
		route.setGeometry(request.geometry() == null || request.geometry().isNull() ? null : request.geometry().toString());
		route.setUpdatedAt(Instant.now());
	}

	Route findById(UUID id) {
		return routeRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Route not found"));
	}

	RouteResponse toResponse(Route route) {
		return new RouteResponse(
			route.getId(),
			route.getRouteNumber(),
			route.getName(),
			route.getOrigin(),
			route.getDestination(),
			route.getDescription(),
			route.isActive(),
			parseGeometry(route.getGeometry())
		);
	}

	private JsonNode parseGeometry(String geometry) {
		if (geometry == null || geometry.isBlank()) return null;
		try { return objectMapper.readTree(geometry); }
		catch (Exception exception) { throw new IllegalStateException("Stored route geometry is invalid", exception); }
	}
}
