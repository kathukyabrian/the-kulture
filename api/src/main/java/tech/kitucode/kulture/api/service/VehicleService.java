package tech.kitucode.kulture.api.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.CrewAssignment;
import tech.kitucode.kulture.api.domain.enumerations.OccupancyStatus;
import tech.kitucode.kulture.api.domain.Vehicle;
import tech.kitucode.kulture.api.domain.VehicleLocation;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.domain.enumerations.ListingState;
import tech.kitucode.kulture.api.repository.CrewAssignmentRepository;
import tech.kitucode.kulture.api.repository.VehicleLocationRepository;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.web.rest.dto.CrewMemberResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationResponse;
import tech.kitucode.kulture.api.web.rest.dto.LocationUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleDetailResponse;
import tech.kitucode.kulture.api.web.rest.dto.VehicleAdminUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleStatusUpdateRequest;
import tech.kitucode.kulture.api.web.rest.dto.VehicleSummaryResponse;
import tech.kitucode.kulture.api.web.rest.dto.PageResponse;

@Service
@Transactional(readOnly = true)
public class VehicleService {

	private final VehicleRepository vehicleRepository;
	private final VehicleLocationRepository locationRepository;
	private final CrewAssignmentRepository crewAssignmentRepository;
	private final RouteService routeService;

	public VehicleService(
		VehicleRepository vehicleRepository,
		VehicleLocationRepository locationRepository,
		CrewAssignmentRepository crewAssignmentRepository,
		RouteService routeService
	) {
		this.vehicleRepository = vehicleRepository;
		this.locationRepository = locationRepository;
		this.crewAssignmentRepository = crewAssignmentRepository;
		this.routeService = routeService;
	}

	public List<VehicleSummaryResponse> list() {
		return vehicleRepository.findByStatusAndListingStateOrderByNameAsc(VehicleStatus.ONLINE, ListingState.ACTIVE).stream()
			.map(this::toSummary)
			.toList();
	}

	public List<VehicleSummaryResponse> list(UUID routeId) {
		if (routeId == null) return list();
		return vehicleRepository.findByStatusAndListingStateAndRouteIdOrderByNameAsc(VehicleStatus.ONLINE, ListingState.ACTIVE, routeId).stream()
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

	public PageResponse<VehicleSummaryResponse> adminList(String query, int page, int size) {
		String normalizedQuery = query == null ? "" : query.trim();
		int safePage = Math.max(0, page);
		int safeSize = Math.min(Math.max(1, size), 50);
		return PageResponse.from(vehicleRepository.searchAdmin(normalizedQuery, PageRequest.of(safePage, safeSize)).map(this::toSummary));
	}

	public List<VehicleSummaryResponse> nearby(BigDecimal latitude, BigDecimal longitude) {
		return vehicleRepository.findByStatusAndListingStateOrderByNameAsc(VehicleStatus.ONLINE, ListingState.ACTIVE).stream()
			.sorted(Comparator.comparing(vehicle -> distanceScore(vehicle, latitude, longitude)))
			.map(this::toSummary)
			.toList();
	}

	public VehicleDetailResponse get(UUID id) {
		Vehicle vehicle = findById(id);
		if (vehicle.getListingState() != ListingState.ACTIVE) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found");
		}
		return toDetailResponse(vehicle);
	}

	public VehicleDetailResponse getForAdmin(UUID id) {
		return toDetailResponse(findById(id));
	}

	private VehicleDetailResponse toDetailResponse(Vehicle vehicle) {
		UUID id = vehicle.getId();
		List<CrewMemberResponse> crew = crewAssignmentRepository.findByVehicleIdAndEndedAtIsNullOrderByRoleAsc(id).stream()
			.map(this::toCrewResponse)
			.toList();
		return toDetail(vehicle, crew);
	}

