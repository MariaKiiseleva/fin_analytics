package ru.marketplace.finance.cost.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.io.IOException;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import ru.marketplace.finance.cost.application.ProductCostCsvImportService;
import ru.marketplace.finance.cost.application.ProductCostExcelImportService;
import ru.marketplace.finance.cost.application.ProductCostImportResult;
import ru.marketplace.finance.cost.application.ProductCostService;
import ru.marketplace.finance.cost.application.ProductCostTemplateExportService;
import ru.marketplace.finance.cost.application.ProductCostView;

@RestController
@RequestMapping("/api/product-costs")
@Validated
public class ProductCostController {

	private final ProductCostService productCostService;
	private final ProductCostCsvImportService csvImportService;
	private final ProductCostExcelImportService excelImportService;
	private final ProductCostTemplateExportService templateExportService;

	public ProductCostController(
			ProductCostService productCostService,
			ProductCostCsvImportService csvImportService,
			ProductCostExcelImportService excelImportService,
			ProductCostTemplateExportService templateExportService) {
		this.productCostService = productCostService;
		this.csvImportService = csvImportService;
		this.excelImportService = excelImportService;
		this.templateExportService = templateExportService;
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProductCostView saveProductCost(@Valid @RequestBody SaveProductCostRequest request) {
		return productCostService.saveProductCost(
				request.userId(),
				request.nmId(),
				request.productName(),
				request.validFrom(),
				request.costAmount());
	}

	@PostMapping("/batch")
	@ResponseStatus(HttpStatus.CREATED)
	public List<ProductCostView> saveProductCosts(@Valid @RequestBody BatchSaveProductCostsRequest request) {
		return productCostService.saveProductCosts(
				request.userId(),
				request.items().stream()
						.map(SaveProductCostItemRequest::toCommand)
						.toList());
	}

	@PostMapping(value = "/import.csv", consumes = "multipart/form-data")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductCostImportResult importProductCostsCsv(
			@RequestParam @Positive Long userId,
			@RequestPart("file") MultipartFile file) throws IOException {
		return csvImportService.importProductCosts(userId, file.getInputStream());
	}

	@PostMapping(value = "/import.xlsx", consumes = "multipart/form-data")
	@ResponseStatus(HttpStatus.CREATED)
	public ProductCostImportResult importProductCostsExcel(
			@RequestParam @Positive Long userId,
			@RequestPart("file") MultipartFile file) throws IOException {
		return excelImportService.importProductCosts(userId, file.getInputStream());
	}

	@GetMapping(value = "/template.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
	public ResponseEntity<byte[]> exportProductCostsTemplate() {
		byte[] content = templateExportService.exportTemplate();
		return ResponseEntity.ok()
				.header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
						.filename("product-costs-template.xlsx")
						.build()
						.toString())
				.contentType(MediaType.parseMediaType(
						"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
				.body(content);
	}

	@GetMapping
	public List<ProductCostView> findProductCosts(
			@RequestParam @Positive Long userId,
			@RequestParam @Positive Long nmId) {
		return productCostService.findProductCosts(userId, nmId);
	}
}
