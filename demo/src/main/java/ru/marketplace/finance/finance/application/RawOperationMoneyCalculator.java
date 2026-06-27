package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.util.Objects;

public class RawOperationMoneyCalculator {

	private static final BigDecimal ZERO = BigDecimal.ZERO;

	public MoneyCalculationResult calculate(MoneyCalculationInput input) {
		Objects.requireNonNull(input, "input must not be null");
		FinancialOperationType operationType = Objects.requireNonNull(
				input.operationType(),
				"operationType must not be null");
		if (operationType == FinancialOperationType.WB_INTERNAL_EXPENSE_COMPENSATION) {
			return zeroResult();
		}
		BigDecimal revenue = resolveRevenue(input);
		BigDecimal commission = calculateCommission(input, operationType);
		BigDecimal logistics = calculateLogistics(input, operationType);

		return new MoneyCalculationResult(
				salesQuantity(input, operationType),
				returnQuantity(input, operationType),
				salesAmount(revenue, operationType),
				returnsAmount(revenue, operationType),
				commission,
				logistics,
				abs(input.acquiringAmount()),
				abs(input.storageAmount()),
				abs(input.acceptanceAmount()),
				abs(input.penaltyAmount()),
				abs(input.deductionAmount()));
	}

	private static MoneyCalculationResult zeroResult() {
		return new MoneyCalculationResult(0, 0, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO, ZERO);
	}

	private static int salesQuantity(MoneyCalculationInput input, FinancialOperationType operationType) {
		return operationType == FinancialOperationType.SALE ? absQuantity(input.quantity()) : 0;
	}

	private static int returnQuantity(MoneyCalculationInput input, FinancialOperationType operationType) {
		return operationType == FinancialOperationType.RETURN ? absQuantity(input.quantity()) : 0;
	}

	private static BigDecimal salesAmount(BigDecimal revenue, FinancialOperationType operationType) {
		return operationType == FinancialOperationType.SALE ? revenue : ZERO;
	}

	private static BigDecimal returnsAmount(BigDecimal revenue, FinancialOperationType operationType) {
		return operationType == FinancialOperationType.RETURN ? revenue : ZERO;
	}

	private static BigDecimal resolveRevenue(MoneyCalculationInput input) {
		if (isPositive(input.retailAmount())) {
			return input.retailAmount();
		}
		if (isPositive(input.retailAmountWithDiscount())) {
			return input.retailAmountWithDiscount();
		}
		return ZERO;
	}

	private static BigDecimal calculateCommission(
			MoneyCalculationInput input,
			FinancialOperationType operationType) {
		if (operationType != FinancialOperationType.SALE && operationType != FinancialOperationType.RETURN) {
			return ZERO;
		}
		if (input.commissionAmount() != null) {
			BigDecimal commission = abs(input.commissionAmount());
			return operationType == FinancialOperationType.RETURN ? commission.negate() : commission;
		}
		BigDecimal retail = resolveRevenue(input);
		BigDecimal sellerAmount = input.sellerAmount();
		if (retail.compareTo(ZERO) == 0 || sellerAmount == null) {
			return ZERO;
		}
		BigDecimal commission = abs(retail)
				.subtract(abs(sellerAmount))
				.subtract(abs(input.acquiringAmount()))
				.max(ZERO);
		return operationType == FinancialOperationType.RETURN ? commission.negate() : commission;
	}

	private static BigDecimal calculateLogistics(
			MoneyCalculationInput input,
			FinancialOperationType operationType) {
		if (operationType != FinancialOperationType.LOGISTICS) {
			return ZERO;
		}
		return abs(input.logisticsAmount())
				.add(abs(input.rebillLogisticsAmount()))
				.add(input.includePvzRewardInLogistics() ? abs(input.pvzRewardAmount()) : ZERO);
	}

	private static boolean isPositive(BigDecimal value) {
		return value != null && value.compareTo(ZERO) > 0;
	}

	private static BigDecimal abs(BigDecimal value) {
		return value == null ? ZERO : value.abs();
	}

	private static int absQuantity(Integer value) {
		return value == null ? 0 : Math.abs(value);
	}
}
