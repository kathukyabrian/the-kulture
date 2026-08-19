package tech.kitucode.kulture.api.web.rest;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.service.UserService;
import tech.kitucode.kulture.api.web.rest.dto.*;

@RestController
@RequestMapping("/api/auth")
public class AuthResource {
	private final UserService users;
	public AuthResource(UserService users) { this.users = users; }

	@PostMapping("/login")
	public AuthUserResponse login(@RequestBody LoginRequest request, HttpServletRequest servletRequest) {
		var user = users.authenticate(request.email(), request.password());
		var authentication = new UsernamePasswordAuthenticationToken(user.getId().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getAccountRole().name())));
		var context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context);
		servletRequest.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
		return users.authResponse(user);
	}

	@PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void logout(HttpServletRequest request) { HttpSession session = request.getSession(false); if (session != null) session.invalidate(); SecurityContextHolder.clearContext(); }

	@PostMapping("/register") @ResponseStatus(HttpStatus.CREATED)
	public AuthUserResponse register(@RequestBody RegisterRequest request) { return users.register(request); }

	@PostMapping("/password/setup") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void setup(@RequestBody PasswordSetupRequest request) { users.setupPassword(request); }

	@PostMapping("/password/forgot") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void forgot(@RequestBody ForgotPasswordRequest request) { users.forgotPassword(request.email()); }

	@PostMapping("/password/reset") @ResponseStatus(HttpStatus.NO_CONTENT)
	public void reset(@RequestBody PasswordSetupRequest request) { users.setupPassword(request); }

	@GetMapping("/me")
	public AuthUserResponse me(org.springframework.security.core.Authentication auth, tech.kitucode.kulture.api.repository.UserAccountRepository repository) {
		if (auth == null || !auth.isAuthenticated()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
		return users.authResponse(repository.findById(java.util.UUID.fromString(auth.getName())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED)));
	}
}
