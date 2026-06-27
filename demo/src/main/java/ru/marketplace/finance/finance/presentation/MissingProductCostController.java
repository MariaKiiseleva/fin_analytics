package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.MissingProductCostSummary;

@RestController
@RequestMapping("/api/reports")
@Validated
public class MissingProductCostController {

	private final DailyFinanceEntryRepository dailyRepository;

	public MissingProductCostController(DailyFinanceEntryRepository dailyRepository) {
		this.dailyRepository = dailyRepository;
	}

	@GetMapping("/missing-product-costs")
	public List<MissingProductCostSummary> findMissingProductCosts(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		return dailyRepository.findMissingProductCostSummaries(userId, dateFrom, dateTo);
	}
}
