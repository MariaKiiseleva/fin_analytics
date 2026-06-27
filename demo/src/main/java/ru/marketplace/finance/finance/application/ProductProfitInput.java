package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;

public record ProductProfitInput(
		BigDecimal netRevenueAmount,
		BigDecimal commissionAmount,
		BigDecimal logisticsAmount,
		BigDecimal acquiringAmount,
		BigDecimal costAmount,
		BigDecimal taxAmount) {
}
