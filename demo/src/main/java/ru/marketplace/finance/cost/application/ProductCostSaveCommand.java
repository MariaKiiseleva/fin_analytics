package ru.marketplace.finance.cost.application;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductCostSaveCommand(
		Long nmId,
		String productName,
		LocalDate validFrom,
		BigDecimal costAmount) {
}
