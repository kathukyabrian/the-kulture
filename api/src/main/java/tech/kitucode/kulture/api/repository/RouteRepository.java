package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.kitucode.kulture.api.domain.Route;

public interface RouteRepository extends JpaRepository<Route, UUID> {

	List<Route> findByActiveTrueOrderByRouteNumberAsc();

	boolean existsByRouteNumberIgnoreCase(String routeNumber);

	boolean existsByRouteNumberIgnoreCaseAndIdNot(String routeNumber, UUID id);

	@Query("""
		select r from Route r
		where :query = ''
		   or lower(r.routeNumber) like lower(concat('%', :query, '%'))
		   or lower(r.name) like lower(concat('%', :query, '%'))
		   or lower(r.origin) like lower(concat('%', :query, '%'))
		   or lower(r.destination) like lower(concat('%', :query, '%'))
		order by r.routeNumber asc
		""")
	Page<Route> searchAdmin(@Param("query") String query, Pageable pageable);
}
