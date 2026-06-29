package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record FinanceAnalyticsReport(
		Summary summary,
		List<DailyPoint> dynamics,
		List<CostSlice> costStructure,
		List<ProductPoint> topProducts,
		List<ProductPoint> lossProducts) {

	public record Summary(
			BigDecimal netRevenue,
			BigDecimal wildberriesExpenses,
			BigDecimal costAmount,
			BigDecimal taxAmount,
			BigDecimal totalProfit,
			BigDecimal marginPercent,
			int productsWithoutCost) {
	}

	public record DailyPoint(
			LocalDate date,
			BigDecimal netRevenue,
			BigDecimal productProfit,
			BigDecimal totalProfit) {
	}

	public record CostSlice(
			String code,
			String label,
			BigDecimal amount,
			BigDecimal sharePercent) {
	}

	public record ProductPoint(
			Long nmId,
			String productName,
			BigDecimal netRevenue,
			BigDecimal profit,
			BigDecimal marginPercent) {
	}
}
