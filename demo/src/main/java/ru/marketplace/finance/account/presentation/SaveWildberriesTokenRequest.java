package ru.marketplace.finance.account.presentation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SaveWildberriesTokenRequest(
		@NotNull
		@Positive
		Long userId,

		@NotBlank
		String token) {
}
