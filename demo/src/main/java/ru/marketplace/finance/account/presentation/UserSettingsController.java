package ru.marketplace.finance.account.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.UserSettingsService;
import ru.marketplace.finance.account.application.UserSettingsView;

@RestController
@RequestMapping("/api/users")
public class UserSettingsController {

	private final UserSettingsService userSettingsService;

	public UserSettingsController(UserSettingsService userSettingsService) {
		this.userSettingsService = userSettingsService;
	}

	@PatchMapping("/{userId}/tax-percent")
	public UserSettingsView updateTaxPercent(
			@PathVariable @Positive Long userId,
			@Valid @RequestBody UpdateTaxPercentRequest request) {
		return userSettingsService.changeTaxPercent(userId, request.taxPercent());
	}
}
