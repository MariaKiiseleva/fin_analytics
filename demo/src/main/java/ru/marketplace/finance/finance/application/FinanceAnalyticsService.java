package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import ru.marketplace.finance.account.domain.User;
import ru.marketplace.finance.account.infrastructure.UserRepository;
import ru.marketplace.finance.cost.infrastructure.ProductCostRepository;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@Service
public class FinanceAnalyticsService {

	private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

	private final DailyFinanceEntryRepository dailyRepository;
	private final ProductCostRepository productCostRepository;
	private final UserRepository userRepository;

	public FinanceAnalyticsService(
			DailyFinanceEntryRepository dailyRepository,
			ProductCostRepository productCostRepository,
			UserRepository userRepository) {
		this.dailyRepository = dailyRepository;
		this.productCostRepository = productCostRepository;
		this.userRepository = userRepository;
	}

	public FinanceAnalyticsReport buildReport(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		if (dateFrom == null || dateTo == null) {
			throw new IllegalArgumentException("dateFrom and dateTo must not be null");
		}
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}

		User user = userRepository.findById(userId)
				.orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
		List<DailyFinanceEntry> entries = dailyRepository
				.findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscNmIdAsc(user.getId(), dateFrom, dateTo);

		List<EntryMetrics> metrics = entries.stream()
				.map(entry -> toMetrics(entry, user.getTaxPercent()))
				.toList();

