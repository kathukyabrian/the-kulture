package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.Route;

public interface RouteRepository extends JpaRepository<Route, UUID> {

	List<Route> findByActiveTrueOrderByRouteNumberAsc();
}
