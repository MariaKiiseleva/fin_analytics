package ru.marketplace.finance.finance.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.domain.RawFinancialOperation;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.RawFinancialOperationRepository;

@Service
public class DailyFinanceRecalculationService {

	private static final int CALCULATION_VERSION = 1;
	private static final BigDecimal ZERO = BigDecimal.ZERO;
	private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");

	private final UserRepository userRepository;
	private final ProductCostRepository productCostRepository;
	private final RawFinancialOperationRepository rawRepository;
	private final DailyFinanceEntryRepository dailyRepository;
	private final ObjectMapper objectMapper;
	private final FinancialOperationClassifier operationClassifier = new FinancialOperationClassifier();
	private final RawOperationMoneyCalculator moneyCalculator = new RawOperationMoneyCalculator();
	private final ProductProfitCalculator profitCalculator = new ProductProfitCalculator();

	public DailyFinanceRecalculationService(
			UserRepository userRepository,
			ProductCostRepository productCostRepository,
			RawFinancialOperationRepository rawRepository,
			DailyFinanceEntryRepository dailyRepository,
			ObjectMapper objectMapper) {
		this.userRepository = userRepository;
		this.productCostRepository = productCostRepository;
		this.rawRepository = rawRepository;
		this.dailyRepository = dailyRepository;
		this.objectMapper = objectMapper;
	}

	@Transactional
	public DailyFinanceRecalculationResult recalculate(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		Objects.requireNonNull(dateFrom, "dateFrom must not be null");
		Objects.requireNonNull(dateTo, "dateTo must not be null");
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		List<RawFinancialOperation> operations = rawRepository
				.findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscIdAsc(userId, dateFrom, dateTo);
		Map<LocalDate, DayAccumulator> days = createDays(dateFrom, dateTo);
		int unrecognizedRows = 0;

		for (RawFinancialOperation operation : operations) {
			FinancialOperationType operationType = classify(operation);
			if (operationType == FinancialOperationType.UNRECOGNIZED) {
				operation.markUnrecognized();
				unrecognizedRows++;
				continue;
			}
			operation.markRecognized(operationType.name());
			MoneyCalculationResult money = moneyCalculator.calculate(toMoneyInput(operation, operationType));
			days.get(operation.getBusinessDate()).add(operation, operationType, money);
		}

		dailyRepository.deleteByUserIdAndBusinessDateBetween(userId, dateFrom, dateTo);
		dailyRepository.flush();
		int savedRows = 0;
		for (DayAccumulator day : days.values()) {
			savedRows += saveDay(user, day);
		}

		return new DailyFinanceRecalculationResult(operations.size(), days.size(), savedRows, unrecognizedRows);
	}

	private int saveDay(User user, DayAccumulator day) {
		day.prepareProductRows();
		DailyFinanceEntry commonRow = DailyFinanceEntry.commonRow(user.getId(), day.businessDate, CALCULATION_VERSION);
		commonRow.replaceCommonExpenses(
				day.commonAcquiring,
				day.commonStorage,
				day.commonAcceptance,
				day.commonPenalty,
				day.commonDeductions.add(day.unassignedDayLogistics));
		dailyRepository.save(commonRow);

		int savedRows = 1;
		for (ProductAccumulator product : day.products.values()) {
			BigDecimal costAmount = resolveCostAmount(user.getId(), product, day.businessDate);
			BigDecimal taxAmount = calculateTax(product.netRevenue(), user.getTaxPercent());
			BigDecimal profitAmount = profitCalculator.calculate(new ProductProfitInput(
					product.netRevenue(),
					product.commission,
					product.logistics,
					product.acquiring,
					costAmount,
					taxAmount));
			DailyFinanceEntry productRow = DailyFinanceEntry.productRow(
					user.getId(),
					day.businessDate,
					product.nmId,
					product.productName,
					CALCULATION_VERSION);
			productRow.replaceProductTotals(
					product.salesQuantity,
					product.returnQuantity,
					product.salesAmount,
					product.returnsAmount,
					product.commission,
					product.logistics,
					product.acquiring,
					costAmount,
					taxAmount,
					profitAmount,
					product.hasCost);
			dailyRepository.save(productRow);
			savedRows++;
		}
		return savedRows;
	}

