package ru.marketplace.finance.account.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.account.application.MarketplaceCredentialService;
import ru.marketplace.finance.account.application.MarketplaceCredentialView;

@RestController
@RequestMapping("/api/credentials")
public class MarketplaceCredentialController {

	private final MarketplaceCredentialService credentialService;

	public MarketplaceCredentialController(MarketplaceCredentialService credentialService) {
		this.credentialService = credentialService;
	}

	@PostMapping("/wildberries")
	@ResponseStatus(HttpStatus.CREATED)
	public MarketplaceCredentialView saveWildberriesToken(@Valid @RequestBody SaveWildberriesTokenRequest request) {
		return credentialService.saveWildberriesToken(request.userId(), request.token());
	}
}
