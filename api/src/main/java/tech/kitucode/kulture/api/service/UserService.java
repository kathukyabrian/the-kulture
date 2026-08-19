package tech.kitucode.kulture.api.service;

import jakarta.persistence.criteria.Predicate;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Locale;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.*;
import tech.kitucode.kulture.api.domain.enumerations.*;
import tech.kitucode.kulture.api.repository.*;
import tech.kitucode.kulture.api.web.rest.dto.*;

@Service
@Transactional(readOnly = true)
public class UserService {
	private static final Logger log = LoggerFactory.getLogger(UserService.class);
	private final UserAccountRepository users;
	private final CrewAssignmentRepository assignments;
	private final PasswordSetupTokenRepository tokens;
	private final VehicleRepository vehicles;
	private final PasswordEncoder passwordEncoder;
	private final SecureRandom random = new SecureRandom();

	public UserService(UserAccountRepository users, CrewAssignmentRepository assignments, PasswordSetupTokenRepository tokens, VehicleRepository vehicles, PasswordEncoder passwordEncoder) {
		this.users = users; this.assignments = assignments; this.tokens = tokens; this.vehicles = vehicles; this.passwordEncoder = passwordEncoder;
	}

	public PageResponse<UserResponse> list(String q, String role, String status, String assignmentRole, int page, int size) {
		Specification<UserAccount> spec = (root, query, cb) -> {
			var predicates = new ArrayList<Predicate>();
			String term = q == null ? "" : q.trim().toLowerCase(Locale.ROOT);
			if (!term.isBlank()) {
				String like = "%" + term + "%";
				predicates.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("email")), like), cb.like(cb.lower(root.get("phoneNumber")), like)));
			}
			if (role != null && !role.isBlank()) predicates.add(cb.equal(root.get("accountRole"), parseRole(role)));
			if (status != null && !status.isBlank()) predicates.add(cb.equal(root.get("status"), parseStatus(status)));
			if (assignmentRole != null && !assignmentRole.isBlank()) {
				var subquery = query.subquery(UUID.class); var assignment = subquery.from(CrewAssignment.class);
				var active = new ArrayList<Predicate>(); active.add(cb.equal(assignment.get("user").get("id"), root.get("id"))); active.add(cb.isNull(assignment.get("endedAt")));
				if (!assignmentRole.equalsIgnoreCase("UNASSIGNED")) active.add(cb.equal(assignment.get("role"), parseCrewRole(assignmentRole)));
				subquery.select(assignment.get("id")).where(active.toArray(Predicate[]::new));
				predicates.add(assignmentRole.equalsIgnoreCase("UNASSIGNED") ? cb.not(cb.exists(subquery)) : cb.exists(subquery));
			}
			return cb.and(predicates.toArray(Predicate[]::new));
		};
		var result = users.findAll(spec, PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), 50), Sort.by(Sort.Direction.DESC, "createdAt")));
		return PageResponse.from(result.map(this::response));
	}

	public UserResponse byPhone(String phone) { return response(users.findByPhoneNumber(normalizePhone(phone)).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"))); }
	public UserResponse get(UUID id) { return response(find(id)); }

	@Transactional
	public UserResponse invite(InviteCrewRequest request) {
		validateIdentity(request.name(), request.email(), request.phoneNumber());
		String email = normalizeEmail(request.email()); String phone = normalizePhone(request.phoneNumber());
		if (users.existsByEmailIgnoreCase(email) || users.existsByPhoneNumber(phone)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email or mobile number already exists");
		Instant now = Instant.now(); UserAccount user = new UserAccount(); user.setId(UUID.randomUUID()); user.setName(request.name().trim()); user.setEmail(email); user.setPhoneNumber(phone); user.setAccountRole(AccountRole.CREW); user.setStatus(UserStatus.INVITED); user.setCreatedAt(now); user.setUpdatedAt(now); users.save(user);
		issueToken(user); return response(user);
	}

	@Transactional
	public UserResponse resendInvitation(UUID id) { UserAccount user = find(id); if (user.getStatus() == UserStatus.ACTIVE) throw new ResponseStatusException(HttpStatus.CONFLICT, "User has already completed setup"); issueToken(user); return response(user); }

	@Transactional
	public UserResponse assign(UUID vehicleId, AssignCrewRequest request) {
		UserAccount user = find(request.userId());
		if (user.getAccountRole() != AccountRole.CREW) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only crew accounts can be assigned");
		Vehicle vehicle = vehicles.findById(vehicleId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
		CrewRole role = parseCrewRole(request.role()); Instant now = Instant.now();
		var current = assignments.findByUserIdAndEndedAtIsNull(user.getId());
		if (current.isPresent()) {
			CrewAssignment existing = current.get();
			if (existing.getVehicle().getId().equals(vehicleId)) { existing.setRole(role); existing.setUpdatedAt(now); return response(user); }
			if (!request.confirmMove()) throw new ResponseStatusException(HttpStatus.CONFLICT, "User is assigned to " + existing.getVehicle().getName() + "; confirm the move");
			existing.setEndedAt(now); existing.setUpdatedAt(now);
		}
		CrewAssignment assignment = new CrewAssignment(); assignment.setId(UUID.randomUUID()); assignment.setUser(user); assignment.setVehicle(vehicle); assignment.setRole(role); assignment.setRating(java.math.BigDecimal.ZERO); assignment.setStartedAt(now); assignment.setCreatedAt(now); assignment.setUpdatedAt(now); assignments.save(assignment);
		return response(user);
	}

	@Transactional
	public void endAssignment(UUID assignmentId) { CrewAssignment assignment = assignments.findById(assignmentId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Assignment not found")); assignment.setEndedAt(Instant.now()); assignment.setUpdatedAt(Instant.now()); }

	@Transactional
	public UserResponse updateStatus(UUID id, UpdateUserStatusRequest request) {
		UserAccount user = find(id); UserStatus status = parseStatus(request.status());
		if (user.getAccountRole() == AccountRole.ADMIN && status == UserStatus.SUSPENDED && users.countByAccountRoleAndStatus(AccountRole.ADMIN, UserStatus.ACTIVE) <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active admin cannot be suspended");
		user.setStatus(status); user.setUpdatedAt(Instant.now()); return response(user);
	}

	@Transactional
	public UserResponse updateRole(UUID id, UpdateUserRoleRequest request) {
		UserAccount user = find(id); AccountRole role = parseRole(request.role());
		if (user.getAccountRole() == AccountRole.ADMIN && role != AccountRole.ADMIN && users.countByAccountRoleAndStatus(AccountRole.ADMIN, UserStatus.ACTIVE) <= 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "The last active admin cannot be demoted");
		if (role != AccountRole.CREW) assignments.findByUserIdAndEndedAtIsNull(id).ifPresent(assignment -> { assignment.setEndedAt(Instant.now()); assignment.setUpdatedAt(Instant.now()); });
		user.setAccountRole(role); user.setUpdatedAt(Instant.now()); return response(user);
	}

	@Transactional
	public void forgotPassword(String email) { users.findByEmailIgnoreCase(normalizeEmail(email)).filter(user -> user.getStatus() != UserStatus.SUSPENDED).ifPresent(this::issueToken); }

	@Transactional
	public AuthUserResponse register(RegisterRequest request) {
		validateIdentity(request.name(), request.email(), request.phoneNumber()); validatePassword(request.password());
		String email = normalizeEmail(request.email()); String phone = normalizePhone(request.phoneNumber());
		if (users.existsByEmailIgnoreCase(email) || users.existsByPhoneNumber(phone)) throw new ResponseStatusException(HttpStatus.CONFLICT, "A user with this email or mobile number already exists");
		Instant now = Instant.now(); UserAccount user = new UserAccount(); user.setId(UUID.randomUUID()); user.setName(request.name().trim()); user.setEmail(email); user.setPhoneNumber(phone); user.setAccountRole(AccountRole.TRAVELLER); user.setPasswordHash(passwordEncoder.encode(request.password())); user.setStatus(UserStatus.ACTIVE); user.setEmailVerifiedAt(now); user.setCreatedAt(now); user.setUpdatedAt(now); users.save(user); return authResponse(user);
	}

	@Transactional
	public void setupPassword(PasswordSetupRequest request) {
		validatePassword(request.password()); PasswordSetupToken token = tokens.findByTokenHashAndUsedAtIsNull(hash(request.token())).filter(item -> item.getExpiresAt().isAfter(Instant.now())).orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Setup link is invalid or expired"));
		Instant now = Instant.now(); UserAccount user = token.getUser(); user.setPasswordHash(passwordEncoder.encode(request.password())); user.setStatus(UserStatus.ACTIVE); user.setEmailVerifiedAt(now); user.setUpdatedAt(now); token.setUsedAt(now);
	}

	public UserAccount authenticate(String email, String password) { UserAccount user = users.findByEmailIgnoreCase(normalizeEmail(email)).filter(item -> item.getStatus() == UserStatus.ACTIVE && item.getPasswordHash() != null && passwordEncoder.matches(password, item.getPasswordHash())).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password")); return user; }
	public AuthUserResponse authResponse(UserAccount user) { return new AuthUserResponse(user.getId(), user.getName(), user.getEmail(), user.getAccountRole().name().toLowerCase(Locale.ROOT)); }
	public UUID assignedVehicleId(String principal) { UUID userId = UUID.fromString(principal); users.findById(userId).filter(user -> user.getAccountRole() == AccountRole.CREW && user.getStatus() == UserStatus.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Crew account is not active")); return assignments.findByUserIdAndEndedAtIsNull(userId).map(item -> item.getVehicle().getId()).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "No active nganya assignment")); }
	public void requireAssignedVehicle(String principal, UUID vehicleId) { if (!assignedVehicleId(principal).equals(vehicleId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You are not assigned to this nganya"); }

	private UserResponse response(UserAccount user) { var active = assignments.findByUserIdAndEndedAtIsNull(user.getId()).orElse(null); return new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getPhoneNumber(), user.getAccountRole().name(), user.getStatus().name(), active == null ? null : active.getId(), active == null ? null : active.getVehicle().getId(), active == null ? null : active.getVehicle().getName(), active == null ? null : active.getRole().name(), user.getCreatedAt()); }
	private UserAccount find(UUID id) { return users.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found")); }
	private void issueToken(UserAccount user) { byte[] bytes = new byte[32]; random.nextBytes(bytes); String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); PasswordSetupToken token = new PasswordSetupToken(); token.setId(UUID.randomUUID()); token.setUser(user); token.setTokenHash(hash(raw)); token.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS)); token.setCreatedAt(Instant.now()); tokens.save(token); log.info("Password setup link for {}: http://localhost:4200/setup-password?token={}", user.getEmail(), raw); }
	private String hash(String value) { try { return java.util.HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); } catch (Exception ex) { throw new IllegalStateException(ex); } }
	private void validateIdentity(String name, String email, String phone) { if (name == null || name.isBlank() || email == null || !email.contains("@") || phone == null || phone.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name, valid email and mobile number are required"); }
	private void validatePassword(String password) { if (password == null || password.length() < 8) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Password must be at least 8 characters"); }
	private String normalizeEmail(String email) { return email == null ? "" : email.trim().toLowerCase(Locale.ROOT); }
	private String normalizePhone(String phone) { String digits = phone == null ? "" : phone.replaceAll("\\D", ""); if (digits.startsWith("0")) digits = "254" + digits.substring(1); if (!digits.startsWith("254") || digits.length() != 12) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Use a valid Kenyan mobile number"); return "+" + digits; }
	private AccountRole parseRole(String role) { try { return AccountRole.valueOf(role.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown account role"); } }
	private UserStatus parseStatus(String status) { try { return UserStatus.valueOf(status.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown user status"); } }
	private CrewRole parseCrewRole(String role) { try { return CrewRole.valueOf(role.trim().toUpperCase(Locale.ROOT)); } catch (Exception ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown crew role"); } }
}
