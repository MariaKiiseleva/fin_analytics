package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RawOperationMoneyCalculatorTest {

	private final RawOperationMoneyCalculator calculator = new RawOperationMoneyCalculator();

	@Test
	void calculatesSaleRevenueQuantityAndCommission() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.SALE,
				2,
				"1000.00",
				null,
				"750.00",
				null,
				"25.00",
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null));

		assertThat(result.salesQuantity()).isEqualTo(2);
		assertThat(result.returnQuantity()).isZero();
		assertThat(result.salesAmount()).isEqualByComparingTo("1000.00");
		assertThat(result.returnsAmount()).isEqualByComparingTo("0");
		assertThat(result.commissionAmount()).isEqualByComparingTo("225.00");
		assertThat(result.acquiringAmount()).isEqualByComparingTo("25.00");
	}

	@Test
	void calculatesReturnAsPositiveReturnAmountAndNegativeCommission() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.RETURN,
				-1,
				"1000.00",
				null,
				"750.00",
				null,
				"25.00",
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null));

		assertThat(result.salesQuantity()).isZero();
		assertThat(result.returnQuantity()).isEqualTo(1);
		assertThat(result.salesAmount()).isEqualByComparingTo("0");
		assertThat(result.returnsAmount()).isEqualByComparingTo("1000.00");
		assertThat(result.commissionAmount()).isEqualByComparingTo("-225.00");
	}

	@Test
	void usesDiscountedRetailAmountWhenRetailAmountIsMissing() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.SALE,
				1,
				null,
				"900.00",
				"700.00",
				null,
				"20.00",
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null));

		assertThat(result.salesAmount()).isEqualByComparingTo("900.00");
		assertThat(result.commissionAmount()).isEqualByComparingTo("180.00");
	}

	@Test
	void prefersExplicitWildberriesCommissionWhenPresent() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.SALE,
				1,
				"1000.00",
				null,
				"750.00",
				"111.00",
				"25.00",
				null,
				null,
				null,
				false,
				null,
				null,
				null,
				null));

		assertThat(result.commissionAmount()).isEqualByComparingTo("111.00");
		assertThat(result.acquiringAmount()).isEqualByComparingTo("25.00");
	}

	@Test
	void calculatesLogisticsOnlyForLogisticsOperation() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.LOGISTICS,
				null,
				null,
				null,
				null,
				null,
				null,
				"120.00",
				"30.00",
				"15.00",
				true,
				null,
				null,
				null,
				null));

		assertThat(result.logisticsAmount()).isEqualByComparingTo("165.00");
		assertThat(result.salesAmount()).isEqualByComparingTo("0");
		assertThat(result.commissionAmount()).isEqualByComparingTo("0");
	}

	@Test
	void doesNotAddPvzRewardToEveryLogisticsOperation() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.LOGISTICS,
				null,
				null,
				null,
				null,
				null,
				null,
				"120.00",
				"30.00",
				"15.00",
				false,
				null,
				null,
				null,
				null));

		assertThat(result.logisticsAmount()).isEqualByComparingTo("150.00");
	}

	@Test
	void accumulatesCommonExpensesAsAbsoluteAmounts() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.DEDUCTION,
				null,
				null,
				null,
				null,
				null,
				"-10.00",
				null,
				null,
				null,
				false,
				"-20.00",
				"30.00",
				"-40.00",
				"50.00"));

		assertThat(result.acquiringAmount()).isEqualByComparingTo("10.00");
		assertThat(result.storageAmount()).isEqualByComparingTo("20.00");
		assertThat(result.acceptanceAmount()).isEqualByComparingTo("30.00");
		assertThat(result.penaltyAmount()).isEqualByComparingTo("40.00");
		assertThat(result.deductionAmount()).isEqualByComparingTo("50.00");
	}

	@Test
	void ignoresWildberriesInternalExpenseCompensationInSellerProfitCalculation() {
		MoneyCalculationResult result = calculator.calculate(input(
				FinancialOperationType.WB_INTERNAL_EXPENSE_COMPENSATION,
				null,
				null,
				null,
				null,
				null,
				"-100.00",
				"120.00",
				"30.00",
				null,
				false,
				"40.00",
				"50.00",
				"60.00",
				"70.00"));

		assertThat(result.salesQuantity()).isZero();
		assertThat(result.returnQuantity()).isZero();
		assertThat(result.salesAmount()).isEqualByComparingTo("0");
		assertThat(result.returnsAmount()).isEqualByComparingTo("0");
		assertThat(result.commissionAmount()).isEqualByComparingTo("0");
		assertThat(result.logisticsAmount()).isEqualByComparingTo("0");
		assertThat(result.acquiringAmount()).isEqualByComparingTo("0");
		assertThat(result.storageAmount()).isEqualByComparingTo("0");
		assertThat(result.acceptanceAmount()).isEqualByComparingTo("0");
		assertThat(result.penaltyAmount()).isEqualByComparingTo("0");
		assertThat(result.deductionAmount()).isEqualByComparingTo("0");
	}

	private static MoneyCalculationInput input(
			FinancialOperationType operationType,
			Integer quantity,
			String retailAmount,
			String retailAmountWithDiscount,
			String sellerAmount,
			String commissionAmount,
			String acquiringAmount,
			String logisticsAmount,
			String rebillLogisticsAmount,
			String pvzRewardAmount,
			boolean includePvzRewardInLogistics,
			String storageAmount,
			String acceptanceAmount,
			String penaltyAmount,
			String deductionAmount) {
		return new MoneyCalculationInput(
				operationType,
				quantity,
				amount(retailAmount),
				amount(retailAmountWithDiscount),
				amount(sellerAmount),
				amount(commissionAmount),
				amount(acquiringAmount),
				amount(logisticsAmount),
				amount(rebillLogisticsAmount),
				amount(pvzRewardAmount),
				includePvzRewardInLogistics,
				amount(storageAmount),
				amount(acceptanceAmount),
				amount(penaltyAmount),
				amount(deductionAmount));
	}

	private static BigDecimal amount(String value) {
		return value == null ? null : new BigDecimal(value);
	}
}
