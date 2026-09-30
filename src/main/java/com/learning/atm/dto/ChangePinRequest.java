package com.learning.atm.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Payload for changing the PIN of the logged-in account
 * ({@code POST /api/accounts/{id}/change-pin}).
 *
 * <p>The "must differ from the current PIN" rule is <b>not</b> enforced here: throwing from a
 * record constructor would be wrapped by Spring into a generic
 * {@code HttpMessageNotReadableException} and the real reason would be lost. It is checked in
 * the service instead, where it surfaces as a clear 400.
 *
 * @param currentPin the PIN in force right now — proves the session is really the owner
 * @param newPin     the replacement PIN
 */
public record ChangePinRequest(
		@NotBlank(message = "currentPin is required")
		String currentPin,

		@NotBlank(message = "newPin is required")
		@Pattern(regexp = "\\d{4,6}", message = "newPin must be 4-6 digits")
		String newPin) {
}
