package ru.marketplace.finance.finance.presentation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.util.List;
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
import ru.marketplace.finance.finance.infrastructure.persistence.DailyFinanceEntryRepository;
import ru.marketplace.finance.finance.infrastructure.persistence.MissingProductCostSummary;

@RestController
@RequestMapping("/api/reports")
@Validated
public class MissingProductCostController {

	private final DailyFinanceEntryRepository dailyRepository;
	private final DailyFinanceCsvExportService csvExportService;

	public MissingProductCostController(
			DailyFinanceEntryRepository dailyRepository,
			DailyFinanceCsvExportService csvExportService) {
		this.dailyRepository = dailyRepository;
		this.csvExportService = csvExportService;
	}

	@GetMapping("/missing-product-costs")
	public List<MissingProductCostSummary> findMissingProductCosts(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		if (dateFrom.isAfter(dateTo)) {
			throw new IllegalArgumentException("dateFrom must not be after dateTo");
		}
		return dailyRepository.findMissingProductCostSummaries(userId, dateFrom, dateTo);
	}

	@GetMapping(value = "/missing-product-costs/export.csv", produces = "text/csv")
	public ResponseEntity<byte[]> exportMissingProductCostsCsv(
			@RequestParam @Positive Long userId,
			@RequestParam @NotNull LocalDate dateFrom,
			@RequestParam @NotNull LocalDate dateTo) {
		byte[] content = csvExportService.exportMissingProductCosts(userId, dateFrom, dateTo);
		String filename = "missing-product-costs-%s-%s.csv".formatted(dateFrom, dateTo);
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename(filename)
						.build()
						.toString())
				.contentType(new MediaType("text", "csv"))
				.body(content);
	}
}
