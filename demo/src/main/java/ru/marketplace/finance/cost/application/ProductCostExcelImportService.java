package ru.marketplace.finance.cost.application;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;

@Service
public class ProductCostExcelImportService {

	private final ProductCostService productCostService;

	public ProductCostExcelImportService(ProductCostService productCostService) {
		this.productCostService = productCostService;
	}

	public ProductCostImportResult importProductCosts(Long userId, InputStream inputStream) {
		List<ProductCostSaveCommand> commands = parse(inputStream);
		if (commands.isEmpty()) {
			return new ProductCostImportResult(0, 0, List.of());
		}
		List<ProductCostView> saved = productCostService.saveProductCosts(userId, commands);
		return new ProductCostImportResult(commands.size(), saved.size(), saved);
	}

	private static List<ProductCostSaveCommand> parse(InputStream inputStream) {
		try (Workbook workbook = WorkbookFactory.create(inputStream)) {
			Sheet sheet = workbook.getSheetAt(0);
			DataFormatter formatter = new DataFormatter(Locale.US);
			Row headerRow = sheet.getRow(sheet.getFirstRowNum());
			if (headerRow == null) {
				throw new IllegalArgumentException("Excel header is required");
			}

			Map<String, Integer> header = headerIndex(headerRow, formatter);
			Integer nmIdColumn = findColumn(header, "nm_id", "nmid", "nm id", "артикул wb", "артикул_wb");
			Integer costColumn = findColumn(header,
					"cost_price",
					"cost_amount",
					"cost_amount_to_fill",
					"cost",
					"cost price",
					"себестоимость");
			Integer dateColumn = findColumn(header,
					"valid_from",
					"effective_from",
					"effective from",
					"date",
					"start_date",
					"start date",
					"дата",
					"дата с",
					"дата_с",
					"дата начала",
					"дата начала действия");
			Integer nameColumn = findColumn(header,
					"product_name",
					"product name",
					"name",
					"наименование",
					"товар",
					"title");

			if (nmIdColumn == null || costColumn == null) {
				throw new IllegalArgumentException("Excel columns are required: nm_id and cost_price");
			}

			List<ProductCostSaveCommand> commands = new ArrayList<>();
			for (int rowIndex = headerRow.getRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
				Row row = sheet.getRow(rowIndex);
				if (row == null) {
					continue;
				}
				Long nmId = parseLong(cellText(row.getCell(nmIdColumn), formatter));
				BigDecimal costAmount = parseMoney(cellText(row.getCell(costColumn), formatter));
				if (nmId == null || nmId <= 0 || costAmount == null || costAmount.signum() < 0) {
					continue;
				}

				LocalDate validFrom = dateColumn == null
						? LocalDate.now()
						: parseDate(row.getCell(dateColumn), formatter);
				if (validFrom == null) {
					validFrom = LocalDate.now();
				}

				String productName = nameColumn == null ? null : emptyToNull(cellText(row.getCell(nameColumn), formatter));
				commands.add(new ProductCostSaveCommand(nmId, productName, validFrom, costAmount));
			}
			return commands;
		}
		catch (IOException exception) {
			throw new IllegalArgumentException("Excel cannot be read", exception);
		}
	}

	private static Map<String, Integer> headerIndex(Row headerRow, DataFormatter formatter) {
		Map<String, Integer> header = new HashMap<>();
		for (Cell cell : headerRow) {
			String normalized = normalizeHeader(cellText(cell, formatter));
			if (!normalized.isBlank()) {
				header.put(normalized, cell.getColumnIndex());
			}
		}
		return header;
	}

	private static Integer findColumn(Map<String, Integer> header, String... names) {
		for (String name : names) {
			Integer column = header.get(normalizeHeader(name));
			if (column != null) {
				return column;
			}
		}
		for (Map.Entry<String, Integer> entry : header.entrySet()) {
			for (String name : names) {
				String normalizedName = normalizeHeader(name);
				if (!normalizedName.isBlank() && entry.getKey().contains(normalizedName)) {
					return entry.getValue();
				}
			}
		}
		return null;
	}

	private static String normalizeHeader(String value) {
		return value == null ? "" : value
				.replace("\uFEFF", "")
				.replace('\u00A0', ' ')
				.trim()
				.toLowerCase(Locale.ROOT)
				.replaceAll("[\\-—]", " ")
				.replaceAll("[/\\\\.]", " ")
				.replaceAll("[^\\p{L}\\p{N}_ ]+", "")
				.replaceAll("\\s+", "_");
	}

	private static String cellText(Cell cell, DataFormatter formatter) {
		if (cell == null) {
			return null;
		}
		String value = formatter.formatCellValue(cell);
		return value == null ? null : value.trim();
	}

	private static Long parseLong(String value) {
		String normalized = emptyToNull(value == null ? null : value.replaceAll("\\s+", ""));
		if (normalized == null) {
			return null;
		}
		try {
			return Long.valueOf(normalized);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private static BigDecimal parseMoney(String value) {
		String normalized = emptyToNull(value);
		if (normalized == null) {
			return null;
		}
		try {
			return new BigDecimal(normalized
					.replace("\u00A0", "")
					.replace(" ", "")
					.replace(",", "."));
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private static LocalDate parseDate(Cell cell, DataFormatter formatter) {
		if (cell == null) {
			return null;
		}
		if (cell.getCellType() == CellType.NUMERIC) {
			if (DateUtil.isCellDateFormatted(cell)) {
				return cell.getDateCellValue().toInstant()
						.atZone(ZoneId.systemDefault())
						.toLocalDate();
			}
			return DateUtil.getJavaDate(cell.getNumericCellValue()).toInstant()
					.atZone(ZoneId.systemDefault())
					.toLocalDate();
		}
		String value = emptyToNull(cellText(cell, formatter));
		if (value == null) {
			return null;
		}
		String normalized = value.replace('\u00A0', ' ').trim();
		for (DateTimeFormatter formatterCandidate : List.of(
				DateTimeFormatter.ISO_LOCAL_DATE,
				DateTimeFormatter.ofPattern("dd.MM.yyyy"),
				DateTimeFormatter.ofPattern("dd/MM/yyyy"),
				DateTimeFormatter.ofPattern("dd-MM-yyyy"))) {
			try {
				return LocalDate.parse(normalized, formatterCandidate);
			}
			catch (DateTimeParseException ignored) {
			}
		}
		if (normalized.length() >= 10) {
			try {
				return LocalDate.parse(normalized.substring(0, 10), DateTimeFormatter.ISO_LOCAL_DATE);
			}
			catch (DateTimeParseException ignored) {
			}
		}
		return null;
	}

	private static String emptyToNull(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value.trim();
	}
}
