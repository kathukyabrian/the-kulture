package tech.kitucode.kulture.api.service;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.enumerations.AccountRole;
import tech.kitucode.kulture.api.domain.enumerations.UserStatus;
import tech.kitucode.kulture.api.repository.CrewAssignmentRepository;
import tech.kitucode.kulture.api.repository.UserAccountRepository;
import tech.kitucode.kulture.api.web.rest.dto.*;

@Service
@Transactional(readOnly = true)
public class CrewContextService {
	private final UserAccountRepository users;
	private final CrewAssignmentRepository assignments;
	private final VehicleService vehicles;
	private final UserService userService;
	public CrewContextService(UserAccountRepository users, CrewAssignmentRepository assignments, VehicleService vehicles, UserService userService) { this.users = users; this.assignments = assignments; this.vehicles = vehicles; this.userService = userService; }
	public CrewContextResponse context(String principal) {
		var user = users.findById(UUID.fromString(principal)).filter(item -> item.getAccountRole() == AccountRole.CREW && item.getStatus() == UserStatus.ACTIVE).orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Crew account is not active"));
		var assignment = assignments.findByUserIdAndEndedAtIsNull(user.getId()).orElse(null);
		return new CrewContextResponse(userService.authResponse(user), assignment == null ? null : new CrewAssignmentContextResponse(assignment.getId(), assignment.getRole().name(), assignment.getStartedAt()), assignment == null ? null : vehicles.getForAdmin(assignment.getVehicle().getId()));
	}
}
