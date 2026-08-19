package tech.kitucode.kulture.api.config;

import java.time.Instant;
import java.util.UUID;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;
import tech.kitucode.kulture.api.domain.UserAccount;
import tech.kitucode.kulture.api.domain.enumerations.AccountRole;
import tech.kitucode.kulture.api.domain.enumerations.UserStatus;
import tech.kitucode.kulture.api.repository.UserAccountRepository;
import tech.kitucode.kulture.api.repository.CrewAssignmentRepository;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.domain.CrewAssignment;
import tech.kitucode.kulture.api.domain.enumerations.CrewRole;
import java.math.BigDecimal;

@Configuration
public class UserBootstrapConfiguration {
	@Bean
	ApplicationRunner bootstrapUsers(UserAccountRepository users, CrewAssignmentRepository assignments, VehicleRepository vehicles, PasswordEncoder encoder) {
		return args -> {
			create(users, encoder, "Fleet Admin", "admin@kulture.test", "+254700000001", AccountRole.ADMIN, "admin123");
			create(users, encoder, "Crew Lead", "crew@kulture.test", "+254700000002", AccountRole.CREW, "crew1234");
			create(users, encoder, "Nganya Rider", "nganya@kulture.test", "+254700000003", AccountRole.TRAVELLER, "nganya123");
			var crew = users.findByEmailIgnoreCase("crew@kulture.test").orElseThrow();
			if (assignments.findByUserIdAndEndedAtIsNull(crew.getId()).isEmpty()) vehicles.findAll().stream().findFirst().ifPresent(vehicle -> { Instant now = Instant.now(); CrewAssignment assignment = new CrewAssignment(); assignment.setId(UUID.randomUUID()); assignment.setUser(crew); assignment.setVehicle(vehicle); assignment.setRole(CrewRole.DRIVER); assignment.setRating(BigDecimal.ZERO); assignment.setStartedAt(now); assignment.setCreatedAt(now); assignment.setUpdatedAt(now); assignments.save(assignment); });
		};

	}

	private void create(UserAccountRepository users, PasswordEncoder encoder, String name, String email, String phone, AccountRole role, String password) {
		if (users.existsByEmailIgnoreCase(email)) return;
		Instant now = Instant.now(); UserAccount user = new UserAccount(); user.setId(UUID.randomUUID()); user.setName(name); user.setEmail(email); user.setPhoneNumber(phone); user.setAccountRole(role); user.setPasswordHash(encoder.encode(password)); user.setStatus(UserStatus.ACTIVE); user.setEmailVerifiedAt(now); user.setCreatedAt(now); user.setUpdatedAt(now); users.save(user);
	}
}
