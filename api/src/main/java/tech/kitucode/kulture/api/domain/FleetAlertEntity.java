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
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fleet_alerts")
public class FleetAlertEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private VehicleEntity vehicle;

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

	protected FleetAlertEntity() {
	}

	public UUID getId() {
		return id;
	}

	public VehicleEntity getVehicle() {
		return vehicle;
	}

	public String getType() {
		return type;
	}

	public String getMessage() {
		return message;
	}

	public AlertSeverity getSeverity() {
		return severity;
	}

	public boolean isResolved() {
		return resolved;
	}

	public void resolve() {
		this.resolved = true;
		this.updatedAt = Instant.now();
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
