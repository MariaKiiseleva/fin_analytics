package ru.marketplace.finance.finance.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class BusinessDateResolverTest {

	private final BusinessDateResolver resolver = new BusinessDateResolver(ZoneId.of("Europe/Moscow"));

	@Test
	void usesReportDateFirst() {
		BusinessDateInput input = new BusinessDateInput(
				Instant.parse("2026-06-15T21:30:00Z"),
				Instant.parse("2026-06-16T00:30:00Z"),
				null,
				null);

		assertThat(resolver.resolve(input)).isEqualTo(LocalDate.of(2026, 6, 16));
	}

	@Test
	void fallsBackToSaleDateWhenReportDateIsMissing() {
		BusinessDateInput input = new BusinessDateInput(
				null,
				Instant.parse("2026-06-15T10:00:00Z"),
				Instant.parse("2026-06-14T10:00:00Z"),
				null);

		assertThat(resolver.resolve(input)).isEqualTo(LocalDate.of(2026, 6, 15));
	}

	@Test
	void fallsBackToOrderDateWhenReportAndSaleDatesAreMissing() {
		BusinessDateInput input = new BusinessDateInput(
				null,
				null,
				Instant.parse("2026-06-14T10:00:00Z"),
				Instant.parse("2026-06-13T10:00:00Z"));

		assertThat(resolver.resolve(input)).isEqualTo(LocalDate.of(2026, 6, 14));
	}

	@Test
	void fallsBackToCreatedDateWhenOtherDatesAreMissing() {
		BusinessDateInput input = new BusinessDateInput(
				null,
				null,
				null,
				Instant.parse("2026-06-13T10:00:00Z"));

		assertThat(resolver.resolve(input)).isEqualTo(LocalDate.of(2026, 6, 13));
	}

	@Test
	void throwsExceptionWhenAllDatesAreMissing() {
		BusinessDateInput input = new BusinessDateInput(null, null, null, null);

		assertThatThrownBy(() -> resolver.resolve(input))
				.isInstanceOf(MissingBusinessDateException.class)
				.hasMessage("Business date cannot be resolved");
	}
}
