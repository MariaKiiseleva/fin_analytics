package ru.marketplace.finance.finance.infrastructure.wb;

public class WbApiException extends RuntimeException {

	public WbApiException(String message) {
		super(message);
	}

	public WbApiException(String message, Throwable cause) {
		super(message, cause);
	}
}
