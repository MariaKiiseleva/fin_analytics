package ru.marketplace.finance.finance.infrastructure.wb;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class WbReportDetailClient {

	private static final String REPORT_DETAIL_PATH = "/api/v5/supplier/reportDetailByPeriod";

	private final HttpClient httpClient;
	private final ObjectMapper objectMapper;
	private final URI statisticsBaseUri;
	private final int maxRetries;
	private final Duration retryDelay;

	public WbReportDetailClient(
			HttpClient httpClient,
			ObjectMapper objectMapper,
			URI statisticsBaseUri,
			int maxRetries,
			Duration retryDelay) {
		this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
		this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
		this.statisticsBaseUri = Objects.requireNonNull(statisticsBaseUri, "statisticsBaseUri must not be null");
		this.maxRetries = Math.max(0, maxRetries);
		this.retryDelay = Objects.requireNonNull(retryDelay, "retryDelay must not be null");
	}

	public List<JsonNode> fetchReportDetailByPeriod(
			String token,
			LocalDate dateFrom,
			LocalDate dateTo,
			String period,
			int limit,
			int maxPages) {
		requireText(token, "token");
		Objects.requireNonNull(dateFrom, "dateFrom must not be null");
		Objects.requireNonNull(dateTo, "dateTo must not be null");
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		if (limit <= 0) {
			throw new IllegalArgumentException("limit must be positive");
		}
		if (maxPages <= 0) {
			throw new IllegalArgumentException("maxPages must be positive");
		}

		List<JsonNode> result = new ArrayList<>();
		long rrdid = 0L;

		for (int page = 1; page <= maxPages; page++) {
			List<JsonNode> rows = fetchPage(token, dateFrom, dateTo, period, limit, rrdid);
			if (rows.isEmpty()) {
				return result;
			}
			result.addAll(rows);
			if (rows.size() < limit) {
				return result;
			}
			long nextRrdid = lastRrdid(rows);
			if (nextRrdid <= rrdid) {
				return result;
			}
			rrdid = nextRrdid;
		}

		throw new WbApiException("WB reportDetailByPeriod exceeded max pages: " + maxPages);
	}

	private List<JsonNode> fetchPage(
			String token,
			LocalDate dateFrom,
			LocalDate dateTo,
			String period,
			int limit,
			long rrdid) {
		Map<String, String> query = new LinkedHashMap<>();
		query.put("dateFrom", dateFrom.toString());
		query.put("dateTo", dateTo.toString());
		query.put("limit", Integer.toString(limit));
		query.put("rrdid", Long.toString(rrdid));
		query.put("period", requireText(period, "period"));
		URI uri = buildUri(query);
		WbReadOnlyGuard.assertReadOnlyMethod("GET");
		HttpRequest request = HttpRequest.newBuilder(uri)
				.timeout(Duration.ofMinutes(5))
				.header("Authorization", token)
				.header("Accept", "application/json")
				.GET()
				.build();

		HttpResponse<String> response = sendWithRetries(request);
		int status = response.statusCode();
		String body = response.body();
		if (status == 401 && isTokenExpiredBody(body)) {
			throw new WbTokenExpiredException("WB token is expired or disabled");
		}
		if (status < 200 || status >= 300) {
			throw new WbApiException("WB API returned HTTP " + status + ": " + shortBody(body));
		}
		return parseRows(body);
	}

	private HttpResponse<String> sendWithRetries(HttpRequest request) {
		int attempt = 0;
		while (true) {
			attempt++;
			try {
				HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
				if (response.statusCode() == 429 && attempt <= maxRetries + 1) {
					sleepBeforeRetry();
					continue;
				}
				return response;
			}
			catch (IOException exception) {
				if (attempt <= maxRetries + 1) {
					sleepBeforeRetry();
					continue;
				}
				throw new WbApiException("WB API request failed", exception);
			}
			catch (InterruptedException exception) {
				Thread.currentThread().interrupt();
				throw new WbApiException("WB API request interrupted", exception);
			}
		}
	}

	private List<JsonNode> parseRows(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			if (!root.isArray()) {
				throw new WbApiException("WB API returned non-array response");
			}
			List<JsonNode> rows = new ArrayList<>();
			root.forEach(rows::add);
			return rows;
		}
		catch (IOException exception) {
			throw new WbApiException("WB API returned invalid JSON", exception);
		}
	}

	private URI buildUri(Map<String, String> query) {
		StringBuilder result = new StringBuilder(statisticsBaseUri.toString().replaceAll("/+$", ""));
		result.append(REPORT_DETAIL_PATH).append("?");
		boolean first = true;
		for (Map.Entry<String, String> entry : query.entrySet()) {
			if (!first) {
				result.append("&");
			}
			first = false;
			result.append(encode(entry.getKey())).append("=").append(encode(entry.getValue()));
		}
		return URI.create(result.toString());
	}

	private void sleepBeforeRetry() {
		if (retryDelay.isZero() || retryDelay.isNegative()) {
			return;
		}
		try {
			Thread.sleep(retryDelay.toMillis());
		}
		catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new WbApiException("WB API retry interrupted", exception);
		}
	}

	private boolean isTokenExpiredBody(String body) {
		try {
			JsonNode root = objectMapper.readTree(body);
			String title = lower(root.path("title").asText(""));
			String detail = lower(root.path("detail").asText(""));
			return ("unauthorized".equals(title) && detail.contains("access token expired"))
					|| detail.contains("token expired")
					|| detail.contains("access token expired");
		}
		catch (IOException exception) {
			return false;
		}
	}

	private static long lastRrdid(List<JsonNode> rows) {
		JsonNode value = rows.get(rows.size() - 1).get("rrd_id");
		return value == null || value.isNull() ? 0L : value.asLong(0L);
	}

	private static String encode(String value) {
		return URLEncoder.encode(value, StandardCharsets.UTF_8);
	}

	private static String shortBody(String body) {
		if (body == null || body.isBlank()) {
			return "(empty)";
		}
		String trimmed = body.trim();
		return trimmed.length() <= 300 ? trimmed : trimmed.substring(0, 300) + "...";
	}

	private static String lower(String value) {
		return value == null ? "" : value.toLowerCase();
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
