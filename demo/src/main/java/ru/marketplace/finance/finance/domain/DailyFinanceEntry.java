package ru.marketplace.finance.finance.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "daily_finance_entries")
public class DailyFinanceEntry {

	private static final BigDecimal ZERO_AMOUNT = BigDecimal.ZERO;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "business_date", nullable = false)
	private LocalDate businessDate;

	@Column(name = "nm_id")
	private Long nmId;

	@Column(name = "product_name")
	private String productName;

	@Column(name = "sales_quantity", nullable = false)
	private Integer salesQuantity;

	@Column(name = "return_quantity", nullable = false)
	private Integer returnQuantity;

	@Column(name = "net_quantity", nullable = false)
	private Integer netQuantity;

	@Column(name = "sales_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal salesAmount;

	@Column(name = "returns_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal returnsAmount;

	@Column(name = "net_revenue_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal netRevenueAmount;

	@Column(name = "commission_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal commissionAmount;

	@Column(name = "logistics_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal logisticsAmount;

	@Column(name = "cost_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal costAmount;

	@Column(name = "tax_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal taxAmount;

	@Column(name = "product_profit_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal productProfitAmount;

	@Column(name = "acquiring_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal acquiringAmount;

	@Column(name = "storage_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal storageAmount;

	@Column(name = "acceptance_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal acceptanceAmount;

	@Column(name = "penalty_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal penaltyAmount;

	@Column(name = "additional_deductions_amount", nullable = false, precision = 19, scale = 2)
	private BigDecimal additionalDeductionsAmount;

	@Column(name = "has_cost", nullable = false)
	private Boolean hasCost;

	@Column(name = "calculation_version", nullable = false)
	private Integer calculationVersion;

	@Column(name = "calculated_at", nullable = false)
	private Instant calculatedAt;

	protected DailyFinanceEntry() {
	}

	private DailyFinanceEntry(Long userId, LocalDate businessDate, Long nmId, String productName, int calculationVersion) {
		this.userId = requirePositive(userId, "userId");
		this.businessDate = requireNonNull(businessDate, "businessDate");
		this.nmId = nmId;
		this.productName = productName;
		this.calculationVersion = requirePositive(calculationVersion, "calculationVersion");
		this.calculatedAt = Instant.now();
		resetTotals();
	}

	public static DailyFinanceEntry productRow(
			Long userId,
			LocalDate businessDate,
			Long nmId,
			String productName,
			int calculationVersion) {
		return new DailyFinanceEntry(
				userId,
				businessDate,
				requirePositive(nmId, "nmId"),
				productName,
				calculationVersion);
	}

	public static DailyFinanceEntry commonRow(Long userId, LocalDate businessDate, int calculationVersion) {
		return new DailyFinanceEntry(userId, businessDate, null, null, calculationVersion);
	}

	public Long getId() {
		return id;
	}

	public Long getUserId() {
		return userId;
	}

	public LocalDate getBusinessDate() {
		return businessDate;
	}

	public Long getNmId() {
		return nmId;
	}

	public String getProductName() {
		return productName;
	}

	public Integer getSalesQuantity() {
		return salesQuantity;
	}

	public Integer getReturnQuantity() {
		return returnQuantity;
	}

	public Integer getNetQuantity() {
		return netQuantity;
	}

	public BigDecimal getSalesAmount() {
		return salesAmount;
	}

	public BigDecimal getReturnsAmount() {
		return returnsAmount;
	}

	public BigDecimal getNetRevenueAmount() {
		return netRevenueAmount;
	}

	public BigDecimal getCommissionAmount() {
		return commissionAmount;
	}

	public BigDecimal getLogisticsAmount() {
		return logisticsAmount;
	}

	public BigDecimal getCostAmount() {
		return costAmount;
	}

	public BigDecimal getTaxAmount() {
		return taxAmount;
	}

	public BigDecimal getProductProfitAmount() {
		return productProfitAmount;
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

	public BigDecimal getAdditionalDeductionsAmount() {
		return additionalDeductionsAmount;
	}

	public Boolean getHasCost() {
		return hasCost;
	}

	public Integer getCalculationVersion() {
		return calculationVersion;
	}

	public void replaceProductTotals(
			int salesQuantity,
			int returnQuantity,
			BigDecimal salesAmount,
			BigDecimal returnsAmount,
			BigDecimal commissionAmount,
			BigDecimal logisticsAmount,
			BigDecimal costAmount,
			BigDecimal taxAmount,
			BigDecimal productProfitAmount,
			boolean hasCost) {
		requireProductRow();
		this.salesQuantity = salesQuantity;
		this.returnQuantity = returnQuantity;
		this.netQuantity = salesQuantity - returnQuantity;
		this.salesAmount = amountOrZero(salesAmount);
		this.returnsAmount = amountOrZero(returnsAmount);
		this.netRevenueAmount = this.salesAmount.subtract(this.returnsAmount);
		this.commissionAmount = amountOrZero(commissionAmount);
		this.logisticsAmount = amountOrZero(logisticsAmount);
		this.costAmount = amountOrZero(costAmount);
		this.taxAmount = amountOrZero(taxAmount);
		this.productProfitAmount = amountOrZero(productProfitAmount);
		this.hasCost = hasCost;
		refreshCalculationTime();
	}

	public void replaceCommonExpenses(
			BigDecimal acquiringAmount,
			BigDecimal storageAmount,
			BigDecimal acceptanceAmount,
			BigDecimal penaltyAmount,
			BigDecimal additionalDeductionsAmount) {
		this.acquiringAmount = amountOrZero(acquiringAmount);
		this.storageAmount = amountOrZero(storageAmount);
		this.acceptanceAmount = amountOrZero(acceptanceAmount);
		this.penaltyAmount = amountOrZero(penaltyAmount);
		this.additionalDeductionsAmount = amountOrZero(additionalDeductionsAmount);
		refreshCalculationTime();
	}

	public void refreshCalculationVersion(int calculationVersion) {
		this.calculationVersion = requirePositive(calculationVersion, "calculationVersion");
		refreshCalculationTime();
	}

	private void resetTotals() {
		this.salesQuantity = 0;
		this.returnQuantity = 0;
		this.netQuantity = 0;
		this.salesAmount = ZERO_AMOUNT;
		this.returnsAmount = ZERO_AMOUNT;
		this.netRevenueAmount = ZERO_AMOUNT;
		this.commissionAmount = ZERO_AMOUNT;
		this.logisticsAmount = ZERO_AMOUNT;
		this.costAmount = ZERO_AMOUNT;
		this.taxAmount = ZERO_AMOUNT;
		this.productProfitAmount = ZERO_AMOUNT;
		this.acquiringAmount = ZERO_AMOUNT;
		this.storageAmount = ZERO_AMOUNT;
		this.acceptanceAmount = ZERO_AMOUNT;
		this.penaltyAmount = ZERO_AMOUNT;
		this.additionalDeductionsAmount = ZERO_AMOUNT;
		this.hasCost = true;
	}

	private void requireProductRow() {
		if (nmId == null) {
			throw new IllegalStateException("Product totals can be set only for product daily row");
		}
	}

	private void refreshCalculationTime() {
		this.calculatedAt = Instant.now();
	}

	private static BigDecimal amountOrZero(BigDecimal value) {
		return value == null ? ZERO_AMOUNT : value;
	}

	private static Long requirePositive(Long value, String fieldName) {
		if (value == null || value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
		}
		return value;
	}

	private static int requirePositive(int value, String fieldName) {
		if (value <= 0) {
			throw new IllegalArgumentException(fieldName + " must be positive");
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
