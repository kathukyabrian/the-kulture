package tech.kitucode.kulture.api.service;

import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.Route;
import tech.kitucode.kulture.api.domain.UserAccount;
import tech.kitucode.kulture.api.domain.enumerations.AccountRole;
import tech.kitucode.kulture.api.domain.enumerations.UserStatus;
import tech.kitucode.kulture.api.repository.UserAccountRepository;
import tech.kitucode.kulture.api.web.rest.dto.AuthUserResponse;
import tech.kitucode.kulture.api.web.rest.dto.TravellerContextResponse;

@Service
@Transactional(readOnly = true)
public class TravellerService {
	private final UserAccountRepository users;
	private final RouteService routes;
	public TravellerService(UserAccountRepository users, RouteService routes) { this.users = users; this.routes = routes; }

	public TravellerContextResponse context(String email) { return response(requireTraveller(email)); }

	@Transactional
	public TravellerContextResponse setDefaultRoute(String email, java.util.UUID routeId) {
		UserAccount user = requireTraveller(email);
		user.setDefaultRoute(requireActiveRoute(routeId));
		user.setUpdatedAt(Instant.now());
		return response(user);
	}

	@Transactional
	public TravellerContextResponse startSampling(String email, java.util.UUID routeId) {
		UserAccount user = requireTraveller(email);
		if (user.getDefaultRoute() != null && user.getDefaultRoute().getId().equals(routeId)) throw new ResponseStatusException(HttpStatus.CONFLICT, "Sampling route must differ from the default route");
		user.setTemporaryRoute(requireActiveRoute(routeId));
		user.setSamplingEnabled(true);
		user.setUpdatedAt(Instant.now());
		return response(user);
	}

	@Transactional
	public TravellerContextResponse stopSampling(String email) {
		UserAccount user = requireTraveller(email);
		user.setSamplingEnabled(false);
		user.setTemporaryRoute(null);
		user.setUpdatedAt(Instant.now());
		return response(user);
	}

	private UserAccount requireTraveller(String email) {
		UserAccount user;
		try {
			user = users.findById(java.util.UUID.fromString(email)).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		} catch (IllegalArgumentException exception) {
			user = users.findByEmailIgnoreCase(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
		}
		if (user.getAccountRole() != AccountRole.TRAVELLER || user.getStatus() != UserStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Traveller account is not active");
		return user;
	}

	private Route requireActiveRoute(java.util.UUID id) {
		Route route = routes.findById(id);
		if (!route.isActive()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Route is inactive");
		return route;
	}

	private TravellerContextResponse response(UserAccount user) {
		Route active = user.isSamplingEnabled() && user.getTemporaryRoute() != null ? user.getTemporaryRoute() : user.getDefaultRoute();
		return new TravellerContextResponse(
			new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), "traveller"),
			user.getDefaultRoute() == null ? null : routes.toResponse(user.getDefaultRoute()),
			user.getTemporaryRoute() == null ? null : routes.toResponse(user.getTemporaryRoute()),
			active == null ? null : routes.toResponse(active),
			user.isSamplingEnabled()
		);
	}
}
