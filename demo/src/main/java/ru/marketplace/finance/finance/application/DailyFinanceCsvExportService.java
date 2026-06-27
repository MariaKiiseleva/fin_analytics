package ru.marketplace.finance.finance.application;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@Service
public class DailyFinanceCsvExportService {

	private static final char DELIMITER = ';';

	private final DailyFinanceEntryRepository dailyRepository;

	public DailyFinanceCsvExportService(DailyFinanceEntryRepository dailyRepository) {
		this.dailyRepository = dailyRepository;
	}

	public byte[] exportDailyReport(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		if (dateFrom == null || dateTo == null) {
			throw new IllegalArgumentException("dateFrom and dateTo must not be null");
		}
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		List<DailyFinanceEntry> entries = dailyRepository
				.findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscNmIdAsc(userId, dateFrom, dateTo)
				.stream()
				.sorted(Comparator
						.comparing(DailyFinanceEntry::getBusinessDate)
						.thenComparing(entry -> entry.getNmId() == null ? 0 : 1)
						.thenComparing(DailyFinanceEntry::getNmId, Comparator.nullsFirst(Comparator.naturalOrder())))
				.toList();

		StringBuilder csv = new StringBuilder();
		csv.append('\uFEFF');
		appendRow(csv, List.of(
				"business_date",
				"nm_id",
				"product_name",
				"sales_quantity",
				"return_quantity",
				"net_quantity",
				"sales_amount",
				"returns_amount",
				"net_revenue_amount",
				"commission_amount",
				"logistics_amount",
				"acquiring_amount",
				"storage_amount",
				"acceptance_amount",
				"penalty_amount",
				"additional_deductions_amount",
				"cost_amount",
				"tax_amount",
				"product_profit_amount",
				"has_cost"));

		for (DailyFinanceEntry entry : entries) {
			appendRow(csv, List.of(
					value(entry.getBusinessDate()),
					value(entry.getNmId()),
					value(entry.getProductName()),
					value(entry.getSalesQuantity()),
					value(entry.getReturnQuantity()),
					value(entry.getNetQuantity()),
					value(entry.getSalesAmount()),
					value(entry.getReturnsAmount()),
					value(entry.getNetRevenueAmount()),
					value(entry.getCommissionAmount()),
					value(entry.getLogisticsAmount()),
					value(entry.getAcquiringAmount()),
					value(entry.getStorageAmount()),
					value(entry.getAcceptanceAmount()),
					value(entry.getPenaltyAmount()),
					value(entry.getAdditionalDeductionsAmount()),
					value(entry.getCostAmount()),
					value(entry.getTaxAmount()),
					value(entry.getProductProfitAmount()),
					value(entry.getHasCost())));
		}

		return csv.toString().getBytes(StandardCharsets.UTF_8);
	}

	private static void appendRow(StringBuilder csv, List<String> values) {
		for (int index = 0; index < values.size(); index++) {
			if (index > 0) {
				csv.append(DELIMITER);
			}
			csv.append(escape(values.get(index)));
		}
		csv.append("\r\n");
	}

	private static String escape(String value) {
		if (value == null) {
			return "";
		}
		if (value.indexOf(DELIMITER) < 0 && !value.contains("\"") && !value.contains("\r") && !value.contains("\n")) {
			return value;
		}
		return "\"" + value.replace("\"", "\"\"") + "\"";
	}

	private static String value(Object value) {
		if (value == null) {
			return "";
		}
		if (value instanceof BigDecimal decimal) {
			return decimal.stripTrailingZeros().toPlainString();
		}
		return value.toString();
	}
}
