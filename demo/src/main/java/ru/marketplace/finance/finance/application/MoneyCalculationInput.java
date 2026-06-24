package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;

public record MoneyCalculationInput(
		FinancialOperationType operationType,
		Integer quantity,
		BigDecimal retailAmount,
		BigDecimal retailAmountWithDiscount,
		BigDecimal sellerAmount,
		BigDecimal acquiringAmount,
		BigDecimal logisticsAmount,
		BigDecimal rebillLogisticsAmount,
		BigDecimal pvzRewardAmount,
		BigDecimal storageAmount,
		BigDecimal acceptanceAmount,
		BigDecimal penaltyAmount,
		BigDecimal deductionAmount) {
}
