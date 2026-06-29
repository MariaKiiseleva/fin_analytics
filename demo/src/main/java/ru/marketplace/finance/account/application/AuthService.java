package ru.marketplace.finance.account.application;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public AuthUserView register(String email, String displayName, String password) {
		if (userRepository.existsByEmail(email)) {
			throw new IllegalArgumentException("Email already exists: " + email);
		}
		User user = new User(email, passwordEncoder.encode(password), displayName);
		return AuthUserView.from(userRepository.save(user));
	}

	@Transactional(readOnly = true)
	public AuthUserView login(String email, String password) {
		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));
		if (!user.isEnabled() || !passwordEncoder.matches(password, user.getPasswordHash())) {
			throw new IllegalArgumentException("Invalid email or password");
		}
		return AuthUserView.from(user);
	}

	@Transactional(readOnly = true)
	public AuthUserView findCurrentUser(Long userId) {
		if (userId == null) {
			return null;
		}
		return userRepository.findById(userId)
				.filter(User::isEnabled)
				.map(AuthUserView::from)
				.orElse(null);
	}
}
