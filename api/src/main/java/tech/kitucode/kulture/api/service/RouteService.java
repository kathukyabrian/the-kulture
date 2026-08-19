package tech.kitucode.kulture.api.service;

import java.util.List;
import java.util.UUID;
import java.time.Instant;
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

	public RouteService(RouteRepository routeRepository) {
		this.routeRepository = routeRepository;
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
	}

	private void apply(Route route, RouteAdminRequest request) {
		route.setRouteNumber(request.routeNumber().trim());
		route.setName(request.name().trim());
		route.setOrigin(request.origin().trim());
		route.setDestination(request.destination().trim());
		route.setDescription(request.description() == null ? "" : request.description().trim());
		route.setActive(request.active());
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
			route.isActive()
		);
	}
}
