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
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.UnrecognizedOperationSummary;

@RestController
@RequestMapping("/api/reports")
@Validated
public class UnrecognizedOperationController {

	private final RawFinancialOperationRepository rawRepository;

	public UnrecognizedOperationController(RawFinancialOperationRepository rawRepository) {
		this.rawRepository = rawRepository;
	}

	@GetMapping("/unrecognized-operations")
	public List<UnrecognizedOperationSummary> findUnrecognizedOperations(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		return rawRepository.findUnrecognizedOperationSummaries(userId, dateFrom, dateTo);
	}
}
