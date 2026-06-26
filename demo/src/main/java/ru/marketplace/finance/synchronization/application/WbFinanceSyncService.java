package ru.marketplace.finance.synchronization.application;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import ru.marketplace.finance.account.application.MarketplaceCredentialService;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationResult;
import ru.marketplace.finance.finance.application.DailyFinanceRecalculationService;
import ru.marketplace.finance.finance.application.RawFinancialOperationImportResult;
import ru.marketplace.finance.finance.application.RawFinancialOperationImportRow;
import ru.marketplace.finance.finance.application.RawFinancialOperationImportService;
import ru.marketplace.finance.finance.infrastructure.wb.WbRealizationReportDetailMapper;
import ru.marketplace.finance.finance.infrastructure.wb.WbReportDetailClient;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Service
public class WbFinanceSyncService {

	private static final String DEFAULT_PERIOD = "daily";
	private static final int DEFAULT_LIMIT = 5_000;
	private static final int DEFAULT_MAX_PAGES = 50;

	private final SyncJobRepository syncJobRepository;
	private final WbReportDetailClient wbReportDetailClient;
	private final WbRealizationReportDetailMapper mapper;
	private final RawFinancialOperationImportService importService;
	private final DailyFinanceRecalculationService recalculationService;
	private final MarketplaceCredentialService credentialService;

	public WbFinanceSyncService(
			SyncJobRepository syncJobRepository,
			WbReportDetailClient wbReportDetailClient,
			WbRealizationReportDetailMapper mapper,
			RawFinancialOperationImportService importService,
			DailyFinanceRecalculationService recalculationService,
			MarketplaceCredentialService credentialService) {
		this.syncJobRepository = syncJobRepository;
		this.wbReportDetailClient = wbReportDetailClient;
		this.mapper = mapper;
		this.importService = importService;
		this.recalculationService = recalculationService;
		this.credentialService = credentialService;
	}

	public WbFinanceSyncResult syncWithSavedToken(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		String token = credentialService.getActiveWildberriesToken(userId);
		return sync(userId, token, dateFrom, dateTo);
	}

	public WbFinanceSyncResult sync(Long userId, String token, LocalDate dateFrom, LocalDate dateTo) {
		return sync(userId, token, dateFrom, dateTo, DEFAULT_PERIOD, DEFAULT_LIMIT, DEFAULT_MAX_PAGES);
	}

	public WbFinanceSyncResult sync(
			Long userId,
			String token,
			LocalDate dateFrom,
			LocalDate dateTo,
			String period,
			int limit,
			int maxPages) {
		validateInput(userId, token, dateFrom, dateTo, period, limit, maxPages);
		SyncJob syncJob = createRunningJob(userId, dateFrom, dateTo);

		try {
			List<JsonNode> wbRows = wbReportDetailClient.fetchReportDetailByPeriod(
					token,
					dateFrom,
					dateTo,
					period,
					limit,
					maxPages);
			List<RawFinancialOperationImportRow> importRows = wbRows.stream()
					.map(mapper::map)
					.toList();
			RawFinancialOperationImportResult importResult = importService.importRows(
					userId,
					syncJob.getId(),
					importRows);
			DailyFinanceRecalculationResult recalculationResult = recalculationService.recalculate(
					userId,
					dateFrom,
					dateTo);
			markCompleted(syncJob.getId(), recalculationResult);
			return new WbFinanceSyncResult(
					syncJob.getId(),
					importResult.receivedRows(),
					importResult.insertedRows(),
					importResult.duplicateRows(),
					recalculationResult.affectedDays(),
					recalculationResult.savedDailyRows(),
					recalculationResult.unrecognizedRows());
		}
		catch (RuntimeException exception) {
			markFailed(syncJob.getId(), exception);
			throw exception;
		}
	}

	protected SyncJob createRunningJob(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		SyncJob syncJob = new SyncJob(userId, dateFrom, dateTo);
		syncJob.markRunning();
		return syncJobRepository.saveAndFlush(syncJob);
	}

	protected void markCompleted(Long syncJobId, DailyFinanceRecalculationResult recalculationResult) {
		SyncJob syncJob = syncJobRepository.findById(syncJobId)
				.orElseThrow(() -> new IllegalStateException("Sync job not found: " + syncJobId));
		syncJob.markDailyRecalculated(recalculationResult.affectedDays(), recalculationResult.unrecognizedRows());
		syncJob.markCompleted();
		syncJobRepository.saveAndFlush(syncJob);
	}

	protected void markFailed(Long syncJobId, RuntimeException exception) {
		SyncJob syncJob = syncJobRepository.findById(syncJobId)
				.orElseThrow(() -> new IllegalStateException("Sync job not found: " + syncJobId));
		syncJob.markFailed(exception.getClass().getSimpleName(), exception.getMessage());
		syncJobRepository.saveAndFlush(syncJob);
	}

	private static void validateInput(
			Long userId,
			String token,
			LocalDate dateFrom,
			LocalDate dateTo,
			String period,
			int limit,
			int maxPages) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		requireText(token, "token");
		Objects.requireNonNull(dateFrom, "dateFrom must not be null");
		Objects.requireNonNull(dateTo, "dateTo must not be null");
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		requireText(period, "period");
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		if (maxPages <= 0) {
			throw new IllegalArgumentException("maxPages must be positive");
		}
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
