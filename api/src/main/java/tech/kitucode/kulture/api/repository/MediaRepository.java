package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.Media;

public interface MediaRepository extends JpaRepository<Media, UUID> {
	List<Media> findByVehicleIdAndApprovedTrueOrderBySortOrderAscCreatedAtAsc(UUID vehicleId);
	List<Media> findByVehicleIdOrderBySortOrderAscCreatedAtAsc(UUID vehicleId);
	long countByVehicleId(UUID vehicleId);
}
