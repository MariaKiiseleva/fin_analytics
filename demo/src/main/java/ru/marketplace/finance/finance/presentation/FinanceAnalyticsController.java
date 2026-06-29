package ru.marketplace.finance.finance.presentation;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.FinanceAnalyticsReport;
import ru.marketplace.finance.finance.application.FinanceAnalyticsService;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/reports/analytics")
public class FinanceAnalyticsController {

	private final FinanceAnalyticsService analyticsService;
	private final CurrentUserService currentUserService;

	public FinanceAnalyticsController(
			FinanceAnalyticsService analyticsService,
			CurrentUserService currentUserService) {
		this.analyticsService = analyticsService;
		this.currentUserService = currentUserService;
	}

	@GetMapping
	public FinanceAnalyticsReport findAnalytics(
			@RequestParam Long userId,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
			@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		return analyticsService.buildReport(userId, dateFrom, dateTo);
	}
}
