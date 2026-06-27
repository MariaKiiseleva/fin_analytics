package ru.marketplace.finance.account.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public record UpdateTaxPercentRequest(
		@NotNull @PositiveOrZero BigDecimal taxPercent) {
}
