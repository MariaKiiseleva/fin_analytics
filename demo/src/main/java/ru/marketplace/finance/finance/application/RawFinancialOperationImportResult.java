package ru.marketplace.finance.finance.application;

public record RawFinancialOperationImportResult(
		int receivedRows,
		int insertedRows,
		int duplicateRows) {
}
