package ru.marketplace.finance.finance.presentation;

import jakarta.servlet.http.HttpSession;
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
import ru.marketplace.finance.finance.application.DailyFinanceXlsxExportService;
import ru.marketplace.finance.security.CurrentUserService;

@RestController
@RequestMapping("/api/reports/daily")
@Validated
public class DailyFinanceExportController {

	private final DailyFinanceCsvExportService csvExportService;
	private final DailyFinanceXlsxExportService xlsxExportService;
	private final CurrentUserService currentUserService;

	public DailyFinanceExportController(
			DailyFinanceCsvExportService csvExportService,
			DailyFinanceXlsxExportService xlsxExportService,
			CurrentUserService currentUserService) {
		this.csvExportService = csvExportService;
		this.xlsxExportService = xlsxExportService;
		this.currentUserService = currentUserService;
	}

	@GetMapping(value = "/export.csv", produces = "text/csv")
	public ResponseEntity<byte[]> exportDailyReportCsv(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
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

	@GetMapping(value = "/export.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> exportDailyReportXlsx(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo,
			HttpSession session) {
		currentUserService.requireSameUser(session, userId);
		byte[] content = xlsxExportService.exportDailyReport(userId, dateFrom, dateTo);
		String filename = "daily-finance-%s-%s.xlsx".formatted(dateFrom, dateTo);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(filename)
						.build()
						.toString())
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(content);
	}
}
