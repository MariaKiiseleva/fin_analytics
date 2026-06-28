package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.infrastructure.persistence.CoverageRowsByDate;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@RestController
@RequestMapping("/api/reports")
@Validated
public class ReportCoverageController {

	private final RawFinancialOperationRepository rawRepository;
	private final DailyFinanceEntryRepository dailyRepository;
	private final SyncJobRepository syncJobRepository;

	public ReportCoverageController(
			RawFinancialOperationRepository rawRepository,
			DailyFinanceEntryRepository dailyRepository,
			SyncJobRepository syncJobRepository) {
		this.rawRepository = rawRepository;
		this.dailyRepository = dailyRepository;
		this.syncJobRepository = syncJobRepository;
	}

	@GetMapping("/coverage")
	public List<ReportCoverageDayView> findCoverage(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}

		Map<LocalDate, Long> rawRows = rowsByDate(rawRepository.countRowsByBusinessDate(userId, dateFrom, dateTo));
		Map<LocalDate, Long> dailyRows = rowsByDate(dailyRepository.countRowsByBusinessDate(userId, dateFrom, dateTo));
		Map<LocalDate, SyncStatus> syncStatuses = syncStatusByDate(userId, dateFrom, dateTo);

		return dateFrom.datesUntil(dateTo.plusDays(1))
				.map(date -> new ReportCoverageDayView(
						date,
						rawRows.getOrDefault(date, 0L) > 0,
						dailyRows.getOrDefault(date, 0L) > 0,
						rawRows.getOrDefault(date, 0L),
						dailyRows.getOrDefault(date, 0L),
						syncStatuses.get(date)))
				.toList();
	}

	private static Map<LocalDate, Long> rowsByDate(List<CoverageRowsByDate> rows) {
		Map<LocalDate, Long> result = new HashMap<>();
		for (CoverageRowsByDate row : rows) {
			result.put(row.getBusinessDate(), row.getRowsCount());
		}
		return result;
	}

	private Map<LocalDate, SyncStatus> syncStatusByDate(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		Map<LocalDate, SyncStatus> result = new HashMap<>();
		List<SyncJob> jobs = syncJobRepository
				.findByUserIdAndDateFromLessThanEqualAndDateToGreaterThanEqualOrderByRequestedAtAsc(
						userId,
						dateTo,
						dateFrom);

		for (SyncJob job : jobs) {
			LocalDate current = job.getDateFrom().isBefore(dateFrom) ? dateFrom : job.getDateFrom();
			LocalDate end = job.getDateTo().isAfter(dateTo) ? dateTo : job.getDateTo();
			while (!current.isAfter(end)) {
				result.put(current, job.getStatus());
				current = current.plusDays(1);
			}
		}
		return result;
	}
}
