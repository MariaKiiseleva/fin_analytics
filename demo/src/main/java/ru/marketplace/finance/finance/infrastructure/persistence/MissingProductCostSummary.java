package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;

public interface MissingProductCostSummary {

	Long getNmId();

	String getProductName();

	LocalDate getFirstBusinessDate();

	LocalDate getLastBusinessDate();

	Long getRowsCount();

	Long getNetQuantity();
}
