package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
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
import ru.marketplace.finance.finance.domain.ClassificationStatus;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@SpringBootTest
class RawFinancialOperationImportServiceTest {

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
	SyncJobRepository syncJobRepository;

	@Autowired
	RawFinancialOperationRepository rawRepository;

	@Autowired
	RawFinancialOperationImportService importService;

	@Test
	void importsRawOperationsAndSkipsDuplicates() {
		User user = userRepository.saveAndFlush(new User("seller-import@example.com", "$2a$10$hash", "Seller"));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(
				user.getId(),
				LocalDate.of(2026, 6, 15),
				LocalDate.of(2026, 6, 16)));
		RawFinancialOperationImportRow firstRow = row(
				"operation-1",
				"srid-1",
				123456789L,
				"Продажа",
				"Продажа",
				Instant.parse("2026-06-15T20:30:00Z"),
				null,
				"{\"rrd_id\":1,\"supplier_oper_name\":\"Продажа\"}");
		RawFinancialOperationImportRow duplicateRow = row(
				"operation-1",
				"srid-1",
				123456789L,
				"Продажа",
				"Продажа",
				Instant.parse("2026-06-15T20:30:00Z"),
				null,
				"{\"rrd_id\":1,\"supplier_oper_name\":\"Продажа\"}");
		RawFinancialOperationImportRow secondRow = row(
				"operation-2",
				"srid-2",
				222222222L,
				"Логистика",
				null,
				null,
				Instant.parse("2026-06-15T21:30:00Z"),
				"{\"rrd_id\":2,\"supplier_oper_name\":\"Логистика\"}");

		RawFinancialOperationImportResult result = importService.importRows(
				user.getId(),
				syncJob.getId(),
				List.of(firstRow, duplicateRow, secondRow));

		assertThat(result.receivedRows()).isEqualTo(3);
		assertThat(result.insertedRows()).isEqualTo(2);
		assertThat(result.duplicateRows()).isEqualTo(1);
		assertThat(rawRepository.findByUserIdAndBusinessDate(user.getId(), LocalDate.of(2026, 6, 15)))
				.hasSize(1)
				.singleElement()
				.satisfies(operation -> {
					assertThat(operation.getSupplierOperationName()).isEqualTo("Продажа");
					assertThat(operation.getClassificationStatus()).isEqualTo(ClassificationStatus.PENDING);
				});
		assertThat(rawRepository.findByUserIdAndBusinessDate(user.getId(), LocalDate.of(2026, 6, 16)))
				.hasSize(1)
				.singleElement()
				.extracting(RawFinancialOperation::getSupplierOperationName)
				.isEqualTo("Логистика");
		SyncJob updatedJob = syncJobRepository.findById(syncJob.getId()).orElseThrow();
		assertThat(updatedJob.getStatus()).isEqualTo(SyncStatus.RAW_SAVED);
		assertThat(updatedJob.getReceivedRows()).isEqualTo(3);
		assertThat(updatedJob.getInsertedRows()).isEqualTo(2);
		assertThat(updatedJob.getDuplicateRows()).isEqualTo(1);
	}

	private static RawFinancialOperationImportRow row(
			String externalOperationId,
			String srid,
			Long nmId,
			String supplierOperationName,
			String documentType,
			Instant reportAt,
			Instant saleAt,
			String rawPayload) {
		return new RawFinancialOperationImportRow(
				externalOperationId,
				srid,
				nmId,
				supplierOperationName,
				documentType,
				null,
				saleAt,
				reportAt,
				null,
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
				null,
				rawPayload);
	}
}
