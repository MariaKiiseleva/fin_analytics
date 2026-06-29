package ru.marketplace.finance.cost.infrastructure;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.cost.domain.ProductCost;

public interface ProductCostRepository extends JpaRepository<ProductCost, Long> {

	Optional<ProductCost> findByUserIdAndNmIdAndValidFrom(Long userId, Long nmId, LocalDate validFrom);

	List<ProductCost> findByUserIdAndNmIdOrderByValidFromDesc(Long userId, Long nmId);

	List<ProductCost> findByUserIdOrderByNmIdAscValidFromDesc(Long userId);

	Optional<ProductCost> findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
			Long userId,
			Long nmId,
			LocalDate businessDate);

	long deleteByUserId(Long userId);

	long countByUserId(Long userId);
}
