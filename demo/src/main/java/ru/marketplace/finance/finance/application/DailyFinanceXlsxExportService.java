package ru.marketplace.finance.finance.application;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import ru.marketplace.finance.finance.domain.DailyFinanceEntry;
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;

@Service
public class DailyFinanceXlsxExportService {

	private static final BigDecimal HUNDRED = new BigDecimal("100");

	private final DailyFinanceEntryRepository dailyRepository;

	public DailyFinanceXlsxExportService(DailyFinanceEntryRepository dailyRepository) {
		this.dailyRepository = dailyRepository;
	}

	public byte[] exportDailyReport(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		if (dateFrom == null || dateTo == null) {
			throw new IllegalArgumentException("dateFrom and dateTo must not be null");
		}
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}

		List<ReportRow> rows = buildRows(userId, dateFrom, dateTo);
		try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
			Sheet sheet = workbook.createSheet("Report");
			Styles styles = new Styles(workbook);
			writeTitle(sheet, styles, dateFrom, dateTo);
			writeHeaders(sheet, styles);
			writeRows(sheet, styles, rows);
			writeTotals(sheet, styles, rows);
			for (int column = 0; column < 14; column++) {
				sheet.autoSizeColumn(column);
			}
			sheet.setAutoFilter(org.apache.poi.ss.util.CellRangeAddress.valueOf("A4:N4"));
			sheet.createFreezePane(0, 4);
			workbook.write(output);
			return output.toByteArray();
		}
		catch (IOException exception) {
			throw new IllegalStateException("XLSX report cannot be created", exception);
		}
	}

	private List<ReportRow> buildRows(Long userId, LocalDate dateFrom, LocalDate dateTo) {
		List<DailyFinanceEntry> entries = dailyRepository
				.findByUserIdAndBusinessDateBetweenOrderByBusinessDateAscNmIdAsc(userId, dateFrom, dateTo);
		Map<Long, MutableReportRow> byProduct = new LinkedHashMap<>();
		MutableReportRow common = new MutableReportRow(null, "Общие удержания");

		for (DailyFinanceEntry entry : entries) {
			MutableReportRow row = entry.getNmId() == null
					? common
					: byProduct.computeIfAbsent(entry.getNmId(), nmId -> new MutableReportRow(nmId, entry.getProductName()));
			row.add(entry);
		}

		List<ReportRow> rows = byProduct.values().stream()
				.map(MutableReportRow::toReportRow)
				.sorted(Comparator.comparing(ReportRow::profit).reversed())
				.toList();
		rows = applyAbc(rows);

		List<ReportRow> result = new ArrayList<>(rows);
		ReportRow commonRow = common.toReportRow();
		if (commonRow.hasValues()) {
			result.add(commonRow);
		}
		return result;
	}

	private static List<ReportRow> applyAbc(List<ReportRow> rows) {
		BigDecimal positiveProfit = rows.stream()
				.map(ReportRow::profit)
				.filter(value -> value.signum() > 0)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
		BigDecimal cumulative = BigDecimal.ZERO;
		List<ReportRow> result = new ArrayList<>();
		for (ReportRow row : rows) {
			String group = "C";
			if (positiveProfit.signum() > 0 && row.profit().signum() > 0) {
				cumulative = cumulative.add(row.profit());
				BigDecimal share = cumulative.divide(positiveProfit, 6, RoundingMode.HALF_UP);
				if (share.compareTo(new BigDecimal("0.80")) <= 0) {
					group = "A";
				}
				else if (share.compareTo(new BigDecimal("0.95")) <= 0) {
					group = "B";
				}
			}
			result.add(row.withAbc(group));
		}
		return result;
	}

	private static void writeTitle(Sheet sheet, Styles styles, LocalDate dateFrom, LocalDate dateTo) {
		Row title = sheet.createRow(0);
		title.createCell(0).setCellValue("Финансовая аналитика - расширенный отчет");
		title.getCell(0).setCellStyle(styles.title);
		sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress.valueOf("A1:N1"));

		Row period = sheet.createRow(1);
		period.createCell(0).setCellValue("Период: " + dateFrom + " - " + dateTo);
		period.getCell(0).setCellStyle(styles.subtitle);
		sheet.addMergedRegion(org.apache.poi.ss.util.CellRangeAddress.valueOf("A2:N2"));
	}

	private static void writeHeaders(Sheet sheet, Styles styles) {
		Row header = sheet.createRow(3);
		List<String> headers = List.of(
				"Товар",
				"NM ID",
				"Себест. (шт)",
				"Кол-во",
				"Выручка",
				"Возвраты",
				"Логистика",
				"Прочее",
				"Комиссия",
				"Налог",
				"Прибыль",
				"Маржа %",
				"Выкуп %",
				"ABC");
		for (int index = 0; index < headers.size(); index++) {
			Cell cell = header.createCell(index);
			cell.setCellValue(headers.get(index));
			cell.setCellStyle(styles.header);
		}
	}

	private static void writeRows(Sheet sheet, Styles styles, List<ReportRow> rows) {
		int rowNumber = 4;
		for (ReportRow reportRow : rows) {
			Row row = sheet.createRow(rowNumber++);
			writeText(row, 0, reportRow.productName(), styles.text);
			writeLong(row, 1, reportRow.nmId(), styles.integer);
			writeMoney(row, 2, reportRow.costUnit(), styles.money);
			writeInteger(row, 3, reportRow.quantity(), styles.integer);
			writeMoney(row, 4, reportRow.sales(), styles.money);
			writeMoney(row, 5, reportRow.returnsAmount(), styles.money);
			writeMoney(row, 6, reportRow.logistics(), styles.money);
			writeMoney(row, 7, reportRow.other(), styles.money);
			writeMoney(row, 8, reportRow.commission(), styles.money);
			writeMoney(row, 9, reportRow.tax(), styles.money);
			writeMoney(row, 10, reportRow.profit(), styles.profit(reportRow.profit()));
			writeMoney(row, 11, reportRow.marginPercent(), styles.percent);
			writeMoney(row, 12, reportRow.buyoutPercent(), styles.percent);
			writeText(row, 13, reportRow.abc(), styles.text);
		}
	}

	private static void writeTotals(Sheet sheet, Styles styles, List<ReportRow> rows) {
		int rowNumber = rows.size() + 4;
		Row total = sheet.createRow(rowNumber);
		writeText(total, 0, "Итого", styles.total);
		writeInteger(total, 3, rows.stream().mapToInt(ReportRow::quantity).sum(), styles.totalInteger);
		writeMoney(total, 4, sum(rows, ReportRow::sales), styles.totalMoney);
		writeMoney(total, 5, sum(rows, ReportRow::returnsAmount), styles.totalMoney);
		writeMoney(total, 6, sum(rows, ReportRow::logistics), styles.totalMoney);
		writeMoney(total, 7, sum(rows, ReportRow::other), styles.totalMoney);
		writeMoney(total, 8, sum(rows, ReportRow::commission), styles.totalMoney);
		writeMoney(total, 9, sum(rows, ReportRow::tax), styles.totalMoney);
		writeMoney(total, 10, sum(rows, ReportRow::profit), styles.totalMoney);
	}

	private static BigDecimal sum(List<ReportRow> rows, MoneyGetter getter) {
		return rows.stream()
				.map(getter::get)
				.reduce(BigDecimal.ZERO, BigDecimal::add);
	}

	private static void writeText(Row row, int column, String value, CellStyle style) {
		Cell cell = row.createCell(column);
		if (value != null) {
			cell.setCellValue(value);
		}
		cell.setCellStyle(style);
	}

	private static void writeInteger(Row row, int column, int value, CellStyle style) {
		Cell cell = row.createCell(column);
		cell.setCellValue(value);
		cell.setCellStyle(style);
	}

	private static void writeLong(Row row, int column, Long value, CellStyle style) {
		Cell cell = row.createCell(column);
		if (value != null) {
			cell.setCellValue(value);
		}
		cell.setCellStyle(style);
	}

	private static void writeMoney(Row row, int column, BigDecimal value, CellStyle style) {
		Cell cell = row.createCell(column);
		if (value != null) {
			cell.setCellValue(value.doubleValue());
		}
		cell.setCellStyle(style);
	}

	private record ReportRow(
			Long nmId,
			String productName,
			BigDecimal costUnit,
			int quantity,
			BigDecimal sales,
			BigDecimal returnsAmount,
			BigDecimal logistics,
			BigDecimal other,
			BigDecimal commission,
			BigDecimal tax,
			BigDecimal profit,
			BigDecimal marginPercent,
			BigDecimal buyoutPercent,
			String abc) {

		private ReportRow withAbc(String group) {
			return new ReportRow(
					nmId,
					productName,
					costUnit,
					quantity,
					sales,
					returnsAmount,
					logistics,
					other,
					commission,
					tax,
					profit,
					marginPercent,
					buyoutPercent,
					group);
		}

		private boolean hasValues() {
			return sales.signum() != 0
					|| returnsAmount.signum() != 0
					|| logistics.signum() != 0
					|| other.signum() != 0
					|| commission.signum() != 0
					|| tax.signum() != 0
					|| profit.signum() != 0;
		}
	}

	private static class MutableReportRow {

		private final Long nmId;
		private String productName;
		private int salesQuantity;
		private int returnQuantity;
		private BigDecimal sales = BigDecimal.ZERO;
		private BigDecimal returnsAmount = BigDecimal.ZERO;
		private BigDecimal logistics = BigDecimal.ZERO;
		private BigDecimal other = BigDecimal.ZERO;
		private BigDecimal commission = BigDecimal.ZERO;
		private BigDecimal tax = BigDecimal.ZERO;
		private BigDecimal cost = BigDecimal.ZERO;
		private BigDecimal profit = BigDecimal.ZERO;

		private MutableReportRow(Long nmId, String productName) {
			this.nmId = nmId;
			this.productName = productName;
		}

		private void add(DailyFinanceEntry entry) {
			if ((productName == null || productName.isBlank()) && entry.getProductName() != null) {
				productName = entry.getProductName();
			}
			salesQuantity += entry.getSalesQuantity();
			returnQuantity += entry.getReturnQuantity();
			sales = sales.add(entry.getSalesAmount());
			returnsAmount = returnsAmount.add(entry.getReturnsAmount());
			logistics = logistics.add(entry.getLogisticsAmount());
			other = other
					.add(entry.getAcquiringAmount())
					.add(entry.getStorageAmount())
					.add(entry.getAcceptanceAmount())
					.add(entry.getPenaltyAmount())
					.add(entry.getAdditionalDeductionsAmount());
			commission = commission.add(entry.getCommissionAmount());
			tax = tax.add(entry.getTaxAmount());
			cost = cost.add(entry.getCostAmount());
			profit = profit.add(entry.getProductProfitAmount());
		}

		private ReportRow toReportRow() {
			int netQuantity = salesQuantity - returnQuantity;
			BigDecimal costUnit = netQuantity == 0
					? null
					: cost.divide(BigDecimal.valueOf(Math.abs(netQuantity)), 2, RoundingMode.HALF_UP);
			BigDecimal margin = sales.signum() == 0
					? null
					: profit.multiply(HUNDRED).divide(sales, 2, RoundingMode.HALF_UP);
			int buyoutBase = salesQuantity + returnQuantity;
			BigDecimal buyout = buyoutBase == 0
					? null
					: BigDecimal.valueOf(salesQuantity)
							.multiply(HUNDRED)
							.divide(BigDecimal.valueOf(buyoutBase), 2, RoundingMode.HALF_UP);
			return new ReportRow(
					nmId,
					productName,
					costUnit,
					netQuantity,
					sales,
					returnsAmount,
					logistics,
					other,
					commission,
					tax,
					profit,
					margin,
					buyout,
					nmId == null ? null : "C");
		}
	}

	@FunctionalInterface
	private interface MoneyGetter {
		BigDecimal get(ReportRow row);
	}

	private static class Styles {

		private final CellStyle title;
		private final CellStyle subtitle;
		private final CellStyle header;
		private final CellStyle text;
		private final CellStyle integer;
		private final CellStyle money;
		private final CellStyle percent;
		private final CellStyle positive;
		private final CellStyle negative;
		private final CellStyle total;
		private final CellStyle totalInteger;
		private final CellStyle totalMoney;

		private Styles(Workbook workbook) {
			short moneyFormat = workbook.createDataFormat().getFormat("#,##0.00");
			short integerFormat = workbook.createDataFormat().getFormat("#,##0");
			short percentFormat = workbook.createDataFormat().getFormat("0.00");

			title = workbook.createCellStyle();
			Font titleFont = workbook.createFont();
			titleFont.setBold(true);
			titleFont.setFontHeightInPoints((short) 14);
			title.setFont(titleFont);

			subtitle = workbook.createCellStyle();
			Font subtitleFont = workbook.createFont();
			subtitleFont.setFontHeightInPoints((short) 11);
			subtitle.setFont(subtitleFont);

			header = bordered(workbook);
			header.setAlignment(HorizontalAlignment.CENTER);
			header.setVerticalAlignment(VerticalAlignment.CENTER);
			header.setWrapText(true);
			header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
			Font headerFont = workbook.createFont();
			headerFont.setBold(true);
			header.setFont(headerFont);

			text = bordered(workbook);
			integer = bordered(workbook);
			integer.setDataFormat(integerFormat);
			money = bordered(workbook);
			money.setDataFormat(moneyFormat);
			percent = bordered(workbook);
			percent.setDataFormat(percentFormat);

			positive = bordered(workbook);
			positive.setDataFormat(moneyFormat);
			Font positiveFont = workbook.createFont();
			positiveFont.setColor(IndexedColors.GREEN.getIndex());
			positive.setFont(positiveFont);

			negative = bordered(workbook);
			negative.setDataFormat(moneyFormat);
			Font negativeFont = workbook.createFont();
			negativeFont.setColor(IndexedColors.RED.getIndex());
			negative.setFont(negativeFont);

			total = bordered(workbook);
			Font totalFont = workbook.createFont();
			totalFont.setBold(true);
			total.setFont(totalFont);
			total.setFillPattern(FillPatternType.SOLID_FOREGROUND);
			total.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());

			totalInteger = workbook.createCellStyle();
			totalInteger.cloneStyleFrom(total);
			totalInteger.setDataFormat(integerFormat);

			totalMoney = workbook.createCellStyle();
			totalMoney.cloneStyleFrom(total);
			totalMoney.setDataFormat(moneyFormat);
		}

		private CellStyle profit(BigDecimal value) {
			return value.signum() < 0 ? negative : positive;
		}

		private static CellStyle bordered(Workbook workbook) {
			CellStyle style = workbook.createCellStyle();
			style.setBorderBottom(BorderStyle.THIN);
			style.setBorderLeft(BorderStyle.THIN);
			style.setBorderRight(BorderStyle.THIN);
			style.setBorderTop(BorderStyle.THIN);
			return style;
		}
	}
}
