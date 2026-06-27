package ru.marketplace.finance.cost.application;

import java.util.List;

public record ProductCostImportResult(
		int parsedRows,
		int savedRows,
		List<ProductCostView> savedCosts) {
}
