package ru.marketplace.finance.finance.infrastructure.persistence;

public record UnrecognizedOperationSummary(
		String supplierOperationName,
		String documentType,
		long rowsCount) {
}
