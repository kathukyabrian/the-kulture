package tech.kitucode.kulture.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vehicle_locations")
public class VehicleLocationEntity {

	@Id
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "vehicle_id", nullable = false)
	private VehicleEntity vehicle;

	@Column(nullable = false)
	private BigDecimal latitude;

	@Column(nullable = false)
	private BigDecimal longitude;

	@Column(name = "speed_kph", nullable = false)
	private int speedKph;

	@Column(name = "recorded_at", nullable = false)
	private Instant recordedAt;

	protected VehicleLocationEntity() {
	}

	public VehicleLocationEntity(VehicleEntity vehicle, BigDecimal latitude, BigDecimal longitude, int speedKph) {
		this.id = UUID.randomUUID();
		this.vehicle = vehicle;
		this.latitude = latitude;
		this.longitude = longitude;
		this.speedKph = speedKph;
		this.recordedAt = Instant.now();
	}

	public UUID getId() {
		return id;
	}

	public VehicleEntity getVehicle() {
		return vehicle;
	}

	public BigDecimal getLatitude() {
		return latitude;
	}

	public BigDecimal getLongitude() {
		return longitude;
	}

	public int getSpeedKph() {
		return speedKph;
	}

	public Instant getRecordedAt() {
		return recordedAt;
	}
}
