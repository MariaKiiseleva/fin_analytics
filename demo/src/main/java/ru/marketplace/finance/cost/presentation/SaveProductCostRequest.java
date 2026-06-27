package ru.marketplace.finance.cost.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;

public record SaveProductCostRequest(
		@NotNull
		@Positive
		Long userId,

		@NotNull
		@Positive
		Long nmId,

		String productName,

		@NotNull
		LocalDate validFrom,

		@NotNull
		@PositiveOrZero
		BigDecimal costAmount) {
}
