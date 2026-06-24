package ru.marketplace.finance.finance.application;

import java.time.Instant;

public record BusinessDateInput(
		Instant reportAt,
		Instant saleAt,
		Instant orderAt,
		Instant createdAt) {
}
