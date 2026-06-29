package ru.marketplace.finance.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "tax_percent", nullable = false, precision = 7, scale = 4)
	private BigDecimal taxPercent;

	@Column(nullable = false)
	private boolean enabled;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private UserRole role;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected User() {
	}

	public User(String email, String passwordHash, String displayName) {
		this.email = requireText(email, "email");
		this.passwordHash = requireText(passwordHash, "passwordHash");
		this.displayName = requireText(displayName, "displayName");
		this.taxPercent = BigDecimal.ZERO;
		this.enabled = true;
		this.role = UserRole.USER;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public String getEmail() {
		return email;
	}

	public String getPasswordHash() {
		return passwordHash;
	}

	public String getDisplayName() {
		return displayName;
	}

	public BigDecimal getTaxPercent() {
		return taxPercent;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public UserRole getRole() {
		return role;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void changeDisplayName(String displayName) {
		this.displayName = requireText(displayName, "displayName");
		touch();
	}

	public void changeEmail(String email) {
		this.email = requireText(email, "email");
		touch();
	}

	public void changePasswordHash(String passwordHash) {
		this.passwordHash = requireText(passwordHash, "passwordHash");
		touch();
	}

	public void changeTaxPercent(BigDecimal taxPercent) {
		Objects.requireNonNull(taxPercent, "taxPercent must not be null");
		if (taxPercent.compareTo(BigDecimal.ZERO) < 0 || taxPercent.compareTo(BigDecimal.valueOf(100)) > 0) {
			throw new IllegalArgumentException("taxPercent must be between 0 and 100");
		}
		this.taxPercent = taxPercent;
		touch();
	}

	public void enable() {
		this.enabled = true;
		touch();
	}

	public void disable() {
		this.enabled = false;
		touch();
	}

	public void makeAdmin() {
		this.role = UserRole.ADMIN;
		touch();
	}

	public void makeUser() {
		this.role = UserRole.USER;
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
