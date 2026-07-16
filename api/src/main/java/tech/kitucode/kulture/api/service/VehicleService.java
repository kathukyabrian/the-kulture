package tech.kitucode.kulture.api.service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.CrewMemberEntity;
import tech.kitucode.kulture.api.domain.OccupancyStatus;
import tech.kitucode.kulture.api.domain.VehicleEntity;
import tech.kitucode.kulture.api.domain.VehicleLocationEntity;
import tech.kitucode.kulture.api.domain.VehicleStatus;
import tech.kitucode.kulture.api.repository.CrewMemberRepository;
import tech.kitucode.kulture.api.repository.VehicleLocationRepository;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.web.rest.dto.CrewMemberResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.RouteResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleStatusUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleSummaryResponse;

@Service
@Transactional(readOnly = true)
public class VehicleService {

	private final VehicleRepository vehicleRepository;
	private final VehicleLocationRepository locationRepository;
	private final CrewMemberRepository crewMemberRepository;
	private final RouteService routeService;

	public VehicleService(
		VehicleRepository vehicleRepository,
		VehicleLocationRepository locationRepository,
		CrewMemberRepository crewMemberRepository,
		RouteService routeService
	) {
		this.vehicleRepository = vehicleRepository;
		this.locationRepository = locationRepository;
		this.crewMemberRepository = crewMemberRepository;
		this.routeService = routeService;
	}

	public List<VehicleSummaryResponse> list() {
		return vehicleRepository.findAll().stream()
			.sorted(Comparator.comparing(VehicleEntity::getName))
			.map(this::toSummary)
			.toList();
	}

	public List<VehicleSummaryResponse> search(String query) {
		if (query == null || query.isBlank()) {
			return list();
		}
		return vehicleRepository.search(query.trim()).stream()
			.map(this::toSummary)
			.toList();
	}

	public List<VehicleSummaryResponse> nearby(BigDecimal latitude, BigDecimal longitude) {
		return vehicleRepository.findByStatusOrderByNameAsc(VehicleStatus.ONLINE).stream()
			.sorted(Comparator.comparing(vehicle -> distanceScore(vehicle, latitude, longitude)))
			.map(this::toSummary)
			.toList();
	}

	public VehicleDetailResponse get(UUID id) {
		VehicleEntity vehicle = findById(id);
		List<CrewMemberResponse> crew = crewMemberRepository.findByVehicleIdAndActiveTrueOrderByRoleAsc(id).stream()
			.map(this::toCrewResponse)
			.toList();
		return toDetail(vehicle, crew);
	}

	@Transactional
	public VehicleDetailResponse goLive(UUID vehicleId) {
		VehicleEntity vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.ONLINE);
		return get(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse goOffline(UUID vehicleId) {
		VehicleEntity vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.OFFLINE);
		return get(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse updateLocation(UUID vehicleId, LocationUpdateRequest request) {
		VehicleEntity vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.ONLINE);
		locationRepository.save(new VehicleLocationEntity(vehicle, request.latitude(), request.longitude(), request.speedKph()));
		return get(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse updateStatus(UUID vehicleId, VehicleStatusUpdateRequest request) {
		VehicleEntity vehicle = findById(vehicleId);
		vehicle.setStatus(parseVehicleStatus(request.status()));
		vehicle.setOccupancyStatus(parseOccupancyStatus(request.occupancyStatus()));
		return get(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse verify(UUID vehicleId) {
		VehicleEntity vehicle = findById(vehicleId);
		vehicle.verify();
		return get(vehicleId);
	}

	public List<VehicleSummaryResponse> pendingVerification() {
		return vehicleRepository.findByVerifiedFalseOrderByUpdatedAtDesc().stream()
			.map(this::toSummary)
			.toList();
	}

	VehicleEntity findById(UUID id) {
		return vehicleRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
	}

	VehicleSummaryResponse toSummary(VehicleEntity vehicle) {
		return new VehicleSummaryResponse(
			vehicle.getId(),
			vehicle.getPlateNumber(),
			vehicle.getName(),
			vehicle.getRoute().getRouteNumber(),
			vehicle.getRoute().getName(),
			vehicle.getRoute().getDestination(),
			vehicle.getStatus().name(),
			vehicle.getOccupancyStatus().name(),
			vehicle.isVerified(),
			etaFor(vehicle),
			latestLocation(vehicle.getId())
		);
	}

	private VehicleDetailResponse toDetail(VehicleEntity vehicle, List<CrewMemberResponse> crew) {
		return new VehicleDetailResponse(
			vehicle.getId(),
			vehicle.getPlateNumber(),
			vehicle.getName(),
			routeService.toResponse(vehicle.getRoute()),
			vehicle.getStatus().name(),
			vehicle.getOccupancyStatus().name(),
			vehicle.isVerified(),
			vehicle.isWifiAvailable(),
			vehicle.getBassLevel(),
			vehicle.getScreenCount(),
			vehicle.getWatcherCount(),
			vehicle.getEarningsToday(),
			vehicle.getFleetPosition(),
			latestLocation(vehicle.getId()),
			crew
		);
	}

	private CrewMemberResponse toCrewResponse(CrewMemberEntity crewMember) {
		return new CrewMemberResponse(
			crewMember.getId(),
			crewMember.getDisplayName(),
			crewMember.getRole().name(),
			crewMember.getRating()
		);
	}

	private LocationResponse latestLocation(UUID vehicleId) {
		return locationRepository.findTopByVehicleIdOrderByRecordedAtDesc(vehicleId)
			.map(location -> new LocationResponse(
				location.getLatitude(),
				location.getLongitude(),
				location.getSpeedKph(),
				location.getRecordedAt()
			))
			.orElse(null);
	}

	private int etaFor(VehicleEntity vehicle) {
		return switch (vehicle.getStatus()) {
			case ONLINE -> 3 + Math.abs(vehicle.getFleetPosition() % 8);
			case MAINTENANCE -> 45;
			case OFFLINE -> 0;
		};
	}

	private double distanceScore(VehicleEntity vehicle, BigDecimal latitude, BigDecimal longitude) {
		LocationResponse latest = latestLocation(vehicle.getId());
		if (latest == null || latitude == null || longitude == null) {
			return Double.MAX_VALUE;
		}
		double lat = latest.latitude().subtract(latitude).doubleValue();
		double lng = latest.longitude().subtract(longitude).doubleValue();
		return (lat * lat) + (lng * lng);
	}

	private VehicleStatus parseVehicleStatus(String status) {
		try {
			return VehicleStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown vehicle status");
		}
	}

	private OccupancyStatus parseOccupancyStatus(String status) {
		try {
			return OccupancyStatus.valueOf(status.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown occupancy status");
		}
	}
}
