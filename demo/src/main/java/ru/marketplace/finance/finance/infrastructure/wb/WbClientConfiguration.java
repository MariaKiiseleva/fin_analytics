package ru.marketplace.finance.finance.infrastructure.wb;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WbClientConfiguration {

	@Bean
	WbReportDetailClient wbReportDetailClient(
			ObjectMapper objectMapper,
			@Value("${wb.statistics.base-url:https://statistics-api.wildberries.ru}") String statisticsBaseUrl,
			@Value("${wb.http.max-retries:2}") int maxRetries,
			@Value("${wb.http.retry-delay-ms:300}") long retryDelayMs) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(Duration.ofSeconds(25))
				.build();
		return new WbReportDetailClient(
				httpClient,
				objectMapper,
				URI.create(statisticsBaseUrl),
				maxRetries,
				Duration.ofMillis(retryDelayMs));
	}

	@Bean
	WbRealizationReportDetailMapper wbRealizationReportDetailMapper(ObjectMapper objectMapper) {
		return new WbRealizationReportDetailMapper(objectMapper);
	}
}
