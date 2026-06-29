package ru.marketplace.finance.finance.application;

public record FinancePeriodDeleteResult(
		long deletedRawRows,
		long deletedDailyRows) {
}
