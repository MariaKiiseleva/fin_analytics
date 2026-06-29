package ru.marketplace.finance.account.application;

import java.time.Instant;
import java.time.LocalDate;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.domain.SyncStatus;

public record AdminSyncJobView(
		Long id,
		Long userId,
		LocalDate dateFrom,
		LocalDate dateTo,
		SyncStatus status,
		Instant requestedAt,
		Instant startedAt,
		Instant finishedAt,
		int receivedRows,
		int insertedRows,
		int updatedRows,
		int duplicateRows,
		int affectedDays,
		int unrecognizedRows,
		String errorCode,
		String errorMessage) {

	static AdminSyncJobView from(SyncJob syncJob) {
		return new AdminSyncJobView(
				syncJob.getId(),
				syncJob.getUserId(),
				syncJob.getDateFrom(),
				syncJob.getDateTo(),
				syncJob.getStatus(),
				syncJob.getRequestedAt(),
				syncJob.getStartedAt(),
				syncJob.getFinishedAt(),
				syncJob.getReceivedRows(),
				syncJob.getInsertedRows(),
				syncJob.getUpdatedRows(),
				syncJob.getDuplicateRows(),
				syncJob.getAffectedDays(),
				syncJob.getUnrecognizedRows(),
				syncJob.getErrorCode(),
				syncJob.getErrorMessage());
	}
}
