package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;

public record MoneyCalculationResult(
		int salesQuantity,
		int returnQuantity,
		BigDecimal salesAmount,
		BigDecimal returnsAmount,
		BigDecimal commissionAmount,
		BigDecimal logisticsAmount,
		BigDecimal acquiringAmount,
		BigDecimal storageAmount,
		BigDecimal acceptanceAmount,
		BigDecimal penaltyAmount,
		BigDecimal deductionAmount) {
}
