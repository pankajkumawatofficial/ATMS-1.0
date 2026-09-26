package com.learning.atm.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Uniform error body returned by every exception handler, so clients only ever need to
 * parse one JSON shape for failures.
 *
 * @param fields per-field validation messages (empty for non-validation errors)
 */
public record ApiError(
		LocalDateTime timestamp,
		int status,
		String error,
		String message,
		Map<String, String> fields) {

	public ApiError {
		fields = fields == null ? Map.of() : fields;
	}
}
