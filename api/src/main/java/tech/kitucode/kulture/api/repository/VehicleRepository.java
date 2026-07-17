package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.kitucode.kulture.api.domain.Vehicle;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	List<Vehicle> findByStatusOrderByNameAsc(VehicleStatus status);

	List<Vehicle> findByVerifiedFalseOrderByUpdatedAtDesc();

	long countByStatus(VehicleStatus status);

	long countByVerifiedFalse();

	@Query("""
		select v from VehicleEntity v
		join v.route r
		where lower(v.name) like lower(concat('%', :query, '%'))
		   or lower(v.plateNumber) like lower(concat('%', :query, '%'))
		   or lower(r.routeNumber) like lower(concat('%', :query, '%'))
		   or lower(r.name) like lower(concat('%', :query, '%'))
		   or lower(r.destination) like lower(concat('%', :query, '%'))
		order by v.name asc
		""")
	List<Vehicle> search(@Param("query") String query);
}
