package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public class FinancialOperationClassifier {

	private static final String PVZ_REWARD_OPERATION = "возмещение за выдачу и возврат товаров на пвз";

	private static final Set<String> SALE_NAMES = Set.of(
			"продажа",
			"корректная продажа",
			"сторно возвратов",
			"частичная компенсация брака",
			"компенсация подмененного товара",
			"оплата брака",
			"оплата потерянного товара",
			"авансовая оплата за товар без движения",
			"коррекция продаж");

	private static final Set<String> RETURN_NAMES = Set.of(
			"возврат",
			"корректный возврат",
			"сторно продаж");

	private static final Set<String> LOGISTICS_NAMES = Set.of(
			"логистика",
			"логистика сторно",
			"коррекция логистики",
			"возмещение издержек по перевозке",
			"возмещение издержек по перевозке/по складским операциям с товаром",
			PVZ_REWARD_OPERATION);

	private static final Set<String> COMPENSATION_NAMES = Set.of(
			"компенсация ущерба");

	private static final Set<String> WB_INTERNAL_EXPENSE_COMPENSATION_NAMES = Set.of(
			"возмещение издержек по перевозке",
			"возмещение издержек по перевозке/по складским операциям с товаром");

	public FinancialOperationType classify(FinancialOperationClassificationInput input) {
		String operation = normalize(input.supplierOperationName());
		String document = normalize(input.documentType());

		if (COMPENSATION_NAMES.contains(operation)) {
			return FinancialOperationType.COMPENSATION;
		}
		if (WB_INTERNAL_EXPENSE_COMPENSATION_NAMES.contains(operation)) {
			return FinancialOperationType.WB_INTERNAL_EXPENSE_COMPENSATION;
		}
		if (RETURN_NAMES.contains(operation) || document.contains("возврат")) {
			return FinancialOperationType.RETURN;
		}
		if (SALE_NAMES.contains(operation) || document.contains("продаж")) {
			return FinancialOperationType.SALE;
		}
		if (operation.equals("хранение") || nonZero(input.storageAmount())) {
			return FinancialOperationType.STORAGE;
		}
		if (LOGISTICS_NAMES.contains(operation)
				|| containsAny(operation, "логист", "перевоз", "складск", "пвз")) {
			return FinancialOperationType.LOGISTICS;
		}
		if (nonZero(input.acquiringAmount())) {
			return FinancialOperationType.ACQUIRING;
		}
		if (nonZero(input.acceptanceAmount())) {
			return FinancialOperationType.ACCEPTANCE;
		}
		if (nonZero(input.penaltyAmount())) {
			return FinancialOperationType.PENALTY;
		}
		if (nonZero(input.deductionAmount())) {
			return FinancialOperationType.DEDUCTION;
		}
		return FinancialOperationType.UNRECOGNIZED;
	}

	private static String normalize(String value) {
		if (value == null) {
			return "";
		}
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private static boolean containsAny(String value, String... fragments) {
		for (String fragment : fragments) {
			if (value.contains(fragment)) {
				return true;
			}
		}
		return false;
	}

	private static boolean nonZero(BigDecimal value) {
		return value != null && value.compareTo(BigDecimal.ZERO) != 0;
	}

	public boolean isPvzRewardLogisticsOperation(String supplierOperationName) {
		return PVZ_REWARD_OPERATION.equals(normalize(supplierOperationName));
	}
}
