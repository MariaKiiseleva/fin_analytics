package ru.marketplace.finance.common.presentation;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import ru.marketplace.finance.finance.infrastructure.wb.WbApiException;
import ru.marketplace.finance.finance.infrastructure.wb.WbReadonlyViolationException;
import ru.marketplace.finance.finance.infrastructure.wb.WbTokenExpiredException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
			IllegalArgumentException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
	}

	@ExceptionHandler(WbTokenExpiredException.class)
	public ResponseEntity<ApiErrorResponse> handleWbTokenExpired(
			WbTokenExpiredException exception,
			HttpServletRequest request) {
		return error(HttpStatus.UNAUTHORIZED, exception.getMessage(), request);
	}

	@ExceptionHandler(WbReadonlyViolationException.class)
	public ResponseEntity<ApiErrorResponse> handleWbReadonlyViolation(
			WbReadonlyViolationException exception,
			HttpServletRequest request) {
		return error(HttpStatus.FORBIDDEN, exception.getMessage(), request);
	}

	@ExceptionHandler(WbApiException.class)
	public ResponseEntity<ApiErrorResponse> handleWbApiException(
			WbApiException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_GATEWAY, exception.getMessage(), request);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiErrorResponse> handleIllegalState(
			IllegalStateException exception,
			HttpServletRequest request) {
		return error(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), request);
	}

	private ResponseEntity<ApiErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
		return ResponseEntity.status(status)
				.body(new ApiErrorResponse(
						Instant.now(),
						status.value(),
						status.getReasonPhrase(),
						message,
						request.getRequestURI()));
	}
}
