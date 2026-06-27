package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

	@Query("""
			select
				entry.nmId as nmId,
				max(entry.productName) as productName,
				min(entry.businessDate) as firstBusinessDate,
				max(entry.businessDate) as lastBusinessDate,
				count(entry.id) as rowsCount,
				sum(entry.netQuantity) as netQuantity
			from DailyFinanceEntry entry
			where entry.userId = :userId
				and entry.businessDate between :dateFrom and :dateTo
				and entry.nmId is not null
				and entry.hasCost = false
			group by entry.nmId
			order by entry.nmId
			""")
	List<MissingProductCostSummary> findMissingProductCostSummaries(
			@Param("userId") Long userId,
			@Param("dateFrom") LocalDate dateFrom,
			@Param("dateTo") LocalDate dateTo);
}
