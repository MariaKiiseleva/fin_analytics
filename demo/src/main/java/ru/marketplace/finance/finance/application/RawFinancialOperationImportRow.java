package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.time.Instant;

public record RawFinancialOperationImportRow(
		String externalOperationId,
		String srid,
		Long nmId,
		String supplierOperationName,
		String documentType,
		Instant orderAt,
		Instant saleAt,
		Instant reportAt,
		Instant createdAt,
		Integer quantity,
		BigDecimal retailAmount,
		BigDecimal retailAmountWithDiscount,
		BigDecimal sellerAmount,
		BigDecimal commissionAmount,
		BigDecimal logisticsAmount,
		BigDecimal rebillLogisticsAmount,
		BigDecimal pvzRewardAmount,
		BigDecimal acquiringAmount,
		BigDecimal storageAmount,
		BigDecimal acceptanceAmount,
		BigDecimal penaltyAmount,
		BigDecimal deductionAmount,
		String rawPayload) {
}
