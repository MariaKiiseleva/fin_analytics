package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.finance.application.DailyFinanceCsvExportService;

@RestController
@RequestMapping("/api/reports/daily")
@Validated
public class DailyFinanceExportController {

	private final DailyFinanceCsvExportService csvExportService;

	public DailyFinanceExportController(DailyFinanceCsvExportService csvExportService) {
		this.csvExportService = csvExportService;
	}

	@GetMapping(value = "/export.csv", produces = "text/csv")
	public ResponseEntity<byte[]> exportDailyReportCsv(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		byte[] content = csvExportService.exportDailyReport(userId, dateFrom, dateTo);
		String filename = "daily-finance-%s-%s.csv".formatted(dateFrom, dateTo);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(filename)
						.build()
						.toString())
				.contentType(new MediaType("text", "csv"))
				.body(content);
	}
}
