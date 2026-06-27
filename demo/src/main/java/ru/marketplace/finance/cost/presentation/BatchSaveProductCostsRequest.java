package ru.marketplace.finance.cost.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record BatchSaveProductCostsRequest(
		@NotNull @Positive Long userId,
		@NotEmpty List<@Valid SaveProductCostItemRequest> items) {
}
