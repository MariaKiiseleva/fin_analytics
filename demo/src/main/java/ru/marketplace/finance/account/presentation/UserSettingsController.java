package ru.marketplace.finance.account.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.UserSettingsService;
import ru.marketplace.finance.account.application.UserSettingsView;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/users")
public class UserSettingsController {

	private final UserSettingsService userSettingsService;
	private final CurrentUserService currentUserService;

	public UserSettingsController(UserSettingsService userSettingsService, CurrentUserService currentUserService) {
		this.userSettingsService = userSettingsService;
		this.currentUserService = currentUserService;
	}

	@GetMapping("/{userId}/settings")
	public UserSettingsView getSettings(@PathVariable @Positive Long userId, HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		return userSettingsService.getSettings(userId);
	}

	@PatchMapping("/{userId}/account")
	public UserSettingsView updateAccount(
			@PathVariable @Positive Long userId,
			@Valid @RequestBody UpdateAccountRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		return userSettingsService.changeAccount(userId, request.displayName(), request.email());
	}

	@PatchMapping("/{userId}/tax-percent")
	public UserSettingsView updateTaxPercent(
			@PathVariable @Positive Long userId,
			@Valid @RequestBody UpdateTaxPercentRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		return userSettingsService.changeTaxPercent(userId, request.taxPercent());
	}

	@PatchMapping("/{userId}/password")
	public void updatePassword(
			@PathVariable @Positive Long userId,
			@Valid @RequestBody UpdatePasswordRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		userSettingsService.changePassword(userId, request.newPassword());
	}

	@DeleteMapping("/{userId}")
	public void deleteAccount(@PathVariable @Positive Long userId, HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		userSettingsService.deleteAccount(userId);
	}
}
