package com.learning.atm.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Payload for {@code POST /api/transfers}. Money is always sent from the logged-in account
 * (taken from the auth token), so no "from" field is needed — the client cannot redirect it.
 */
public record TransferRequest(
		@NotBlank(message = "toAccountNumber is required")
		String toAccountNumber,

		@NotNull(message = "amount is required")
		@DecimalMin(value = "0.01", message = "amount must be at least 0.01")
		@Digits(integer = 10, fraction = 2, message = "amount can have at most 2 decimal places")
		BigDecimal amount,

		@NotBlank(message = "pin is required")
		String pin) {
}
