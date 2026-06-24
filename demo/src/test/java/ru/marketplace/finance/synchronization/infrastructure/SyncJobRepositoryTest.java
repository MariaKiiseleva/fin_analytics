package ru.marketplace.finance.synchronization.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
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
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;

@Testcontainers
@DataJpaTest
class SyncJobRepositoryTest {

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

	@Test
	void savesJobAndFindsActiveStatus() {
		User user = userRepository.saveAndFlush(new User("seller@example.com", "$2a$10$hash", "Seller"));
		SyncJob job = new SyncJob(user.getId(), LocalDate.of(2026, 6, 1), LocalDate.of(2026, 6, 30));
		job.markRunning();

		SyncJob saved = syncJobRepository.saveAndFlush(job);

		assertThat(saved.getId()).isNotNull();
		assertThat(syncJobRepository.existsByUserIdAndStatusIn(
				user.getId(),
				List.of(SyncStatus.CREATED, SyncStatus.RUNNING, SyncStatus.RAW_SAVED, SyncStatus.DAILY_RECALCULATED)))
				.isTrue();

		saved.markRawSaved(100, 90, 5, 5, 2);
		saved.markDailyRecalculated(30);
		saved.markCompleted();
		syncJobRepository.saveAndFlush(saved);

		assertThat(syncJobRepository.findByUserIdOrderByRequestedAtDesc(user.getId()))
				.singleElement()
				.satisfies(found -> {
					assertThat(found.getStatus()).isEqualTo(SyncStatus.COMPLETED);
					assertThat(found.getReceivedRows()).isEqualTo(100);
					assertThat(found.getAffectedDays()).isEqualTo(30);
					assertThat(found.getFinishedAt()).isNotNull();
				});
	}
}
