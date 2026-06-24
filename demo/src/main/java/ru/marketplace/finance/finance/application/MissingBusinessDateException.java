package ru.marketplace.finance.finance.application;

public class MissingBusinessDateException extends RuntimeException {

	public MissingBusinessDateException(String message) {
		super(message);
	}
}
