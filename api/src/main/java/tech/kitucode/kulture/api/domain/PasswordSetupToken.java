package tech.kitucode.kulture.api.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
@Entity
@Table(name = "password_setup_tokens")
public class PasswordSetupToken {
	@Id private UUID id;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
	@Column(name = "token_hash", nullable = false, unique = true) private String tokenHash;
	@Column(name = "expires_at", nullable = false) private Instant expiresAt;
	@Column(name = "used_at") private Instant usedAt;
	@Column(name = "created_at", nullable = false) private Instant createdAt;
}
