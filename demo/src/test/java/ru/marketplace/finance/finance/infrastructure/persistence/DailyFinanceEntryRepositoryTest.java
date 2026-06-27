package ru.marketplace.finance.finance.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;

@Testcontainers
@DataJpaTest
class DailyFinanceEntryRepositoryTest {

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
	DailyFinanceEntryRepository dailyRepository;

	@Test
	void savesProductAndCommonRowsForSameDate() {
		User user = userRepository.saveAndFlush(new User("seller@example.com", "$2a$10$hash", "Seller"));
		LocalDate businessDate = LocalDate.of(2026, 6, 15);
		DailyFinanceEntry productRow = DailyFinanceEntry.productRow(
				user.getId(),
				businessDate,
				123456789L,
				"Test product",
				1);
		productRow.replaceProductTotals(
				3,
				1,
				new BigDecimal("3000.00"),
				new BigDecimal("1000.00"),
				new BigDecimal("250.00"),
				new BigDecimal("180.00"),
				new BigDecimal("45.00"),
				new BigDecimal("900.00"),
				new BigDecimal("120.00"),
				new BigDecimal("550.00"),
				true);
		DailyFinanceEntry commonRow = DailyFinanceEntry.commonRow(user.getId(), businessDate, 1);
		commonRow.replaceCommonExpenses(
				new BigDecimal("45.00"),
				new BigDecimal("30.00"),
				new BigDecimal("0.00"),
				new BigDecimal("0.00"),
				new BigDecimal("15.00"));

		dailyRepository.saveAndFlush(productRow);
		dailyRepository.saveAndFlush(commonRow);

		assertThat(dailyRepository.findByUserIdAndBusinessDate(user.getId(), businessDate)).hasSize(2);
		assertThat(dailyRepository.findByUserIdAndBusinessDateAndNmId(
						user.getId(),
						businessDate,
						123456789L))
				.get()
				.satisfies(found -> {
					assertThat(found.getNetQuantity()).isEqualTo(2);
					assertThat(found.getNetRevenueAmount()).isEqualByComparingTo("2000.00");
					assertThat(found.getProductProfitAmount()).isEqualByComparingTo("550.00");
				});
		assertThat(dailyRepository.findByUserIdAndBusinessDateAndNmIdIsNull(user.getId(), businessDate))
				.get()
				.satisfies(found -> {
					assertThat(found.getNmId()).isNull();
					assertThat(found.getCalculationVersion()).isEqualTo(1);
				});
	}
}
