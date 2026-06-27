package ru.marketplace.finance.account.application;

import java.math.BigDecimal;
import ru.marketplace.finance.account.domain.User;

public record UserSettingsView(
		Long userId,
		BigDecimal taxPercent) {

	public static UserSettingsView from(User user) {
		return new UserSettingsView(user.getId(), user.getTaxPercent());
	}
}
