package ru.marketplace.finance.finance.infrastructure.wb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class WbReportDetailClientTest {

	private final ObjectMapper objectMapper = new ObjectMapper();
	private final List<CapturedRequest> capturedRequests = new ArrayList<>();
	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void fetchesReportDetailRowsWithPagination() throws Exception {
		startServer(exchange -> {
			capturedRequests.add(CapturedRequest.from(exchange));
			Map<String, String> query = parseQuery(exchange.getRequestURI().getRawQuery());
			if ("0".equals(query.get("rrdid"))) {
				writeJson(exchange, 200, """
						[
						  {"rrd_id": 10, "supplier_oper_name": "Продажа"},
						  {"rrd_id": 20, "supplier_oper_name": "Логистика"}
						]
						""");
				return;
			}
			writeJson(exchange, 200, """
					[
					  {"rrd_id": 30, "supplier_oper_name": "Возврат"}
					]
					""");
		});
		WbReportDetailClient client = client(0);

		List<JsonNode> rows = client.fetchReportDetailByPeriod(
				"token-123",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 7),
				"daily",
				2,
				5);

		assertThat(rows).hasSize(3);
		assertThat(capturedRequests).hasSize(2);
		assertThat(capturedRequests.get(0).path()).isEqualTo("/api/v5/supplier/reportDetailByPeriod");
		assertThat(capturedRequests.get(0).authorization()).isEqualTo("token-123");
		assertThat(capturedRequests.get(0).query()).containsEntry("dateFrom", "2026-06-01");
		assertThat(capturedRequests.get(0).query()).containsEntry("dateTo", "2026-06-07");
		assertThat(capturedRequests.get(0).query()).containsEntry("limit", "2");
		assertThat(capturedRequests.get(0).query()).containsEntry("rrdid", "0");
		assertThat(capturedRequests.get(0).query()).containsEntry("period", "daily");
		assertThat(capturedRequests.get(1).query()).containsEntry("rrdid", "20");
	}

	@Test
	void retriesTooManyRequests() throws Exception {
		startServer(new ExchangeHandler() {
			private int requests;

			@Override
			public void handle(HttpExchange exchange) throws IOException {
				requests++;
				capturedRequests.add(CapturedRequest.from(exchange));
				if (requests == 1) {
					writeJson(exchange, 429, "{\"error\":\"too many requests\"}");
					return;
				}
				writeJson(exchange, 200, "[{\"rrd_id\": 1}]");
			}
		});
		WbReportDetailClient client = client(1);

		List<JsonNode> rows = client.fetchReportDetailByPeriod(
				"token-123",
				LocalDate.of(2026, 6, 1),
				LocalDate.of(2026, 6, 1),
				"daily",
				100,
				1);

		assertThat(rows).hasSize(1);
		assertThat(capturedRequests).hasSize(2);
	}

	@Test
	void throwsTokenExpiredExceptionForExpiredTokenResponse() throws Exception {
		startServer(exchange -> writeJson(exchange, 401, """
				{"title":"Unauthorized","detail":"access token expired"}
				"""));
		WbReportDetailClient client = client(0);

		assertThatThrownBy(() -> client.fetchReportDetailByPeriod(
						"expired-token",
						LocalDate.of(2026, 6, 1),
						LocalDate.of(2026, 6, 1),
						"daily",
						100,
						1))
				.isInstanceOf(WbTokenExpiredException.class);
	}

	private WbReportDetailClient client(int maxRetries) {
		return new WbReportDetailClient(
				HttpClient.newHttpClient(),
				objectMapper,
				URI.create("http://localhost:" + server.getAddress().getPort()),
				maxRetries,
				Duration.ZERO);
	}

	private void startServer(ExchangeHandler handler) throws IOException {
		server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
		server.createContext("/api/v5/supplier/reportDetailByPeriod", handler::handle);
		server.start();
	}

	private static void writeJson(HttpExchange exchange, int status, String body) throws IOException {
		byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
		exchange.getResponseHeaders().add("Content-Type", "application/json");
		exchange.sendResponseHeaders(status, bytes.length);
		exchange.getResponseBody().write(bytes);
		exchange.close();
	}

	private static Map<String, String> parseQuery(String rawQuery) {
		Map<String, String> result = new LinkedHashMap<>();
		if (rawQuery == null || rawQuery.isBlank()) {
			return result;
		}
		for (String part : rawQuery.split("&")) {
			String[] pieces = part.split("=", 2);
			String key = decode(pieces[0]);
			String value = pieces.length == 2 ? decode(pieces[1]) : "";
			result.put(key, value);
		}
		return result;
	}

	private static String decode(String value) {
		return URLDecoder.decode(value, StandardCharsets.UTF_8);
	}

	private record CapturedRequest(String path, Map<String, String> query, String authorization) {

		private static CapturedRequest from(HttpExchange exchange) {
			return new CapturedRequest(
					exchange.getRequestURI().getPath(),
					parseQuery(exchange.getRequestURI().getRawQuery()),
					exchange.getRequestHeaders().getFirst("Authorization"));
		}
	}

	private interface ExchangeHandler {

		void handle(HttpExchange exchange) throws IOException;
	}
}
