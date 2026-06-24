package ru.marketplace.finance.account.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "marketplace_credentials")
public class MarketplaceCredential {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, unique = true)
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 30)
	private MarketplaceProvider provider;

	@Column(name = "encrypted_token", nullable = false)
	private String encryptedToken;

	@Column(name = "token_mask", length = 30)
	private String tokenMask;

	@Column(nullable = false)
	private boolean active;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected MarketplaceCredential() {
	}

	public MarketplaceCredential(Long userId, MarketplaceProvider provider, String encryptedToken, String tokenMask) {
		if (userId == null) {
			throw new IllegalArgumentException("userId must not be null");
		}
		if (provider == null) {
			throw new IllegalArgumentException("provider must not be null");
		}
		this.userId = userId;
		this.provider = provider;
		this.encryptedToken = requireText(encryptedToken, "encryptedToken");
		this.tokenMask = tokenMask;
		this.active = true;
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

	public MarketplaceProvider getProvider() {
		return provider;
	}

	public String getEncryptedToken() {
		return encryptedToken;
	}

	public String getTokenMask() {
		return tokenMask;
	}

	public boolean isActive() {
		return active;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void replaceToken(String encryptedToken, String tokenMask) {
		this.encryptedToken = requireText(encryptedToken, "encryptedToken");
		this.tokenMask = tokenMask;
		touch();
	}

	public void activate() {
		this.active = true;
		touch();
	}

	public void deactivate() {
		this.active = false;
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
