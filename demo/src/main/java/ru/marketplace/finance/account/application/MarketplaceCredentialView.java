package ru.marketplace.finance.account.application;

import ru.marketplace.finance.account.domain.MarketplaceCredential;
import ru.marketplace.finance.account.domain.MarketplaceProvider;

public record MarketplaceCredentialView(
		Long id,
		Long userId,
		MarketplaceProvider provider,
		String tokenMask,
		boolean active) {

	static MarketplaceCredentialView from(MarketplaceCredential credential) {
		return new MarketplaceCredentialView(
				credential.getId(),
				credential.getUserId(),
				credential.getProvider(),
				credential.getTokenMask(),
				credential.isActive());
	}
}
