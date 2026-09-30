package com.learning.atm.exception;

import com.learning.atm.dto.ApiError;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.validation.FieldError;

/**
 * Turns exceptions thrown anywhere in the app into consistent {@link ApiError} JSON bodies.
 *
 * <p>Learning note: without this class every exception would surface as a default Spring Boot
 * error page (or an empty 500). Centralising it keeps controllers clean — they just throw.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(AccountNotFoundException.class)
	public ResponseEntity<ApiError> handleNotFound(AccountNotFoundException ex) {
		return build(404, "Not Found", ex.getMessage());
	}

	@ExceptionHandler(BeneficiaryNotFoundException.class)
	public ResponseEntity<ApiError> handleBeneficiaryNotFound(BeneficiaryNotFoundException ex) {
		return build(404, "Not Found", ex.getMessage());
	}

	@ExceptionHandler({InvalidCredentialsException.class, InvalidPinException.class})
	public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
		return build(401, "Unauthorized", ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
		return build(403, "Forbidden", ex.getMessage());
	}

	@ExceptionHandler(DailyLimitExceededException.class)
	public ResponseEntity<ApiError> handleDailyLimit(DailyLimitExceededException ex) {
		return build(400, "Bad Request", ex.getMessage());
	}

	@ExceptionHandler({InsufficientFundsException.class, IllegalArgumentException.class})
	public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
		return build(400, "Bad Request", ex.getMessage());
	}

	@ExceptionHandler({DuplicateAccountNumberException.class, DuplicateBeneficiaryException.class})
	public ResponseEntity<ApiError> handleConflict(RuntimeException ex) {
		return build(409, "Conflict", ex.getMessage());
	}

	/** Bean Validation failures on the request body — collects one message per bad field. */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidation(MethodArgumentNotValidException ex) {
		Map<String, String> fields = ex.getBindingResult().getFieldErrors().stream()
				.collect(Collectors.toMap(
						FieldError::getField,
						fe -> Optional.ofNullable(fe.getDefaultMessage()).orElse("is invalid"),
						(first, second) -> first));
		return ResponseEntity.badRequest().body(
				new ApiError(LocalDateTime.now(), 400, "Bad Request", "Validation failed", fields));
	}

	/** Body that is not valid JSON / does not match the expected shape. */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ApiError> handleUnreadable(HttpMessageNotReadableException ex) {
		return build(400, "Bad Request", "Malformed or unreadable request body");
	}

	/**
	 * A path with no endpoint — e.g. a browser opening {@code http://localhost:8080/}.
	 * Without this handler the generic {@link Exception} fallback below would turn it
	 * into a misleading 500 instead of an honest 404.
	 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ApiError> handleNoResource(NoResourceFoundException ex) {
		return build(404, "Not Found", "No page or endpoint at this path — see README.md for API paths");
	}

	/**
	 * Wrong HTTP method — e.g. opening a POST-only endpoint in the browser, which sends GET.
	 * Returns 405 instead of falling through to the generic 500.
	 */
	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ApiError> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
		return build(405, "Method Not Allowed",
				ex.getMethod() + " is not supported here — check README.md for the correct method");
	}

	/** Safety net: anything unexpected becomes a generic 500 without leaking stack traces. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiError> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return build(500, "Internal Server Error", "Unexpected server error");
	}

	private static ResponseEntity<ApiError> build(int status, String error, String message) {
		return ResponseEntity.status(status)
				.body(new ApiError(LocalDateTime.now(), status, error, message, Map.of()));
	}
}
