package ru.marketplace.finance.synchronization.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.application.MarketplaceCredentialService;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.finance.infrastructure.wb.WbReportDetailClient;
import ru.marketplace.finance.synchronization.domain.SyncStatus;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class WildberriesSyncControllerTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("app.security.token-encryption-key", () -> "sync-controller-test-key");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	MarketplaceCredentialService credentialService;

	@Autowired
	SyncJobRepository syncJobRepository;

	@MockitoBean
	WbReportDetailClient wbReportDetailClient;

	@Test
	void startsWildberriesSyncUsingSavedToken() throws Exception {
		LocalDate businessDate = LocalDate.of(2026, 6, 19);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-controller@example.com",
				"$2a$10$hash",
				"Seller"));
		credentialService.saveWildberriesToken(user.getId(), "saved-controller-token");
		when(wbReportDetailClient.fetchReportDetailByPeriod(
				eq("saved-controller-token"),
				eq(businessDate),
				eq(businessDate),
				eq("daily"),
				eq(5_000),
				eq(50)))
				.thenReturn(List.of(saleRow()));

		mockMvc.perform(post("/api/sync/wildberries")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "dateFrom": "2026-06-19",
								  "dateTo": "2026-06-19"
								}
								""".formatted(user.getId())))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.receivedRows").value(1))
				.andExpect(jsonPath("$.insertedRows").value(1))
				.andExpect(jsonPath("$.affectedDays").value(1));

		assertThat(syncJobRepository.findByUserIdOrderByRequestedAtDesc(user.getId()))
				.singleElement()
				.satisfies(syncJob -> assertThat(syncJob.getStatus()).isEqualTo(SyncStatus.COMPLETED));
	}

	@Test
	void returnsSyncJobsForUser() throws Exception {
		LocalDate businessDate = LocalDate.of(2026, 6, 20);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-history@example.com",
				"$2a$10$hash",
				"Seller"));
		credentialService.saveWildberriesToken(user.getId(), "saved-history-token");
		when(wbReportDetailClient.fetchReportDetailByPeriod(
				eq("saved-history-token"),
				eq(businessDate),
				eq(businessDate),
				eq("daily"),
				eq(5_000),
				eq(50)))
				.thenReturn(List.of(saleRow()));
		mockMvc.perform(post("/api/sync/wildberries")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "dateFrom": "2026-06-20",
								  "dateTo": "2026-06-20"
								}
								""".formatted(user.getId())))
				.andExpect(status().isAccepted());

		mockMvc.perform(get("/api/sync/jobs")
						.with(user("seller"))
						.param("userId", user.getId().toString()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value(user.getId()))
				.andExpect(jsonPath("$[0].dateFrom").value("2026-06-20"))
				.andExpect(jsonPath("$[0].dateTo").value("2026-06-20"))
				.andExpect(jsonPath("$[0].status").value("COMPLETED"))
				.andExpect(jsonPath("$[0].receivedRows").value(1))
				.andExpect(jsonPath("$[0].insertedRows").value(1));
	}

	private ObjectNode saleRow() {
		ObjectNode row = objectMapper.createObjectNode();
		row.put("rrd_id", 1);
		row.put("srid", "srid-controller-1");
		row.put("nm_id", 123456789);
		row.put("supplier_oper_name", "\u041f\u0440\u043e\u0434\u0430\u0436\u0430");
		row.put("doc_type_name", "\u041f\u0440\u043e\u0434\u0430\u0436\u0430");
		row.put("rr_dt", "2026-06-19T12:00:00");
		row.put("quantity", 1);
		row.put("retail_amount", "1000.00");
		row.put("ppvz_for_pay", "800.00");
		row.put("acquiring_fee", "20.00");
		return row;
	}
}
