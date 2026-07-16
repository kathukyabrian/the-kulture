package tech.kitucode.kulture.api.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.VehicleLocationEntity;

public interface VehicleLocationRepository extends JpaRepository<VehicleLocationEntity, UUID> {

	Optional<VehicleLocationEntity> findTopByVehicleIdOrderByRecordedAtDesc(UUID vehicleId);
}
