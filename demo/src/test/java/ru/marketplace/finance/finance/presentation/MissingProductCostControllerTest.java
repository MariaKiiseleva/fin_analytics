package ru.marketplace.finance.finance.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MissingProductCostControllerTest {

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
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Test
	void returnsProductsWithMissingCosts() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-missing-costs@example.com",
				"$2a$10$hash",
				"Seller"));
		dailyRepository.save(missingCostRow(user.getId(), LocalDate.of(2026, 6, 21), 111111111L, "First product", 1));
		dailyRepository.save(missingCostRow(user.getId(), LocalDate.of(2026, 6, 22), 111111111L, "First product", 2));
		dailyRepository.save(hasCostRow(user.getId(), LocalDate.of(2026, 6, 21), 222222222L));
		dailyRepository.flush();

		mockMvc.perform(get("/api/reports/missing-product-costs")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("dateFrom", "2026-06-21")
						.param("dateTo", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].nmId").value(111111111))
				.andExpect(jsonPath("$[0].productName").value("First product"))
				.andExpect(jsonPath("$[0].firstBusinessDate").value("2026-06-21"))
				.andExpect(jsonPath("$[0].lastBusinessDate").value("2026-06-22"))
				.andExpect(jsonPath("$[0].rowsCount").value(2))
				.andExpect(jsonPath("$[0].netQuantity").value(3))
				.andExpect(jsonPath("$[1]").doesNotExist());
	}

	private static DailyFinanceEntry missingCostRow(
			Long userId,
			LocalDate businessDate,
			Long nmId,
			String productName,
			int netQuantity) {
		DailyFinanceEntry entry = DailyFinanceEntry.productRow(userId, businessDate, nmId, productName, 1);
		entry.replaceProductTotals(
				netQuantity,
				0,
				new BigDecimal("1000.00"),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal("1000.00"),
				false);
		return entry;
	}

	private static DailyFinanceEntry hasCostRow(Long userId, LocalDate businessDate, Long nmId) {
		DailyFinanceEntry entry = DailyFinanceEntry.productRow(userId, businessDate, nmId, "Second product", 1);
		entry.replaceProductTotals(
				1,
				0,
				new BigDecimal("1000.00"),
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				BigDecimal.ZERO,
				new BigDecimal("100.00"),
				BigDecimal.ZERO,
				new BigDecimal("900.00"),
				true);
		return entry;
	}
}
