package ru.marketplace.finance.account.infrastructure;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.account.domain.MarketplaceCredential;
import ru.marketplace.finance.account.domain.MarketplaceProvider;

public interface MarketplaceCredentialRepository extends JpaRepository<MarketplaceCredential, Long> {

	Optional<MarketplaceCredential> findByUserIdAndProvider(Long userId, MarketplaceProvider provider);

	boolean existsByUserIdAndProvider(Long userId, MarketplaceProvider provider);
}
