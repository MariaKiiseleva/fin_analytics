package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public class ProductProfitCalculator {

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	public BigDecimal calculate(ProductProfitInput input) {
		Objects.requireNonNull(input, "input must not be null");

		return amount(input.netRevenueAmount())
				.subtract(amount(input.commissionAmount()))
				.subtract(amount(input.logisticsAmount()))
				.subtract(amount(input.costAmount()))
				.subtract(amount(input.taxAmount()))
				.setScale(2, RoundingMode.HALF_UP);
	}

	private static BigDecimal amount(BigDecimal value) {
		return value == null ? ZERO : value;
	}
}
