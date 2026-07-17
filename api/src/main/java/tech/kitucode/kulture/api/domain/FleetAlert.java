package tech.kitucode.kulture.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import tech.kitucode.kulture.api.domain.enumerations.AlertSeverity;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Table(name = "fleet_alerts")
public class FleetAlert {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(nullable = false)
	private String type;

	@Column(nullable = false)
	private String message;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private AlertSeverity severity;

	@Column(nullable = false)
	private boolean resolved;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	public void resolve() {
		this.resolved = true;
		this.updatedAt = Instant.now();
	}

}
