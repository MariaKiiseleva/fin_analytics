package ru.marketplace.finance.common.presentation;

import java.time.Instant;

public record ApiErrorResponse(
		Instant timestamp,
		int status,
		String error,
		String message,
		String path) {
}
