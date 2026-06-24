package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ProductProfitCalculatorTest {

	private final ProductProfitCalculator calculator = new ProductProfitCalculator();

	@Test
	void subtractsMarketplaceExpensesCostAndTaxFromNetRevenue() {
		BigDecimal profit = calculator.calculate(new ProductProfitInput(
				new BigDecimal("2000.00"),
				new BigDecimal("225.00"),
				new BigDecimal("150.00"),
				new BigDecimal("900.00"),
				new BigDecimal("120.00")));

		assertThat(profit).isEqualByComparingTo("605.00");
	}

	@Test
	void treatsMissingAmountsAsZero() {
		BigDecimal profit = calculator.calculate(new ProductProfitInput(
				new BigDecimal("100.00"),
				null,
				null,
				null,
				null));

		assertThat(profit).isEqualByComparingTo("100.00");
	}
}
