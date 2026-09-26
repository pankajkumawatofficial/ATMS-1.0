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
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

	@ExceptionHandler({InvalidCredentialsException.class, InvalidPinException.class})
	public ResponseEntity<ApiError> handleUnauthorized(RuntimeException ex) {
		return build(401, "Unauthorized", ex.getMessage());
	}

	@ExceptionHandler(ForbiddenException.class)
	public ResponseEntity<ApiError> handleForbidden(ForbiddenException ex) {
		return build(403, "Forbidden", ex.getMessage());
	}

	@ExceptionHandler({InsufficientFundsException.class, IllegalArgumentException.class})
	public ResponseEntity<ApiError> handleBadRequest(RuntimeException ex) {
		return build(400, "Bad Request", ex.getMessage());
	}

	@ExceptionHandler(DuplicateAccountNumberException.class)
	public ResponseEntity<ApiError> handleConflict(DuplicateAccountNumberException ex) {
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
