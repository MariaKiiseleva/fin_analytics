package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;

public interface RawFinancialOperationRepository extends JpaRepository<RawFinancialOperation, Long> {

	boolean existsByUserIdAndRowHash(Long userId, String rowHash);

	List<RawFinancialOperation> findByUserIdAndBusinessDate(Long userId, LocalDate businessDate);

	List<RawFinancialOperation> findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscIdAsc(
			Long userId,
			LocalDate dateFrom,
			LocalDate dateTo);
}