	@Transactional
	public VehicleDetailResponse goLive(UUID vehicleId) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.ONLINE);
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse goOffline(UUID vehicleId) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.OFFLINE);
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse updateLocation(UUID vehicleId, LocationUpdateRequest request) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.setStatus(VehicleStatus.ONLINE);
		locationRepository.save(new VehicleLocation(vehicle, request.latitude(), request.longitude(), request.speedKph()));
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse updateStatus(UUID vehicleId, VehicleStatusUpdateRequest request) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.setStatus(parseVehicleStatus(request.status()));
		vehicle.setOccupancyStatus(parseOccupancyStatus(request.occupancyStatus()));
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse updateOccupancy(UUID vehicleId, String occupancyStatus) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.setOccupancyStatus(parseOccupancyStatus(occupancyStatus));
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse verify(UUID vehicleId) {
		Vehicle vehicle = findById(vehicleId);
		vehicle.verify();
		return getForAdmin(vehicleId);
	}

	@Transactional
	public VehicleDetailResponse createByAdmin(VehicleAdminUpdateRequest request) {
		validateAdminRequest(request, null);
		Vehicle vehicle = new Vehicle();
		vehicle.setId(UUID.randomUUID());
		vehicle.setVerified(false);
		vehicle.setWatcherCount(0);
		vehicle.setFleetPosition((int) vehicleRepository.count() + 1);
		vehicle.setCreatedAt(Instant.now());
		applyAdminUpdate(vehicle, request);
		vehicleRepository.save(vehicle);
		return getForAdmin(vehicle.getId());
	}

	@Transactional
	public VehicleDetailResponse updateByAdmin(UUID vehicleId, VehicleAdminUpdateRequest request) {
		Vehicle vehicle = findById(vehicleId);
		validateAdminRequest(request, vehicleId);
		applyAdminUpdate(vehicle, request);
		return getForAdmin(vehicleId);
	}

	private void validateAdminRequest(VehicleAdminUpdateRequest request, UUID vehicleId) {
		if (request.name() == null || request.name().isBlank() || request.plateNumber() == null || request.plateNumber().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Name and plate number are required");
		}
		if (request.routeId() == null || request.status() == null || request.occupancyStatus() == null || request.listingState() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Route and status are required");
		}
		if (request.bassLevel() < 0 || request.bassLevel() > 100 || request.screenCount() < 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid feature values");
		}
		String plateNumber = request.plateNumber().trim();
		boolean plateExists = vehicleId == null
			? vehicleRepository.existsByPlateNumberIgnoreCase(plateNumber)
			: vehicleRepository.existsByPlateNumberIgnoreCaseAndIdNot(plateNumber, vehicleId);
		if (plateExists) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Plate number is already in use");
		}
	}

	private void applyAdminUpdate(Vehicle vehicle, VehicleAdminUpdateRequest request) {
		vehicle.setName(request.name().trim());
		vehicle.setPlateNumber(request.plateNumber().trim().toUpperCase());
		vehicle.setRoute(routeService.findById(request.routeId()));
		vehicle.setStatus(parseVehicleStatus(request.status()));
		vehicle.setOccupancyStatus(parseOccupancyStatus(request.occupancyStatus()));
		vehicle.setListingState(parseListingState(request.listingState()));
		vehicle.setWifiAvailable(request.wifiAvailable());
		vehicle.setBassLevel(request.bassLevel());
		vehicle.setScreenCount(request.screenCount());
		vehicle.setSoundSystem(request.soundSystem() == null || request.soundSystem().isBlank() ? "Not specified" : request.soundSystem().trim());
		vehicle.setCustomFeatures(request.customFeatures() == null ? "" : request.customFeatures().trim());
	}

	public List<VehicleSummaryResponse> pendingVerification() {
		return vehicleRepository.findByVerifiedFalseOrderByUpdatedAtDesc().stream()
			.map(this::toSummary)
			.toList();
	}

	Vehicle findById(UUID id) {
		return vehicleRepository.findById(id)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Vehicle not found"));
	}

	VehicleSummaryResponse toSummary(Vehicle vehicle) {
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
			vehicle.getListingState().name(),
			etaFor(vehicle),
			latestLocation(vehicle.getId())
		);
	}

	private VehicleDetailResponse toDetail(Vehicle vehicle, List<CrewMemberResponse> crew) {
		return new VehicleDetailResponse(
			vehicle.getId(),
			vehicle.getPlateNumber(),
			vehicle.getName(),
			routeService.toResponse(vehicle.getRoute()),
			vehicle.getStatus().name(),
			vehicle.getOccupancyStatus().name(),
			vehicle.isVerified(),
			vehicle.getListingState().name(),
			vehicle.isWifiAvailable(),
			vehicle.getBassLevel(),
			vehicle.getScreenCount(),
			vehicle.getSoundSystem(),
			vehicle.getCustomFeatures(),
			vehicle.getWatcherCount(),
			vehicle.getFleetPosition(),
			latestLocation(vehicle.getId()),
			crew
		);
	}

	private CrewMemberResponse toCrewResponse(CrewAssignment crewMember) {
		return new CrewMemberResponse(
			crewMember.getId(),
			crewMember.getUser().getName(),
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

	private int etaFor(Vehicle vehicle) {
		return switch (vehicle.getStatus()) {
			case ONLINE -> 3 + Math.abs(vehicle.getFleetPosition() % 8);
			case MAINTENANCE -> 45;
			case OFFLINE -> 0;
		};
	}

	private double distanceScore(Vehicle vehicle, BigDecimal latitude, BigDecimal longitude) {
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

	private ListingState parseListingState(String state) {
		try {
			return ListingState.valueOf(state.trim().toUpperCase());
		} catch (IllegalArgumentException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown listing state");
		}
	}
}
