package ru.marketplace.finance.finance.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.FinancePeriodDeleteResult;
import ru.marketplace.finance.finance.application.FinancePeriodDeleteService;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/reports")
public class FinancePeriodDeleteController {

	private final FinancePeriodDeleteService deleteService;
	private final CurrentUserService currentUserService;

	public FinancePeriodDeleteController(
			FinancePeriodDeleteService deleteService,
			CurrentUserService currentUserService) {
		this.deleteService = deleteService;
		this.currentUserService = currentUserService;
	}

	@DeleteMapping("/period")
	public FinancePeriodDeleteResult deletePeriod(
			@Valid @RequestBody DeleteFinancePeriodRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, request.userId());
		return deleteService.deletePeriod(request.userId(), request.dateFrom(), request.dateTo());
	}
}
