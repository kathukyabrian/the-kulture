package tech.kitucode.kulture.api.web.rest;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import tech.kitucode.kulture.api.service.TravellerService;
import tech.kitucode.kulture.api.web.rest.dto.RoutePreferenceRequest;
import tech.kitucode.kulture.api.web.rest.dto.TravellerContextResponse;

@RestController
@RequestMapping("/api/traveller")
public class TravellerResource {
	private final TravellerService travellers;
	public TravellerResource(TravellerService travellers) { this.travellers = travellers; }
	@GetMapping("/me") public TravellerContextResponse context(Authentication auth) { return travellers.context(auth.getName()); }
	@PutMapping("/default-route") public TravellerContextResponse setDefaultRoute(Authentication auth, @Valid @RequestBody RoutePreferenceRequest request) { return travellers.setDefaultRoute(auth.getName(), request.routeId()); }
	@PostMapping("/sampling") public TravellerContextResponse startSampling(Authentication auth, @Valid @RequestBody RoutePreferenceRequest request) { return travellers.startSampling(auth.getName(), request.routeId()); }
	@DeleteMapping("/sampling") public TravellerContextResponse stopSampling(Authentication auth) { return travellers.stopSampling(auth.getName()); }
}
