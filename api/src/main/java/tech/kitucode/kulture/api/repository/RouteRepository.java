package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.RouteEntity;

public interface RouteRepository extends JpaRepository<RouteEntity, UUID> {

	List<RouteEntity> findByActiveTrueOrderByRouteNumberAsc();
}
