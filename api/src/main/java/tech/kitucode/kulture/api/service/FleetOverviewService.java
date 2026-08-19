package tech.kitucode.kulture.api.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.domain.enumerations.ListingState;
import tech.kitucode.kulture.api.repository.VehicleRepository;
import tech.kitucode.kulture.api.web.rest.dto.FleetOverviewResponse;

@Service
@Transactional(readOnly = true)
public class FleetOverviewService {

	private final VehicleRepository vehicleRepository;
	private final VehicleService vehicleService;

	public FleetOverviewService(VehicleRepository vehicleRepository, VehicleService vehicleService) {
		this.vehicleRepository = vehicleRepository;
		this.vehicleService = vehicleService;
	}

	public FleetOverviewResponse overview() {
		var activeFleet = vehicleRepository.findByStatusAndListingStateOrderByNameAsc(VehicleStatus.ONLINE, ListingState.ACTIVE).stream()
			.map(vehicleService::toSummary)
			.toList();
		return new FleetOverviewResponse(
			vehicleRepository.countByStatus(VehicleStatus.ONLINE),
			vehicleRepository.countByVerifiedFalse(),
			activeFleet
		);
	}
}
