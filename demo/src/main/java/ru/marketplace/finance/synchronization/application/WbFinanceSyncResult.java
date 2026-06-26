package ru.marketplace.finance.synchronization.application;

public record WbFinanceSyncResult(
		Long syncJobId,
		int receivedRows,
		int insertedRows,
		int duplicateRows,
		int affectedDays,
		int savedDailyRows,
		int unrecognizedRows) {
}