	private BigDecimal resolveCostAmount(Long userId, ProductAccumulator product, LocalDate businessDate) {
		if (product.netQuantity() <= 0) {
			product.hasCost = true;
			return ZERO;
		}
		return productCostRepository
				.findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
						userId,
						product.nmId,
						businessDate)
				.map(cost -> cost.getCostAmount().multiply(BigDecimal.valueOf(product.netQuantity())))
				.map(DailyFinanceRecalculationService::money)
				.orElseGet(() -> {
					product.hasCost = false;
					return ZERO;
				});
	}

	private Map<LocalDate, DayAccumulator> createDays(LocalDate dateFrom, LocalDate dateTo) {
		Map<LocalDate, DayAccumulator> days = new LinkedHashMap<>();
		LocalDate current = dateFrom;
		while (!current.isAfter(dateTo)) {
			days.put(current, new DayAccumulator(current));
			current = current.plusDays(1);
		}
		return days;
	}

	private FinancialOperationType classify(RawFinancialOperation operation) {
		return operationClassifier.classify(new FinancialOperationClassificationInput(
				operation.getSupplierOperationName(),
				operation.getDocumentType(),
				operation.getAcquiringAmount(),
				operation.getStorageAmount(),
				operation.getAcceptanceAmount(),
				operation.getPenaltyAmount(),
				operation.getDeductionAmount()));
	}

	private MoneyCalculationInput toMoneyInput(
			RawFinancialOperation operation,
			FinancialOperationType operationType) {
		return new MoneyCalculationInput(
				operationType,
				operation.getQuantity(),
				operation.getRetailAmount(),
				operation.getRetailAmountWithDiscount(),
				operation.getSellerAmount(),
				operation.getCommissionAmount(),
				operation.getAcquiringAmount(),
				operation.getLogisticsAmount(),
				operation.getRebillLogisticsAmount(),
				operation.getPvzRewardAmount(),
				operationClassifier.isPvzRewardLogisticsOperation(operation.getSupplierOperationName()),
				operation.getStorageAmount(),
				operation.getAcceptanceAmount(),
				operation.getPenaltyAmount(),
				operation.getDeductionAmount());
	}

	private static BigDecimal calculateTax(BigDecimal netRevenue, BigDecimal taxPercent) {
		if (netRevenue.compareTo(ZERO) <= 0 || taxPercent == null || taxPercent.compareTo(ZERO) == 0) {
			return ZERO;
		}
		return netRevenue.multiply(taxPercent)
				.divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP);
	}

	private static boolean hasProduct(RawFinancialOperation operation) {
		return operation.getNmId() != null && operation.getNmId() > 0;
	}

	private static boolean hasOrder(RawFinancialOperation operation) {
		return operation.getSrid() != null && !operation.getSrid().isBlank();
	}

	private static BigDecimal money(BigDecimal value) {
		return value.setScale(2, RoundingMode.HALF_UP);
	}

	private final class DayAccumulator {

		private final LocalDate businessDate;
		private final Map<OrderProductKey, OrderProductAccumulator> orderProducts = new LinkedHashMap<>();
		private final Map<Long, ProductAccumulator> products = new LinkedHashMap<>();
		private final Map<String, OrderLogisticsAccumulator> orderLogistics = new LinkedHashMap<>();
		private BigDecimal commonAcquiring = ZERO;
		private BigDecimal commonStorage = ZERO;
		private BigDecimal commonAcceptance = ZERO;
		private BigDecimal commonPenalty = ZERO;
		private BigDecimal commonDeductions = ZERO;
		private BigDecimal unassignedDayLogistics = ZERO;

		private DayAccumulator(LocalDate businessDate) {
			this.businessDate = businessDate;
		}

		private void add(
				RawFinancialOperation operation,
				FinancialOperationType operationType,
				MoneyCalculationResult money) {
			commonStorage = commonStorage.add(money.storageAmount());
			commonAcceptance = commonAcceptance.add(money.acceptanceAmount());
			commonPenalty = commonPenalty.add(money.penaltyAmount());
			commonDeductions = commonDeductions.add(money.deductionAmount());

			if (hasProduct(operation)) {
				OrderProductKey orderProductKey = OrderProductKey.from(operation);
				orderProducts.computeIfAbsent(orderProductKey, OrderProductAccumulator::new)
						.add(operationType, money, extractProductName(operation));
				if (hasOrder(operation)) {
					orderLogistics.computeIfAbsent(operation.getSrid(), OrderLogisticsAccumulator::new)
							.addProduct(orderProductKey);
				}
				return;
			}
			commonAcquiring = commonAcquiring.add(money.acquiringAmount());
			if (money.logisticsAmount().compareTo(ZERO) == 0) {
				return;
			}
			if (hasOrder(operation)) {
				orderLogistics.computeIfAbsent(operation.getSrid(), OrderLogisticsAccumulator::new)
						.addLogistics(money.logisticsAmount());
				return;
			}
			unassignedDayLogistics = unassignedDayLogistics.add(money.logisticsAmount());
		}

		private void prepareProductRows() {
			distributeOrderLogistics();
			aggregateOrderProducts();
			distributeUnassignedDayLogistics();
		}

		private void distributeOrderLogistics() {
			for (OrderLogisticsAccumulator order : orderLogistics.values()) {
				if (order.logistics.compareTo(ZERO) == 0) {
					continue;
				}
				if (order.productKeys.isEmpty()) {
					unassignedDayLogistics = unassignedDayLogistics.add(order.logistics);
					continue;
				}
				distributeBetweenOrderProducts(order.logistics, order.productKeys);
			}
		}

		private void aggregateOrderProducts() {
			for (OrderProductAccumulator orderProduct : orderProducts.values()) {
				products.computeIfAbsent(orderProduct.nmId(), ProductAccumulator::new)
						.add(orderProduct);
			}
		}

		private void distributeUnassignedDayLogistics() {
			if (unassignedDayLogistics.compareTo(ZERO) == 0 || products.isEmpty()) {
				return;
			}
			distributeBetweenProducts(unassignedDayLogistics, products.keySet());
			unassignedDayLogistics = ZERO;
		}

		private void distributeBetweenOrderProducts(BigDecimal amount, Iterable<OrderProductKey> orderProductKeys) {
			List<OrderProductKey> keys = new java.util.ArrayList<>();
			for (OrderProductKey key : orderProductKeys) {
				keys.add(key);
			}
			BigDecimal productCount = BigDecimal.valueOf(keys.size());
			BigDecimal baseShare = amount.divide(productCount, 2, RoundingMode.DOWN);
			BigDecimal distributed = ZERO;
			int index = 0;
			for (OrderProductKey key : keys) {
				index++;
				BigDecimal share = index == keys.size()
						? amount.subtract(distributed)
						: baseShare;
				OrderProductAccumulator orderProduct = orderProducts.get(key);
				orderProduct.logistics = orderProduct.logistics.add(share);
				distributed = distributed.add(share);
			}
		}

		private void distributeBetweenProducts(BigDecimal amount, Iterable<Long> productIds) {
			List<Long> ids = new java.util.ArrayList<>();
			for (Long productId : productIds) {
				ids.add(productId);
			}
			BigDecimal productCount = BigDecimal.valueOf(ids.size());
			BigDecimal baseShare = amount.divide(productCount, 2, RoundingMode.DOWN);
			BigDecimal distributed = ZERO;
			int index = 0;
			for (Long productId : ids) {
				index++;
				BigDecimal share = index == ids.size()
						? amount.subtract(distributed)
						: baseShare;
				ProductAccumulator product = products.get(productId);
				product.logistics = product.logistics.add(share);
				distributed = distributed.add(share);
			}
		}
	}

	private String extractProductName(RawFinancialOperation operation) {
		try {
			JsonNode payload = objectMapper.readTree(operation.getRawPayload());
			List<String> parts = new ArrayList<>();
			addTextPart(parts, payload, "brand_name");
			addTextPart(parts, payload, "subject_name");
			addTextPart(parts, payload, "sa_name");
			addTextPart(parts, payload, "ts_name");
			return parts.isEmpty() ? null : String.join(" / ", parts);
		}
		catch (JsonProcessingException exception) {
			return null;
		}
	}

	private static void addTextPart(List<String> parts, JsonNode payload, String fieldName) {
		JsonNode value = payload.get(fieldName);
		if (value == null || value.isNull()) {
			return;
		}
		String text = value.asText().trim();
		if (text.isEmpty() || "0".equals(text) || parts.contains(text)) {
			return;
		}
		parts.add(text);
	}

	private static final class OrderLogisticsAccumulator {

		private final String srid;
		private final Set<OrderProductKey> productKeys = new java.util.LinkedHashSet<>();
		private BigDecimal logistics = ZERO;

		private OrderLogisticsAccumulator(String srid) {
			this.srid = srid;
		}

		private void addProduct(OrderProductKey orderProductKey) {
			productKeys.add(orderProductKey);
		}

		private void addLogistics(BigDecimal amount) {
			logistics = logistics.add(amount);
		}
	}

	private record OrderProductKey(String srid, Long nmId) {

		private static OrderProductKey from(RawFinancialOperation operation) {
			return new OrderProductKey(operation.getSrid(), operation.getNmId());
		}
	}

	private static final class OrderProductAccumulator {

		private final OrderProductKey key;
		private String productName;
		private int salesQuantity;
		private int returnQuantity;
		private BigDecimal salesAmount = ZERO;
		private BigDecimal returnsAmount = ZERO;
		private BigDecimal commission = ZERO;
		private BigDecimal logistics = ZERO;
		private BigDecimal acquiring = ZERO;

		private OrderProductAccumulator(OrderProductKey key) {
			this.key = key;
		}

		private Long nmId() {
			return key.nmId();
		}

		private void add(FinancialOperationType operationType, MoneyCalculationResult money, String productName) {
			if (this.productName == null && productName != null && !productName.isBlank()) {
				this.productName = productName;
			}
			if (operationType == FinancialOperationType.SALE || operationType == FinancialOperationType.RETURN) {
				salesQuantity += money.salesQuantity();
				returnQuantity += money.returnQuantity();
				salesAmount = salesAmount.add(money.salesAmount());
				returnsAmount = returnsAmount.add(money.returnsAmount());
				commission = commission.add(money.commissionAmount());
				acquiring = acquiring.add(money.acquiringAmount());
			}
			logistics = logistics.add(money.logisticsAmount());
		}
	}

	private static final class ProductAccumulator {

		private final Long nmId;
		private String productName;
		private int salesQuantity;
		private int returnQuantity;
		private BigDecimal salesAmount = ZERO;
		private BigDecimal returnsAmount = ZERO;
		private BigDecimal commission = ZERO;
		private BigDecimal logistics = ZERO;
		private BigDecimal acquiring = ZERO;
		private boolean hasCost = true;

		private ProductAccumulator(Long nmId) {
			this.nmId = nmId;
		}

		private void add(OrderProductAccumulator orderProduct) {
			if (productName == null && orderProduct.productName != null && !orderProduct.productName.isBlank()) {
				productName = orderProduct.productName;
			}
			salesQuantity += orderProduct.salesQuantity;
			returnQuantity += orderProduct.returnQuantity;
			salesAmount = salesAmount.add(orderProduct.salesAmount);
			returnsAmount = returnsAmount.add(orderProduct.returnsAmount);
			commission = commission.add(orderProduct.commission);
			logistics = logistics.add(orderProduct.logistics);
			acquiring = acquiring.add(orderProduct.acquiring);
		}

		private int netQuantity() {
			return salesQuantity - returnQuantity;
		}

		private BigDecimal netRevenue() {
			return salesAmount.subtract(returnsAmount);
		}
	}
}
