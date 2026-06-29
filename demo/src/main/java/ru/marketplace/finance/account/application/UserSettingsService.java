package ru.marketplace.finance.account.application;

import java.math.BigDecimal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.MarketplaceCredentialRepository;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Service
public class UserSettingsService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final MarketplaceCredentialRepository credentialRepository;
	private final ProductCostRepository productCostRepository;
	private final DailyFinanceEntryRepository dailyFinanceEntryRepository;
	private final RawFinancialOperationRepository rawFinancialOperationRepository;
	private final SyncJobRepository syncJobRepository;

	public UserSettingsService(
			UserRepository userRepository,
			PasswordEncoder passwordEncoder,
			MarketplaceCredentialRepository credentialRepository,
			ProductCostRepository productCostRepository,
			DailyFinanceEntryRepository dailyFinanceEntryRepository,
			RawFinancialOperationRepository rawFinancialOperationRepository,
			SyncJobRepository syncJobRepository) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.credentialRepository = credentialRepository;
		this.productCostRepository = productCostRepository;
		this.dailyFinanceEntryRepository = dailyFinanceEntryRepository;
		this.rawFinancialOperationRepository = rawFinancialOperationRepository;
		this.syncJobRepository = syncJobRepository;
	}

	@Transactional(readOnly = true)
	public UserSettingsView getSettings(Long userId) {
		User user = findUser(userId);
		return UserSettingsView.from(user);
	}

	@Transactional
	public UserSettingsView changeAccount(Long userId, String displayName, String email) {
		User user = findUser(userId);
		userRepository.findByEmail(email)
				.filter(existing -> !existing.getId().equals(userId))
				.ifPresent(existing -> {
					throw new IllegalArgumentException("Email already exists: " + email);
				});
		user.changeDisplayName(displayName);
		user.changeEmail(email);
		return UserSettingsView.from(user);
	}

	@Transactional
	public UserSettingsView changeTaxPercent(Long userId, BigDecimal taxPercent) {
		User user = findUser(userId);
		user.changeTaxPercent(taxPercent);
		return UserSettingsView.from(user);
	}

	@Transactional
	public void changePassword(Long userId, String newPassword) {
		User user = findUser(userId);
		user.changePasswordHash(passwordEncoder.encode(newPassword));
	}

	@Transactional
	public void deleteAccount(Long userId) {
		User user = findUser(userId);
		dailyFinanceEntryRepository.deleteByUserId(userId);
		rawFinancialOperationRepository.deleteByUserId(userId);
		productCostRepository.deleteByUserId(userId);
		credentialRepository.deleteByUserId(userId);
		syncJobRepository.deleteByUserId(userId);
		userRepository.delete(user);
	}

	private User findUser(Long userId) {
		return userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
	}
}
