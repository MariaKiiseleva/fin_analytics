package ru.marketplace.finance.cost.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import ru.marketplace.finance.cost.domain.ProductCost;

public record ProductCostView(
		Long id,
		Long userId,
		Long nmId,
		String productName,
		LocalDate validFrom,
		BigDecimal costAmount) {

	public static ProductCostView from(ProductCost productCost) {
		return new ProductCostView(
				productCost.getId(),
				productCost.getUserId(),
				productCost.getNmId(),
				productCost.getProductName(),
				productCost.getValidFrom(),
				productCost.getCostAmount());
	}
}
