package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class FinancialOperationClassifierTest {

	private final FinancialOperationClassifier classifier = new FinancialOperationClassifier();

	@Test
	void classifiesSaleBySupplierOperationName() {
		assertThat(classify("Продажа", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Корректная продажа", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Коррекция продаж", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Частичная компенсация брака", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Компенсация подмененного товара", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Оплата брака", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Оплата потерянного товара", null)).isEqualTo(FinancialOperationType.SALE);
		assertThat(classify("Авансовая оплата за товар без движения", null)).isEqualTo(FinancialOperationType.SALE);
	}

	@Test
	void classifiesReturnBySupplierOperationNameOrDocumentType() {
		assertThat(classify("Возврат", null)).isEqualTo(FinancialOperationType.RETURN);
		assertThat(classify("Сторно продаж", null)).isEqualTo(FinancialOperationType.RETURN);
		assertThat(classify("Неизвестная операция", "Возврат")).isEqualTo(FinancialOperationType.RETURN);
	}

	@Test
	void classifiesLogisticsAndStorage() {
		assertThat(classify("Логистика", null)).isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Логистика сторно", null)).isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Коррекция логистики", null)).isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Возмещение издержек по перевозке", null)).isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Возмещение издержек по перевозке/по складским операциям с товаром", null))
				.isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Возмещение за выдачу и возврат товаров на ПВЗ", null))
				.isEqualTo(FinancialOperationType.LOGISTICS);
		assertThat(classify("Хранение", null)).isEqualTo(FinancialOperationType.STORAGE);
		assertThat(classifier.classify(input(null, null, null, "25.00", null, null, null)))
				.isEqualTo(FinancialOperationType.STORAGE);
	}

	@Test
	void classifiesCommonMoneyOperationsByNonZeroAmounts() {
		assertThat(classifier.classify(input(null, null, "10.00", null, null, null, null)))
				.isEqualTo(FinancialOperationType.ACQUIRING);
		assertThat(classifier.classify(input(null, null, null, null, "10.00", null, null)))
				.isEqualTo(FinancialOperationType.ACCEPTANCE);
		assertThat(classifier.classify(input(null, null, null, null, null, "10.00", null)))
				.isEqualTo(FinancialOperationType.PENALTY);
		assertThat(classifier.classify(input(null, null, null, null, null, null, "10.00")))
				.isEqualTo(FinancialOperationType.DEDUCTION);
	}

	@Test
	void classifiesDamageCompensationSeparately() {
		assertThat(classify("Компенсация ущерба", null)).isEqualTo(FinancialOperationType.COMPENSATION);
	}

	@Test
	void returnsUnrecognizedWhenNoRuleMatches() {
		assertThat(classify("Неизвестная операция", null)).isEqualTo(FinancialOperationType.UNRECOGNIZED);
	}

	private FinancialOperationType classify(String supplierOperationName, String documentType) {
		return classifier.classify(input(supplierOperationName, documentType, null, null, null, null, null));
	}

	private static FinancialOperationClassificationInput input(
			String supplierOperationName,
			String documentType,
			String acquiringAmount,
			String storageAmount,
			String acceptanceAmount,
			String penaltyAmount,
			String deductionAmount) {
		return new FinancialOperationClassificationInput(
				supplierOperationName,
				documentType,
				amount(acquiringAmount),
				amount(storageAmount),
				amount(acceptanceAmount),
				amount(penaltyAmount),
				amount(deductionAmount));
	}

	private static BigDecimal amount(String value) {
		return value == null ? null : new BigDecimal(value);
	}
}
