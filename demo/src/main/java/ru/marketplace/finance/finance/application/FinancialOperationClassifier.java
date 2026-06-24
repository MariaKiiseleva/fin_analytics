package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Set;

public class FinancialOperationClassifier {

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

	public FinancialOperationType classify(FinancialOperationClassificationInput input) {
		String operation = normalize(input.supplierOperationName());
		String document = normalize(input.documentType());

		if (RETURN_NAMES.contains(operation) || document.contains("возврат")) {
			return FinancialOperationType.RETURN;
		}
		if (SALE_NAMES.contains(operation) || document.contains("продаж")) {
			return FinancialOperationType.SALE;
		}
		if (operation.equals("хранение") || nonZero(input.storageAmount())) {
			return FinancialOperationType.STORAGE;
		}
		if (containsAny(operation, "логист", "перевоз", "пвз", "склад")) {
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
}
