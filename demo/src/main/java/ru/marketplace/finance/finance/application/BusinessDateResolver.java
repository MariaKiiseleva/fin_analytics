package ru.marketplace.finance.finance.application;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Objects;
import java.util.stream.Stream;

public class BusinessDateResolver {

	private final ZoneId reportZone;

	public BusinessDateResolver(ZoneId reportZone) {
		this.reportZone = Objects.requireNonNull(reportZone, "reportZone must not be null");
	}

	public LocalDate resolve(BusinessDateInput input) {
		Objects.requireNonNull(input, "input must not be null");

		return Stream.of(input.reportAt(), input.saleAt(), input.orderAt(), input.createdAt())
				.filter(Objects::nonNull)
				.findFirst()
				.map(this::toReportDate)
				.orElseThrow(() -> new MissingBusinessDateException("Business date cannot be resolved"));
	}

	private LocalDate toReportDate(Instant value) {
		return value.atZone(reportZone).toLocalDate();
	}
}
