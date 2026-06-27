package ru.marketplace.finance.cost.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.marketplace.finance.cost.application.ProductCostService;
import ru.marketplace.finance.cost.application.ProductCostView;

@RestController
@RequestMapping("/api/product-costs")
@Validated
public class ProductCostController {

	private final ProductCostService productCostService;

	public ProductCostController(ProductCostService productCostService) {
		this.productCostService = productCostService;
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

	@GetMapping
	public List<ProductCostView> findProductCosts(
			@RequestParam @Positive Long userId,
			@RequestParam @Positive Long nmId) {
		return productCostService.findProductCosts(userId, nmId);
	}
}
