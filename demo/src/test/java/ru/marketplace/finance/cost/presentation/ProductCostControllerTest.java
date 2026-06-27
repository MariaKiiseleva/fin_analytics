package ru.marketplace.finance.cost.presentation;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class ProductCostControllerTest {

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
	MockMvc mockMvc;

	@Autowired
	UserRepository userRepository;

	@Test
	void savesProductCostAndReturnsHistory() throws Exception {
		User user = userRepository.saveAndFlush(new User(
				"seller-product-cost-controller@example.com",
				"$2a$10$hash",
				"Seller"));

		mockMvc.perform(post("/api/product-costs")
						.with(user("seller"))
						.with(csrf())
						.contentType(MediaType.APPLICATION_JSON)
						.content("""
								{
								  "userId": %d,
								  "nmId": 125167917,
								  "productName": "Test product",
								  "validFrom": "2026-06-01",
								  "costAmount": 650.00
								}
								""".formatted(user.getId())))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.userId").value(user.getId()))
				.andExpect(jsonPath("$.nmId").value(125167917))
				.andExpect(jsonPath("$.productName").value("Test product"))
				.andExpect(jsonPath("$.validFrom").value("2026-06-01"))
				.andExpect(jsonPath("$.costAmount").value(650.00));

		mockMvc.perform(get("/api/product-costs")
						.with(user("seller"))
						.param("userId", user.getId().toString())
						.param("nmId", "125167917"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].userId").value(user.getId()))
				.andExpect(jsonPath("$[0].nmId").value(125167917))
				.andExpect(jsonPath("$[0].costAmount").value(650.00));
	}
}
