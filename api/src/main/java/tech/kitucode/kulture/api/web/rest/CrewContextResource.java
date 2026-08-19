package tech.kitucode.kulture.api.web.rest;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import tech.kitucode.kulture.api.service.CrewContextService;
import tech.kitucode.kulture.api.web.rest.dto.CrewContextResponse;

@RestController
@RequestMapping("/api/crew")
public class CrewContextResource {
	private final CrewContextService service;
	public CrewContextResource(CrewContextService service) { this.service = service; }
	@GetMapping("/me") public CrewContextResponse me(Authentication auth) { return service.context(auth.getName()); }
}
