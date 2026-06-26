package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@RestController
@RequestMapping("/api/reports")
@Validated
public class DailyFinanceReportController {

	private final DailyFinanceEntryRepository dailyRepository;

	public DailyFinanceReportController(DailyFinanceEntryRepository dailyRepository) {
		this.dailyRepository = dailyRepository;
	}

	@GetMapping("/daily")
	public List<DailyFinanceEntryView> findDailyReport(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		return dailyRepository
				.findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscNmIdAsc(userId, dateFrom, dateTo)
				.stream()
				.map(DailyFinanceEntryView::from)
				.sorted(Comparator
						.comparing(DailyFinanceEntryView::businessDate)
						.thenComparing(entry -> entry.nmId() == null ? 0 : 1)
						.thenComparing(DailyFinanceEntryView::nmId, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();
	}
}
