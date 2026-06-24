package ru.marketplace.finance.cost.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
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
import ru.marketplace.finance.cost.domain.ProductCost;

@Testcontainers
@DataJpaTest
class ProductCostRepositoryTest {

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

	@Test
	void findsLatestCostThatIsValidForBusinessDate() {
		User user = userRepository.saveAndFlush(new User("seller@example.com", "$2a$10$hash", "Seller"));
		productCostRepository.save(new ProductCost(
				user.getId(),
				123456L,
				"Product",
				LocalDate.of(2026, 1, 1),
				new BigDecimal("100.00")));
		productCostRepository.save(new ProductCost(
				user.getId(),
				123456L,
				"Product",
				LocalDate.of(2026, 3, 1),
				new BigDecimal("120.00")));
		productCostRepository.flush();

		assertThat(productCostRepository.findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
				user.getId(),
				123456L,
				LocalDate.of(2026, 2, 10)))
				.isPresent()
				.get()
				.extracting(ProductCost::getCostAmount)
				.isEqualTo(new BigDecimal("100.00"));

		assertThat(productCostRepository.findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
				user.getId(),
				123456L,
				LocalDate.of(2026, 3, 15)))
				.isPresent()
				.get()
				.extracting(ProductCost::getCostAmount)
				.isEqualTo(new BigDecimal("120.00"));
	}
}
