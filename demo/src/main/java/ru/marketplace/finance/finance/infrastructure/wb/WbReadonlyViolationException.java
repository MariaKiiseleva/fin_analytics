package ru.marketplace.finance.finance.infrastructure.wb;

public class WbReadonlyViolationException extends WbApiException {

	public WbReadonlyViolationException(String message) {
		super(message);
	}
}
