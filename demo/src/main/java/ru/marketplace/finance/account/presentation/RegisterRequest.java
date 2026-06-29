package ru.marketplace.finance.account.presentation;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
		@NotBlank @Email String email,
		@NotBlank String displayName,
		@NotBlank @Size(min = 8) String password) {
}
