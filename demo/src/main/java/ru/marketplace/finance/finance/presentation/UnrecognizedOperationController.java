package ru.marketplace.finance.finance.presentation;

import jakarta.servlet.http.HttpSession;
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
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/reports")
@Validated
public class UnrecognizedOperationController {

	private final RawFinancialOperationRepository rawRepository;
	private final CurrentUserService currentUserService;

	public UnrecognizedOperationController(
			RawFinancialOperationRepository rawRepository,
			CurrentUserService currentUserService) {
		this.rawRepository = rawRepository;
		this.currentUserService = currentUserService;
	}

	@GetMapping("/unrecognized-operations")
	public List<UnrecognizedOperationSummary> findUnrecognizedOperations(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		return rawRepository.findUnrecognizedOperationSummaries(userId, dateFrom, dateTo);
	}
}
