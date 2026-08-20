package tech.kitucode.kulture.api.repository;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.VehicleLatestLocation;

public interface VehicleLatestLocationRepository extends JpaRepository<VehicleLatestLocation, UUID> {}
