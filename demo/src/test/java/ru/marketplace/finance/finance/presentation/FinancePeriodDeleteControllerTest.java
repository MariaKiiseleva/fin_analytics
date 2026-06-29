package ru.marketplace.finance.finance.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationService;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class FinancePeriodDeleteControllerTest {

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
	SyncJobRepository syncJobRepository;

	@Autowired
	RawFinancialOperationRepository rawRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Autowired
	DailyFinanceRecalculationService recalculationService;

	@Test
	void deletesRawAndDailyRowsForPeriod() throws Exception {
		LocalDate businessDate = LocalDate.of(2026, 6, 21);
		User user = userRepository.saveAndFlush(new User(
				"seller-delete-period-controller@example.com",
				"$2a$10$hash",
				"Seller"));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(user.getId(), businessDate, businessDate));
		rawRepository.saveAndFlush(sale(user.getId(), syncJob.getId(), businessDate));
		recalculationService.recalculate(user.getId(), businessDate, businessDate);

		mockMvc.perform(delete("/api/reports/period")
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
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.deletedRawRows").value(1))
				.andExpect(jsonPath("$.deletedDailyRows").value(2));

		assertThat(rawRepository.findByUserIdAndBusinessDate(user.getId(), businessDate)).isEmpty();
		assertThat(dailyRepository.findByUserIdAndBusinessDate(user.getId(), businessDate)).isEmpty();
	}

	private static RawFinancialOperation sale(Long userId, Long syncJobId, LocalDate businessDate) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				"0000000000000000000000000000000000000000000000000000000000000300",
				businessDate,
				"{\"test\":\"sale\"}");
		operation.setOperationIdentity("sale-delete-1", "srid-delete-period-1", 123456789L);
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
