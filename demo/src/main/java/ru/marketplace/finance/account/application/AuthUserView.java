package ru.marketplace.finance.account.application;

import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.domain.UserRole;

public record AuthUserView(
		Long userId,
		String email,
		String displayName,
		UserRole role) {

	public static AuthUserView from(User user) {
		return new AuthUserView(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole());
	}
}
