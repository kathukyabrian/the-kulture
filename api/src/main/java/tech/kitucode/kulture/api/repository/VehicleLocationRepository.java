package tech.kitucode.kulture.api.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.VehicleLocation;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocation, UUID> {

	Optional<VehicleLocation> findTopByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);
}
