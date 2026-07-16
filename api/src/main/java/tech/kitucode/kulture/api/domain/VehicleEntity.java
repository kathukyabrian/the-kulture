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
@Table(name = "vehicles")
public class VehicleEntity {

	@Id
	private UUID id;

	@Column(name = "plate_number", nullable = false, unique = true)
	private String plateNumber;

	@Column(nullable = false)
	private String name;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "route_id", nullable = false)
	private RouteEntity route;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private VehicleStatus status;

	@Enumerated(EnumType.STRING)
	@Column(name = "occupancy_status", nullable = false)
	private OccupancyStatus occupancyStatus;

	@Column(nullable = false)
	private boolean verified;

	@Column(name = "wifi_available", nullable = false)
	private boolean wifiAvailable;

	@Column(name = "bass_level", nullable = false)
	private int bassLevel;

	@Column(name = "screen_count", nullable = false)
	private int screenCount;

	@Column(name = "watcher_count", nullable = false)
	private int watcherCount;

	@Column(name = "earnings_today", nullable = false)
	private int earningsToday;

	@Column(name = "fleet_position", nullable = false)
	private int fleetPosition;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected VehicleEntity() {
	}

	public UUID getId() {
		return id;
	}

	public String getPlateNumber() {
		return plateNumber;
	}

	public String getName() {
		return name;
	}

	public RouteEntity getRoute() {
		return route;
	}

	public VehicleStatus getStatus() {
		return status;
	}

	public void setStatus(VehicleStatus status) {
		this.status = status;
		this.updatedAt = Instant.now();
	}

	public OccupancyStatus getOccupancyStatus() {
		return occupancyStatus;
	}

	public void setOccupancyStatus(OccupancyStatus occupancyStatus) {
		this.occupancyStatus = occupancyStatus;
		this.updatedAt = Instant.now();
	}

	public boolean isVerified() {
		return verified;
	}

	public void verify() {
		this.verified = true;
		this.updatedAt = Instant.now();
	}

	public boolean isWifiAvailable() {
		return wifiAvailable;
	}

	public int getBassLevel() {
		return bassLevel;
	}

	public int getScreenCount() {
		return screenCount;
	}

	public int getWatcherCount() {
		return watcherCount;
	}

	public int getEarningsToday() {
		return earningsToday;
	}

	public int getFleetPosition() {
		return fleetPosition;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}
}
