package ru.marketplace.finance.cost.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.domain.ProductCost;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;

@Service
public class ProductCostService {

	private final UserRepository userRepository;
	private final ProductCostRepository productCostRepository;

	public ProductCostService(UserRepository userRepository, ProductCostRepository productCostRepository) {
		this.userRepository = userRepository;
		this.productCostRepository = productCostRepository;
	}

	@Transactional
	public ProductCostView saveProductCost(
			Long userId,
			Long nmId,
			String productName,
			LocalDate validFrom,
			BigDecimal costAmount) {
		requireExistingUser(userId);
		requirePositive(nmId, "nmId");
		Objects.requireNonNull(validFrom, "validFrom must not be null");
		requireNonNegative(costAmount);

		ProductCost productCost = productCostRepository
				.findByUserIdAndNmIdAndValidFrom(userId, nmId, validFrom)
				.map(existing -> {
					existing.changeProductName(productName);
					existing.changeCostAmount(costAmount);
					return existing;
				})
				.orElseGet(() -> new ProductCost(userId, nmId, productName, validFrom, costAmount));

		return ProductCostView.from(productCostRepository.saveAndFlush(productCost));
	}

	@Transactional(readOnly = true)
	public List<ProductCostView> findProductCosts(Long userId, Long nmId) {
		requireExistingUser(userId);
		requirePositive(nmId, "nmId");
		return productCostRepository.findByUserIdAndNmIdOrderByValidFromDesc(userId, nmId).stream()
				.map(ProductCostView::from)
				.toList();
	}

	private void requireExistingUser(Long userId) {
		requirePositive(userId, "userId");
		if (!userRepository.existsById(userId)) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
	}

	private static void requirePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
	}

	private static void requireNonNegative(BigDecimal value) {
		Objects.requireNonNull(value, "costAmount must not be null");
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("costAmount must not be negative");
		}
	}
}
