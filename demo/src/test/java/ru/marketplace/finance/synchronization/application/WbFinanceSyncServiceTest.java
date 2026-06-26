package ru.marketplace.finance.synchronization.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.application.MarketplaceCredentialService;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.domain.ProductCost;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.domain.ClassificationStatus;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.finance.infrastructure.wb.WbApiException;
import ru.marketplace.finance.finance.infrastructure.wb.WbReportDetailClient;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
class WbFinanceSyncServiceTest {

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
	ObjectMapper objectMapper;

	@Autowired
	UserRepository userRepository;

	@Autowired
	ProductCostRepository productCostRepository;

	@Autowired
	RawFinancialOperationRepository rawRepository;

	@Autowired
	DailyFinanceEntryRepository dailyRepository;

	@Autowired
	SyncJobRepository syncJobRepository;

	@Autowired
	MarketplaceCredentialService credentialService;

	@Autowired
	WbFinanceSyncService syncService;

	@MockitoBean
	WbReportDetailClient wbReportDetailClient;

	@Test
	void syncsUsingSavedWildberriesToken() {
		LocalDate businessDate = LocalDate.of(2026, 6, 17);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-saved-token@example.com",
				"$2a$10$hash",
				"Seller"));
		credentialService.saveWildberriesToken(user.getId(), "saved-read-only-token");
		when(wbReportDetailClient.fetchReportDetailByPeriod(
				eq("saved-read-only-token"),
				eq(businessDate),
				eq(businessDate),
				eq("daily"),
				eq(5_000),
				eq(50)))
				.thenReturn(List.of(saleRow()));

		WbFinanceSyncResult result = syncService.syncWithSavedToken(
				user.getId(),
				businessDate,
				businessDate);

		assertThat(result.receivedRows()).isEqualTo(1);
		assertThat(result.insertedRows()).isEqualTo(1);
		assertThat(result.affectedDays()).isEqualTo(1);
		assertThat(syncJobRepository.findById(result.syncJobId()).orElseThrow().getStatus())
				.isEqualTo(SyncStatus.COMPLETED);
	}

	@Test
	void doesNotCreateSyncJobWhenSavedTokenIsMissing() {
		LocalDate businessDate = LocalDate.of(2026, 6, 18);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-without-token@example.com",
				"$2a$10$hash",
				"Seller"));

		assertThatThrownBy(() -> syncService.syncWithSavedToken(user.getId(), businessDate, businessDate))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Active Wildberries credential not found");

		assertThat(syncJobRepository.findByUserIdOrderByRequestedAtDesc(user.getId())).isEmpty();
		verifyNoInteractions(wbReportDetailClient);
	}

	@Test
	void syncsWbReportRowsIntoRawOperationsAndDailyFinanceEntries() {
		LocalDate businessDate = LocalDate.of(2026, 6, 15);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-success@example.com",
				"$2a$10$hash",
				"Seller"));
		productCostRepository.saveAndFlush(new ProductCost(
				user.getId(),
				123456789L,
				"Test product",
				LocalDate.of(2026, 1, 1),
				new BigDecimal("100.00")));
		when(wbReportDetailClient.fetchReportDetailByPeriod(
				eq("read-only-token"),
				eq(businessDate),
				eq(businessDate),
				eq("daily"),
				eq(5_000),
				eq(50)))
				.thenReturn(List.of(saleRow(), unrecognizedRow()));

		WbFinanceSyncResult result = syncService.sync(
				user.getId(),
				"read-only-token",
				businessDate,
				businessDate);

		assertThat(result.receivedRows()).isEqualTo(2);
		assertThat(result.insertedRows()).isEqualTo(2);
		assertThat(result.duplicateRows()).isZero();
		assertThat(result.affectedDays()).isEqualTo(1);
		assertThat(result.savedDailyRows()).isEqualTo(2);
		assertThat(result.unrecognizedRows()).isEqualTo(1);

		SyncJob syncJob = syncJobRepository.findById(result.syncJobId()).orElseThrow();
		assertThat(syncJob.getStatus()).isEqualTo(SyncStatus.COMPLETED);
		assertThat(syncJob.getReceivedRows()).isEqualTo(2);
		assertThat(syncJob.getInsertedRows()).isEqualTo(2);
		assertThat(syncJob.getAffectedDays()).isEqualTo(1);
		assertThat(syncJob.getUnrecognizedRows()).isEqualTo(1);
		assertThat(syncJob.getFinishedAt()).isNotNull();

		assertThat(rawRepository.findByUserIdAndBusinessDate(user.getId(), businessDate))
				.hasSize(2)
				.extracting(operation -> operation.getClassificationStatus())
				.containsExactlyInAnyOrder(ClassificationStatus.RECOGNIZED, ClassificationStatus.UNRECOGNIZED);

		DailyFinanceEntry productRow = dailyRepository
				.findByUserIdAndBusinessDateAndNmId(user.getId(), businessDate, 123456789L)
				.orElseThrow();
		assertThat(productRow.getNetRevenueAmount()).isEqualByComparingTo("1000.00");
		assertThat(productRow.getCommissionAmount()).isEqualByComparingTo("180.00");
		assertThat(productRow.getCostAmount()).isEqualByComparingTo("100.00");
		assertThat(productRow.getProductProfitAmount()).isEqualByComparingTo("720.00");
	}

	@Test
	void marksSyncJobAsFailedWhenWbClientFails() {
		LocalDate businessDate = LocalDate.of(2026, 6, 16);
		User user = userRepository.saveAndFlush(new User(
				"seller-sync-fail@example.com",
				"$2a$10$hash",
				"Seller"));
		when(wbReportDetailClient.fetchReportDetailByPeriod(
				eq("bad-token"),
				eq(businessDate),
				eq(businessDate),
				eq("daily"),
				eq(5_000),
				eq(50)))
				.thenThrow(new WbApiException("WB is unavailable"));

		assertThatThrownBy(() -> syncService.sync(user.getId(), "bad-token", businessDate, businessDate))
				.isInstanceOf(WbApiException.class)
				.hasMessageContaining("WB is unavailable");

		List<SyncJob> userJobs = syncJobRepository.findByUserIdOrderByRequestedAtDesc(user.getId());
		assertThat(userJobs)
				.singleElement()
				.satisfies(syncJob -> {
					assertThat(syncJob.getStatus()).isEqualTo(SyncStatus.FAILED);
					assertThat(syncJob.getErrorCode()).isEqualTo("WbApiException");
					assertThat(syncJob.getErrorMessage()).contains("WB is unavailable");
					assertThat(syncJob.getFinishedAt()).isNotNull();
				});
	}

	private JsonNode saleRow() {
		ObjectNode row = objectMapper.createObjectNode();
		row.put("rrd_id", 1);
		row.put("srid", "srid-1");
		row.put("nm_id", 123456789);
		row.put("supplier_oper_name", "\u041f\u0440\u043e\u0434\u0430\u0436\u0430");
		row.put("doc_type_name", "\u041f\u0440\u043e\u0434\u0430\u0436\u0430");
		row.put("rr_dt", "2026-06-15T12:00:00");
		row.put("quantity", 1);
		row.put("retail_amount", "1000.00");
		row.put("ppvz_for_pay", "800.00");
		row.put("acquiring_fee", "20.00");
		return row;
	}

	private JsonNode unrecognizedRow() {
		ObjectNode row = objectMapper.createObjectNode();
		row.put("rrd_id", 2);
		row.put("srid", "srid-2");
		row.put("supplier_oper_name", "Unknown WB operation");
		row.put("rr_dt", "2026-06-15T13:00:00");
		return row;
	}
}
