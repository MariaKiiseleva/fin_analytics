package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;

public interface DailyFinanceEntryRepository extends JpaRepository<DailyFinanceEntry, Long> {

	List<DailyFinanceEntry> findByUserIdAndBusinessDate(Long userId, LocalDate businessDate);

	List<DailyFinanceEntry> findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscNmIdAsc(
			Long userId,
			LocalDate dateFrom,
			LocalDate dateTo);

	Optional<DailyFinanceEntry> findByUserIdAndBusinessDateAndNmId(
			Long userId,
			LocalDate businessDate,
			Long nmId);

	Optional<DailyFinanceEntry> findByUserIdAndBusinessDateAndNmIdIsNull(Long userId, LocalDate businessDate);

	long deleteByUserIdAndBusinessDateBetween(Long userId, LocalDate dateFrom, LocalDate dateTo);
}
