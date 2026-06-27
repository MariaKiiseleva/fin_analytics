package ru.marketplace.finance.cost.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.time.LocalDate;
import ru.marketplace.finance.cost.application.ProductCostSaveCommand;

public record SaveProductCostItemRequest(
		@NotNull @Positive Long nmId,
		String productName,
		@NotNull LocalDate validFrom,
		@NotNull @PositiveOrZero BigDecimal costAmount) {

	ProductCostSaveCommand toCommand() {
		return new ProductCostSaveCommand(nmId, productName, validFrom, costAmount);
	}
}
