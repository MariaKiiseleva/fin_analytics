package ru.marketplace.finance.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "financial_operations_raw")
public class RawFinancialOperation {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "sync_job_id", nullable = false)
	private Long syncJobId;

	@Column(name = "row_hash", nullable = false, length = 64)
	private String rowHash;

	@Column(name = "external_operation_id")
	private String externalOperationId;

	private String srid;

	@Column(name = "nm_id")
	private Long nmId;

	@Column(name = "supplier_oper_name")
	private String supplierOperationName;

	@Column(name = "document_type", length = 100)
	private String documentType;

	@Column(name = "order_at")
	private Instant orderAt;

	@Column(name = "sale_at")
	private Instant saleAt;

	@Column(name = "report_at")
	private Instant reportAt;

	@Column(name = "business_date", nullable = false)
	private LocalDate businessDate;

	private Integer quantity;

	@Column(name = "retail_amount", precision = 19, scale = 2)
	private BigDecimal retailAmount;

	@Column(name = "retail_amount_with_discount", precision = 19, scale = 2)
	private BigDecimal retailAmountWithDiscount;

	@Column(name = "seller_amount", precision = 19, scale = 2)
	private BigDecimal sellerAmount;

	@Column(name = "commission_amount", precision = 19, scale = 2)
	private BigDecimal commissionAmount;

	@Column(name = "logistics_amount", precision = 19, scale = 2)
	private BigDecimal logisticsAmount;

	@Column(name = "rebill_logistics_amount", precision = 19, scale = 2)
	private BigDecimal rebillLogisticsAmount;

	@Column(name = "pvz_reward_amount", precision = 19, scale = 2)
	private BigDecimal pvzRewardAmount;

	@Column(name = "acquiring_amount", precision = 19, scale = 2)
	private BigDecimal acquiringAmount;

	@Column(name = "storage_amount", precision = 19, scale = 2)
	private BigDecimal storageAmount;

	@Column(name = "acceptance_amount", precision = 19, scale = 2)
	private BigDecimal acceptanceAmount;

	@Column(name = "penalty_amount", precision = 19, scale = 2)
	private BigDecimal penaltyAmount;

	@Column(name = "deduction_amount", precision = 19, scale = 2)
	private BigDecimal deductionAmount;

	@Enumerated(EnumType.STRING)
	@Column(name = "classification_status", nullable = false, length = 30)
	private ClassificationStatus classificationStatus;

	@Column(name = "classification_code", length = 60)
	private String classificationCode;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "raw_payload", nullable = false, columnDefinition = "jsonb")
	private String rawPayload;

	@Column(name = "created_at", nullable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected RawFinancialOperation() {
	}

	public RawFinancialOperation(Long userId, Long syncJobId, String rowHash, LocalDate businessDate, String rawPayload) {
		this.userId = requirePositive(userId, "userId");
		this.syncJobId = requirePositive(syncJobId, "syncJobId");
		this.rowHash = requireText(rowHash, "rowHash");
		this.businessDate = requireNonNull(businessDate, "businessDate");
		this.rawPayload = requireText(rawPayload, "rawPayload");
		this.classificationStatus = ClassificationStatus.PENDING;
		Instant now = Instant.now();
		this.createdAt = now;
		this.updatedAt = now;
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public Long getSyncJobId() {
		return syncJobId;
	}

	public String getRowHash() {
		return rowHash;
	}

	public String getSrid() {
		return srid;
	}

	public Long getNmId() {
		return nmId;
	}

	public String getSupplierOperationName() {
		return supplierOperationName;
	}

	public String getDocumentType() {
		return documentType;
	}

	public LocalDate getBusinessDate() {
		return businessDate;
	}

	public Integer getQuantity() {
		return quantity;
	}

	public BigDecimal getRetailAmount() {
		return retailAmount;
	}

	public BigDecimal getRetailAmountWithDiscount() {
		return retailAmountWithDiscount;
	}

	public BigDecimal getSellerAmount() {
		return sellerAmount;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public BigDecimal getLogisticsAmount() {
		return logisticsAmount;
	}

	public BigDecimal getRebillLogisticsAmount() {
		return rebillLogisticsAmount;
	}

	public BigDecimal getPvzRewardAmount() {
		return pvzRewardAmount;
	}

	public BigDecimal getAcquiringAmount() {
		return acquiringAmount;
	}

	public BigDecimal getStorageAmount() {
		return storageAmount;
	}

	public BigDecimal getAcceptanceAmount() {
		return acceptanceAmount;
	}

	public BigDecimal getPenaltyAmount() {
		return penaltyAmount;
	}

	public BigDecimal getDeductionAmount() {
		return deductionAmount;
	}

	public ClassificationStatus getClassificationStatus() {
		return classificationStatus;
	}

	public String getClassificationCode() {
		return classificationCode;
	}

	public String getRawPayload() {
		return rawPayload;
	}

	public void setOperationIdentity(String externalOperationId, String srid, Long nmId) {
		this.externalOperationId = externalOperationId;
		this.srid = srid;
		this.nmId = nmId;
		touch();
	}

	public void setOperationNames(String supplierOperationName, String documentType) {
		this.supplierOperationName = supplierOperationName;
		this.documentType = documentType;
		touch();
	}

	public void setOperationDates(Instant orderAt, Instant saleAt, Instant reportAt) {
		this.orderAt = orderAt;
		this.saleAt = saleAt;
		this.reportAt = reportAt;
		touch();
	}

	public void setAmounts(
			Integer quantity,
			BigDecimal retailAmount,
			BigDecimal retailAmountWithDiscount,
			BigDecimal sellerAmount,
			BigDecimal commissionAmount,
			BigDecimal logisticsAmount,
			BigDecimal rebillLogisticsAmount,
			BigDecimal pvzRewardAmount,
			BigDecimal acquiringAmount,
			BigDecimal storageAmount,
			BigDecimal acceptanceAmount,
			BigDecimal penaltyAmount,
			BigDecimal deductionAmount) {
		this.quantity = quantity;
		this.retailAmount = retailAmount;
		this.retailAmountWithDiscount = retailAmountWithDiscount;
		this.sellerAmount = sellerAmount;
		this.commissionAmount = commissionAmount;
		this.logisticsAmount = logisticsAmount;
		this.rebillLogisticsAmount = rebillLogisticsAmount;
		this.pvzRewardAmount = pvzRewardAmount;
		this.acquiringAmount = acquiringAmount;
		this.storageAmount = storageAmount;
		this.acceptanceAmount = acceptanceAmount;
		this.penaltyAmount = penaltyAmount;
		this.deductionAmount = deductionAmount;
		touch();
	}

	public void markRecognized(String classificationCode) {
		this.classificationStatus = ClassificationStatus.RECOGNIZED;
		this.classificationCode = requireText(classificationCode, "classificationCode");
		touch();
	}

	public void markUnrecognized() {
		this.classificationStatus = ClassificationStatus.UNRECOGNIZED;
		this.classificationCode = null;
		touch();
	}

	private void touch() {
		this.updatedAt = Instant.now();
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

	private static <T> T requireNonNull(T value, String fieldName) {
		if (value == null) {
			throw new IllegalArgumentException(fieldName + " must not be null");
		}
		return value;
	}
}
