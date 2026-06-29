package ru.marketplace.finance.account.presentation;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.MarketplaceCredentialService;
import ru.marketplace.finance.account.application.MarketplaceCredentialView;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/credentials")
public class MarketplaceCredentialController {

	private final MarketplaceCredentialService credentialService;
	private final CurrentUserService currentUserService;

	public MarketplaceCredentialController(
			MarketplaceCredentialService credentialService,
			CurrentUserService currentUserService) {
		this.credentialService = credentialService;
		this.currentUserService = currentUserService;
	}

	@PostMapping("/wildberries")
	@ResponseStatus(HttpStatus.CREATED)
	public MarketplaceCredentialView saveWildberriesToken(
			@Valid @RequestBody SaveWildberriesTokenRequest request,
			HttpSession session) {
		currentUserService.requireSameUser(session, request.userId());
		return credentialService.saveWildberriesToken(request.userId(), request.token());
	}
}
