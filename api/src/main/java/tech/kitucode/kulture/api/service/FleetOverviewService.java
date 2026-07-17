package tech.kitucode.kulture.api.service;

import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tech.kitucode.kulture.api.domain.FleetAlert;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.repository.FleetAlertRepository;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.web.rest.dto.FleetAlertResponse;
import tech.kitucode.kulture.api.web.rest.dto.FleetOverviewResponse;

@Service
@Transactional(readOnly = true)
public class FleetOverviewService {

	private final VehicleRepository vehicleRepository;
	private final FleetAlertRepository alertRepository;
	private final VehicleService vehicleService;

	public FleetOverviewService(
		VehicleRepository vehicleRepository,
		FleetAlertRepository alertRepository,
		VehicleService vehicleService
	) {
		this.vehicleRepository = vehicleRepository;
		this.alertRepository = alertRepository;
		this.vehicleService = vehicleService;
	}

	public FleetOverviewResponse overview() {
		var activeFleet = vehicleRepository.findByStatusOrderByNameAsc(VehicleStatus.ONLINE).stream()
			.map(vehicleService::toSummary)
			.toList();
		int projectedRevenue = activeFleet.size() * 8500;

		return new FleetOverviewResponse(
			vehicleRepository.countByStatus(VehicleStatus.ONLINE),
			vehicleRepository.countByVerifiedFalse(),
			alertRepository.countByResolvedFalse(),
			projectedRevenue,
			activeFleet,
			alerts()
		);
	}

	public List<FleetAlertResponse> alerts() {
		return alertRepository.findByResolvedFalseOrderByCreatedAtDesc().stream()
			.map(this::toResponse)
			.toList();
	}

	@Transactional
	public FleetAlertResponse resolve(UUID alertId) {
		FleetAlert alert = alertRepository.findById(alertId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Alert not found"));
		alert.resolve();
		return toResponse(alert);
	}

	private FleetAlertResponse toResponse(FleetAlert alert) {
		return new FleetAlertResponse(
			alert.getId(),
			alert.getVehicle().getName(),
			alert.getVehicle().getPlateNumber(),
			alert.getType(),
			alert.getMessage(),
			alert.getSeverity().name(),
			alert.isResolved(),
			alert.getCreatedAt()
		);
	}
}
