package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.CrewAssignment;

public interface CrewAssignmentRepository extends JpaRepository<CrewAssignment, UUID> {
	List<CrewAssignment> findByVehicleIdAndEndedAtIsNullOrderByRoleAsc(UUID vehicleId);
	Optional<CrewAssignment> findByUserIdAndEndedAtIsNull(UUID userId);
	List<CrewAssignment> findByUserIdOrderByStartedAtDesc(UUID userId);
}
