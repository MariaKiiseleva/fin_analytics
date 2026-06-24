package ru.marketplace.finance.finance.infrastructure.wb;

import java.util.Locale;

public final class WbReadOnlyGuard {

	private WbReadOnlyGuard() {
	}

	public static void assertReadOnlyMethod(String method) {
		if (method == null || method.isBlank()) {
			throw new IllegalArgumentException("method must not be blank");
		}
		String normalizedMethod = method.trim().toUpperCase(Locale.ROOT);
		if (!"GET".equals(normalizedMethod)) {
			throw new WbReadonlyViolationException("WB API is read-only. Method " + normalizedMethod + " is forbidden.");
		}
	}
}
