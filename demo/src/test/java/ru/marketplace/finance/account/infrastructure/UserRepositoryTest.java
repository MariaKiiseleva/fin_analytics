package ru.marketplace.finance.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.domain.UserRole;

@Testcontainers
@DataJpaTest
class UserRepositoryTest {

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

	@Test
	void savesAndFindsUserByEmail() {
		User user = new User("seller@example.com", "$2a$10$hash", "Seller");
		user.changeTaxPercent(new BigDecimal("7.5000"));

		User saved = userRepository.saveAndFlush(user);

		assertThat(saved.getId()).isNotNull();
		assertThat(userRepository.existsByEmail("seller@example.com")).isTrue();
		assertThat(userRepository.findByEmail("seller@example.com"))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getDisplayName()).isEqualTo("Seller");
					assertThat(found.getTaxPercent()).isEqualByComparingTo("7.5000");
					assertThat(found.getRole()).isEqualTo(UserRole.USER);
					assertThat(found.isEnabled()).isTrue();
				});
	}
}
