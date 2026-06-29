package ru.marketplace.finance.account.application;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.MarketplaceProvider;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.MarketplaceCredentialRepository;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Service
public class AdminUserService {

	private final UserRepository userRepository;
	private final MarketplaceCredentialRepository credentialRepository;
	private final ProductCostRepository productCostRepository;
	private final RawFinancialOperationRepository rawFinancialOperationRepository;
	private final DailyFinanceEntryRepository dailyFinanceEntryRepository;
	private final SyncJobRepository syncJobRepository;

	public AdminUserService(
			UserRepository userRepository,
			MarketplaceCredentialRepository credentialRepository,
			ProductCostRepository productCostRepository,
			RawFinancialOperationRepository rawFinancialOperationRepository,
			DailyFinanceEntryRepository dailyFinanceEntryRepository,
			SyncJobRepository syncJobRepository) {
		this.userRepository = userRepository;
		this.credentialRepository = credentialRepository;
		this.productCostRepository = productCostRepository;
		this.rawFinancialOperationRepository = rawFinancialOperationRepository;
		this.dailyFinanceEntryRepository = dailyFinanceEntryRepository;
		this.syncJobRepository = syncJobRepository;
	}

	@Transactional(readOnly = true)
	public List<AdminUserCardView> findUsers() {
		return userRepository.findAllByOrderByCreatedAtDesc().stream()
				.map(this::toCard)
				.toList();
	}

	private AdminUserCardView toCard(User user) {
		List<SyncJob> jobs = syncJobRepository.findTop20ByUserIdOrderByRequestedAtDesc(user.getId());
		Instant lastSyncAt = jobs.stream()
				.map(SyncJob::getRequestedAt)
				.max(Comparator.naturalOrder())
				.orElse(null);
		return new AdminUserCardView(
				user.getId(),
				user.getEmail(),
				user.getDisplayName(),
				user.getRole(),
				user.isEnabled(),
				user.getTaxPercent(),
				user.getCreatedAt(),
				user.getUpdatedAt(),
				credentialRepository.existsByUserIdAndProvider(user.getId(), MarketplaceProvider.WILDBERRIES),
				productCostRepository.countByUserId(user.getId()),
				rawFinancialOperationRepository.countByUserId(user.getId()),
				dailyFinanceEntryRepository.countByUserId(user.getId()),
				syncJobRepository.countByUserId(user.getId()),
				lastSyncAt,
				jobs.stream().map(AdminSyncJobView::from).toList());
	}
}
