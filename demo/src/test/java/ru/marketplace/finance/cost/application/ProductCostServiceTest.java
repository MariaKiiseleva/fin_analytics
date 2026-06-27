package ru.marketplace.finance.cost.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDate;
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
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;

@Testcontainers
@SpringBootTest
class ProductCostServiceTest {

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
	ProductCostRepository productCostRepository;

	@Autowired
	ProductCostService productCostService;

	@Test
	void createsAndUpdatesProductCostForSameValidFromDate() {
		User user = userRepository.saveAndFlush(new User("seller-cost@example.com", "$2a$10$hash", "Seller"));
		LocalDate validFrom = LocalDate.of(2026, 6, 1);

		ProductCostView created = productCostService.saveProductCost(
				user.getId(),
				125167917L,
				"Test product",
				validFrom,
				new BigDecimal("650.00"));
		ProductCostView updated = productCostService.saveProductCost(
				user.getId(),
				125167917L,
				"Updated product",
				validFrom,
				new BigDecimal("720.00"));

		assertThat(updated.id()).isEqualTo(created.id());
		assertThat(updated.productName()).isEqualTo("Updated product");
		assertThat(updated.costAmount()).isEqualByComparingTo("720.00");
		assertThat(productCostRepository.findAll()).hasSize(1);
	}

	@Test
	void returnsProductCostsFromNewestValidFromDate() {
		User user = userRepository.saveAndFlush(new User("seller-cost-history@example.com", "$2a$10$hash", "Seller"));
		productCostService.saveProductCost(
				user.getId(),
				125167917L,
				"Test product",
				LocalDate.of(2026, 6, 1),
				new BigDecimal("650.00"));
		productCostService.saveProductCost(
				user.getId(),
				125167917L,
				"Test product",
				LocalDate.of(2026, 7, 1),
				new BigDecimal("720.00"));

		assertThat(productCostService.findProductCosts(user.getId(), 125167917L))
				.extracting(ProductCostView::costAmount)
				.containsExactly(new BigDecimal("720.00"), new BigDecimal("650.00"));
	}

	@Test
	void rejectsNegativeCostAmount() {
		User user = userRepository.saveAndFlush(new User("seller-cost-invalid@example.com", "$2a$10$hash", "Seller"));

		assertThatThrownBy(() -> productCostService.saveProductCost(
				user.getId(),
				125167917L,
				"Test product",
				LocalDate.of(2026, 6, 1),
				new BigDecimal("-1.00")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessage("costAmount must not be negative");
	}
}
