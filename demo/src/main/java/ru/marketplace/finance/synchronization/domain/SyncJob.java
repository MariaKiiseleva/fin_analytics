package ru.marketplace.finance.synchronization.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;

@Entity
@Table(name = "sync_jobs")
public class SyncJob {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "date_from", nullable = false)
	private LocalDate dateFrom;

	@Column(name = "date_to", nullable = false)
	private LocalDate dateTo;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 40)
	private SyncStatus status;

	@Column(name = "requested_at", nullable = false)
	private Instant requestedAt;

	@Column(name = "started_at")
	private Instant startedAt;

	@Column(name = "finished_at")
	private Instant finishedAt;

	@Column(name = "received_rows", nullable = false)
	private int receivedRows;

	@Column(name = "inserted_rows", nullable = false)
	private int insertedRows;

	@Column(name = "updated_rows", nullable = false)
	private int updatedRows;

	@Column(name = "duplicate_rows", nullable = false)
	private int duplicateRows;

	@Column(name = "affected_days", nullable = false)
	private int affectedDays;

	@Column(name = "unrecognized_rows", nullable = false)
	private int unrecognizedRows;

	@Column(name = "error_code", length = 100)
	private String errorCode;

	@Column(name = "error_message")
	private String errorMessage;

	protected SyncJob() {
	}

	public SyncJob(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		if (userId == null || userId <= 0) {
			throw new IllegalArgumentException("userId must be positive");
		}
		this.dateFrom = Objects.requireNonNull(dateFrom, "dateFrom must not be null");
		this.dateTo = Objects.requireNonNull(dateTo, "dateTo must not be null");
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		this.userId = userId;
		this.status = SyncStatus.CREATED;
		this.requestedAt = Instant.now();
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public LocalDate getDateFrom() {
		return dateFrom;
	}

	public LocalDate getDateTo() {
		return dateTo;
	}

	public SyncStatus getStatus() {
		return status;
	}

	public Instant getRequestedAt() {
		return requestedAt;
	}

	public Instant getStartedAt() {
		return startedAt;
	}

	public Instant getFinishedAt() {
		return finishedAt;
	}

	public int getReceivedRows() {
		return receivedRows;
	}

	public int getInsertedRows() {
		return insertedRows;
	}

	public int getUpdatedRows() {
		return updatedRows;
	}

	public int getDuplicateRows() {
		return duplicateRows;
	}

	public int getAffectedDays() {
		return affectedDays;
	}

	public int getUnrecognizedRows() {
		return unrecognizedRows;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void markRunning() {
		this.status = SyncStatus.RUNNING;
		this.startedAt = Instant.now();
	}

	public void markRawSaved(int receivedRows, int insertedRows, int updatedRows, int duplicateRows, int unrecognizedRows) {
		this.status = SyncStatus.RAW_SAVED;
		this.receivedRows = requireNonNegative(receivedRows, "receivedRows");
		this.insertedRows = requireNonNegative(insertedRows, "insertedRows");
		this.updatedRows = requireNonNegative(updatedRows, "updatedRows");
		this.duplicateRows = requireNonNegative(duplicateRows, "duplicateRows");
		this.unrecognizedRows = requireNonNegative(unrecognizedRows, "unrecognizedRows");
	}

	public void markDailyRecalculated(int affectedDays) {
		this.status = SyncStatus.DAILY_RECALCULATED;
		this.affectedDays = requireNonNegative(affectedDays, "affectedDays");
	}

	public void markCompleted() {
		this.status = SyncStatus.COMPLETED;
		this.finishedAt = Instant.now();
	}

	public void markFailed(String errorCode, String errorMessage) {
		this.status = SyncStatus.FAILED;
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.finishedAt = Instant.now();
	}

	private static int requireNonNegative(int value, String fieldName) {
		if (value < 0) {
			throw new IllegalArgumentException(fieldName + " must not be negative");
		}
		return value;
	}
}
