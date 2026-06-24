package ru.marketplace.finance.finance.application;

public record DailyFinanceRecalculationResult(
		int rawRows,
		int affectedDays,
		int savedDailyRows,
		int unrecognizedRows) {
}
