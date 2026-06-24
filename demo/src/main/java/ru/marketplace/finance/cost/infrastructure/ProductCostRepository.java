package ru.marketplace.finance.cost.infrastructure;

import java.time.LocalDate;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.cost.domain.ProductCost;

public interface ProductCostRepository extends JpaRepository<ProductCost, Long> {

	Optional<ProductCost> findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
			Long userId,
			Long nmId,
			LocalDate businessDate);
}
