package ru.marketplace.finance.cost.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "product_costs")
public class ProductCost {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "nm_id", nullable = false)
	private Long nmId;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "valid_from", nullable = false)
	private LocalDate validFrom;

	@Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal costAmount;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected ProductCost() {
	}

	public ProductCost(Long userId, Long nmId, String productName, LocalDate validFrom, BigDecimal costAmount) {
		this.userId = requirePositive(userId, "userId");
		this.nmId = requirePositive(nmId, "nmId");
		this.productName = productName;
		this.validFrom = Objects.requireNonNull(validFrom, "validFrom must not be null");
		this.costAmount = requireNonNegative(costAmount);
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getNmId() {
		return nmId;
	}

	public String getProductName() {
		return productName;
	}

	public LocalDate getValidFrom() {
		return validFrom;
	}

	public BigDecimal getCostAmount() {
		return costAmount;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void changeProductName(String productName) {
		this.productName = productName;
		touch();
	}

	public void changeCostAmount(BigDecimal costAmount) {
		this.costAmount = requireNonNegative(costAmount);
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	private static Long requirePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}

	private static BigDecimal requireNonNegative(BigDecimal value) {
		Objects.requireNonNull(value, "costAmount must not be null");
		if (value.compareTo(BigDecimal.ZERO) < 0) {
			throw new IllegalArgumentException("costAmount must not be negative");
		}
		return value;
	}
}
