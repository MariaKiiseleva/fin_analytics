package ru.marketplace.finance.account.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.MarketplaceCredential;
import ru.marketplace.finance.account.domain.MarketplaceProvider;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.MarketplaceCredentialRepository;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
class MarketplaceCredentialServiceTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("app.security.token-encryption-key", () -> "test-token-encryption-key");
	}

	@Autowired
	UserRepository userRepository;

	@Autowired
	MarketplaceCredentialRepository credentialRepository;

	@Autowired
	MarketplaceCredentialService credentialService;

	@Test
	void savesMaskedEncryptedWildberriesTokenAndCanDecryptItForApiCalls() {
		User user = userRepository.saveAndFlush(new User(
				"seller-credential-create@example.com",
				"$2a$10$hash",
				"Seller"));

		MarketplaceCredentialView view = credentialService.saveWildberriesToken(
				user.getId(),
				"abcd1234567890wxyz");

		assertThat(view.id()).isNotNull();
		assertThat(view.provider()).isEqualTo(MarketplaceProvider.WILDBERRIES);
		assertThat(view.tokenMask()).isEqualTo("abcd...wxyz");
		assertThat(view.active()).isTrue();

		MarketplaceCredential credential = credentialRepository
				.findByUserIdAndProvider(user.getId(), MarketplaceProvider.WILDBERRIES)
				.orElseThrow();
		assertThat(credential.getEncryptedToken()).isNotEqualTo("abcd1234567890wxyz");
		assertThat(credential.getEncryptedToken()).startsWith("v1:");
		assertThat(credentialService.getActiveWildberriesToken(user.getId()))
				.isEqualTo("abcd1234567890wxyz");
	}

	@Test
	void updatesExistingWildberriesTokenInsteadOfCreatingDuplicate() {
		User user = userRepository.saveAndFlush(new User(
				"seller-credential-update@example.com",
				"$2a$10$hash",
				"Seller"));
		MarketplaceCredentialView first = credentialService.saveWildberriesToken(
				user.getId(),
				"first-token-1234");

		MarketplaceCredentialView second = credentialService.saveWildberriesToken(
				user.getId(),
				"second-token-5678");

		assertThat(second.id()).isEqualTo(first.id());
		assertThat(second.tokenMask()).isEqualTo("seco...5678");
		assertThat(credentialRepository.findAll())
				.filteredOn(credential -> credential.getUserId().equals(user.getId()))
				.hasSize(1);
		assertThat(credentialService.getActiveWildberriesToken(user.getId()))
				.isEqualTo("second-token-5678");
	}

	@Test
	void rejectsBlankToken() {
		User user = userRepository.saveAndFlush(new User(
				"seller-credential-invalid@example.com",
				"$2a$10$hash",
				"Seller"));

		assertThatThrownBy(() -> credentialService.saveWildberriesToken(user.getId(), " "))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("token must not be blank");
	}
}
