package ru.marketplace.finance.finance.presentation;

import java.math.BigDecimal;
import java.time.LocalDate;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;

public record DailyFinanceEntryView(
		Long id,
		Long userId,
		LocalDate businessDate,
		Long nmId,
		String productName,
		Integer salesQuantity,
		Integer returnQuantity,
		Integer netQuantity,
		BigDecimal salesAmount,
		BigDecimal returnsAmount,
		BigDecimal netRevenueAmount,
		BigDecimal commissionAmount,
		BigDecimal logisticsAmount,
		BigDecimal costAmount,
		BigDecimal taxAmount,
		BigDecimal productProfitAmount,
		BigDecimal acquiringAmount,
		BigDecimal storageAmount,
		BigDecimal acceptanceAmount,
		BigDecimal penaltyAmount,
		BigDecimal additionalDeductionsAmount,
		Boolean hasCost,
		Integer calculationVersion) {

	static DailyFinanceEntryView from(DailyFinanceEntry entry) {
		return new DailyFinanceEntryView(
				entry.getId(),
				entry.getUserId(),
				entry.getBusinessDate(),
				entry.getNmId(),
				entry.getProductName(),
				entry.getSalesQuantity(),
				entry.getReturnQuantity(),
				entry.getNetQuantity(),
				entry.getSalesAmount(),
				entry.getReturnsAmount(),
				entry.getNetRevenueAmount(),
				entry.getCommissionAmount(),
				entry.getLogisticsAmount(),
				entry.getCostAmount(),
				entry.getTaxAmount(),
				entry.getProductProfitAmount(),
				entry.getAcquiringAmount(),
				entry.getStorageAmount(),
				entry.getAcceptanceAmount(),
				entry.getPenaltyAmount(),
				entry.getAdditionalDeductionsAmount(),
				entry.getHasCost(),
				entry.getCalculationVersion());
	}
}
