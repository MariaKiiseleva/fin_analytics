package ru.marketplace.finance.account.application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import ru.marketplace.finance.account.domain.UserRole;

public record AdminUserCardView(
		Long userId,
		String email,
		String displayName,
		UserRole role,
		boolean enabled,
		BigDecimal taxPercent,
		Instant createdAt,
		Instant updatedAt,
		boolean hasWildberriesToken,
		long productCostRows,
		long rawRows,
		long dailyRows,
		long syncJobsCount,
		Instant lastSyncAt,
		List<AdminSyncJobView> syncJobs) {
}