		return new FinanceAnalyticsReport(
				buildSummary(metrics),
				buildDynamics(metrics),
				buildCostStructure(metrics),
				buildTopProducts(metrics),
				buildLossProducts(metrics));
	}

	private EntryMetrics toMetrics(DailyFinanceEntry entry, BigDecimal taxPercent) {
		CostLookup costLookup = findActualCostAmount(entry);
		BigDecimal taxAmount = calculateTax(entry.getNetRevenueAmount(), taxPercent);
		BigDecimal profit = entry.getNetRevenueAmount()
				.subtract(entry.getCommissionAmount())
				.subtract(entry.getLogisticsAmount())
				.subtract(entry.getAcquiringAmount())
				.subtract(entry.getStorageAmount())
				.subtract(entry.getAcceptanceAmount())
				.subtract(entry.getPenaltyAmount())
				.subtract(entry.getAdditionalDeductionsAmount())
				.subtract(costLookup.costAmount())
				.subtract(taxAmount);
		return new EntryMetrics(entry, costLookup.costAmount(), costLookup.hasCost(), taxAmount, profit);
	}

	private CostLookup findActualCostAmount(DailyFinanceEntry entry) {
		if (entry.getNmId() == null || entry.getNetQuantity() == 0) {
			return new CostLookup(BigDecimal.ZERO, true);
		}
		return productCostRepository
				.findFirstByUserIdAndNmIdAndValidFromLessThanEqualOrderByValidFromDesc(
						entry.getUserId(),
						entry.getNmId(),
						entry.getBusinessDate())
				.map(productCost -> new CostLookup(
						productCost.getCostAmount().multiply(BigDecimal.valueOf(entry.getNetQuantity())),
						true))
				.orElseGet(() -> new CostLookup(BigDecimal.ZERO, entry.getNetQuantity() <= 0));
	}

	private FinanceAnalyticsReport.Summary buildSummary(List<EntryMetrics> metrics) {
		BigDecimal netRevenue = sum(metrics, metric -> metric.entry().getNetRevenueAmount());
		BigDecimal commission = sum(metrics, metric -> metric.entry().getCommissionAmount());
		BigDecimal logistics = sum(metrics, metric -> metric.entry().getLogisticsAmount());
		BigDecimal acquiring = sum(metrics, metric -> metric.entry().getAcquiringAmount());
		BigDecimal storage = sum(metrics, metric -> metric.entry().getStorageAmount());
		BigDecimal acceptance = sum(metrics, metric -> metric.entry().getAcceptanceAmount());
		BigDecimal penalty = sum(metrics, metric -> metric.entry().getPenaltyAmount());
		BigDecimal deductions = sum(metrics, metric -> metric.entry().getAdditionalDeductionsAmount());
		BigDecimal cost = sum(metrics, EntryMetrics::costAmount);
		BigDecimal tax = sum(metrics, EntryMetrics::taxAmount);
		BigDecimal profit = sum(metrics, EntryMetrics::profit);
		Set<Long> productsWithoutCost = new LinkedHashSet<>();
		for (EntryMetrics metric : metrics) {
			DailyFinanceEntry entry = metric.entry();
			if (entry.getNmId() != null && entry.getNetQuantity() > 0 && !metric.hasCost()) {
				productsWithoutCost.add(entry.getNmId());
			}
		}
		BigDecimal wbExpenses = commission
				.add(logistics)
				.add(acquiring)
				.add(storage)
				.add(acceptance)
				.add(penalty)
				.add(deductions);
		return new FinanceAnalyticsReport.Summary(
				netRevenue,
				wbExpenses,
				cost,
				tax,
				profit,
				percent(profit, netRevenue),
				productsWithoutCost.size());
	}

	private List<FinanceAnalyticsReport.DailyPoint> buildDynamics(List<EntryMetrics> metrics) {
		Map<LocalDate, MutableDailyPoint> byDate = new LinkedHashMap<>();
		for (EntryMetrics metric : metrics) {
			DailyFinanceEntry entry = metric.entry();
			MutableDailyPoint point = byDate.computeIfAbsent(entry.getBusinessDate(), MutableDailyPoint::new);
			point.netRevenue = point.netRevenue.add(entry.getNetRevenueAmount());
			point.totalProfit = point.totalProfit.add(metric.profit());
			if (entry.getNmId() != null) {
				point.productProfit = point.productProfit.add(metric.profit());
			}
		}
		return byDate.values().stream()
				.map(MutableDailyPoint::toPoint)
				.toList();
	}

	private List<FinanceAnalyticsReport.CostSlice> buildCostStructure(List<EntryMetrics> metrics) {
		List<MutableCostSlice> slices = List.of(
				new MutableCostSlice("COMMISSION", "Комиссия маркетплейса", sum(metrics, metric -> metric.entry().getCommissionAmount())),
				new MutableCostSlice("LOGISTICS", "Логистика", sum(metrics, metric -> metric.entry().getLogisticsAmount())),
				new MutableCostSlice("ACQUIRING", "Эквайринг", sum(metrics, metric -> metric.entry().getAcquiringAmount())),
				new MutableCostSlice("STORAGE", "Хранение", sum(metrics, metric -> metric.entry().getStorageAmount())),
				new MutableCostSlice("ACCEPTANCE", "Приемка", sum(metrics, metric -> metric.entry().getAcceptanceAmount())),
				new MutableCostSlice("PENALTY", "Штрафы", sum(metrics, metric -> metric.entry().getPenaltyAmount())),
				new MutableCostSlice("DEDUCTIONS", "Дополнительные удержания", sum(metrics, metric -> metric.entry().getAdditionalDeductionsAmount())),
				new MutableCostSlice("COST", "Себестоимость", sum(metrics, EntryMetrics::costAmount)),
				new MutableCostSlice("TAX", "Налог", sum(metrics, EntryMetrics::taxAmount)));
		BigDecimal total = slices.stream()
				.map(MutableCostSlice::amount)
				.filter(amount -> amount.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		return slices.stream()
				.filter(slice -> slice.amount().signum() > 0)
				.map(slice -> new FinanceAnalyticsReport.CostSlice(
						slice.code(),
						slice.label(),
						slice.amount(),
						percent(slice.amount(), total)))
				.toList();
	}

	private List<FinanceAnalyticsReport.ProductPoint> buildTopProducts(List<EntryMetrics> metrics) {
		return buildProductPoints(metrics).stream()
				.sorted(Comparator.comparing(FinanceAnalyticsReport.ProductPoint::profit).reversed())
				.limit(5)
				.toList();
	}

	private List<FinanceAnalyticsReport.ProductPoint> buildLossProducts(List<EntryMetrics> metrics) {
		return buildProductPoints(metrics).stream()
				.filter(point -> point.profit().signum() < 0)
				.sorted(Comparator.comparing(FinanceAnalyticsReport.ProductPoint::profit))
				.toList();
	}

	private List<FinanceAnalyticsReport.ProductPoint> buildProductPoints(List<EntryMetrics> metrics) {
		Map<Long, MutableProductPoint> byProduct = new LinkedHashMap<>();
		for (EntryMetrics metric : metrics) {
			DailyFinanceEntry entry = metric.entry();
			if (entry.getNmId() == null) {
				continue;
			}
			MutableProductPoint point = byProduct.computeIfAbsent(
					entry.getNmId(),
					nmId -> new MutableProductPoint(nmId, entry.getProductName()));
			point.netRevenue = point.netRevenue.add(entry.getNetRevenueAmount());
			point.profit = point.profit.add(metric.profit());
		}
		return byProduct.values().stream()
				.map(MutableProductPoint::toPoint)
				.toList();
	}

	private static BigDecimal calculateTax(BigDecimal netRevenue, BigDecimal taxPercent) {
		if (netRevenue.compareTo(BigDecimal.ZERO) <= 0 || taxPercent == null || taxPercent.compareTo(BigDecimal.ZERO) == 0) {
			return BigDecimal.ZERO;
		}
		return netRevenue.multiply(taxPercent)
				.divide(HUNDRED, 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal percent(BigDecimal part, BigDecimal total) {
		if (total == null || total.compareTo(BigDecimal.ZERO) == 0) {
			return null;
		}
		return part.multiply(HUNDRED)
				.divide(total, 2, RoundingMode.HALF_UP);
	}

	private static BigDecimal sum(List<EntryMetrics> metrics, MoneyGetter getter) {
		return metrics.stream()
				.map(getter::get)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private interface MoneyGetter {

		BigDecimal get(EntryMetrics metric);
	}

	private record CostLookup(BigDecimal costAmount, boolean hasCost) {
	}

	private record EntryMetrics(
			DailyFinanceEntry entry,
			BigDecimal costAmount,
			boolean hasCost,
			BigDecimal taxAmount,
			BigDecimal profit) {
	}

	private record MutableCostSlice(String code, String label, BigDecimal amount) {
	}

	private static class MutableDailyPoint {

		private final LocalDate date;
		private BigDecimal netRevenue = BigDecimal.ZERO;
		private BigDecimal productProfit = BigDecimal.ZERO;
		private BigDecimal totalProfit = BigDecimal.ZERO;

		private MutableDailyPoint(LocalDate date) {
			this.date = date;
		}

		private FinanceAnalyticsReport.DailyPoint toPoint() {
			return new FinanceAnalyticsReport.DailyPoint(date, netRevenue, productProfit, totalProfit);
		}
	}

	private static class MutableProductPoint {

		private final Long nmId;
		private final String productName;
		private BigDecimal netRevenue = BigDecimal.ZERO;
		private BigDecimal profit = BigDecimal.ZERO;

		private MutableProductPoint(Long nmId, String productName) {
			this.nmId = nmId;
			this.productName = productName;
		}

		private FinanceAnalyticsReport.ProductPoint toPoint() {
			return new FinanceAnalyticsReport.ProductPoint(nmId, productName, netRevenue, profit, percent(profit, netRevenue));
		}
	}
}
