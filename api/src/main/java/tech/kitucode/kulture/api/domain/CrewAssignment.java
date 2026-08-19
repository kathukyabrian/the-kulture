package tech.kitucode.kulture.api.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;
import tech.kitucode.kulture.api.domain.enumerations.CrewRole;

@Data
@Entity
@Table(name = "crew_assignments")
public class CrewAssignment {
	@Id private UUID id;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "user_id", nullable = false) private UserAccount user;
	@ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "vehicle_id", nullable = false) private Vehicle vehicle;
	@Enumerated(EnumType.STRING) @Column(name = "crew_role", nullable = false) private CrewRole role;
	@Column(nullable = false) private BigDecimal rating;
	@Column(name = "started_at", nullable = false) private Instant startedAt;
	@Column(name = "ended_at") private Instant endedAt;
	@Column(name = "created_at", nullable = false) private Instant createdAt;
	@Column(name = "updated_at", nullable = false) private Instant updatedAt;
}
