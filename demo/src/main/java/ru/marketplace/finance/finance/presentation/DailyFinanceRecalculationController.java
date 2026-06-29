package ru.marketplace.finance.finance.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationResult;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationService;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/reports/daily")
public class DailyFinanceRecalculationController {

	private final DailyFinanceRecalculationService recalculationService;
	private final CurrentUserService currentUserService;

	public DailyFinanceRecalculationController(
			DailyFinanceRecalculationService recalculationService,
			CurrentUserService currentUserService) {
		this.recalculationService = recalculationService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/recalculate")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DailyFinanceRecalculationResult recalculate(
			@Valid @RequestBody RecalculateDailyFinanceRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, request.userId());
		return recalculationService.recalculate(request.userId(), request.dateFrom(), request.dateTo());
	}
}
