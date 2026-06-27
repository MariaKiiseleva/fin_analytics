package ru.marketplace.finance.finance.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationResult;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationService;

@RestController
@RequestMapping("/api/reports/daily")
public class DailyFinanceRecalculationController {

	private final DailyFinanceRecalculationService recalculationService;

	public DailyFinanceRecalculationController(DailyFinanceRecalculationService recalculationService) {
		this.recalculationService = recalculationService;
	}

	@PostMapping("/recalculate")
	@ResponseStatus(HttpStatus.ACCEPTED)
	public DailyFinanceRecalculationResult recalculate(@Valid @RequestBody RecalculateDailyFinanceRequest request) {
		return recalculationService.recalculate(request.userId(), request.dateFrom(), request.dateTo());
	}
}
