package ru.marketplace.finance.finance.infrastructure.wb;

public class WbTokenExpiredException extends WbApiException {

	public WbTokenExpiredException(String message) {
		super(message);
	}
}
