package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;

public record FinancialOperationClassificationInput(
		String supplierOperationName,
		String documentType,
		BigDecimal acquiringAmount,
		BigDecimal storageAmount,
		BigDecimal acceptanceAmount,
		BigDecimal penaltyAmount,
		BigDecimal deductionAmount) {
}
