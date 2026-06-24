package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.domain.ProductCost;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
class DailyFinanceRecalculationServiceTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
	}

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProductCostRepository productCostRepository;

	@Autowired
	SyncJobRepository syncJobRepository;

	@Autowired
	RawFinancialOperationRepository rawRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Autowired
	DailyFinanceRecalculationService recalculationService;

	@Test
	void recalculatesRawOperationsIntoDailyFinanceEntries() {
		User user = new User("seller@example.com", "$2a$10$hash", "Seller");
		user.changeTaxPercent(new BigDecimal("10.0000"));
		user = userRepository.saveAndFlush(user);
		productCostRepository.saveAndFlush(new ProductCost(
				user.getId(),
				123456789L,
				"Test product",
				LocalDate.of(2026, 1, 1),
				new BigDecimal("100.00")));
		productCostRepository.saveAndFlush(new ProductCost(
				user.getId(),
				222222222L,
				"Second product",
				LocalDate.of(2026, 1, 1),
				new BigDecimal("50.00")));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(
				user.getId(),
				LocalDate.of(2026, 6, 15),
				LocalDate.of(2026, 6, 15)));
		LocalDate businessDate = LocalDate.of(2026, 6, 15);
		rawRepository.save(sale(user.getId(), syncJob.getId(), businessDate));
		rawRepository.save(returnOperation(user.getId(), syncJob.getId(), businessDate));
		rawRepository.save(secondProductSale(user.getId(), syncJob.getId(), businessDate));
		rawRepository.save(orderLogistics(user.getId(), syncJob.getId(), businessDate));
		rawRepository.save(deduction(user.getId(), syncJob.getId(), businessDate));
		rawRepository.flush();

		DailyFinanceRecalculationResult result = recalculationService.recalculate(
				user.getId(),
				businessDate,
				businessDate);

		assertThat(result.rawRows()).isEqualTo(5);
		assertThat(result.affectedDays()).isEqualTo(1);
		assertThat(result.savedDailyRows()).isEqualTo(3);
		DailyFinanceEntry productRow = dailyRepository
				.findByUserIdAndBusinessDateAndNmId(user.getId(), businessDate, 123456789L)
				.orElseThrow();
		assertThat(productRow.getSalesQuantity()).isEqualTo(2);
		assertThat(productRow.getReturnQuantity()).isEqualTo(1);
		assertThat(productRow.getNetQuantity()).isEqualTo(1);
		assertThat(productRow.getNetRevenueAmount()).isEqualByComparingTo("600.00");
		assertThat(productRow.getCommissionAmount()).isEqualByComparingTo("135.00");
		assertThat(productRow.getLogisticsAmount()).isEqualByComparingTo("90.00");
		assertThat(productRow.getCostAmount()).isEqualByComparingTo("100.00");
		assertThat(productRow.getTaxAmount()).isEqualByComparingTo("60.00");
		assertThat(productRow.getProductProfitAmount()).isEqualByComparingTo("215.00");
		assertThat(productRow.getHasCost()).isTrue();
		DailyFinanceEntry secondProductRow = dailyRepository
				.findByUserIdAndBusinessDateAndNmId(user.getId(), businessDate, 222222222L)
				.orElseThrow();
		assertThat(secondProductRow.getNetRevenueAmount()).isEqualByComparingTo("500.00");
		assertThat(secondProductRow.getCommissionAmount()).isEqualByComparingTo("90.00");
		assertThat(secondProductRow.getLogisticsAmount()).isEqualByComparingTo("0.00");
		assertThat(secondProductRow.getProductProfitAmount()).isEqualByComparingTo("310.00");

		DailyFinanceEntry commonRow = dailyRepository
				.findByUserIdAndBusinessDateAndNmIdIsNull(user.getId(), businessDate)
				.orElseThrow();
		assertThat(commonRow.getAcquiringAmount()).isEqualByComparingTo("45.00");
		assertThat(commonRow.getAdditionalDeductionsAmount()).isEqualByComparingTo("20.00");
	}

	private static RawFinancialOperation sale(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = raw(userId, syncJobId, businessDate, "sale");
		operation.setOperationIdentity("sale-1", "srid-1", 123456789L);
		operation.setOperationNames("Продажа", "Продажа");
		operation.setAmounts(
				2,
				new BigDecimal("1000.00"),
				null,
				new BigDecimal("750.00"),
				null,
				null,
				null,
				null,
				new BigDecimal("25.00"),
				null,
				null,
				null,
				null);
		return operation;
	}

	private static RawFinancialOperation returnOperation(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = raw(userId, syncJobId, businessDate, "return");
		operation.setOperationIdentity("return-1", "srid-1", 123456789L);
		operation.setOperationNames("Возврат", "Возврат");
		operation.setAmounts(
				1,
				new BigDecimal("400.00"),
				null,
				new BigDecimal("300.00"),
				null,
				null,
				null,
				null,
				new BigDecimal("10.00"),
				null,
				null,
				null,
				null);
		return operation;
	}

	private static RawFinancialOperation secondProductSale(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = raw(userId, syncJobId, businessDate, "second-sale");
		operation.setOperationIdentity("sale-2", "srid-2", 222222222L);
		operation.setOperationNames("Продажа", "Продажа");
		operation.setAmounts(
				1,
				new BigDecimal("500.00"),
				null,
				new BigDecimal("400.00"),
				null,
				null,
				null,
				null,
				new BigDecimal("10.00"),
				null,
				null,
				null,
				null);
		return operation;
	}

	private static RawFinancialOperation orderLogistics(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = raw(userId, syncJobId, businessDate, "logistics");
		operation.setOperationIdentity("logistics-1", "srid-1", null);
		operation.setOperationNames("Логистика", null);
		operation.setAmounts(
				null,
				null,
				null,
				null,
				null,
				new BigDecimal("70.00"),
				new BigDecimal("20.00"),
				null,
				null,
				null,
				null,
				null,
				null);
		return operation;
	}

	private static RawFinancialOperation deduction(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = raw(userId, syncJobId, businessDate, "deduction");
		operation.setOperationNames("Удержание", null);
		operation.setAmounts(
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				null,
				new BigDecimal("20.00"));
		return operation;
	}

	private static RawFinancialOperation raw(
			Long userId,
			Long syncJobId,
			LocalDate businessDate,
			String suffix) {
		return new RawFinancialOperation(
				userId,
				syncJobId,
				"00000000000000000000000000000000000000000000000000000000" + hashSuffix(suffix),
				businessDate,
				"{\"test\":\"" + suffix + "\"}");
	}

	private static String hashSuffix(String value) {
		return switch (value) {
			case "sale" -> "0001";
			case "return" -> "0002";
			case "second-sale" -> "0003";
			case "logistics" -> "0004";
			case "deduction" -> "0005";
			default -> "9999";
		};
	}
}
