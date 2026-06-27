package ru.marketplace.finance.cost.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProductCostCsvImportService {

	private final ProductCostService productCostService;

	public ProductCostCsvImportService(ProductCostService productCostService) {
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
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
			String headerLine = reader.readLine();
			if (headerLine == null || headerLine.isBlank()) {
				throw new IllegalArgumentException("CSV header is required");
			}
			Map<String, Integer> header = headerIndex(parseLine(removeBom(headerLine)));
			requireColumn(header, "nm_id");
			requireColumn(header, "valid_from");
			requireColumn(header, "cost_amount_to_fill");

			List<ProductCostSaveCommand> commands = new ArrayList<>();
			String line;
			int lineNumber = 1;
			while ((line = reader.readLine()) != null) {
				lineNumber++;
				if (line.isBlank()) {
					continue;
				}
				List<String> values = parseLine(line);
				String costAmount = value(values, header, "cost_amount_to_fill");
				if (costAmount == null || costAmount.isBlank()) {
					continue;
				}
				commands.add(new ProductCostSaveCommand(
						parseLong(value(values, header, "nm_id"), "nm_id", lineNumber),
						value(values, header, "product_name"),
						parseDate(value(values, header, "valid_from"), "valid_from", lineNumber),
						parseMoney(costAmount, "cost_amount_to_fill", lineNumber)));
			}
			return commands;
		}
		catch (IOException exception) {
			throw new IllegalArgumentException("CSV cannot be read", exception);
		}
	}

	private static List<String> parseLine(String line) {
		List<String> values = new ArrayList<>();
		StringBuilder current = new StringBuilder();
		boolean quoted = false;
		for (int index = 0; index < line.length(); index++) {
			char character = line.charAt(index);
			if (character == '"') {
				if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
					current.append('"');
					index++;
				}
				else {
					quoted = !quoted;
				}
				continue;
			}
			if (character == ';' && !quoted) {
				values.add(current.toString().trim());
				current.setLength(0);
				continue;
			}
			current.append(character);
		}
		values.add(current.toString().trim());
		return values;
	}

	private static Map<String, Integer> headerIndex(List<String> columns) {
		Map<String, Integer> header = new HashMap<>();
		for (int index = 0; index < columns.size(); index++) {
			header.put(columns.get(index), index);
		}
		return header;
	}

	private static void requireColumn(Map<String, Integer> header, String column) {
		if (!header.containsKey(column)) {
			throw new IllegalArgumentException("CSV column is required: " + column);
		}
	}

	private static String value(List<String> values, Map<String, Integer> header, String column) {
		Integer index = header.get(column);
		if (index == null || index >= values.size()) {
			return null;
		}
		String value = values.get(index);
		return value == null || value.isBlank() ? null : value;
	}

	private static Long parseLong(String value, String column, int lineNumber) {
		try {
			return Long.valueOf(value);
		}
		catch (NumberFormatException exception) {
			throw invalidValue(column, lineNumber, exception);
		}
	}

	private static LocalDate parseDate(String value, String column, int lineNumber) {
		try {
			return LocalDate.parse(value);
		}
		catch (RuntimeException exception) {
			throw invalidValue(column, lineNumber, exception);
		}
	}

	private static BigDecimal parseMoney(String value, String column, int lineNumber) {
		try {
			return new BigDecimal(value.replace(" ", "").replace(",", "."));
		}
		catch (NumberFormatException exception) {
			throw invalidValue(column, lineNumber, exception);
		}
	}

	private static IllegalArgumentException invalidValue(String column, int lineNumber, RuntimeException exception) {
		return new IllegalArgumentException("Invalid CSV value in column " + column + " at line " + lineNumber, exception);
	}

	private static String removeBom(String value) {
		return value.startsWith("\uFEFF") ? value.substring(1) : value;
	}
}
