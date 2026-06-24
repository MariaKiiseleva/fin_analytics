package ru.marketplace.finance.finance.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;
import ru.marketplace.finance.synchronization.domain.SyncJob;
import ru.marketplace.finance.synchronization.infrastructure.SyncJobRepository;

@Service
public class RawFinancialOperationImportService {

	private final RawFinancialOperationRepository rawRepository;
	private final SyncJobRepository syncJobRepository;
	private final BusinessDateResolver businessDateResolver = new BusinessDateResolver(ZoneId.of("Europe/Moscow"));

	public RawFinancialOperationImportService(
			RawFinancialOperationRepository rawRepository,
			SyncJobRepository syncJobRepository) {
		this.rawRepository = rawRepository;
		this.syncJobRepository = syncJobRepository;
	}

	@Transactional
	public RawFinancialOperationImportResult importRows(
			Long userId,
			Long syncJobId,
			List<RawFinancialOperationImportRow> rows) {
		requirePositive(userId, "userId");
		requirePositive(syncJobId, "syncJobId");
		Objects.requireNonNull(rows, "rows must not be null");

		SyncJob syncJob = syncJobRepository.findById(syncJobId)
				.filter(job -> job.getUserId().equals(userId))
				.orElseThrow(() -> new IllegalArgumentException("Sync job not found for user: " + syncJobId));
		Set<String> seenHashes = new HashSet<>();
		int insertedRows = 0;
		int duplicateRows = 0;

		for (RawFinancialOperationImportRow row : rows) {
			String rowHash = hash(requireText(row.rawPayload(), "rawPayload"));
			if (!seenHashes.add(rowHash) || rawRepository.existsByUserIdAndRowHash(userId, rowHash)) {
				duplicateRows++;
				continue;
			}
			rawRepository.save(toRawOperation(userId, syncJobId, rowHash, row));
			insertedRows++;
		}

		syncJob.markRawSaved(rows.size(), insertedRows, 0, duplicateRows, 0);
		return new RawFinancialOperationImportResult(rows.size(), insertedRows, duplicateRows);
	}

	private RawFinancialOperation toRawOperation(
			Long userId,
			Long syncJobId,
			String rowHash,
			RawFinancialOperationImportRow row) {
		RawFinancialOperation operation = new RawFinancialOperation(
				userId,
				syncJobId,
				rowHash,
				businessDateResolver.resolve(new BusinessDateInput(
						row.reportAt(),
						row.saleAt(),
						row.orderAt(),
						row.createdAt())),
				row.rawPayload());
		operation.setOperationIdentity(row.externalOperationId(), row.srid(), row.nmId());
		operation.setOperationNames(row.supplierOperationName(), row.documentType());
		operation.setOperationDates(row.orderAt(), row.saleAt(), row.reportAt());
		operation.setAmounts(
				row.quantity(),
				row.retailAmount(),
				row.retailAmountWithDiscount(),
				row.sellerAmount(),
				row.commissionAmount(),
				row.logisticsAmount(),
				row.rebillLogisticsAmount(),
				row.pvzRewardAmount(),
				row.acquiringAmount(),
				row.storageAmount(),
				row.acceptanceAmount(),
				row.penaltyAmount(),
				row.deductionAmount());
		return operation;
	}

	private static String hash(String value) {
		try {
			MessageDigest digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
			StringBuilder result = new StringBuilder(hash.length * 2);
			for (byte item : hash) {
				result.append(String.format("%02x", item));
			}
			return result.toString();
		}
		catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException("SHA-256 is not available", exception);
		}
	}

	private static Long requirePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
