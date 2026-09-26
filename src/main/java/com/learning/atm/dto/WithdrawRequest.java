package com.learning.atm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload for {@code POST /api/accounts/{id}/withdraw}.
 *
 * <p>The PIN is required again even though the caller already logged in — a real ATM asks
 * you to re-enter the PIN when you ask for cash.
 */
public record WithdrawRequest(
		@NotNull(message = "amount is required")
		@DecimalMin(value = "0.01", message = "amount must be at least 0.01")
		@Digits(integer = 10, fraction = 2, message = "amount can have at most 2 decimal places")
		BigDecimal amount,

		@NotBlank(message = "pin is required")
		String pin) {
}
