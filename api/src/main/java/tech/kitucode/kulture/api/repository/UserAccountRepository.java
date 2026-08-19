package tech.kitucode.kulture.api.repository;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import tech.kitucode.kulture.api.domain.UserAccount;
import tech.kitucode.kulture.api.domain.enumerations.AccountRole;

public interface UserAccountRepository extends JpaRepository<UserAccount, UUID>, JpaSpecificationExecutor<UserAccount> {
	Optional<UserAccount> findByEmailIgnoreCase(String email);
	Optional<UserAccount> findByPhoneNumber(String phoneNumber);
	boolean existsByEmailIgnoreCase(String email);
	boolean existsByPhoneNumber(String phoneNumber);
	long countByAccountRoleAndStatus(AccountRole role, tech.kitucode.kulture.api.domain.enumerations.UserStatus status);
}
