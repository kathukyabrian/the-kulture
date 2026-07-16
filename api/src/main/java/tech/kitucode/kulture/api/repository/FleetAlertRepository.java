package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.FleetAlertEntity;

public interface FleetAlertRepository extends JpaRepository<FleetAlertEntity, UUID> {

	List<FleetAlertEntity> findByResolvedFalseOrderByCreatedAtDesc();

	long countByResolvedFalse();
}
