package ru.marketplace.finance.synchronization.domain;

public enum SyncStatus {
	CREATED,
	RUNNING,
	RAW_SAVED,
	DAILY_RECALCULATED,
	COMPLETED,
	FAILED
}
