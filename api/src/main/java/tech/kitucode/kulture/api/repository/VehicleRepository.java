package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tech.kitucode.kulture.api.domain.Vehicle;
import tech.kitucode.kulture.api.domain.enumerations.VehicleStatus;
import tech.kitucode.kulture.api.domain.enumerations.ListingState;

public interface VehicleRepository extends JpaRepository<Vehicle, UUID> {

	List<Vehicle> findByStatusOrderByNameAsc(VehicleStatus status);

	List<Vehicle> findByStatusAndListingStateOrderByNameAsc(VehicleStatus status, ListingState listingState);

	List<Vehicle> findByListingStateOrderByNameAsc(ListingState listingState);
	List<Vehicle> findByListingStateAndRouteIdOrderByNameAsc(ListingState listingState, UUID routeId);

	List<Vehicle> findByVerifiedFalseOrderByUpdatedAtDesc();

	long countByStatus(VehicleStatus status);

	long countByVerifiedFalse();

	boolean existsByPlateNumberIgnoreCase(String plateNumber);

	boolean existsByPlateNumberIgnoreCaseAndIdNot(String plateNumber, UUID id);

	@Query("""
		select v from Vehicle v
		join v.route r
		where v.listingState = tech.kitucode.kulture.api.domain.enumerations.ListingState.ACTIVE
		  and (lower(v.name) like lower(concat('%', :query, '%'))
		   or lower(v.plateNumber) like lower(concat('%', :query, '%'))
		   or lower(r.routeNumber) like lower(concat('%', :query, '%'))
		   or lower(r.name) like lower(concat('%', :query, '%'))
		   or lower(r.destination) like lower(concat('%', :query, '%')))
		order by v.name asc
		""")
	List<Vehicle> search(@Param("query") String query);

	@Query("""
		select v from Vehicle v
		join v.route r
		where :query = ''
		   or lower(v.name) like lower(concat('%', :query, '%'))
		   or lower(v.plateNumber) like lower(concat('%', :query, '%'))
		   or lower(r.routeNumber) like lower(concat('%', :query, '%'))
		   or lower(r.name) like lower(concat('%', :query, '%'))
		order by v.name asc
		""")
	Page<Vehicle> searchAdmin(@Param("query") String query, Pageable pageable);
}
