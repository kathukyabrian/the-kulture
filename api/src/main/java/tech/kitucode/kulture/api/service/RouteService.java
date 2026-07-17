package tech.kitucode.kulture.api.service;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tech.kitucode.kulture.api.domain.Route;
import tech.kitucode.kulture.api.repository.RouteRepository;
import tech.kitucode.kulture.api.web.rest.dto.RouteResponse;

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
