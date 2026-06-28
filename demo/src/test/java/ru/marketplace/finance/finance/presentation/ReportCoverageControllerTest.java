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
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationService;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ReportCoverageControllerTest {

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
	DailyFinanceRecalculationService recalculationService;

	@Test
	void returnsCoverageForEveryDateInPeriod() throws Exception {
		LocalDate coveredDate = LocalDate.of(2026, 6, 21);
		LocalDate rawOnlyDate = LocalDate.of(2026, 6, 22);
		LocalDate emptyDate = LocalDate.of(2026, 6, 23);
		User user = userRepository.saveAndFlush(new User(
				"seller-coverage@example.com",
				"$2a$10$hash",
				"Seller"));

		SyncJob completedJob = syncJobRepository.saveAndFlush(new SyncJob(user.getId(), coveredDate, rawOnlyDate));
		rawRepository.save(sale(user.getId(), completedJob.getId(), coveredDate, "coverage-1", "srid-coverage-1"));
		rawRepository.save(sale(user.getId(), completedJob.getId(), rawOnlyDate, "coverage-2", "srid-coverage-2"));
		rawRepository.flush();
		completedJob.markCompleted();
		syncJobRepository.saveAndFlush(completedJob);

		SyncJob failedJob = syncJobRepository.saveAndFlush(new SyncJob(user.getId(), emptyDate, emptyDate));
		failedJob.markFailed("WbApiException", "WB test failure");
		syncJobRepository.saveAndFlush(failedJob);

		recalculationService.recalculate(user.getId(), coveredDate, coveredDate);

		mockMvc.perform(get("/api/reports/coverage")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("dateFrom", "2026-06-21")
						.param("dateTo", "2026-06-23"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].date").value("2026-06-21"))
				.andExpect(jsonPath("$[0].hasRawData").value(true))
				.andExpect(jsonPath("$[0].hasDailyReport").value(true))
				.andExpect(jsonPath("$[0].rawRows").value(1))
				.andExpect(jsonPath("$[0].dailyRows").value(2))
				.andExpect(jsonPath("$[0].lastSyncStatus").value("COMPLETED"))
				.andExpect(jsonPath("$[1].date").value("2026-06-22"))
				.andExpect(jsonPath("$[1].hasRawData").value(true))
				.andExpect(jsonPath("$[1].hasDailyReport").value(false))
				.andExpect(jsonPath("$[1].rawRows").value(1))
				.andExpect(jsonPath("$[1].dailyRows").value(0))
				.andExpect(jsonPath("$[1].lastSyncStatus").value("COMPLETED"))
				.andExpect(jsonPath("$[2].date").value("2026-06-23"))
				.andExpect(jsonPath("$[2].hasRawData").value(false))
				.andExpect(jsonPath("$[2].hasDailyReport").value(false))
				.andExpect(jsonPath("$[2].rawRows").value(0))
				.andExpect(jsonPath("$[2].dailyRows").value(0))
				.andExpect(jsonPath("$[2].lastSyncStatus").value("FAILED"));
	}

	private static RawFinancialOperation sale(
			Long userId,
			Long syncJobId,
			LocalDate businessDate,
			String operationId,
			String srid) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				operationId,
				businessDate,
				"{\"test\":\"coverage\"}");
		operation.setOperationIdentity(operationId, srid, 123456789L);
		operation.setOperationNames("Продажа", "Продажа");
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
