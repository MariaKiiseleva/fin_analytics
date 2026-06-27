package ru.marketplace.finance.finance.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
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
@AutoConfigureMockMvc
class DailyFinanceRecalculationControllerTest {

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
	ProductCostRepository productCostRepository;

	@Autowired
	SyncJobRepository syncJobRepository;

	@Autowired
	RawFinancialOperationRepository rawRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Test
	void recalculatesDailyFinanceEntriesFromSavedRawOperations() throws Exception {
		LocalDate businessDate = LocalDate.of(2026, 6, 21);
		User user = userRepository.saveAndFlush(new User(
				"seller-recalculate-controller@example.com",
				"$2a$10$hash",
				"Seller"));
		productCostRepository.saveAndFlush(new ProductCost(
				user.getId(),
				123456789L,
				"Test product",
				LocalDate.of(2026, 1, 1),
				new BigDecimal("100.00")));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(user.getId(), businessDate, businessDate));
		rawRepository.saveAndFlush(sale(user.getId(), syncJob.getId(), businessDate));

		mockMvc.perform(post("/api/reports/daily/recalculate")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "dateFrom": "2026-06-21",
								  "dateTo": "2026-06-21"
								}
								""".formatted(user.getId())))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.rawRows").value(1))
				.andExpect(jsonPath("$.affectedDays").value(1))
				.andExpect(jsonPath("$.savedDailyRows").value(2))
				.andExpect(jsonPath("$.unrecognizedRows").value(0));

		DailyFinanceEntry productRow = dailyRepository
				.findByUserIdAndBusinessDateAndNmId(user.getId(), businessDate, 123456789L)
				.orElseThrow();
		org.assertj.core.api.Assertions.assertThat(productRow.getCostAmount()).isEqualByComparingTo("100.00");
		org.assertj.core.api.Assertions.assertThat(productRow.getProductProfitAmount()).isEqualByComparingTo("700.00");
	}

	private static RawFinancialOperation sale(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				"0000000000000000000000000000000000000000000000000000000000000200",
				businessDate,
				"{\"test\":\"sale\"}");
		operation.setOperationIdentity("sale-1", "srid-recalculate-1", 123456789L);
		operation.setOperationNames("\u041f\u0440\u043e\u0434\u0430\u0436\u0430", "\u041f\u0440\u043e\u0434\u0430\u0436\u0430");
		operation.setAmounts(
				1,
				new BigDecimal("1000.00"),
				null,
				new BigDecimal("800.00"),
				null,
				null,
				null,
				null,
				new BigDecimal("20.00"),
				null,
				null,
				null,
				null);
		return operation;
	}
}
