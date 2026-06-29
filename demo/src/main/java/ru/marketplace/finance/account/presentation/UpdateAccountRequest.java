package ru.marketplace.finance.account.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateAccountRequest(
		@NotBlank String displayName,
		@NotBlank @Email String email) {
}
