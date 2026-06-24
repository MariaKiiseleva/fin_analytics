package ru.marketplace.finance.finance.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

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
import ru.marketplace.finance.finance.domain.ClassificationStatus;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Testcontainers
@DataJpaTest
class RawFinancialOperationRepositoryTest {

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

	@Test
	void savesAndFindsRawOperationByDateAndHash() {
		User user = userRepository.saveAndFlush(new User("seller@example.com", "$2a$10$hash", "Seller"));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(
				user.getId(),
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 30)));
		RawFinancialOperation operation = new RawFinancialOperation(
				user.getId(),
				syncJob.getId(),
				"0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef",
				LocalDate.of(2026, 6, 15),
				"{\"supplier_oper_name\":\"Продажа\"}");
		operation.markRecognized("SALE");

		RawFinancialOperation saved = rawRepository.saveAndFlush(operation);

		assertThat(saved.getId()).isNotNull();
		assertThat(rawRepository.existsByUserIdAndRowHash(user.getId(), operation.getRowHash())).isTrue();
		assertThat(rawRepository.findByUserIdAndBusinessDate(user.getId(), LocalDate.of(2026, 6, 15)))
				.singleElement()
				.satisfies(found -> {
					assertThat(found.getClassificationStatus()).isEqualTo(ClassificationStatus.RECOGNIZED);
					assertThat(found.getClassificationCode()).isEqualTo("SALE");
					assertThat(found.getRawPayload()).contains("Продажа");
				});
	}

	@Test
	void groupsUnrecognizedOperationsByOperationNameAndDocumentType() {
		User user = userRepository.saveAndFlush(new User("seller2@example.com", "$2a$10$hash", "Seller"));
		SyncJob syncJob = syncJobRepository.saveAndFlush(new SyncJob(
				user.getId(),
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 30)));
		rawRepository.save(unrecognized(
				user.getId(),
				syncJob.getId(),
				"0000000000000000000000000000000000000000000000000000000000000001",
				"Новая операция",
				"Документ 1",
				LocalDate.of(2026, 6, 15)));
		rawRepository.save(unrecognized(
				user.getId(),
				syncJob.getId(),
				"0000000000000000000000000000000000000000000000000000000000000002",
				"Новая операция",
				"Документ 1",
				LocalDate.of(2026, 6, 16)));
		rawRepository.save(unrecognized(
				user.getId(),
				syncJob.getId(),
				"0000000000000000000000000000000000000000000000000000000000000003",
				"Другая операция",
				null,
				LocalDate.of(2026, 6, 16)));
		rawRepository.save(recognized(
				user.getId(),
				syncJob.getId(),
				"0000000000000000000000000000000000000000000000000000000000000004",
				LocalDate.of(2026, 6, 16)));
		rawRepository.flush();

		assertThat(rawRepository.findUnrecognizedOperationSummaries(
						user.getId(),
						LocalDate.of(2026, 6, 1),
						LocalDate.of(2026, 6, 30)))
				.extracting(
						UnrecognizedOperationSummary::supplierOperationName,
						UnrecognizedOperationSummary::documentType,
						UnrecognizedOperationSummary::rowsCount)
				.containsExactly(
						org.assertj.core.groups.Tuple.tuple("Новая операция", "Документ 1", 2L),
						org.assertj.core.groups.Tuple.tuple("Другая операция", null, 1L));
	}

	private static RawFinancialOperation recognized(
			Long userId,
			Long syncJobId,
			String rowHash,
			LocalDate businessDate) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				rowHash,
				businessDate,
				"{\"supplier_oper_name\":\"Продажа\"}");
		operation.setOperationNames("Продажа", "Продажа");
		operation.markRecognized("SALE");
		return operation;
	}

	private static RawFinancialOperation unrecognized(
			Long userId,
			Long syncJobId,
			String rowHash,
			String supplierOperationName,
			String documentType,
			LocalDate businessDate) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				rowHash,
				businessDate,
				"{\"supplier_oper_name\":\"" + supplierOperationName + "\"}");
		operation.setOperationNames(supplierOperationName, documentType);
		operation.markUnrecognized();
		return operation;
	}
}
