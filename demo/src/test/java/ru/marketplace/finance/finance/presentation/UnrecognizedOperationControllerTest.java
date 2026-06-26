package ru.marketplace.finance.finance.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class UnrecognizedOperationControllerTest {

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
	void returnsUnrecognizedOperationSummaries() throws Exception {
		LocalDate businessDate = LocalDate.of(2026, 6, 22);
		User user = userRepository.saveAndFlush(new User(
				"seller-unrecognized-report@example.com",
				"$2a$10$hash",
				"Seller"));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(user.getId(), businessDate, businessDate));
		rawRepository.save(unrecognized(user.getId(), syncJob.getId(), businessDate, "unknown-1"));
		rawRepository.save(unrecognized(user.getId(), syncJob.getId(), businessDate, "unknown-2"));
		rawRepository.flush();
		recalculationService.recalculate(user.getId(), businessDate, businessDate);

		mockMvc.perform(get("/api/reports/unrecognized-operations")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("dateFrom", "2026-06-22")
						.param("dateTo", "2026-06-22"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].supplierOperationName").value("Unknown WB Operation"))
				.andExpect(jsonPath("$[0].documentType").value("Unknown Document"))
				.andExpect(jsonPath("$[0].rowsCount").value(2));
	}

	private static RawFinancialOperation unrecognized(
			Long userId,
			Long syncJobId,
			LocalDate businessDate,
			String suffix) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				"0000000000000000000000000000000000000000000000000000000000000" + suffix.charAt(suffix.length() - 1),
				businessDate,
				"{\"test\":\"" + suffix + "\"}");
		operation.setOperationIdentity(suffix, "srid-" + suffix, 123456789L);
		operation.setOperationNames("Unknown WB Operation", "Unknown Document");
		operation.setAmounts(null, null, null, null, null, null, null, null, null, null, null, null, null);
		return operation;
	}
}
