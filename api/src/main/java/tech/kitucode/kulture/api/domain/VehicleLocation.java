package tech.kitucode.kulture.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "vehicle_locations")
public class VehicleLocation {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private Vehicle vehicle;

	@Column(nullable = false)
	private BigDecimal latitude;

	@Column(nullable = false)
	private BigDecimal longitude;

	@Column(name = "speed_kph", nullable = false)
	private int speedKph;

	@Column(name = "sample_id", nullable = false, unique = true)
	private UUID sampleId;

	@Column(name = "session_id")
	private UUID sessionId;

	@Column(name = "accuracy_meters")
	private BigDecimal accuracyMeters;

	@Column(name = "heading_degrees")
	private BigDecimal headingDegrees;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	@Column(name = "received_at", nullable = false)
	private Instant receivedAt;

	public VehicleLocation(Vehicle vehicle, BigDecimal latitude, BigDecimal longitude, int speedKph) {
		this(vehicle, UUID.randomUUID(), null, latitude, longitude, speedKph, null, null, Instant.now());
	}

	public VehicleLocation(Vehicle vehicle, UUID sampleId, UUID sessionId, BigDecimal latitude, BigDecimal longitude, int speedKph, BigDecimal accuracyMeters, BigDecimal headingDegrees, Instant recordedAt) {
		this.id = UUID.randomUUID();
		this.vehicle = vehicle;
		this.sampleId = sampleId;
		this.sessionId = sessionId;
		this.latitude = latitude;
		this.longitude = longitude;
		this.speedKph = speedKph;
		this.accuracyMeters = accuracyMeters;
		this.headingDegrees = headingDegrees;
		this.recordedAt = recordedAt;
		this.receivedAt = Instant.now();
	}

}
