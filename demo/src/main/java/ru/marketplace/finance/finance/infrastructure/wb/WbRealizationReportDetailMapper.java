package ru.marketplace.finance.finance.infrastructure.wb;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import ru.marketplace.finance.finance.application.RawFinancialOperationImportRow;

public class WbRealizationReportDetailMapper {

	private static final ZoneId WB_ZONE = ZoneId.of("Europe/Moscow");

	private final ObjectMapper objectMapper;

	public WbRealizationReportDetailMapper(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public RawFinancialOperationImportRow map(JsonNode row) {
		return new RawFinancialOperationImportRow(
				text(row, "rrd_id"),
				text(row, "srid"),
				longValue(row, "nm_id"),
				text(row, "supplier_oper_name"),
				text(row, "doc_type_name"),
				instant(row, "order_dt"),
				instant(row, "sale_dt"),
				instant(row, "rr_dt"),
				instant(row, "create_dt"),
				integer(row, "quantity"),
				money(row, "retail_amount", "retail_price"),
				money(row, "retail_price_withdisc_rub"),
				money(row, "ppvz_for_pay"),
				money(row, "ppvz_sales_commission"),
				money(row, "delivery_rub"),
				money(row, "rebill_logistic_cost"),
				money(row, "ppvz_reward"),
				money(row, "acquiring_fee"),
				money(row, "storage_fee"),
				money(row, "acceptance"),
				money(row, "penalty"),
				money(row, "deduction"),
				rawPayload(row));
	}

	private String rawPayload(JsonNode row) {
		try {
			return objectMapper.writeValueAsString(row);
		}
		catch (JsonProcessingException exception) {
			throw new IllegalArgumentException("WB row cannot be serialized to JSON", exception);
		}
	}

	private static String text(JsonNode row, String fieldName) {
		JsonNode value = row.get(fieldName);
		if (value == null || value.isNull()) {
			return null;
		}
		String text = value.asText().trim();
		return text.isEmpty() ? null : text;
	}

	private static Long longValue(JsonNode row, String fieldName) {
		String value = text(row, fieldName);
		if (value == null) {
			return null;
		}
		try {
			return Long.valueOf(value);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private static Integer integer(JsonNode row, String fieldName) {
		String value = text(row, fieldName);
		if (value == null) {
			return null;
		}
		try {
			return Integer.valueOf(value);
		}
		catch (NumberFormatException exception) {
			return null;
		}
	}

	private static BigDecimal money(JsonNode row, String... fieldNames) {
		for (String fieldName : fieldNames) {
			String value = text(row, fieldName);
			if (value == null) {
				continue;
			}
			String normalized = value
					.replace(" ", "")
					.replace("\u00A0", "")
					.replace("руб.", "")
					.replace("₽", "")
					.replace(",", ".");
			try {
				return new BigDecimal(normalized);
			}
			catch (NumberFormatException exception) {
				return null;
			}
		}
		return null;
	}

	private static Instant instant(JsonNode row, String fieldName) {
		String value = text(row, fieldName);
		if (value == null) {
			return null;
		}
		try {
			return OffsetDateTime.parse(value).toInstant();
		}
		catch (DateTimeParseException ignored) {
		}
		try {
			return Instant.parse(value);
		}
		catch (DateTimeParseException ignored) {
		}
		try {
			LocalDateTime dateTime = LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			return dateTime.atZone(WB_ZONE).toInstant();
		}
		catch (DateTimeParseException exception) {
			return null;
		}
	}
}
