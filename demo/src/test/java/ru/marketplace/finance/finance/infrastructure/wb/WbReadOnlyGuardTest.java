package ru.marketplace.finance.finance.infrastructure.wb;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class WbReadOnlyGuardTest {

	@Test
	void allowsGetRequests() {
		assertThatCode(() -> WbReadOnlyGuard.assertReadOnlyMethod("GET"))
				.doesNotThrowAnyException();
		assertThatCode(() -> WbReadOnlyGuard.assertReadOnlyMethod(" get "))
				.doesNotThrowAnyException();
	}

	@Test
	void blocksWriteRequests() {
		assertThatThrownBy(() -> WbReadOnlyGuard.assertReadOnlyMethod("POST"))
				.isInstanceOf(WbReadonlyViolationException.class);
		assertThatThrownBy(() -> WbReadOnlyGuard.assertReadOnlyMethod("PUT"))
				.isInstanceOf(WbReadonlyViolationException.class);
		assertThatThrownBy(() -> WbReadOnlyGuard.assertReadOnlyMethod("DELETE"))
				.isInstanceOf(WbReadonlyViolationException.class);
		assertThatThrownBy(() -> WbReadOnlyGuard.assertReadOnlyMethod("PATCH"))
				.isInstanceOf(WbReadonlyViolationException.class);
	}
}
