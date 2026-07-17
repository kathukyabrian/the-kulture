package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.CrewMember;

public interface CrewMemberRepository extends JpaRepository<CrewMember, UUID> {

	List<CrewMember> findByVehicleIdAndActiveTrueOrderByRoleAsc(UUID vehicleId);
}
