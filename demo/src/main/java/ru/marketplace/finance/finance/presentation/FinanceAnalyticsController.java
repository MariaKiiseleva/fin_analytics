package ru.marketplace.finance.finance.presentation;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.FinanceAnalyticsReport;
import ru.marketplace.finance.finance.application.FinanceAnalyticsService;

@RestController
@RequestMapping("/api/reports/analytics")
public class FinanceAnalyticsController {

	private final FinanceAnalyticsService analyticsService;

	public FinanceAnalyticsController(FinanceAnalyticsService analyticsService) {
		this.analyticsService = analyticsService;
	}

	@GetMapping
	public FinanceAnalyticsReport findAnalytics(
			@RequestParam Long userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
		return analyticsService.buildReport(userId, dateFrom, dateTo);
	}
}
