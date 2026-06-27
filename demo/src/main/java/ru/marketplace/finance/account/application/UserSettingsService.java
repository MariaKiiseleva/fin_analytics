package ru.marketplace.finance.account.application;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Service
public class UserSettingsService {

	private final UserRepository userRepository;

	public UserSettingsService(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Transactional
	public UserSettingsView changeTaxPercent(Long userId, BigDecimal taxPercent) {
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		user.changeTaxPercent(taxPercent);
		return UserSettingsView.from(user);
	}
}
