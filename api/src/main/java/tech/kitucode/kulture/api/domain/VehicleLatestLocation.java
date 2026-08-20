package tech.kitucode.kulture.api.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@NoArgsConstructor
@Entity
@Table(name = "vehicle_latest_locations")
public class VehicleLatestLocation {
	@Id
	@Column(name = "vehicle_id")
	private UUID vehicleId;

	@Column(name = "sample_id", nullable = false)
	private UUID sampleId;
	@Column(nullable = false) private BigDecimal latitude;
	@Column(nullable = false) private BigDecimal longitude;
	@Column(name = "speed_kph", nullable = false) private int speedKph;
	@Column(name = "accuracy_meters") private BigDecimal accuracyMeters;
	@Column(name = "heading_degrees") private BigDecimal headingDegrees;
	@Column(name = "recorded_at", nullable = false) private Instant recordedAt;
	@Column(name = "received_at", nullable = false) private Instant receivedAt;

	public VehicleLatestLocation(Vehicle vehicle) {
		this.vehicleId = vehicle.getId();
	}

	public void updateFrom(VehicleLocation location) {
		this.sampleId = location.getSampleId();
		this.latitude = location.getLatitude();
		this.longitude = location.getLongitude();
		this.speedKph = location.getSpeedKph();
		this.accuracyMeters = location.getAccuracyMeters();
		this.headingDegrees = location.getHeadingDegrees();
		this.recordedAt = location.getRecordedAt();
		this.receivedAt = location.getReceivedAt();
	}
}
