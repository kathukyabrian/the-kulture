package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.FleetAlert;

public interface FleetAlertRepository extends JpaRepository<FleetAlert, UUID> {

	List<FleetAlert> findByResolvedFalseOrderByCreatedAtDesc();

	long countByResolvedFalse();
}
