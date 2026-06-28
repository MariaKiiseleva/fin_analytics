package ru.marketplace.finance.finance.infrastructure.persistence;

import java.time.LocalDate;

public interface CoverageRowsByDate {

	LocalDate getBusinessDate();

	long getRowsCount();
}
