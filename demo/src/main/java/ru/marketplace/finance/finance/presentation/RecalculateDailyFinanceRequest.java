package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record RecalculateDailyFinanceRequest(
		@NotNull @Positive Long userId,
		@NotNull LocalDate dateFrom,
		@NotNull LocalDate dateTo) {
}
