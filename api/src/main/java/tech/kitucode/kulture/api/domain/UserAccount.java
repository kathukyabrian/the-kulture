package tech.kitucode.kulture.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import tech.kitucode.kulture.api.domain.enumerations.AccountRole;
import tech.kitucode.kulture.api.domain.enumerations.UserStatus;

@Data
@Entity
@Table(name = "users")
public class UserAccount {
	@Id private UUID id;
	@Column(nullable = false) private String name;
	@Column(nullable = false, unique = true) private String email;
	@Column(name = "phone_number", unique = true) private String phoneNumber;
	@Enumerated(EnumType.STRING) @Column(name = "account_role", nullable = false) private AccountRole accountRole;
	@Column(name = "password_hash") private String passwordHash;
	@Enumerated(EnumType.STRING) @Column(nullable = false) private UserStatus status;
	@Column(name = "email_verified_at") private Instant emailVerifiedAt;
	@Column(name = "created_at", nullable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
