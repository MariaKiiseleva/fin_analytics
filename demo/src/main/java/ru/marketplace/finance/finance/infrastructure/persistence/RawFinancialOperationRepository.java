package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;

public interface RawFinancialOperationRepository extends JpaRepository<RawFinancialOperation, Long> {

	boolean existsByUserIdAndRowHash(Long userId, String rowHash);

	List<RawFinancialOperation> findByUserIdAndBusinessDate(Long userId, LocalDate businessDate);

	List<RawFinancialOperation> findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscIdAsc(
			Long userId,
			LocalDate dateFrom,
			LocalDate dateTo);

	long deleteByUserIdAndBusinessDateBetween(Long userId, LocalDate dateFrom, LocalDate dateTo);

	long deleteByUserId(Long userId);

	long countByUserId(Long userId);

	@Query("""
			select
				operation.businessDate as businessDate,
				count(operation.id) as rowsCount
			from RawFinancialOperation operation
			where operation.userId = :userId
				and operation.businessDate between :dateFrom and :dateTo
			group by operation.businessDate
			order by operation.businessDate
			""")
	List<CoverageRowsByDate> countRowsByBusinessDate(
			@Param("userId") Long userId,
			@Param("dateFrom") LocalDate dateFrom,
			@Param("dateTo") LocalDate dateTo);

	@Query("""
			select new ru.marketplace.finance.finance.infrastructure.persistence.UnrecognizedOperationSummary(
				operation.supplierOperationName,
				operation.documentType,
				count(operation)
			)
			from RawFinancialOperation operation
			where operation.userId = :userId
				and operation.businessDate between :dateFrom and :dateTo
				and operation.classificationStatus = ru.marketplace.finance.finance.domain.ClassificationStatus.UNRECOGNIZED
			group by operation.supplierOperationName, operation.documentType
			order by count(operation) desc, operation.supplierOperationName asc, operation.documentType asc
			""")
	List<UnrecognizedOperationSummary> findUnrecognizedOperationSummaries(
			Long userId,
			LocalDate dateFrom,
			LocalDate dateTo);
}
