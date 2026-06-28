package ru.marketplace.finance.finance.presentation;

import java.time.LocalDate;
import ru.marketplace.finance.synchronization.domain.SyncStatus;

public record ReportCoverageDayView(
		LocalDate date,
		boolean hasRawData,
		boolean hasDailyReport,
		long rawRows,
		long dailyRows,
		SyncStatus lastSyncStatus) {
}
