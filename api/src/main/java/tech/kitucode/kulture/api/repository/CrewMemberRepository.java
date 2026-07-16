package tech.kitucode.kulture.api.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.CrewMemberEntity;

public interface CrewMemberRepository extends JpaRepository<CrewMemberEntity, UUID> {

	List<CrewMemberEntity> findByVehicleIdAndActiveTrueOrderByRoleAsc(UUID vehicleId);
}
