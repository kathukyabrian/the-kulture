package tech.kitucode.kulture.api.web.rest;

import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.RouteService;
import tech.kitucode.kulture.api.web.rest.dto.RouteResponse;

@RestController
@RequestMapping("/api/routes")
public class RouteResource {

	private final RouteService routeService;

	public RouteResource(RouteService routeService) {
		this.routeService = routeService;
	}

	@GetMapping
	public List<RouteResponse> list() {
		return routeService.list();
	}

	@GetMapping("/{id}")
	public RouteResponse get(@PathVariable UUID id) {
		return routeService.get(id);
	}
}
