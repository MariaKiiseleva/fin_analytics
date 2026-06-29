package ru.marketplace.finance.finance.presentation;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.FinancePeriodDeleteResult;
import ru.marketplace.finance.finance.application.FinancePeriodDeleteService;

@RestController
@RequestMapping("/api/reports")
public class FinancePeriodDeleteController {

	private final FinancePeriodDeleteService deleteService;

	public FinancePeriodDeleteController(FinancePeriodDeleteService deleteService) {
		this.deleteService = deleteService;
	}

	@DeleteMapping("/period")
	public FinancePeriodDeleteResult deletePeriod(@Valid @RequestBody DeleteFinancePeriodRequest request) {
		return deleteService.deletePeriod(request.userId(), request.dateFrom(), request.dateTo());
	}
}
