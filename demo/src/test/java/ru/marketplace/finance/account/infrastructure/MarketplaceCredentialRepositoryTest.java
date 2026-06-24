package ru.marketplace.finance.account.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.MarketplaceCredential;
import ru.marketplace.finance.account.domain.MarketplaceProvider;
import ru.marketplace.finance.account.domain.User;

@Testcontainers
@DataJpaTest
class MarketplaceCredentialRepositoryTest {

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
	MarketplaceCredentialRepository credentialRepository;

	@Test
	void savesAndFindsCredentialByUserAndProvider() {
		User user = userRepository.saveAndFlush(new User("seller@example.com", "$2a$10$hash", "Seller"));
		MarketplaceCredential credential = new MarketplaceCredential(
				user.getId(),
				MarketplaceProvider.WILDBERRIES,
				"encrypted-token",
				"abcd...wxyz");

		MarketplaceCredential saved = credentialRepository.saveAndFlush(credential);

		assertThat(saved.getId()).isNotNull();
		assertThat(credentialRepository.existsByUserIdAndProvider(user.getId(), MarketplaceProvider.WILDBERRIES))
				.isTrue();
		assertThat(credentialRepository.findByUserIdAndProvider(user.getId(), MarketplaceProvider.WILDBERRIES))
				.isPresent()
				.get()
				.satisfies(found -> {
					assertThat(found.getEncryptedToken()).isEqualTo("encrypted-token");
					assertThat(found.getTokenMask()).isEqualTo("abcd...wxyz");
					assertThat(found.isActive()).isTrue();
				});
	}
}
