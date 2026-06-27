package ru.marketplace.finance.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LocalSecurityConfigTest {

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@DynamicPropertySource
	static void configurePostgres(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
		registry.add("spring.flyway.enabled", () -> "true");
		registry.add("app.security.token-encryption-key", () -> "local-security-test-key");
	}

	@Autowired
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Test
	void allowsApiPostRequestsWithoutAuthenticationAndCsrfInLocalProfile() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-local-security@example.com",
				"$2a$10$hash",
				"Seller"));

		mockMvc.perform(post("/api/credentials/wildberries")
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "token": "local-token-1234"
								}
								""".formatted(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(user.getId()))
				.andExpect(jsonPath("$.tokenMask").value("loca...1234"));
	}
}
