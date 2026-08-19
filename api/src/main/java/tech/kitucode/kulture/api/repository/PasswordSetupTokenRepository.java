package tech.kitucode.kulture.api.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import tech.kitucode.kulture.api.domain.PasswordSetupToken;

public interface PasswordSetupTokenRepository extends JpaRepository<PasswordSetupToken, UUID> {
	Optional<PasswordSetupToken> findByTokenHashAndUsedAtIsNull(String tokenHash);
}
