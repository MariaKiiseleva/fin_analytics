package ru.marketplace.finance.synchronization.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record StartWildberriesSyncRequest(
		@NotNull
		@Positive
		Long userId,

		@NotNull
		LocalDate dateFrom,

		@NotNull
		LocalDate dateTo) {
}
