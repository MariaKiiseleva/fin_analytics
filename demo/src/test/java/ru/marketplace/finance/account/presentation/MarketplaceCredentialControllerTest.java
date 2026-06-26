package ru.marketplace.finance.account.presentation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.MarketplaceProvider;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.MarketplaceCredentialRepository;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceCredentialControllerTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("app.security.token-encryption-key", () -> "credential-controller-test-key");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Autowired
	MarketplaceCredentialRepository credentialRepository;

	@Test
	void savesWildberriesTokenAndReturnsMaskedCredential() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-credential-controller@example.com",
				"$2a$10$hash",
				"Seller"));

		mockMvc.perform(post("/api/credentials/wildberries")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "token": "abcd1234567890wxyz"
								}
								""".formatted(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(user.getId()))
				.andExpect(jsonPath("$.provider").value("WILDBERRIES"))
				.andExpect(jsonPath("$.tokenMask").value("abcd...wxyz"))
				.andExpect(jsonPath("$.active").value(true))
				.andExpect(jsonPath("$.encryptedToken").doesNotExist());

		var credential = credentialRepository
				.findByUserIdAndProvider(user.getId(), MarketplaceProvider.WILDBERRIES)
				.orElseThrow();
		assertThat(credential.getEncryptedToken()).startsWith("v1:");
		assertThat(credential.getEncryptedToken()).doesNotContain("abcd1234567890wxyz");
	}
}
