package ru.marketplace.finance.account.application;

import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.MarketplaceCredential;
import ru.marketplace.finance.account.domain.MarketplaceProvider;
import ru.marketplace.finance.account.infrastructure.MarketplaceCredentialRepository;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Service
public class MarketplaceCredentialService {

	private static final MarketplaceProvider WB_PROVIDER = MarketplaceProvider.WILDBERRIES;

	private final UserRepository userRepository;
	private final MarketplaceCredentialRepository credentialRepository;
	private final TokenCipher tokenCipher;

	public MarketplaceCredentialService(
			UserRepository userRepository,
			MarketplaceCredentialRepository credentialRepository,
			TokenCipher tokenCipher) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.tokenCipher = tokenCipher;
	}

	@Transactional
	public MarketplaceCredentialView saveWildberriesToken(Long userId, String token) {
		requireExistingUser(userId);
		String normalizedToken = requireText(token, "token").trim();
		String encryptedToken = tokenCipher.encrypt(normalizedToken);
		String tokenMask = mask(normalizedToken);

		MarketplaceCredential credential = credentialRepository
				.findByUserIdAndProvider(userId, WB_PROVIDER)
				.map(existing -> {
					existing.replaceToken(encryptedToken, tokenMask);
					existing.activate();
					return existing;
				})
				.orElseGet(() -> new MarketplaceCredential(userId, WB_PROVIDER, encryptedToken, tokenMask));

		return MarketplaceCredentialView.from(credentialRepository.saveAndFlush(credential));
	}

	@Transactional(readOnly = true)
	public MarketplaceCredentialView getWildberriesCredential(Long userId) {
		requireExistingUser(userId);
		return credentialRepository.findByUserIdAndProvider(userId, WB_PROVIDER)
				.map(MarketplaceCredentialView::from)
				.orElseThrow(() -> new IllegalArgumentException("Wildberries credential not found for user: " + userId));
	}

	@Transactional(readOnly = true)
	public String getActiveWildberriesToken(Long userId) {
		requireExistingUser(userId);
		MarketplaceCredential credential = credentialRepository.findByUserIdAndProvider(userId, WB_PROVIDER)
				.filter(MarketplaceCredential::isActive)
				.orElseThrow(() -> new IllegalArgumentException("Active Wildberries credential not found for user: " + userId));
		return tokenCipher.decrypt(credential.getEncryptedToken());
	}

	private void requireExistingUser(Long userId) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		if (!userRepository.existsById(userId)) {
			throw new IllegalArgumentException("User not found: " + userId);
		}
	}

	private static String mask(String token) {
		Objects.requireNonNull(token, "token must not be null");
		if (token.length() <= 8) {
			return "****";
		}
		return token.substring(0, 4) + "..." + token.substring(token.length() - 4);
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
